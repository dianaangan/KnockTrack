package com.knocktrack.knocktrack.presenter

import com.knocktrack.knocktrack.model.HomeModel
import com.knocktrack.knocktrack.model.DoorbellModel
import com.knocktrack.knocktrack.model.DoorbellEvent
import com.knocktrack.knocktrack.view.HomeView
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.*
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.bluetooth.BluetoothAdapter

/**
 * Presenter for the Home screen.
 * Coordinates between View and Model:
 * - Gets user data from Firebase Auth
 * - Pushes UI updates and navigation commands to View
 */
class HomePresenter {
    private var view: HomeView? = null
    private val model = HomeModel()
    private val doorbellModel = DoorbellModel()
    private val historyModel = com.knocktrack.knocktrack.model.HistoryModel()
    private var context: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var firebaseListener: com.google.firebase.database.ValueEventListener? = null
    private var eventsRef: com.google.firebase.database.DatabaseReference? = null

    /** The View calls this when it's ready to receive updates. */
    fun attachView(view: HomeView, context: Context) {
        this.view = view
        this.context = context
    }

    /** The View calls this on destroy to avoid leaks. */
    fun detachView() {
        this.view = null
    }

    /**
     * Loads and displays current user data from authentication service.
     * - Checks if user is signed in
     * - Gets user profile from database
     * - Updates the View with user information
     */
    fun loadUserData() {
        val currentUser = model.getCurrentUser()
        
        if (currentUser == null || !model.isUserSignedIn()) {
            // User not signed in, navigate to login
            view?.navigateToLogin()
            return
        }
        
        // Get user ID and email from Firebase user
        val userId = currentUser.uid
        val userEmail = currentUser.email ?: ""
        val userDisplayName = currentUser.displayName ?: "User"
        
        // Get user profile data from database
        model.getUserProfile(userId) { result ->
            result.fold(
                onSuccess = { userData ->
                    // Extract user data from database
                    val firstName = userData["firstName"] as? String ?: ""
                    val lastName = userData["lastName"] as? String ?: ""
                    val email = userData["email"] as? String ?: userEmail
                    
                    // Format display data
                    val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        model.formatDisplayName(firstName, lastName)
                    } else {
                        userDisplayName
                    }
                    val welcomeMessage = model.getWelcomeMessage(displayName)
                    val emailDisplay = model.getEmailDisplay(email)
                    
                    // Update the view
                    view?.showUserData(welcomeMessage, emailDisplay)
                },
                onFailure = { exception ->
                    // If we can't get profile data, use basic user data
                    val welcomeMessage = model.getWelcomeMessage(userDisplayName)
                    val emailDisplay = model.getEmailDisplay(userEmail)
                    
                    view?.showUserData(welcomeMessage, emailDisplay)
                }
            )
        }
    }

    /**
     * Handles user logout.
     * - Signs out from authentication service
     * - Navigates to login screen
     */
    fun logout() {
        try {
            model.signOut()
            view?.onLogoutSuccess()
            view?.navigateToLogin()
        } catch (e: Exception) {
            view?.onLogoutFailed("Logout failed: ${e.message ?: "Unknown error"}")
        }
    }
    
    /**
     * Loads doorbell data and sets up real-time listeners.
     * Only loads data if device is connected.
     */
    fun loadDoorbellData() {
        scope.launch {
            try {
                // Check if device is connected first
                if (!isDeviceConnected()) {
                    view?.showNotConnectedState()
                    return@launch
                }
                
                // Load recent activity from Firebase
                val recentActivity = doorbellModel.getRecentActivity()
                val activityStrings = recentActivity.map { event ->
                    "🟢 Doorbell pressed        ${formatTime(event.time)}"
                }
                view?.showRecentActivity(activityStrings)
                
                // Load and show doorbell analytics
                val analytics = historyModel.getDoorbellAnalytics(context!!)
                view?.showDoorbellAnalytics(analytics)
                
                // Check connection status
                checkConnectionStatus()
                
                // Set up real-time listener for automatic updates
                setupRealtimeListener()
                
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Sets up a real-time Firebase listener to automatically update recent activity.
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
                        
                        val activityStrings = events.take(2).map { event ->
                            "🟢 Doorbell pressed        ${formatTime(event.time)}"
                        }
                        
                        view?.showRecentActivity(activityStrings)
                        
                        // Update analytics when events change
                        context?.let {
                            val analytics = historyModel.getDoorbellAnalytics(it)
                            view?.showDoorbellAnalytics(analytics)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomePresenter", "Error updating recent activity: ${e.message}")
                    }
                }
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("HomePresenter", "Firebase listener cancelled: ${error.message}")
            }
        }
        
        eventsRef?.addValueEventListener(firebaseListener!!)
        android.util.Log.d("HomePresenter", "Real-time listener set up for recent activity")
    }
    
    // Firebase real-time listening now handled globally by GlobalFirebaseListener
    // No need for local listening in HomePresenter
    
    /**
     * Checks WiFi and Bluetooth connection status.
     */
    private fun checkConnectionStatus() {
        val wifiConnected = isWifiConnected()
        val bluetoothConnected = isBluetoothConnected()
        view?.showConnectionStatus(wifiConnected, bluetoothConnected)
    }
    
    /**
     * Checks if WiFi is connected.
     */
    private fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if Bluetooth is connected.
     */
    private fun isBluetoothConnected(): Boolean {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Formats time to match the UI format from the picture.
     * Converts timestamp to readable time format like "10:32 AM"
     */
    private fun formatTime(timeString: String): String {
        return try {
            // If timeString is a timestamp, convert it
            if (timeString.matches("\\d+".toRegex())) {
                val timestamp = timeString.toLong()
                val date = java.util.Date(timestamp * 1000) // Convert to milliseconds
                val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                formatter.format(date)
            } else {
                // If it's already formatted, return as is
                timeString
            }
        } catch (e: Exception) {
            // Fallback to current time if parsing fails
            val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            formatter.format(java.util.Date())
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


