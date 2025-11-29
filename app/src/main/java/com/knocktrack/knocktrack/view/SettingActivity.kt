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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

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
     * Saves device configuration to SharedPreferences after validating with Firebase.
     * Validates that the device ID exists and auth key matches in Firebase backend.
     */
    private fun saveConfiguration(etDeviceId: EditText, etAuthKey: EditText, tvDeviceStatus: TextView, btnSaveConfig: Button, btnReset: Button) {
        val deviceId = etDeviceId.text.toString().trim()
        val authKey = etAuthKey.text.toString().trim()
        
        if (deviceId.isEmpty() || authKey.isEmpty()) {
            Toast.makeText(this, "Please enter both Device ID and Auth Key to connect to your doorbell", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Disable button and show loading state
        btnSaveConfig.isEnabled = false
        btnSaveConfig.text = "Validating..."
        btnSaveConfig.alpha = 0.6f
        
        // Validate device ID and auth key with Firebase backend
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val isValid = withContext(Dispatchers.IO) {
                    validateDeviceCredentials(deviceId, authKey)
                }
                
                if (isValid) {
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
                    
                    Toast.makeText(this@SettingActivity, "Connected to doorbell successfully!", Toast.LENGTH_SHORT).show()
                    
                    android.util.Log.d("SettingActivity", "Device configuration saved for account $userEmail - ID: $deviceId, Key: $authKey")
                } else {
                    // Validation failed - restore button state
                    btnSaveConfig.isEnabled = true
                    btnSaveConfig.text = "Connect to Doorbell"
                    btnSaveConfig.alpha = 1.0f
                    
                    Toast.makeText(this@SettingActivity, "Invalid Device ID or Auth Key. Please check your credentials and try again.", Toast.LENGTH_LONG).show()
                    
                    android.util.Log.w("SettingActivity", "Device validation failed - ID: $deviceId")
                }
            } catch (e: Exception) {
                // Error during validation - restore button state
                btnSaveConfig.isEnabled = true
                btnSaveConfig.text = "Connect to Doorbell"
                btnSaveConfig.alpha = 1.0f
                
                Toast.makeText(this@SettingActivity, "Error validating credentials. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
                
                android.util.Log.e("SettingActivity", "Error validating device credentials: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Validates device ID and auth key against Firebase database backend.
     * Reads from: devices/{deviceId}/auth_key
     * 
     * @param deviceId The device ID to validate (e.g., "DOORBELL_001")
     * @param authKey The auth key to validate (e.g., "12345")
     * @return true if device exists and auth key matches, false otherwise
     */
    private suspend fun validateDeviceCredentials(deviceId: String, authKey: String): Boolean {
        return try {
            val database = FirebaseDatabase.getInstance().reference
            // Read from: devices/{deviceId}
            val deviceRef = database.child("devices").child(deviceId)
            
            // Check if device exists in Firebase backend
            val snapshot = deviceRef.get().await()
            
            if (!snapshot.exists()) {
                android.util.Log.w("SettingActivity", "Device ID does not exist in Firebase backend: $deviceId")
                return false
            }
            
            // Read auth_key from: devices/{deviceId}/auth_key
            val storedAuthKey = snapshot.child("auth_key").getValue(String::class.java)
            
            if (storedAuthKey == null || storedAuthKey != authKey) {
                android.util.Log.w("SettingActivity", "Auth key mismatch for device: $deviceId (expected: $storedAuthKey, got: $authKey)")
                return false
            }
            
            android.util.Log.d("SettingActivity", "Device credentials validated successfully from Firebase backend: $deviceId")
            true
        } catch (e: Exception) {
            android.util.Log.e("SettingActivity", "Error validating device credentials from Firebase: ${e.message}")
            false
        }
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



