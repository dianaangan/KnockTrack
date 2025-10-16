package com.knocktrack.knocktrack.view

/**
 * Contract for the Register screen View in MVP.
 * Implemented by `RegisterActivity` and used by `RegisterPresenter`.
 */
interface RegisterView {
    fun showProgress()
    fun hideProgress()
    fun onRegisterSuccess(email: String, password: String)
    fun onRegisterFailed(message: String)
    fun showError(message: String)
    fun clearErrors()
    fun setFirstNameError(error: String)
    fun setLastNameError(error: String)
    fun setEmailError(error: String)
    fun setPasswordError(error: String)
    fun setConfirmPasswordError(error: String)
    fun navigateToLogin()
    fun navigateToLogin(email: String, password: String)
}


