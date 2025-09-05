package com.knocktrack.knocktrack

import android.util.Patterns

class LoginPresenter {
    private var view: LoginView? = null
    private val model = LoginModel()

    fun attachView(view: LoginView) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun login(email: String, password: String) {
        view?.clearErrors()
        
        // First, do comprehensive validation using model
        if (!model.validateLoginData(email, password)) {
            view?.showError("Please enter both email and password")
            return
        }
        
        // Then do detailed validation for specific error messages
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        
        view?.showProgress()
        
        if (model.authenticate(email.trim(), password)) {
            val userData = model.prepareUserData(email, password)
            view?.hideProgress()
            view?.onLoginSuccess("User", userData.first, userData.second)
        } else {
            view?.hideProgress()
            view?.onLoginFailed("Invalid email or password")
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                view?.showError("Email is required")
                view?.setEmailError("Please enter your email")
                false
            }
            !model.checkEmailFormat(email) -> {
                view?.showError("Please enter a valid email address")
                view?.setEmailError("Invalid email format")
                false
            }
            else -> true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                view?.showError("Password is required")
                view?.setPasswordError("Please enter your password")
                false
            }
            !model.checkPasswordNotEmpty(password) -> {
                view?.showError("Password is required")
                view?.setPasswordError("Please enter your password")
                false
            }
            else -> true
        }
    }

    fun goToRegister() {
        view?.navigateToRegister()
    }
}