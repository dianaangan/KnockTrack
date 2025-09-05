package com.knocktrack.knocktrack

import android.util.Patterns

class RegisterPresenter {
    private var view: RegisterView? = null
    private val model = RegisterModel()

    fun attachView(view: RegisterView) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun register(email: String, name: String, password: String, confirmPassword: String) {
        view?.clearErrors()
        
        // First, do comprehensive validation using model
        if (!model.validateRegistrationData(name, email, password, confirmPassword)) {
            view?.showError("Please fill in all fields and ensure passwords match")
            return
        }
        
        // Then do detailed validation for specific error messages
        if (!validateName(name)) return
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return
        
        view?.showProgress()
        
        if (model.registerUser(name.trim(), email.trim(), password)) {
            val userData = model.prepareUserData(name, email, password)
            view?.hideProgress()
            view?.onRegisterSuccess(userData.first, userData.second, userData.third)
        } else {
            view?.hideProgress()
            view?.onRegisterFailed("Registration failed")
        }
    }

    private fun validateName(name: String): Boolean {
        return when {
            name.isEmpty() -> {
                view?.showError("Name is required")
                view?.setNameError("Please enter your full name")
                false
            }
            !model.checkNameFormat(name) -> {
                view?.showError("Name must be at least 2 characters and contain only letters")
                view?.setNameError("Invalid name format")
                false
            }
            name.length > 50 -> {
                view?.showError("Name must be less than 50 characters")
                view?.setNameError("Name too long")
                false
            }
            else -> true
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
            email.length > 100 -> {
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
            !model.checkPasswordStrength(password) -> {
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

    fun goToLogin() {
        view?.navigateToLogin()
    }
}