package com.knocktrack.knocktrack.view

/**
 * Contract for the Home screen View in MVP.
 * Implemented by `HomeActivity` and consumed by `HomePresenter`.
 */
interface HomeView {
    fun showUserData(name: String, email: String)
    fun onLogoutSuccess()
    fun onLogoutFailed(message: String)
    fun navigateToLogin()
    
    // Doorbell event methods
    fun showDoorbellPressed(time: String, date: String, timestamp: Long)
    fun showSystemStatus(status: String, isActive: Boolean)
    fun showRecentActivity(activities: List<String>)
    fun showNotConnectedState()
    fun showConnectionStatus(wifiConnected: Boolean, bluetoothConnected: Boolean)
    fun showDoorbellAnalytics(analytics: com.knocktrack.knocktrack.model.DoorbellAnalytics)
    fun showDeviceActiveStatus(isActive: Boolean)  // Show if ESP32 device is active/online
}


