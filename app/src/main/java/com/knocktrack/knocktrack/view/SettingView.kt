package com.knocktrack.knocktrack.view

/**
 * Contract for the Settings screen View in MVP.
 * Implemented by `SettingActivity` and consumed by `SettingPresenter`.
 */
interface SettingView {
    fun showLoading(show: Boolean)
    fun showDeviceConfiguration(deviceId: String, authKey: String, isConnected: Boolean)
    fun showConnectionStatus(connected: Boolean)
    fun showSuccess(message: String)
    fun showError(message: String)
    fun enableFields(enabled: Boolean)
    fun navigateToHome()
    fun navigateToHistory()
}

