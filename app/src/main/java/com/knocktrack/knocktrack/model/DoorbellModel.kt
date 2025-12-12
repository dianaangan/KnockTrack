package com.knocktrack.knocktrack.model

import com.google.firebase.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Model for handling doorbell-related data operations.
 * Manages Firebase Realtime Database interactions for doorbell events.
 */
class DoorbellModel(private val deviceId: String = "DOORBELL_001") {
    
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private var deviceStatusListener: ValueEventListener? = null
    
    // Track when we last received a real-time update from Firebase
    // If we receive an update, the device is definitely online
    private var lastListenerUpdateTime: Long = 0L
    private var lastSeenFromListener: Long = 0L
    
    /**
     * Listens for doorbell press events from Firebase in real-time.
     * Returns a Flow of doorbell events.
     */
    fun listenForDoorbellEvents(): Flow<DoorbellEvent> = flow {
        val eventsRef = database.child("devices").child(deviceId).child("events")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the latest event (most recent timestamp)
                val latestEvent = snapshot.children.maxByOrNull { 
                    it.key?.toLongOrNull() ?: 0L 
                }
                
                latestEvent?.let { eventSnapshot ->
                    val event = eventSnapshot.getValue(DoorbellEvent::class.java)
                    event?.let { 
                        // This would emit the event in a real Flow implementation
                        // For now, the real-time listening is handled in HomePresenter
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("DoorbellModel", "Firebase listener cancelled: ${error.message}")
            }
        }
        
        eventsRef.addValueEventListener(listener)
    }
    
