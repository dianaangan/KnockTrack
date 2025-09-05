package com.knocktrack.knocktrack

interface HomeView {
    fun showUserData(name: String, email: String)
    fun onLogoutSuccess()
    fun onLogoutFailed(message: String)
    fun navigateToLogin()
    fun navigateToLogin(name: String, email: String, password: String)
}
