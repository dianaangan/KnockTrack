package com.knocktrack.knocktrack.utils

import android.app.Activity
import com.knocktrack.knocktrack.model.DoorbellModel
import com.knocktrack.knocktrack.model.DoorbellEvent
import kotlinx.coroutines.*
import android.util.Log
import java.lang.ref.WeakReference

/**
 * Global Firebase listener that works across all activities.
 * Listens for doorbell events and shows alerts regardless of current screen.
 */
object GlobalFirebaseListener {
    
    private var isListening = false
    private var scope: CoroutineScope? = null
    private var doorbellModel: DoorbellModel? = null
    private var lastEventTimestamp: Long = 0L
    private var currentActivityRef: WeakReference<Activity>? = null
    private var firebaseListener: com.google.firebase.database.ValueEventListener? = null
    private var eventsRef: com.google.firebase.database.DatabaseReference? = null
    
    // Enhanced tracking for better reliability
    private var lastActivityName: String? = null
    private var listenerStartTime: Long = 0L
    
    /**
     * Starts global Firebase listening for doorbell events.
     */
    fun startListening(activity: Activity) {
        val activityName = activity.javaClass.simpleName
        
        // Always update the current activity reference
        currentActivityRef = WeakReference(activity)
        lastActivityName = activityName
        Log.d("GlobalFirebaseListener", "Updated current activity to $activityName")
        
        if (isListening) {
            Log.d("GlobalFirebaseListener", "Already listening, current activity updated to $activityName")
            return
        }
        
        Log.d("GlobalFirebaseListener", "Starting NEW global Firebase listener for $activityName")
        
        isListening = true
        listenerStartTime = System.currentTimeMillis()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        // Initialize DoorbellModel with device ID from settings
        val deviceId = getDeviceIdFromSettings(activity)
        doorbellModel = DoorbellModel(deviceId)
        Log.d("GlobalFirebaseListener", "Initialized DoorbellModel with device ID: $deviceId")
        
        // Check if device is properly connected
        if (!isDeviceConnected(activity)) {
            Log.w("GlobalFirebaseListener", "Device not connected - stopping listener")
            isListening = false
            scope?.cancel()
            scope = null
            return
        }
        
        scope?.launch {
            try {
                Log.d("GlobalFirebaseListener", "Starting global Firebase listening with real-time listener")
                
                val deviceId = getDeviceIdFromSettings(getCurrentActivity() ?: activity)
                eventsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .reference
                    .child("devices")
                    .child(deviceId)
                    .child("events")
                
                Log.d("GlobalFirebaseListener", "Listening to Firebase path: devices/$deviceId/events")
                
                // Use real-time listener instead of polling
                firebaseListener = object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        if (!isListening) return
                        
                        Log.d("GlobalFirebaseListener", "Firebase data changed, checking for new events")
                        
                        // Get all events and find the latest one
                        var latestEvent: DoorbellEvent? = null
                        var latestTimestamp: Long = 0L
                        
                        snapshot.children.forEach { eventSnapshot ->
                            val event = eventSnapshot.getValue(DoorbellEvent::class.java)
                            if (event != null && event.timestamp > latestTimestamp) {
                                latestTimestamp = event.timestamp
                                latestEvent = event
                            }
                        }
                        
                        // Store in immutable variable to allow smart cast
                        val currentLatestEvent = latestEvent
                        if (currentLatestEvent != null) {
                            Log.d("GlobalFirebaseListener", "Found doorbell event: ${currentLatestEvent.time}, timestamp: ${currentLatestEvent.timestamp}, last seen: $lastEventTimestamp")
                            
                            // Check if this is a NEW event (timestamp is newer than last seen)
                            if (currentLatestEvent.timestamp > lastEventTimestamp) {
                                lastEventTimestamp = currentLatestEvent.timestamp
                                
                                Log.d("GlobalFirebaseListener", "NEW DOORBELL EVENT DETECTED: ${currentLatestEvent.time} (timestamp: ${currentLatestEvent.timestamp})")
                                
                                // Show global alert dialog - ALWAYS show alert regardless of current activity
                                val eventId = "${currentLatestEvent.timestamp}_${currentLatestEvent.time}_${currentLatestEvent.date}"
                                
                                // Enhanced activity detection with fallback
                                val currentActivityLocal = getCurrentActivity()
                                
                                // Launch coroutine to show alert on main thread
                                scope?.launch(Dispatchers.Main) {
                                    if (currentActivityLocal != null) {
                                        Log.d("GlobalFirebaseListener", "Showing alert on current activity: ${currentActivityLocal.javaClass.simpleName}")
                                        GlobalAlertManager.showDoorbellAlert(
                                            currentActivityLocal, 
                                            currentLatestEvent.time, 
                                            currentLatestEvent.date, 
                                            currentLatestEvent.timestamp, 
                                            eventId
                                        )
                                    } else {
                                        Log.w("GlobalFirebaseListener", "No current activity available, but doorbell event detected!")
                                        Log.d("GlobalFirebaseListener", "Listener status: ${debugStatus()}")
                                        // Try to get activity from BaseActivity or show notification only
                                        val fallbackActivity = currentActivityRef?.get()
                                        if (fallbackActivity != null) {
                                            GlobalAlertManager.showDoorbellAlert(
                                                fallbackActivity, 
                                                currentLatestEvent.time, 
                                                currentLatestEvent.date, 
                                                currentLatestEvent.timestamp, 
                                                eventId
                                            )
                                        } else {
                                            // At least show notification - use activity context if available
                                            val contextForNotification = activity.applicationContext
                                            GlobalAlertManager.showNotificationOnly(
                                                currentLatestEvent.time, 
                                                currentLatestEvent.date,
                                                contextForNotification
                                            )
                                        }
                                    }
                                }
                            } else {
                                Log.d("GlobalFirebaseListener", "Event is not new (timestamp: ${currentLatestEvent.timestamp} <= last seen: $lastEventTimestamp)")
                            }
                        } else {
                            Log.d("GlobalFirebaseListener", "No doorbell events found in snapshot")
                        }
                    }
                    
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        Log.e("GlobalFirebaseListener", "Firebase listener cancelled: ${error.message}")
                        // Try to restart listener after a delay
                        if (isListening) {
                            scope?.launch {
                                delay(5000)
                                if (isListening) {
                                    Log.d("GlobalFirebaseListener", "Attempting to restart listener after cancellation")
                                    val currentActivity = getCurrentActivity() ?: activity
                                    if (currentActivity != null) {
                                        startListening(currentActivity)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Add the real-time listener
                eventsRef?.addValueEventListener(firebaseListener!!)
                Log.d("GlobalFirebaseListener", "Real-time Firebase listener attached successfully")
                
                // Keep the coroutine alive while listening
                while (isListening) {
                    delay(10000) // Just keep alive, the listener handles events
                }
            } catch (e: Exception) {
                Log.e("GlobalFirebaseListener", "Error in global Firebase listening: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Stops global Firebase listening.
     * This should only be called when the app is being closed.
     */
    private fun stopListening() {
        isListening = false
        
        // Remove Firebase listener
        firebaseListener?.let { listener ->
            eventsRef?.removeEventListener(listener)
            Log.d("GlobalFirebaseListener", "Removed Firebase listener")
        }
        firebaseListener = null
        eventsRef = null
        
        scope?.cancel()
        scope = null
        currentActivityRef = null
        Log.d("GlobalFirebaseListener", "Stopped global Firebase listening")
    }
    
    /**
     * Updates the current activity (when user navigates between screens).
     */
    fun updateCurrentActivity(activity: Activity?) {
        currentActivityRef = if (activity != null) WeakReference(activity) else null
        if (activity != null) {
            lastActivityName = activity.javaClass.simpleName
            Log.d("GlobalFirebaseListener", "Updated current activity: ${activity.javaClass.simpleName}")
        } else {
            Log.d("GlobalFirebaseListener", "Current activity set to null (activity destroyed)")
        }
    }
    
    /**
     * Gets the current activity with enhanced reliability.
     */
    private fun getCurrentActivity(): Activity? {
        val activity = currentActivityRef?.get()
        if (activity == null && lastActivityName != null) {
            Log.w("GlobalFirebaseListener", "Activity reference lost for $lastActivityName, but listener still active")
        }
        return activity
    }
    
    /**
     * Public method to get current activity (for debugging).
     */
    fun getCurrentActivityPublic(): Activity? {
        return getCurrentActivity()
    }
    
    /**
     * Checks if currently listening.
     */
    fun isListening(): Boolean {
        return isListening
    }
    
    /**
     * Manually triggers a test alert (for debugging).
     */
    fun triggerTestAlert(activity: Activity) {
        Log.d("GlobalFirebaseListener", "Manually triggering test alert on ${activity.javaClass.simpleName}")
        val timestamp = System.currentTimeMillis()
        val eventId = "test_${timestamp}_TestTime_TestDate"
        GlobalAlertManager.showDoorbellAlert(activity, "Test Time", "Test Date", timestamp, eventId)
    }
    
    /**
     * Debug method to check current status.
     */
    fun debugStatus(): String {
        val currentActivity = currentActivityRef?.get()
        val uptime = if (listenerStartTime > 0) System.currentTimeMillis() - listenerStartTime else 0
        return "Listening: $isListening, Current Activity: ${currentActivity?.javaClass?.simpleName ?: "null"}, Last Activity: $lastActivityName, Uptime: ${uptime}ms"
    }
    
    /**
     * Force refresh the listener (useful for recovery).
     */
    fun forceRefresh(activity: Activity) {
        Log.d("GlobalFirebaseListener", "Force refreshing listener for ${activity.javaClass.simpleName}")
        
        // Force update the activity reference
        currentActivityRef = WeakReference(activity)
        lastActivityName = activity.javaClass.simpleName
        
        // Check if device is connected before starting listener
        if (!isDeviceConnected(activity)) {
            Log.w("GlobalFirebaseListener", "Device not connected - cannot start listener")
            isListening = false
            scope?.cancel()
            scope = null
            return
        }
        
        // If listener is not active, restart it
        if (!isListening) {
            Log.d("GlobalFirebaseListener", "Listener not active, restarting...")
            startListening(activity)
        }
        
        Log.d("GlobalFirebaseListener", "Listener refreshed, status: ${debugStatus()}")
    }
    
    /**
     * Gets device ID from settings.
     */
    private fun getDeviceIdFromSettings(activity: Activity): String {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = activity.getSharedPreferences("device_config_$userEmail", Activity.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "DOORBELL_001") ?: "DOORBELL_001"
        Log.d("GlobalFirebaseListener", "Retrieved device ID from settings for account $userEmail: $deviceId")
        return deviceId
    }
    
    /**
     * Checks if the device is connected (has valid Device ID and Auth Key).
     */
    private fun isDeviceConnected(activity: Activity): Boolean {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = activity.getSharedPreferences("device_config_$userEmail", Activity.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "") ?: ""
        val authKey = prefs.getString("auth_key", "") ?: ""
        val isConnected = prefs.getBoolean("device_connected", false)
        
        return isConnected && deviceId.isNotEmpty() && authKey.isNotEmpty()
    }
}