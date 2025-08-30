package com.knocktrack.knocktrack

import android.util.Patterns

class RegisterPresenter {
    private var view: RegisterActivity? = null

    fun attachView(view: RegisterActivity) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun register(email: String, name: String, password: String, confirmPassword: String) {
        view?.clearErrors()
        
        if (!validateName(name)) return
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return
        
        view?.showSuccess("Registration successful! Please login.")
        view?.navigateToLogin(email.trim(), name.trim(), password)
    }

    private fun validateName(name: String): Boolean {
        val trimmedName = name.trim()
        
        return when {
            trimmedName.isEmpty() -> {
                view?.showError("Name is required")
                view?.setNameError("Please enter your full name")
                false
            }
            trimmedName.length < 2 -> {
                view?.showError("Name must be at least 2 characters")
                view?.setNameError("Name too short")
                false
            }
            trimmedName.length > 50 -> {
                view?.showError("Name must be less than 50 characters")
                view?.setNameError("Name too long")
                false
            }
            !trimmedName.matches(Regex("^[a-zA-Z\\s]+$")) -> {
                view?.showError("Name can only contain letters and spaces")
                view?.setNameError("Invalid characters")
                false
            }
            else -> true
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
            trimmedEmail.length > 100 -> {
                view?.showError("Email is too long")
                view?.setEmailError("Email too long")
                false
            }
            else -> true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                view?.showError("Password is required")
                view?.setPasswordError("Please enter a password")
                false
            }
            password.length < 6 -> {
                view?.showError("Password must be at least 6 characters")
                view?.setPasswordError("Too short")
                false
            }
            password.length > 128 -> {
                view?.showError("Password is too long")
                view?.setPasswordError("Too long")
                false
            }
            else -> true
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return when {
            confirmPassword.isEmpty() -> {
                view?.showError("Please confirm your password")
                view?.setConfirmPasswordError("Confirm password required")
                false
            }
            password != confirmPassword -> {
                view?.showError("Passwords don't match")
                view?.setConfirmPasswordError("Passwords don't match")
                false
            }
            else -> true
        }
    }
}