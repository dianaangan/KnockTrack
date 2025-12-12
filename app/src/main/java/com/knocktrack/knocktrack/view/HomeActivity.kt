package com.knocktrack.knocktrack.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.media.RingtoneManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.view.View
import android.widget.*
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.presenter.HomePresenter
import com.knocktrack.knocktrack.utils.GlobalAlertManager

/**
 * Home screen of the app (View in MVP).
 *
 * Responsibilities:
 * - Inflate and bind UI elements
 * - Forward user actions to the Presenter
 * - Render state received from the Presenter
 * - Perform navigation as instructed by the Presenter
 */
class HomeActivity : BaseActivity(), HomeView {

    private lateinit var presenter: HomePresenter
    private lateinit var tvWelcome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    /**
     * Android lifecycle: initializes the UI and wires MVP components.
     * - Loads user data from Firebase Authentication
     * - Displays user information or navigates to login if not authenticated
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        initPresenter()
        setupListeners()

        // Initialize GlobalAlertManager with persisted data
        GlobalAlertManager.initialize(this)
        
        // Load user data from Firebase instead of intent extras
        presenter.loadUserData()
        
        // Initialize UI state - hide notification banner initially
        val notificationBanner = findViewById<LinearLayout>(R.id.notificationBanner)
        notificationBanner.visibility = android.view.View.GONE
        
        // Load doorbell data
        presenter.loadDoorbellData()
        
        // Check and display connection status (will show "Checking..." until verified)
        checkConnectionStatus()
        
        // Check device status immediately when screen opens
        // This will verify if ESP32 is actually active before showing status
        if (isDeviceConnected()) {
            // Don't show status until we verify from Firebase
            presenter.checkDeviceStatusNow()
        }
    }

    /** Binds views from the layout. */
    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogout = findViewById(R.id.btnLogout)
        
