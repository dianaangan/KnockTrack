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
    fun navigateToLogin(name: String, email: String, password: String)
}


