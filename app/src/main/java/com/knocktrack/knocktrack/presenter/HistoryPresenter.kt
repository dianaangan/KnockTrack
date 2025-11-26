package com.knocktrack.knocktrack.presenter

import com.knocktrack.knocktrack.model.HistoryModel
import com.knocktrack.knocktrack.model.DoorbellEvent
import com.knocktrack.knocktrack.view.HistoryView
import kotlinx.coroutines.*
import android.content.Context

/**
 * Presenter for the History screen.
 * Coordinates between View and Model:
 * - Loads doorbell events from Firebase
 * - Handles history clearing
 * - Manages UI state updates
 */
class HistoryPresenter {
    private var view: HistoryView? = null
    private val model = HistoryModel()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var context: Context? = null
    private var firebaseListener: com.google.firebase.database.ValueEventListener? = null
    private var eventsRef: com.google.firebase.database.DatabaseReference? = null

    /** The View calls this when it's ready to receive updates. */
    fun attachView(view: HistoryView, context: Context) {
        this.view = view
        this.context = context
    }

    /** The View calls this on destroy to avoid leaks. */
    fun detachView() {
        this.view = null
    }

    /**
     * Loads all doorbell events from Firebase.
     * Only loads data if device is connected.
     */
    fun loadDoorbellEvents() {
        scope.launch {
            try {
                view?.showLoading(true)
                
                // Check if device is connected first
                if (!isDeviceConnected()) {
                    view?.showError("Please connect to your doorbell device in Settings")
                    view?.showEmptyState(true)
                    view?.showDoorbellEvents(emptyList())
                    return@launch
                }
                
                val events = model.getAllDoorbellEvents(context!!)
                
                if (events.isEmpty()) {
                    view?.showEmptyState(true)
                    view?.showDoorbellEvents(emptyList())
                } else {
                    view?.showEmptyState(false)
                    view?.showDoorbellEvents(events)
                }
                
                // Set up real-time listener for automatic updates
                setupRealtimeListener()
                
            } catch (e: Exception) {
                view?.showError("Failed to load history: ${e.message ?: "Unknown error"}")
            } finally {
                view?.showLoading(false)
            }
        }
    }
    
    /**
     * Sets up a real-time Firebase listener to automatically update history.
     */
    private fun setupRealtimeListener() {
        if (!isDeviceConnected() || context == null) return
        
        // Get device ID
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        val prefs = context?.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
        val deviceId = prefs?.getString("device_id", "DOORBELL_001") ?: "DOORBELL_001"
        
        // Remove existing listener if any
        firebaseListener?.let { listener ->
            eventsRef?.removeEventListener(listener)
        }
        
        // Set up new listener
        eventsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .reference
            .child("devices")
            .child(deviceId)
            .child("events")
        
        firebaseListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                scope.launch {
                    try {
                        val events = snapshot.children.mapNotNull { 
                            it.getValue(DoorbellEvent::class.java) 
                        }.sortedByDescending { it.timestamp }
                        
                        if (events.isEmpty()) {
                            view?.showEmptyState(true)
                            view?.showDoorbellEvents(emptyList())
                        } else {
                            view?.showEmptyState(false)
                            view?.showDoorbellEvents(events)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HistoryPresenter", "Error updating history: ${e.message}")
                    }
                }
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("HistoryPresenter", "Firebase listener cancelled: ${error.message}")
            }
        }
        
        eventsRef?.addValueEventListener(firebaseListener!!)
        android.util.Log.d("HistoryPresenter", "Real-time listener set up for history")
    }

    /**
     * Clears all doorbell history.
     */
    fun clearHistory() {
        scope.launch {
            try {
                view?.showLoading(true)
                
                val success = model.clearHistory(context!!)
                
                if (success) {
                    view?.onHistoryCleared()
                    view?.showDoorbellEvents(emptyList())
                    view?.showEmptyState(true)
                } else {
                    view?.showError("Failed to clear history")
                }
                
            } catch (e: Exception) {
                view?.showError("Failed to clear history: ${e.message ?: "Unknown error"}")
            } finally {
                view?.showLoading(false)
            }
        }
    }

    /**
     * Refreshes the doorbell events.
     */
    fun refreshEvents() {
        loadDoorbellEvents()
    }
    
    /**
     * Deletes a single doorbell event.
     */
    fun deleteEvent(event: DoorbellEvent) {
        scope.launch {
            try {
                android.util.Log.d("HistoryPresenter", "Attempting to delete event with timestamp: ${event.timestamp}")
                val success = model.deleteEvent(context!!, event.timestamp)
                
                if (success) {
                    android.util.Log.d("HistoryPresenter", "Event deleted successfully, reloading events")
                    // Reload events to update the list
                    loadDoorbellEvents()
                } else {
                    android.util.Log.w("HistoryPresenter", "Failed to delete event - event not found or error occurred")
                    view?.showError("Failed to delete event")
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryPresenter", "Error deleting event: ${e.message}")
                e.printStackTrace()
                view?.showError("Failed to delete event: ${e.message ?: "Unknown error"}")
            }
        }
    }

    /**
     * Checks if the device is connected (has valid Device ID and Auth Key).
     */
    private fun isDeviceConnected(): Boolean {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = context?.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
        val deviceId = prefs?.getString("device_id", "") ?: ""
        val authKey = prefs?.getString("auth_key", "") ?: ""
        val isConnected = prefs?.getBoolean("device_connected", false) ?: false
        
        return isConnected && deviceId.isNotEmpty() && authKey.isNotEmpty()
    }
    
    /**
     * Cleans up resources.
     */
    fun cleanup() {
        // Remove Firebase listener
        firebaseListener?.let { listener ->
            eventsRef?.removeEventListener(listener)
        }
        firebaseListener = null
        eventsRef = null
        
        scope.cancel()
    }
}