    /**
     * Gets the latest doorbell event.
     */
    suspend fun getLatestDoorbellEvent(): DoorbellEvent? {
        return try {
            val snapshot = database.child("devices").child(deviceId).child("events")
                .orderByKey()
                .limitToLast(1)
                .get()
                .await()
            
            snapshot.children.firstOrNull()?.getValue(DoorbellEvent::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets recent doorbell activity (last 10 events).
     */
    suspend fun getRecentActivity(): List<DoorbellEvent> {
        return try {
            val snapshot = database.child("devices").child(deviceId).child("events")
                .orderByKey()
                .limitToLast(10)
                .get()
                .await()
            
            snapshot.children.mapNotNull { 
                it.getValue(DoorbellEvent::class.java) 
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Gets current system status.
     */
    suspend fun getSystemStatus(): String {
        return try {
            val snapshot = database.child("doorbell").child("current_status").get().await()
            snapshot.getValue(String::class.java) ?: "System Ready"
        } catch (e: Exception) {
            "System Ready"
        }
    }
    
    /**
     * Checks if device is active. ESP32 heartbeat = 5s, threshold = 8s.
     * Trusts listener if it recently detected a new heartbeat.
     */
    suspend fun isDeviceActive(): Boolean {
        return try {
            // If listener recently detected a heartbeat change, device is active
            val listenerAge = System.currentTimeMillis() - lastListenerUpdateTime
            if (lastListenerUpdateTime > 0 && listenerAge < 8000) { // 5s heartbeat + 3s buffer
                android.util.Log.d("DoorbellModel", "✅ Active (listener detected heartbeat ${listenerAge/1000}s ago)")
                return true
            }
            
            val snapshot = database.child("devices").child(deviceId).child("info").get().await()
            if (!snapshot.exists()) return false
            
            val status = snapshot.child("status").getValue(String::class.java)
            if (status != "online") return false
            
            val lastSeen = snapshot.child("last_seen").getValue(Long::class.java) ?: return false
            val diff = (System.currentTimeMillis() / 1000) - lastSeen
            
            // Check if timestamp changed since last check (indicates new heartbeat)
            val timestampChanged = lastSeen != lastSeenFromListener && lastSeenFromListener != 0L
            if (timestampChanged) {
                lastListenerUpdateTime = System.currentTimeMillis()
                lastSeenFromListener = lastSeen
                android.util.Log.d("DoorbellModel", "✅ Active (timestamp changed - new heartbeat)")
                return true
            }
            
            // Fallback to timestamp check: 5s heartbeat + 3s buffer = 8s threshold
            val isActive = diff in -120..8
            android.util.Log.d("DoorbellModel", "⏱️ diff: ${diff}s → ${if (isActive) "✅ Active" else "❌ Offline"}")
            isActive
        } catch (e: Exception) {
            android.util.Log.e("DoorbellModel", "Error: ${e.message}")
            false
        }
    }
    
    /**
     * Gets device info from Firebase.
     * @return Map containing device info or null if not found
     */
    suspend fun getDeviceInfo(): Map<String, Any>? {
        return try {
            val infoPath = database.child("devices").child(deviceId).child("info")
            val snapshot = infoPath.get().await()
            
            if (!snapshot.exists()) {
                return null
            }
            
            val info = mutableMapOf<String, Any>()
            snapshot.children.forEach { child ->
                val value = when {
                    child.value is Long -> child.value as Long
                    child.value is String -> child.value as String
                    child.value is Boolean -> child.value as Boolean
                    else -> child.value.toString()
                }
                child.key?.let { info[it] = value }
            }
            
            info
        } catch (e: Exception) {
            android.util.Log.e("DoorbellModel", "Error getting device info: ${e.message}")
            null
        }
    }
    
    /**
     * Real-time listener for device status. Checks timestamp to determine if device is active.
     */
    fun listenToDeviceStatus(callback: (Boolean) -> Unit) {
        deviceStatusListener?.let { database.child("devices").child(deviceId).child("info").removeEventListener(it) }
        
        val infoPath = database.child("devices").child(deviceId).child("info")
        deviceStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.child("status").getValue(String::class.java) != "online") {
                    callback(false)
                    return
                }
                
                val lastSeen = snapshot.child("last_seen").getValue(Long::class.java)
                if (lastSeen == null || lastSeen == 0L) {
                    callback(false)
                    return
                }
                
                // Check if this is a NEW update (timestamp changed) or just initial attach
                val isNewUpdate = lastSeen != lastSeenFromListener
                val currentTime = System.currentTimeMillis() / 1000
                val diff = currentTime - lastSeen
                
                android.util.Log.d("DoorbellModel", "📡 Listener: lastSeen=$lastSeen, prev=$lastSeenFromListener, diff=${diff}s, isNew=$isNewUpdate")
                
                // KEY: If timestamp CHANGED, ESP32 just wrote to Firebase = ONLINE
                // Don't check diff value - ESP32 clock might be wrong but device is definitely online
                val isActive = if (isNewUpdate && lastSeenFromListener != 0L) {
                    // Timestamp changed from previous value = ESP32 just sent heartbeat = ONLINE
                    lastListenerUpdateTime = System.currentTimeMillis()
                    lastSeenFromListener = lastSeen
                    android.util.Log.d("DoorbellModel", "📡 NEW heartbeat detected - device ACTIVE")
                    true
                } else if (lastSeenFromListener == 0L) {
                    // First time seeing data - check if timestamp is reasonable
                    lastSeenFromListener = lastSeen
                    if (diff in -120..8) { // 5s heartbeat + 3s buffer = 8s threshold
                        lastListenerUpdateTime = System.currentTimeMillis()
                        android.util.Log.d("DoorbellModel", "📡 Initial attach - timestamp recent - ACTIVE")
                        true
                    } else {
                        android.util.Log.d("DoorbellModel", "📡 Initial attach - timestamp old (${diff}s) - OFFLINE")
                        false
                    }
                } else {
                    // Same timestamp as before - check if still recent based on listener time
                    val listenerAge = System.currentTimeMillis() - lastListenerUpdateTime
                    if (lastListenerUpdateTime > 0 && listenerAge < 8000) { // 5s heartbeat + 3s buffer
                        android.util.Log.d("DoorbellModel", "📡 Same timestamp but listener recent (${listenerAge/1000}s) - ACTIVE")
                        true
                    } else {
                        android.util.Log.d("DoorbellModel", "📡 Same timestamp, listener stale - OFFLINE")
                        false
                    }
                }
                
                callback(isActive)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        }
        
        infoPath.addValueEventListener(deviceStatusListener!!)
        android.util.Log.d("DoorbellModel", "📡 Real-time listener attached")
    }
    
    /**
     * Removes the device status listener.
     */
    fun removeDeviceStatusListener() {
        deviceStatusListener?.let { listener ->
            val infoPath = database.child("devices").child(deviceId).child("info")
            infoPath.removeEventListener(listener)
            deviceStatusListener = null
            android.util.Log.d("DoorbellModel", "Device status listener removed")
        }
    }
}

/**
 * Data class representing a doorbell event.
 */
data class DoorbellEvent(
    val timestamp: Long = 0L,
    val time: String = "",
    val date: String = "",
    val device_id: String = "",
    val status: String = "",
    val location: String = ""
)

/**
 * Data class representing doorbell analytics/statistics.
 */
data class DoorbellAnalytics(
    val totalNotifications: Int = 0,
    val todayNotifications: Int = 0,
    val weekNotifications: Int = 0,
    val lastNotification: String = "Never"
)
