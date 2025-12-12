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
    private var doorbellModel: DoorbellModel? = null
    private val historyModel = com.knocktrack.knocktrack.model.HistoryModel()
    private var context: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var firebaseListener: com.google.firebase.database.ValueEventListener? = null
    private var eventsRef: com.google.firebase.database.DatabaseReference? = null
    private var deviceStatusValidationJob: Job? = null

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
                
                // Get device ID and create DoorbellModel
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val userEmail = currentUser?.email ?: "default"
                val prefs = context?.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
                val deviceId = prefs?.getString("device_id", "DOORBELL_001") ?: "DOORBELL_001"
                doorbellModel = DoorbellModel(deviceId)
                
                // Load recent activity from Firebase
                val recentActivity = doorbellModel?.getRecentActivity() ?: emptyList()
                val activityStrings = recentActivity.map { event ->
                    "🟢 Doorbell pressed        ${formatTime(event.time)}"
                }
                view?.showRecentActivity(activityStrings)
                
                // Load and show doorbell analytics
                val analytics = historyModel.getDoorbellAnalytics(context!!)
                view?.showDoorbellAnalytics(analytics)
                
                // Check connection status
                checkConnectionStatus()
                
                // IMPORTANT: Check device status FIRST before setting up listeners
                // This ensures we show accurate status immediately when app opens
                // Use a small delay to ensure Firebase is ready
                delay(500) // Small delay to ensure everything is initialized
                checkDeviceActiveStatusImmediate()
                
                // Then set up real-time listener for device status (more efficient than polling)
                setupDeviceStatusListener()
                
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
        if (!isDeviceConnected() || context == null || doorbellModel == null) return
        
        // Remove existing listener if any
        firebaseListener?.let { listener ->
            eventsRef?.removeEventListener(listener)
        }
        
        // Get device ID from doorbellModel (already has the correct device ID)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        val prefs = context?.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
        val deviceId = prefs?.getString("device_id", "DOORBELL_001") ?: "DOORBELL_001"
        
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
     * Sets up a real-time listener for device status changes.
     * This is more efficient than polling - Firebase will notify us when status changes.
     */
    private fun setupDeviceStatusListener() {
        if (!isDeviceConnected() || context == null) {
            view?.showDeviceActiveStatus(false)
            return
        }
        
        try {
            // Use the doorbellModel instance already created in loadDoorbellData
            // If not created yet, create it now
            if (doorbellModel == null) {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val userEmail = currentUser?.email ?: "default"
                val prefs = context?.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
                val deviceId = prefs?.getString("device_id", "DOORBELL_001") ?: "DOORBELL_001"
                doorbellModel = DoorbellModel(deviceId)
            }
            
            // IMPORTANT: Don't set up real-time listener until AFTER initial check completes
            // This prevents showing cached/old data before we verify actual status
            // The initial check will set the correct status first
            
            // Set up periodic validation to detect when device goes offline
            // (Real-time listener won't fire if data doesn't change, so we need to validate timestamp)
            startDeviceStatusValidation()
            
            // Set up real-time listener AFTER initial check (with small delay to ensure initial check completes)
            scope.launch {
                delay(1000) // Wait 1 second for initial check to complete
                doorbellModel?.listenToDeviceStatus { isActive ->
                    view?.showDeviceActiveStatus(isActive)
                    android.util.Log.d("HomePresenter", "Device active status updated (real-time): $isActive")
                }
            }
            
            android.util.Log.d("HomePresenter", "Real-time device status listener set up with periodic validation")
        } catch (e: Exception) {
            android.util.Log.e("HomePresenter", "Error setting up device status listener: ${e.message}")
            view?.showDeviceActiveStatus(false)
        }
    }
    
    /**
     * Performs an immediate check of device active status.
     * Called on screen load to ensure we show current status right away.
     */
    private fun checkDeviceActiveStatusImmediate() {
        if (!isDeviceConnected() || context == null) {
            android.util.Log.w("HomePresenter", "❌ Cannot check device status - not connected or context null")
            view?.showDeviceActiveStatus(false)
            return
        }
        
        if (doorbellModel == null) {
            android.util.Log.w("HomePresenter", "❌ Cannot check device status - doorbellModel is null, will retry...")
            // Retry after a short delay if doorbellModel isn't ready yet
            scope.launch {
                delay(1000)
                if (doorbellModel != null) {
                    checkDeviceActiveStatusImmediate()
                } else {
                    view?.showDeviceActiveStatus(false)
                }
            }
            return
        }
        
        scope.launch {
            try {
                android.util.Log.d("HomePresenter", "🔍 Starting device status verification from Firebase...")
                android.util.Log.d("HomePresenter", "   UI should currently show 'Checking...'")
                android.util.Log.d("HomePresenter", "   Reading EXACT timestamp from Firebase database...")
                
                // This reads FRESH data from Firebase to verify actual device status
                // It uses the EXACT timestamp from the database to determine if device is active
                val isActive = withContext(Dispatchers.IO) {
                    doorbellModel?.isDeviceActive() ?: false
                }
                
                android.util.Log.d("HomePresenter", "✅ Firebase verification complete: active=$isActive")
                android.util.Log.d("HomePresenter", "   Now updating UI to show: ${if (isActive) "Active ✓" else "Doorbell Offline"}")
                
                // ONLY update status AFTER we've verified from Firebase
                // This ensures we never show "Active" without checking first
                view?.showDeviceActiveStatus(isActive)
            } catch (e: Exception) {
                android.util.Log.e("HomePresenter", "❌ Error in device status check: ${e.message}")
                e.printStackTrace()
                // If check fails, assume offline for safety
                android.util.Log.w("HomePresenter", "   Check failed - showing 'Doorbell Offline' for safety")
                view?.showDeviceActiveStatus(false)
            }
        }
    }
    
    /**
     * Public method to force immediate device status check.
     * Called from Activity onResume/onCreate to ensure status is current.
     * This ensures the app always shows the latest status when user opens it.
     */
    fun checkDeviceStatusNow() {
        android.util.Log.d("HomePresenter", "🔄 User opened/resumed app - checking device status NOW")
        checkDeviceActiveStatusImmediate()
    }
    
    /**
     * Starts periodic validation of device status.
     * Since the real-time listener only fires on data changes, we need to periodically
     * check if the timestamp has expired (device went offline without data changing).
     */
    private fun startDeviceStatusValidation() {
        // Cancel existing job if any
        deviceStatusValidationJob?.cancel()
        
        deviceStatusValidationJob = scope.launch {
            // Check IMMEDIATELY first (no delay), then periodically while app is open
            // This ensures status is always up-to-date from time to time
            while (true) {
                try {
                    if (!isDeviceConnected() || context == null || view == null || doorbellModel == null) {
                        android.util.Log.d("HomePresenter", "Stopping device status validation - disconnected or view detached")
                        break // Stop if disconnected or view detached
                    }
                    
                    // Validate by checking the timestamp - force fresh read from Firebase
                    // This reads the LATEST data from Firebase every time
                    android.util.Log.d("HomePresenter", "🔄 Periodic status check - reading latest from Firebase...")
                    val isActive = withContext(Dispatchers.IO) {
                        doorbellModel?.isDeviceActive() ?: false
                    }
                    
                    view?.showDeviceActiveStatus(isActive)
                    android.util.Log.d("HomePresenter", "✅ Periodic device status check: active=$isActive")
                    
                    // Check every 2 seconds for very fast updates
                    delay(2000)
                } catch (e: Exception) {
                    android.util.Log.e("HomePresenter", "❌ Error in device status validation: ${e.message}")
                    e.printStackTrace()
                    delay(2000) // Still wait before retrying
                }
            }
        }
        
        android.util.Log.d("HomePresenter", "Device status validation started (checking immediately, then every 15s)")
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
     * Formats time to "11:30:05 AM" format (with seconds).
     * ESP32 sends format like "07:30:45.123 PM" - we extract "07:30:45 PM"
     */
    private fun formatTime(timeString: String): String {
        return try {
            // If timeString is a timestamp number, convert it
            if (timeString.matches("\\d+".toRegex())) {
                val timestamp = timeString.toLong()
                val date = java.util.Date(timestamp * 1000)
                val formatter = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
                formatter.format(date)
            } else {
                // ESP32 format: "07:30:45.123 PM" -> extract "07:30:45 PM"
                val parts = timeString.trim().split(" ")
                if (parts.size >= 2) {
                    val timePart = parts[0] // "07:30:45.123"
                    val amPm = parts[1] // "PM"
                    // Extract HH:MM:SS (first 8 characters, before milliseconds)
                    val hourMinuteSec = if (timePart.contains(".")) {
                        timePart.substring(0, timePart.indexOf(".")) // "07:30:45"
                    } else if (timePart.length >= 8) {
                        timePart.substring(0, 8) // "07:30:45"
                    } else {
                        timePart
                    }
                    "$hourMinuteSec $amPm" // "07:30:45 PM"
                } else {
                    timeString
                }
            }
        } catch (e: Exception) {
            // Fallback to current time if parsing fails
            val formatter = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
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
        
        // Remove device status listener
        doorbellModel?.removeDeviceStatusListener()
        doorbellModel = null
        
        // Cancel device status validation job
        deviceStatusValidationJob?.cancel()
        deviceStatusValidationJob = null
        
        scope.cancel()
    }
}


