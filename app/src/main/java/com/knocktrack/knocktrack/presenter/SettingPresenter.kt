package com.knocktrack.knocktrack.presenter

import android.content.Context
import com.knocktrack.knocktrack.model.SettingModel
import com.knocktrack.knocktrack.view.SettingView
import kotlinx.coroutines.*

/**
 * Presenter for the Settings screen.
 * Coordinates between View and Model:
 * - Loads device configuration
 * - Validates and saves device credentials
 * - Handles connection reset
 */
class SettingPresenter {
    private var view: SettingView? = null
    private val model = SettingModel()
    private var context: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Attaches the view and context to the presenter.
     */
    fun attachView(view: SettingView, context: Context) {
        this.view = view
        this.context = context
    }
    
    /**
     * Detaches the view to avoid memory leaks.
     */
    fun detachView() {
        this.view = null
        this.context = null
    }
    
    /**
     * Loads saved device configuration and displays it in the view.
     */
    fun loadDeviceConfiguration() {
        context?.let { ctx ->
            val (deviceId, authKey, isConnected) = model.loadDeviceConfiguration(ctx)
            view?.showDeviceConfiguration(deviceId, authKey, isConnected)
            view?.showConnectionStatus(isConnected)
            view?.enableFields(!isConnected)
        }
    }
    
    /**
     * Validates and saves device configuration.
     * @param deviceId Device ID entered by user
     * @param authKey Auth key entered by user
     */
    fun saveConfiguration(deviceId: String, authKey: String) {
        // Validate inputs
        if (!model.validateInputs(deviceId, authKey)) {
            view?.showError("Please enter both Device ID and Auth Key to connect to your doorbell")
            return
        }
        
        // Show loading state
        view?.showLoading(true)
        view?.enableFields(false)
        
        scope.launch {
            try {
                // Validate credentials with Firebase
                val isValid = withContext(Dispatchers.IO) {
                    model.validateDeviceCredentials(deviceId.trim(), authKey.trim())
                }
                
                if (isValid) {
                    // Save configuration
                    context?.let { ctx ->
                        model.saveDeviceConfiguration(ctx, deviceId.trim(), authKey.trim(), true)
                        view?.showConnectionStatus(true)
                        view?.enableFields(false)
                        view?.showSuccess("Connected to doorbell successfully!")
                        
                        android.util.Log.d("SettingPresenter", "Device configuration saved - ID: $deviceId")
                    }
                } else {
                    // Validation failed
                    view?.enableFields(true)
                    view?.showError("Invalid Device ID or Auth Key. Please check your credentials and try again.")
                    
                    android.util.Log.w("SettingPresenter", "Device validation failed - ID: $deviceId")
                }
            } catch (e: Exception) {
                // Error during validation
                view?.enableFields(true)
                view?.showError("Error validating credentials. Please check your internet connection and try again.")
                
                android.util.Log.e("SettingPresenter", "Error validating device credentials: ${e.message}")
                e.printStackTrace()
            } finally {
                view?.showLoading(false)
            }
        }
    }
    
    /**
     * Resets device configuration and clears connection.
     */
    fun resetConfiguration() {
        context?.let { ctx ->
            model.clearDeviceConfiguration(ctx)
            view?.showDeviceConfiguration("", "", false)
            view?.showConnectionStatus(false)
            view?.enableFields(true)
            view?.showSuccess("Doorbell connection reset successfully")
            
            android.util.Log.d("SettingPresenter", "Device configuration reset")
        }
    }
    
    /**
     * Gets the current device configuration.
     * @return Pair of (deviceId, authKey)
     */
    fun getDeviceConfiguration(): Pair<String, String> {
        return context?.let { ctx ->
            val (deviceId, authKey, _) = model.loadDeviceConfiguration(ctx)
            Pair(deviceId, authKey)
        } ?: Pair("", "")
    }
    
    /**
     * Cleans up resources.
     */
    fun cleanup() {
        scope.cancel()
    }
}

