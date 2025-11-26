package com.knocktrack.knocktrack.model

import com.google.firebase.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Model for handling history-related data operations.
 * Manages Firebase Realtime Database interactions for doorbell history.
 */
class HistoryModel {
    
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    
    /**
     * Gets the device ID from settings for the current user.
     */
    private fun getDeviceId(context: Context): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = context.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
        return prefs.getString("device_id", "DOORBELL_001") ?: "DOORBELL_001"
    }
    
    /**
     * Gets all doorbell events from Firebase.
     * Returns a list of doorbell events sorted by timestamp (newest first).
     */
    suspend fun getAllDoorbellEvents(context: Context): List<DoorbellEvent> {
        return try {
            val deviceId = getDeviceId(context)
            val snapshot = database.child("devices").child(deviceId).child("events")
                .orderByKey()
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
     * Gets doorbell events with pagination.
     * @param limit Maximum number of events to return
     * @param startAfterTimestamp Start after this timestamp (for pagination)
     */
    suspend fun getDoorbellEvents(context: Context, limit: Int = 50, startAfterTimestamp: Long? = null): List<DoorbellEvent> {
        return try {
            val deviceId = getDeviceId(context)
            var query = database.child("devices").child(deviceId).child("events")
                .orderByKey()
                .limitToLast(limit)
            
            if (startAfterTimestamp != null) {
                query = query.endBefore(startAfterTimestamp.toString())
            }
            
            val snapshot = query.get().await()
            
            snapshot.children.mapNotNull { 
                it.getValue(DoorbellEvent::class.java) 
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clears all doorbell history from Firebase.
     */
    suspend fun clearHistory(context: Context): Boolean {
        return try {
            val deviceId = getDeviceId(context)
            database.child("devices").child(deviceId).child("events").removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Deletes a single doorbell event by timestamp from Firebase.
     * Finds the event by matching timestamp and deletes it using its Firebase key.
     */
    suspend fun deleteEvent(context: Context, timestamp: Long): Boolean {
        return try {
            val deviceId = getDeviceId(context)
            val eventsRef = database.child("devices").child(deviceId).child("events")
            
            // Get all events to find the one with matching timestamp
            val snapshot = eventsRef.get().await()
            
            // Find the event with matching timestamp
            var eventKey: String? = null
            snapshot.children.forEach { child ->
                val event = child.getValue(DoorbellEvent::class.java)
                if (event != null && event.timestamp == timestamp) {
                    eventKey = child.key
                    return@forEach
                }
            }
            
            // Delete the event using its Firebase key
            if (eventKey != null) {
                eventsRef.child(eventKey!!).removeValue().await()
                android.util.Log.d("HistoryModel", "Deleted event with key: $eventKey, timestamp: $timestamp")
                true
            } else {
                android.util.Log.w("HistoryModel", "Event with timestamp $timestamp not found")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("HistoryModel", "Error deleting event: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Gets doorbell events count.
     */
    suspend fun getEventsCount(context: Context): Int {
        return try {
            val deviceId = getDeviceId(context)
            val snapshot = database.child("devices").child(deviceId).child("events").get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Gets doorbell analytics/statistics.
     */
    suspend fun getDoorbellAnalytics(context: Context): DoorbellAnalytics {
        return try {
            val events = getAllDoorbellEvents(context)
            val now = System.currentTimeMillis()
            val todayStart = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val weekStart = todayStart - (7 * 24 * 60 * 60 * 1000L)
            
            val totalCount = events.size
            val todayCount = events.count { it.timestamp >= todayStart }
            val weekCount = events.count { it.timestamp >= weekStart }
            val lastNotification = events.firstOrNull()?.let { 
                "${it.time} - ${it.date}"
            } ?: "Never"
            
            DoorbellAnalytics(
                totalNotifications = totalCount,
                todayNotifications = todayCount,
                weekNotifications = weekCount,
                lastNotification = lastNotification
            )
        } catch (e: Exception) {
            android.util.Log.e("HistoryModel", "Error getting analytics: ${e.message}")
            DoorbellAnalytics(0, 0, 0, "Never")
        }
    }
    
    /**
     * Listens for new doorbell events in real-time.
     */
    fun listenForNewEvents(context: Context): Flow<DoorbellEvent> = flow {
        val deviceId = getDeviceId(context)
        val eventsRef = database.child("devices").child(deviceId).child("events")
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val event = snapshot.getValue(DoorbellEvent::class.java)
                event?.let { 
                    // Emit the new event
                    // Note: This is a simplified implementation
                    // In a real app, you'd use a proper Flow implementation
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        
        eventsRef.addChildEventListener(listener)
    }
}
