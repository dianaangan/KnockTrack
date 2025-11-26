package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.knocktrack.knocktrack.utils.GlobalAlertManager
import com.knocktrack.knocktrack.utils.GlobalFirebaseListener

/**
 * Base activity that handles global doorbell alerts.
 * All activities should extend this to receive global alerts.
 */
abstract class BaseActivity : Activity() {
    
    override fun onResume() {
        super.onResume()
        
        // Always update current activity reference first
        GlobalFirebaseListener.updateCurrentActivity(this)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // Check if device is connected before starting listener
        if (isDeviceConnected()) {
            // Start global Firebase listening when activity becomes active
            GlobalFirebaseListener.startListening(this)
            android.util.Log.d("BaseActivity", "Started global Firebase listening for ${javaClass.simpleName}")
            
            // Enhanced reliability check
            if (!GlobalFirebaseListener.isListening()) {
                android.util.Log.w("BaseActivity", "Global listener not active, restarting...")
                GlobalFirebaseListener.startListening(this)
            }
        } else {
            android.util.Log.w("BaseActivity", "Device not connected - not starting global listener")
        }
        
        // Log current status for debugging
        android.util.Log.d("BaseActivity", "Global listener status: ${GlobalFirebaseListener.debugStatus()}")
    }
    
    /**
     * Requests notification permission for Android 13+.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("BaseActivity", "Requesting notification permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                android.util.Log.d("BaseActivity", "Notification permission already granted")
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Update current activity for global listener
        GlobalFirebaseListener.updateCurrentActivity(this)
        android.util.Log.d("BaseActivity", "Activity paused: ${javaClass.simpleName}, listener status: ${GlobalFirebaseListener.debugStatus()}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Don't stop global listening when activity is destroyed
        // Only update current activity - let the listener continue running
        GlobalFirebaseListener.updateCurrentActivity(null)
        android.util.Log.d("BaseActivity", "Activity destroyed: ${javaClass.simpleName}, but keeping global listener active")
    }
    
    /**
     * Navigates to history screen.
     * Override in specific activities if needed.
     */
    protected open fun navigateToHistory() {
        // Default implementation - override in specific activities
        android.util.Log.d("BaseActivity", "Navigate to history - override in specific activity")
    }
    
    /**
     * Test method to trigger a doorbell alert on any screen.
     * Long press any screen to test alerts.
     */
    protected fun testDoorbellAlert() {
        android.util.Log.d("BaseActivity", "Testing doorbell alert on ${javaClass.simpleName}")
        // Use the force test method to bypass duplicate detection
        GlobalAlertManager.forceShowTestAlert(this)
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
