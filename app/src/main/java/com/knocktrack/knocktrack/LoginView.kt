package com.knocktrack.knocktrack

interface LoginView {
    fun showProgress()
    fun hideProgress()
    fun onLoginSuccess(name: String, email: String, password: String)
    fun onLoginFailed(message: String)
    fun showError(message: String)
    fun clearErrors()
    fun setEmailError(error: String)
    fun setPasswordError(error: String)
    fun navigateToRegister()
    fun navigateToHome(name: String, email: String, password: String)
}
