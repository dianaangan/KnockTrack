package com.knocktrack.knocktrack

interface RegisterView {
    fun showProgress()
    fun hideProgress()
    fun onRegisterSuccess(name: String, email: String, password: String)
    fun onRegisterFailed(message: String)
    fun showError(message: String)
    fun clearErrors()
    fun setNameError(error: String)
    fun setEmailError(error: String)
    fun setPasswordError(error: String)
    fun setConfirmPasswordError(error: String)
    fun navigateToLogin()
    fun navigateToLogin(name: String, email: String, password: String)
}