        // Bottom navigation views
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navHistory = findViewById<LinearLayout>(R.id.navHistory)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)
        
        // Set up navigation listeners
        navHome.setOnClickListener {
            // Already on home, do nothing
        }
        
        navHistory.setOnClickListener {
            navigateToHistory()
        }
        
        navSettings.setOnClickListener {
            navigateToSettings()
        }
        
        // Test alert button (for debugging - remove in production)
        tvWelcome.setOnLongClickListener {
            // Long press welcome text to test global alert
            testDoorbellAlert()
            true
        }
        
        // Add a test button for doorbell simulation (for debugging)
        val testButton = findViewById<Button>(R.id.btnLogout) // Reuse logout button for testing
        testButton.setOnLongClickListener {
            // Long press logout button to simulate doorbell press
            android.util.Log.d("HomeActivity", "Simulating doorbell press from HomeActivity")
            simulateDoorbellPress()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Debug: Log the current status of the global listener
        android.util.Log.d("HomeActivity", "onResume - Global listener status: ${com.knocktrack.knocktrack.utils.GlobalFirebaseListener.debugStatus()}")
        
        // Only refresh listener if device is connected
        if (isDeviceConnected()) {
            // Force refresh the listener to ensure it's working properly
            com.knocktrack.knocktrack.utils.GlobalFirebaseListener.forceRefresh(this)
            
            // Reset duplicate detection to ensure alerts work after navigation
            com.knocktrack.knocktrack.utils.GlobalAlertManager.resetDuplicateDetection(this)
        } else {
            android.util.Log.w("HomeActivity", "Device not connected - not refreshing global listener")
        }
        
        android.util.Log.d("HomeActivity", "HomeActivity resumed - listener refreshed and duplicate detection reset")
        
        // Verify the listener is properly tracking this activity
        val currentActivity = com.knocktrack.knocktrack.utils.GlobalFirebaseListener.getCurrentActivityPublic()
        if (currentActivity == this) {
            android.util.Log.d("HomeActivity", "✅ Global listener is properly tracking HomeActivity")
        } else {
            android.util.Log.w("HomeActivity", "❌ Global listener is NOT tracking HomeActivity! Current: ${currentActivity?.javaClass?.simpleName}")
        }
        
        // Refresh connection status
        checkConnectionStatus()
        
        // Refresh analytics and device status when screen resumes
        if (isDeviceConnected()) {
            presenter.loadDoorbellData()
            // Force immediate status check on resume
            presenter.checkDeviceStatusNow()
        }
    }

    /** Creates the Presenter and attaches this Activity as its View. */
    private fun initPresenter() {
        presenter = HomePresenter()
        presenter.attachView(this, this)
    }

    /** Wires user interactions to Presenter actions. */
    private fun setupListeners() {
        btnLogout.setOnClickListener {
            presenter.logout()
        }
    }

    /** Renders the formatted user data from the Presenter. */
    override fun showUserData(name: String, email: String) {
        // Extract full name from welcome message (format: "Welcome, John Doe!")
        val nameOnly = name.replace("Welcome, ", "").replace("!", "").trim()
        
        // Display full name (firstname and lastname)
        tvWelcome.text = if (nameOnly.isNotEmpty() && nameOnly != "User") {
            "Welcome, $nameOnly"
        } else {
            "Welcome, User"
        }
        tvEmail.visibility = android.view.View.GONE
    }

    override fun onLogoutSuccess() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onLogoutFailed(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Navigates to Login screen. */
    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /** Override from BaseActivity - handles global alert navigation */
    override fun navigateToHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }
    
    /** Navigates to Settings screen. */
    private fun navigateToSettings() {
        startActivity(Intent(this, SettingActivity::class.java))
    }

    /** Detaches Presenter to avoid memory leaks. */
    override fun onDestroy() {
        presenter.cleanup()
        presenter.detachView()
        super.onDestroy()
    }

    /**
     * Prevent navigating back to previous screens after a successful login.
     * Keeps the user on Home unless they explicitly log out.
     */
    override fun onBackPressed() {
        // Optionally minimize the app instead of leaving Home
        moveTaskToBack(true)
    }
    
    // Doorbell event methods - Two states: Ready vs Pressed
    override fun showDoorbellPressed(time: String, date: String, timestamp: Long) {
        android.util.Log.d("HomeActivity", "showDoorbellPressed called with time: $time, date: $date, timestamp: $timestamp")
        
        // Create unique event ID from Firebase data
        val eventId = "${timestamp}_${time}_${date}"
        
        // Show global alert dialog (prevents duplicates using real Firebase timestamp)
        GlobalAlertManager.showDoorbellAlert(this, time, date, timestamp, eventId)
        
        // Update system status to show pressed state
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        tvStatus.text = "🔔 Active – Doorbell Pressed\nSystem Active"
        tvStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
        
        // Keep the original bell icon - no need to change it
        // The global alert dialog will show the pressed state
        
        // Log for debugging
        android.util.Log.d("HomeActivity", "Global doorbell alert triggered at $time with eventId: $eventId")
    }
    
    override fun showSystemStatus(status: String, isActive: Boolean) {
        // System status display removed - no longer showing status text
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        
        if (isActive) {
            // STATE 2: Button IS pressed - Show pressed state
            tvStatus.text = "🔔 Active – Doorbell Pressed\n$status"
            tvStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
            // Keep the original bell icon - no need to change it
        } else {
            // STATE 1: Button NOT pressed - Show ready state (like in your picture)
            tvStatus.text = "✅ Doorbell Active"
            tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            // Keep the original bell icon - no need to change it
        }
    }
    
    override fun showRecentActivity(activities: List<String>) {
        // Update the recent activity section with real Firebase data
        val recentActivityContainer = findViewById<LinearLayout>(R.id.recentActivityContainer)
        val tvNoRecentActivity = findViewById<TextView>(R.id.tvNoRecentActivity)
        val tvRecentActivityTitle = findViewById<TextView>(R.id.tvRecentActivityTitle)
        val tvNotConnectedMessage = findViewById<TextView>(R.id.tvNotConnectedMessage)
        
        // Hide not connected message
        tvNotConnectedMessage.visibility = android.view.View.GONE
        // Show recent activity title
        tvRecentActivityTitle.visibility = android.view.View.VISIBLE
        
        // Clear existing activity items
        recentActivityContainer.removeAllViews()
        
        // Show/hide empty state message
        if (activities.isEmpty()) {
            tvNoRecentActivity.visibility = android.view.View.VISIBLE
        } else {
            tvNoRecentActivity.visibility = android.view.View.GONE
        
        // Add activity items dynamically from Firebase data
        activities.take(2).forEachIndexed { index, activity ->
            val activityItem = createActivityItem(activity, index)
            recentActivityContainer.addView(activityItem)
            }
        }
        
        // Log for debugging
        android.util.Log.d("HomeActivity", "Recent Activities from Firebase: ${activities.joinToString(", ")}")
    }
    
    override fun showNotConnectedState() {
        // Show not connected message but keep Recent Activity title
        val recentActivityContainer = findViewById<LinearLayout>(R.id.recentActivityContainer)
        val tvNoRecentActivity = findViewById<TextView>(R.id.tvNoRecentActivity)
        val tvRecentActivityTitle = findViewById<TextView>(R.id.tvRecentActivityTitle)
        val tvNotConnectedMessage = findViewById<TextView>(R.id.tvNotConnectedMessage)
        
        // Keep recent activity title visible
        tvRecentActivityTitle.visibility = android.view.View.VISIBLE
        
        // Hide activity container
        recentActivityContainer.removeAllViews()
        recentActivityContainer.visibility = android.view.View.GONE
        tvNoRecentActivity.visibility = android.view.View.GONE
        
        // Show not connected message
        tvNotConnectedMessage.visibility = android.view.View.VISIBLE
        
        // Show empty analytics
        showDoorbellAnalytics(com.knocktrack.knocktrack.model.DoorbellAnalytics(0, 0, 0, "Never"))
    }
    
    override fun showDoorbellAnalytics(analytics: com.knocktrack.knocktrack.model.DoorbellAnalytics) {
        val tvTotalNotifications = findViewById<TextView>(R.id.tvTotalNotifications)
        val tvTodayNotifications = findViewById<TextView>(R.id.tvTodayNotifications)
        val tvWeekNotifications = findViewById<TextView>(R.id.tvWeekNotifications)
        
        tvTotalNotifications.text = analytics.totalNotifications.toString()
        tvTodayNotifications.text = analytics.todayNotifications.toString()
        tvWeekNotifications.text = analytics.weekNotifications.toString()
    }
    
    /**
     * Creates a dynamic activity item view
     */
    private fun createActivityItem(activityText: String, index: Int): LinearLayout {
        val activityItem = LinearLayout(this)
        activityItem.orientation = LinearLayout.HORIZONTAL
        activityItem.gravity = android.view.Gravity.CENTER_VERTICAL
        activityItem.setPadding(48, 48, 48, 48) // 12dp padding
        activityItem.background = resources.getDrawable(R.drawable.activity_item_border)
        
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomMargin = 32 // 8dp margin
        activityItem.layoutParams = layoutParams
        
        // Green dot
        val greenDot = View(this)
        val dotParams = LinearLayout.LayoutParams(32, 32) // 8dp
        dotParams.marginEnd = 48 // 12dp margin
        greenDot.layoutParams = dotParams
        greenDot.background = resources.getDrawable(R.drawable.circle_green)
        activityItem.addView(greenDot)
        
        // Activity text
        val activityTextView = TextView(this)
        val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        activityTextView.layoutParams = textParams
        activityTextView.text = "Doorbell pressed"
        activityTextView.setTextColor(resources.getColor(android.R.color.black))
        activityTextView.textSize = 16f
        activityItem.addView(activityTextView)
        
        // Time text - extract time from Firebase data
        val timeTextView = TextView(this)
        val timeParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        timeTextView.layoutParams = timeParams
        
        // Parse time from activity string format: "🟢 Doorbell pressed        10:32 AM"
        val timeText = if (activityText.contains("        ")) {
            activityText.split("        ").lastOrNull() ?: "Unknown time"
        } else {
            "Unknown time"
        }
        timeTextView.text = timeText
        timeTextView.setTextColor(resources.getColor(android.R.color.darker_gray))
        timeTextView.textSize = 14f
        activityItem.addView(timeTextView)
        
        return activityItem
    }
    
    // Alert dialog now handled by GlobalAlertManager - no local method needed
    
    // Alert dialog handles its own dismissal - no separate dismiss method needed
    
    /**
     * Plays alert sound
     */
    private fun playAlertSound() {
        try {
            val ringtone = RingtoneManager.getRingtone(
                this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            ringtone?.play()
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error playing alert sound: ${e.message}")
        }
    }
    
    /**
     * Vibrates the device
     */
    @SuppressLint("MissingPermission")
    private fun vibrateDevice() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                // Vibrate pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 500ms
                val pattern = longArrayOf(0, 500, 200, 500)
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error vibrating device: ${e.message}")
        }
    }
    
    override fun showConnectionStatus(wifiConnected: Boolean, bluetoothConnected: Boolean) {
        // Log connection status for debugging
        android.util.Log.d("HomeActivity", "WiFi: $wifiConnected, Bluetooth: $bluetoothConnected")
    }
    
    /**
     * Simulates a doorbell press for testing.
     */
    private fun simulateDoorbellPress() {
        android.util.Log.d("HomeActivity", "Simulating doorbell press - triggering global alert")
        
        // Get current time
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val currentDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val timestamp = System.currentTimeMillis()
        val eventId = "simulated_${timestamp}_${currentTime}_${currentDate}"
        
        // Trigger the global alert directly
        com.knocktrack.knocktrack.utils.GlobalAlertManager.showDoorbellAlert(
            this,
            currentTime,
            currentDate,
            timestamp,
            eventId
        )
        
        android.util.Log.d("HomeActivity", "Simulated doorbell alert triggered")
    }
    
    /**
     * Checks and displays the doorbell connection status.
     * This checks if device credentials are configured.
     */
    private fun checkConnectionStatus() {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = getSharedPreferences("device_config_$userEmail", MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "")
        val authKey = prefs.getString("auth_key", "")
        val isConnected = prefs.getBoolean("device_connected", false)
        
        val deviceConnected = isConnected && !deviceId.isNullOrEmpty() && !authKey.isNullOrEmpty()
        
        // Update connection status in the main section
        val tvConnectionStatus = findViewById<TextView>(R.id.tvConnectionStatus)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        
        if (deviceConnected) {
            // Set the device label
            tvStatus.text = "Doorbell Device"
            tvStatus.setTextColor(resources.getColor(android.R.color.black))
            
            // CRITICAL: Always show "Checking..." first when app opens
            // This will be updated by showDeviceActiveStatus ONLY after Firebase verification completes
            // We NEVER show "Active" until we've verified from Firebase database
            tvConnectionStatus.text = "Checking"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.darker_gray))
            android.util.Log.d("HomeActivity", "🔍 Initial status: 'Checking...' - waiting for Firebase verification")
            android.util.Log.d("HomeActivity", "   Device credentials found: $deviceId")
            android.util.Log.d("HomeActivity", "   Will verify actual device status from Firebase database...")
        } else {
            tvStatus.text = "Doorbell Device"
            tvStatus.setTextColor(resources.getColor(android.R.color.black))
            tvConnectionStatus.text = "Status: Not Connected"
            tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            android.util.Log.d("HomeActivity", "Doorbell connection status: Not Connected for account: $userEmail")
        }
        
    }
    
    /**
     * Shows the device active status (if ESP32 is online/active).
     * Called by presenter after checking Firebase heartbeat.
     * This ONLY updates after Firebase verification completes.
     */
    override fun showDeviceActiveStatus(isActive: Boolean) {
        val tvConnectionStatus = findViewById<TextView>(R.id.tvConnectionStatus)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        
        // Only update if device is connected (credentials configured)
        if (isDeviceConnected()) {
            // Set the device label
            tvStatus.text = "Doorbell Device"
            tvStatus.setTextColor(resources.getColor(android.R.color.black))
            
            // This is called AFTER Firebase check completes, so update with actual verified status
            if (isActive) {
                // Device is online and active
                tvConnectionStatus.text = "Status: Active"
                tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                android.util.Log.d("HomeActivity", "✅ ESP32 Device Status: Active (verified from Firebase)")
            } else {
                // Device is offline
                tvConnectionStatus.text = "Status: Offline"
                tvConnectionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                android.util.Log.d("HomeActivity", "❌ ESP32 Device Status: Offline (verified from Firebase)")
            }
        } else {
            // If not connected, keep "Not Connected" status (don't override)
            android.util.Log.d("HomeActivity", "Device not connected - keeping 'Not Connected' status")
        }
    }
    
    /**
     * Checks if the device is connected (has valid Device ID and Auth Key).
     */
    private fun isDeviceConnected(): Boolean {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = getSharedPreferences("device_config_$userEmail", MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "") ?: ""
        val authKey = prefs.getString("auth_key", "") ?: ""
        val isConnected = prefs.getBoolean("device_connected", false)
        
        return isConnected && deviceId.isNotEmpty() && authKey.isNotEmpty()
    }
}


