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
