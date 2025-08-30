package com.knocktrack.knocktrack

import android.util.Patterns

class LoginPresenter {
    private var view: LoginActivity? = null
    private var userEmail: String = ""
    private var userName: String = ""
    private var userPassword: String = ""

    fun attachView(view: LoginActivity) {
        this.view = view
        userEmail = view.intent.getStringExtra("user_email") ?: ""
        userName = view.intent.getStringExtra("user_name") ?: ""
        userPassword = view.intent.getStringExtra("user_password") ?: ""
    }

    fun detachView() {
        this.view = null
    }

    fun login(email: String, password: String) {
        view?.clearErrors()
        
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        
        if (validateCredentials(email, password)) {
            view?.showSuccess("Login successful!")
            view?.navigateToHome(userName, userEmail, userPassword)
        }
    }

    private fun validateEmail(email: String): Boolean {
        val trimmedEmail = email.trim()
        
        return when {
            trimmedEmail.isEmpty() -> {
                view?.showError("Email is required")
                view?.setEmailError("Please enter your email")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
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
            else -> true
        }
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        return when {
            userEmail.isEmpty() -> {
                view?.showError("User not registered. Please register first.")
                view?.setEmailError("User not found")
                view?.setPasswordError("Invalid credentials")
                false
            }
            email.trim() != userEmail -> {
                view?.showError("Invalid email or password")
                view?.setEmailError("User not found")
                view?.setPasswordError("Invalid credentials")
                false
            }
            password != userPassword -> {
                view?.showError("Invalid email or password")
                view?.setEmailError("User not found")
                view?.setPasswordError("Invalid credentials")
                false
            }
            else -> true
        }
    }

    fun goToRegister() {
        view?.navigateToRegister()
    }
}