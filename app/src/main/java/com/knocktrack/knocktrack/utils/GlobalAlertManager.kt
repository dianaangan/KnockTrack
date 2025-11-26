package com.knocktrack.knocktrack.utils

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.view.HistoryActivity

/**
 * Global alert manager that shows doorbell alerts across all activities.
 * Prevents duplicate alerts for the same doorbell event.
 */
object GlobalAlertManager {
    
    private const val CHANNEL_ID = "doorbell_alerts"
    private const val CHANNEL_NAME = "Doorbell Alerts"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "doorbell_notifications"
    private const val KEY_LAST_TIMESTAMP = "last_alert_timestamp"
    private const val KEY_LAST_EVENT_ID = "last_event_id"
    private const val KEY_NOTIFIED_IDS = "notified_event_ids"
    
    private var lastAlertTimestamp: Long = 0L
    private var currentActivity: Activity? = null
    private var lastEventId: String? = null // Track unique event ID
    private val notifiedEventIds = mutableSetOf<String>() // Track all notified event IDs to prevent duplicates
    private var currentUserEmail: String? = null // Track current user to detect user changes
    
    /**
     * Initializes the alert manager by loading persisted data.
     * Should be called when user logs in or app starts.
     */
    fun initialize(context: Context) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        // If user changed, reload data
        if (currentUserEmail != userEmail) {
            android.util.Log.d("GlobalAlertManager", "User changed from $currentUserEmail to $userEmail, reloading data")
            currentUserEmail = userEmail
            loadPersistedData(context)
        } else if (notifiedEventIds.isEmpty() && lastEventId == null) {
            // First time initialization
            loadPersistedData(context)
        }
        
