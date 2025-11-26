package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.knocktrack.knocktrack.R

class SettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        
        initViews()
    }
    
    private fun initViews() {
        // Back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
        
        // Device configuration fields
        val etDeviceId = findViewById<EditText>(R.id.etDeviceId)
        val etAuthKey = findViewById<EditText>(R.id.etAuthKey)
        val btnSaveConfig = findViewById<Button>(R.id.btnSaveConfig)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val tvDeviceStatus = findViewById<TextView>(R.id.tvDeviceStatus)
        
        // Load saved configuration
        loadSavedConfiguration(etDeviceId, etAuthKey, tvDeviceStatus, btnSaveConfig, btnReset)
        
        // Save configuration button
        btnSaveConfig.setOnClickListener {
            saveConfiguration(etDeviceId, etAuthKey, tvDeviceStatus, btnSaveConfig, btnReset)
        }
        
        // Reset button
        btnReset.setOnClickListener {
            resetConfiguration(etDeviceId, etAuthKey, tvDeviceStatus, btnSaveConfig, btnReset)
        }
        
        // Bottom navigation
        val navHome = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(0) as LinearLayout
        val navHistory = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(1) as LinearLayout
        val navSettings = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(2) as LinearLayout
        
        navHome.setOnClickListener {
            navigateToHome()
        }
        
        navHistory.setOnClickListener {
            navigateToHistory()
        }
        
        navSettings.setOnClickListener {
            // Already on settings, do nothing
        }
    }
    
    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
    
    /** Override from BaseActivity - handles global alert navigation */
    override fun navigateToHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }
    
    override fun onResume() {
        super.onResume()
        
        // Force refresh the listener to ensure it's working properly
        com.knocktrack.knocktrack.utils.GlobalFirebaseListener.forceRefresh(this)
        
        // Add test gesture for Settings screen
        // Long press anywhere on the screen to test alerts
        findViewById<android.view.View>(android.R.id.content).setOnLongClickListener {
            testDoorbellAlert()
            true
        }
    }
    
    /**
     * Loads saved device configuration from SharedPreferences.
     */
    private fun loadSavedConfiguration(etDeviceId: EditText, etAuthKey: EditText, tvDeviceStatus: TextView, btnSaveConfig: Button, btnReset: Button) {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = getSharedPreferences("device_config_$userEmail", MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "")
        val authKey = prefs.getString("auth_key", "")
        val isConnected = prefs.getBoolean("device_connected", false)
        
        etDeviceId.setText(deviceId)
        etAuthKey.setText(authKey)
        
        if (isConnected) {
            tvDeviceStatus.text = "Connected ✓"
            tvDeviceStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            btnReset.visibility = android.view.View.VISIBLE
            btnSaveConfig.text = "Connected"
            btnSaveConfig.isEnabled = false
            btnSaveConfig.alpha = 0.6f
            // Make fields non-editable and grey when connected
            etDeviceId.isEnabled = false
            etDeviceId.isFocusable = false
            etDeviceId.isClickable = false
            etDeviceId.setTextColor(resources.getColor(android.R.color.darker_gray))
            etDeviceId.setBackgroundResource(R.drawable.edittext_border_grey)
            etAuthKey.isEnabled = false
            etAuthKey.isFocusable = false
            etAuthKey.isClickable = false
            etAuthKey.setTextColor(resources.getColor(android.R.color.darker_gray))
            etAuthKey.setBackgroundResource(R.drawable.edittext_border_grey)
        } else {
            tvDeviceStatus.text = "Not Connected"
            tvDeviceStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            btnReset.visibility = android.view.View.GONE
            btnSaveConfig.text = "Connect to Doorbell"
            btnSaveConfig.isEnabled = true
            btnSaveConfig.alpha = 1.0f
            // Make fields editable and normal color when not connected
            etDeviceId.isEnabled = true
            etDeviceId.isFocusable = true
            etDeviceId.isFocusableInTouchMode = true
            etDeviceId.isClickable = true
            etDeviceId.setTextColor(resources.getColor(android.R.color.black))
            etDeviceId.setBackgroundResource(R.drawable.edittext_border)
            etAuthKey.isEnabled = true
            etAuthKey.isFocusable = true
            etAuthKey.isFocusableInTouchMode = true
            etAuthKey.isClickable = true
            etAuthKey.setTextColor(resources.getColor(android.R.color.black))
            etAuthKey.setBackgroundResource(R.drawable.edittext_border)
        }
    }
    
    /**
     * Saves device configuration to SharedPreferences.
     */
    private fun saveConfiguration(etDeviceId: EditText, etAuthKey: EditText, tvDeviceStatus: TextView, btnSaveConfig: Button, btnReset: Button) {
        val deviceId = etDeviceId.text.toString().trim()
        val authKey = etAuthKey.text.toString().trim()
        
        if (deviceId.isEmpty() || authKey.isEmpty()) {
            Toast.makeText(this, "Please enter both Device ID and Auth Key to connect to your doorbell", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        // Save to account-specific SharedPreferences
        val prefs = getSharedPreferences("device_config_$userEmail", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("device_id", deviceId)
        editor.putString("auth_key", authKey)
        editor.putBoolean("device_connected", true) // Mark this account as connected
        editor.apply()
        
        // Update UI
        tvDeviceStatus.text = "Connected ✓"
        tvDeviceStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        btnReset.visibility = android.view.View.VISIBLE
        btnSaveConfig.text = "Connected"
        btnSaveConfig.isEnabled = false
        btnSaveConfig.alpha = 0.6f
        // Make fields non-editable and grey when connected
        etDeviceId.isEnabled = false
        etDeviceId.isFocusable = false
        etDeviceId.isClickable = false
        etDeviceId.setTextColor(resources.getColor(android.R.color.darker_gray))
        etDeviceId.setBackgroundResource(R.drawable.edittext_border_grey)
        etAuthKey.isEnabled = false
        etAuthKey.isFocusable = false
        etAuthKey.isClickable = false
        etAuthKey.setTextColor(resources.getColor(android.R.color.darker_gray))
        etAuthKey.setBackgroundResource(R.drawable.edittext_border_grey)
        
        Toast.makeText(this, "Connected to doorbell successfully!", Toast.LENGTH_SHORT).show()
        
        android.util.Log.d("SettingActivity", "Device configuration saved for account $userEmail - ID: $deviceId, Key: $authKey")
    }
    
    /**
     * Resets the device configuration and clears the connection.
     */
    private fun resetConfiguration(etDeviceId: EditText, etAuthKey: EditText, tvDeviceStatus: TextView, btnSaveConfig: Button, btnReset: Button) {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        // Clear SharedPreferences
        val prefs = getSharedPreferences("device_config_$userEmail", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
        
        // Clear input fields
        etDeviceId.setText("")
        etAuthKey.setText("")
        
        // Update UI
        tvDeviceStatus.text = "Not Connected"
        tvDeviceStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        btnReset.visibility = android.view.View.GONE
        btnSaveConfig.text = "Connect to Doorbell"
        btnSaveConfig.isEnabled = true
        btnSaveConfig.alpha = 1.0f
        // Make fields editable and normal color again when reset
        etDeviceId.isEnabled = true
        etDeviceId.isFocusable = true
        etDeviceId.isFocusableInTouchMode = true
        etDeviceId.isClickable = true
        etDeviceId.setTextColor(resources.getColor(android.R.color.black))
        etDeviceId.setBackgroundResource(R.drawable.edittext_border)
        etAuthKey.isEnabled = true
        etAuthKey.isFocusable = true
        etAuthKey.isFocusableInTouchMode = true
        etAuthKey.isClickable = true
        etAuthKey.setTextColor(resources.getColor(android.R.color.black))
        etAuthKey.setBackgroundResource(R.drawable.edittext_border)
        
        Toast.makeText(this, "Doorbell connection reset successfully", Toast.LENGTH_SHORT).show()
        
        android.util.Log.d("SettingActivity", "Device configuration reset for account $userEmail")
    }
    
    /**
     * Gets the current device configuration.
     */
    fun getDeviceConfiguration(): Pair<String, String> {
        // Get current user email for account-specific storage
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: "default"
        
        val prefs = getSharedPreferences("device_config_$userEmail", MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "") ?: ""
        val authKey = prefs.getString("auth_key", "") ?: ""
        return Pair(deviceId, authKey)
    }
}