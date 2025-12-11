package com.knocktrack.knocktrack.model

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Model for Settings screen business logic and data operations.
 * Handles device configuration persistence and Firebase validation.
 */
class SettingModel {
    
    /**
     * Gets the current user's email for account-specific storage.
     */
    private fun getUserEmail(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.email ?: "default"
    }
    
    /**
     * Gets SharedPreferences instance for device configuration.
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        val userEmail = getUserEmail()
        return context.getSharedPreferences("device_config_$userEmail", Context.MODE_PRIVATE)
    }
    
    /**
     * Loads saved device configuration from SharedPreferences.
     * @return Triple of (deviceId, authKey, isConnected)
     */
    fun loadDeviceConfiguration(context: Context): Triple<String, String, Boolean> {
        val prefs = getSharedPreferences(context)
        val deviceId = prefs.getString("device_id", "") ?: ""
        val authKey = prefs.getString("auth_key", "") ?: ""
        val isConnected = prefs.getBoolean("device_connected", false)
        return Triple(deviceId, authKey, isConnected)
    }
    
    /**
     * Saves device configuration to SharedPreferences.
     * @param deviceId Device ID to save
     * @param authKey Auth key to save
     * @param isConnected Connection status
     */
    fun saveDeviceConfiguration(context: Context, deviceId: String, authKey: String, isConnected: Boolean) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        editor.putString("device_id", deviceId)
        editor.putString("auth_key", authKey)
        editor.putBoolean("device_connected", isConnected)
        editor.apply()
    }
    
    /**
     * Validates device ID and auth key against Firebase database.
     * Reads from: devices/{deviceId}/auth_key
     * 
     * @param deviceId The device ID to validate
     * @param authKey The auth key to validate
     * @return true if device exists and auth key matches, false otherwise
     */
    suspend fun validateDeviceCredentials(deviceId: String, authKey: String): Boolean {
        return try {
            val database = FirebaseDatabase.getInstance().reference
            val deviceRef = database.child("devices").child(deviceId)
            
            // Check if device exists in Firebase backend
            val snapshot = deviceRef.get().await()
            
            if (!snapshot.exists()) {
                android.util.Log.w("SettingModel", "Device ID does not exist in Firebase backend: $deviceId")
                return false
            }
            
            // Read auth_key from: devices/{deviceId}/auth_key
            val storedAuthKey = snapshot.child("auth_key").getValue(String::class.java)
            
            if (storedAuthKey == null || storedAuthKey != authKey) {
                android.util.Log.w("SettingModel", "Auth key mismatch for device: $deviceId")
                return false
            }
            
            android.util.Log.d("SettingModel", "Device credentials validated successfully: $deviceId")
            true
        } catch (e: Exception) {
            android.util.Log.e("SettingModel", "Error validating device credentials: ${e.message}")
            false
        }
    }
    
    /**
     * Clears device configuration from SharedPreferences.
     */
    fun clearDeviceConfiguration(context: Context) {
        val prefs = getSharedPreferences(context)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
    
    /**
     * Validates that device ID and auth key are not empty.
     */
    fun validateInputs(deviceId: String, authKey: String): Boolean {
        return deviceId.isNotBlank() && authKey.isNotBlank()
    }
}