        android.util.Log.d("GlobalAlertManager", "Initialized with ${notifiedEventIds.size} previously notified events for user: $userEmail")
    }
    
    /**
     * Gets the SharedPreferences name for the current user.
     */
    private fun getPrefsName(context: Context): String {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        return "${PREFS_NAME}_$userEmail"
    }
    
    /**
     * Shows a global doorbell alert dialog.
     * Only shows if it's a new event (not duplicate).
     */
    fun showDoorbellAlert(activity: Activity, time: String, date: String, timestamp: Long, eventId: String? = null) {
        android.util.Log.d("GlobalAlertManager", "showDoorbellAlert called with time: $time, date: $date, timestamp: $timestamp")
        
        // Ensure data is loaded for current user
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        // If user changed or data not loaded, reload
        if (currentUserEmail != userEmail || (notifiedEventIds.isEmpty() && lastEventId == null)) {
            currentUserEmail = userEmail
            loadPersistedData(activity)
        }
        
        // Create unique event identifier
        val currentEventId = eventId ?: "${timestamp}_${time}_${date}"
        android.util.Log.d("GlobalAlertManager", "Event ID: $currentEventId, Last Event ID: $lastEventId")
        
        // Check if this event has already been notified (prevent duplicate notifications)
        if (notifiedEventIds.contains(currentEventId)) {
            android.util.Log.d("GlobalAlertManager", "Event already notified - preventing duplicate notification: $currentEventId")
            return
        }
        
        // Check if this is the same event as the last one (additional check)
        if (currentEventId == lastEventId) {
            android.util.Log.d("GlobalAlertManager", "Duplicate alert prevented for event: $currentEventId")
            return
        }
        
        // Check if this is an older event (shouldn't happen but safety check)
        if (timestamp < lastAlertTimestamp) {
            android.util.Log.d("GlobalAlertManager", "Older event ignored: $timestamp < $lastAlertTimestamp")
            return
        }
        
        // Mark this event as notified BEFORE showing notification
        notifiedEventIds.add(currentEventId)
        
        // Update tracking variables
        lastAlertTimestamp = timestamp
        lastEventId = currentEventId
        
        // Persist the data
        savePersistedData(activity)
        
        // Store current activity (for potential future use)
        currentActivity = activity
        
        // Show notification only (works in foreground and background)
        // Sound and vibration are handled by the notification
        showNotification(activity, time, date)
        
        android.util.Log.d("GlobalAlertManager", "Doorbell alert notification shown for event: $currentEventId")
    }
    
    
    /**
     * Gets the current activity showing the alert.
     */
    fun getCurrentActivity(): Activity? {
        return currentActivity
    }
    
    /**
     * Test method to force show an alert (bypasses duplicate detection).
     */
    fun forceShowTestAlert(activity: Activity) {
        android.util.Log.d("GlobalAlertManager", "Force showing test alert")
        
        // Reset tracking variables to bypass duplicate detection
        lastAlertTimestamp = 0L
        lastEventId = null
        notifiedEventIds.clear()
        
        // Show test alert
        val timestamp = System.currentTimeMillis()
        val eventId = "test_${timestamp}_TestTime_TestDate"
        showDoorbellAlert(activity, "Test Time", "Test Date", timestamp, eventId)
    }
    
    /**
     * Reset duplicate detection (useful for debugging).
     */
    fun resetDuplicateDetection(context: Context? = null) {
        android.util.Log.d("GlobalAlertManager", "Resetting duplicate detection")
        lastAlertTimestamp = 0L
        lastEventId = null
        notifiedEventIds.clear()
        
        // Clear persisted data
        context?.let {
            val prefsName = getPrefsName(it)
            val prefs = it.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }
    
    /**
     * Checks if an event has already been notified.
     */
    fun hasEventBeenNotified(eventId: String): Boolean {
        return notifiedEventIds.contains(eventId)
    }
    
    /**
     * Loads persisted notification data from SharedPreferences.
     */
    private fun loadPersistedData(context: Context) {
        try {
            val prefsName = getPrefsName(context)
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            lastAlertTimestamp = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)
            lastEventId = prefs.getString(KEY_LAST_EVENT_ID, null)
            
            // Load notified event IDs set
            val notifiedIdsString = prefs.getStringSet(KEY_NOTIFIED_IDS, null)
            notifiedEventIds.clear()
            notifiedIdsString?.let {
                notifiedEventIds.addAll(it)
            }
            
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val userEmail = currentUser?.email ?: "default"
            android.util.Log.d("GlobalAlertManager", "Loaded persisted data for $userEmail: lastTimestamp=$lastAlertTimestamp, lastEventId=$lastEventId, notifiedCount=${notifiedEventIds.size}")
        } catch (e: Exception) {
            android.util.Log.e("GlobalAlertManager", "Error loading persisted data: ${e.message}")
        }
    }
    
    /**
     * Saves notification data to SharedPreferences.
     */
    private fun savePersistedData(context: Context) {
        try {
            val prefsName = getPrefsName(context)
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putLong(KEY_LAST_TIMESTAMP, lastAlertTimestamp)
            editor.putString(KEY_LAST_EVENT_ID, lastEventId)
            editor.putStringSet(KEY_NOTIFIED_IDS, notifiedEventIds.toSet())
            editor.apply()
            
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val userEmail = currentUser?.email ?: "default"
            android.util.Log.d("GlobalAlertManager", "Saved persisted data for $userEmail: lastTimestamp=$lastAlertTimestamp, lastEventId=$lastEventId, notifiedCount=${notifiedEventIds.size}")
        } catch (e: Exception) {
            android.util.Log.e("GlobalAlertManager", "Error saving persisted data: ${e.message}")
        }
    }
    
    /**
     * Creates a notification channel for Android 8.0+.
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for doorbell alerts"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Shows a notification for the doorbell alert.
     */
    private fun showNotification(context: Context, time: String, date: String) {
        try {
            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.areNotificationsEnabled()) {
                    android.util.Log.w("GlobalAlertManager", "Notifications are disabled by user")
                    // If it's an Activity, we could request permission, but for now just log
                    if (context is Activity) {
                        android.util.Log.d("GlobalAlertManager", "Notification permission not granted - user needs to enable in settings")
                    }
                    return
                }
            }
            
            // Create notification channel for Android 8.0+
            createNotificationChannel(context)
            
            // Create intent to open HistoryActivity when notification is tapped
            val intent = Intent(context, HistoryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                pendingIntentFlags
            )
            
            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert_bell)
                .setContentTitle("🚨 DOORBELL ALERT!")
                .setContentText("Someone is at your door!")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Someone is at your door!\n\nTime: $time\nDate: $date"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()
            
            // Show notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            android.util.Log.d("GlobalAlertManager", "Notification shown successfully")
        } catch (e: Exception) {
            android.util.Log.e("GlobalAlertManager", "Error showing notification: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Shows only a notification (without dialog) when activity is not available.
     */
    fun showNotificationOnly(time: String, date: String, context: Context? = null) {
        try {
            // Use provided context or try to get from current activity
            val appContext = context ?: currentActivity?.applicationContext
            if (appContext == null) {
                android.util.Log.e("GlobalAlertManager", "Cannot show notification: no context available")
                return
            }
            
            android.util.Log.d("GlobalAlertManager", "Showing notification only (no activity available)")
            // Sound and vibration are handled by the notification
            showNotification(appContext, time, date)
        } catch (e: Exception) {
            android.util.Log.e("GlobalAlertManager", "Error showing notification only: ${e.message}")
            e.printStackTrace()
        }
    }
}
