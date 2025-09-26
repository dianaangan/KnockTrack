package com.knocktrack.knocktrack.presenter

import com.knocktrack.knocktrack.model.RegisterModel
import com.knocktrack.knocktrack.view.RegisterView

/**
 * Presenter for the Registration screen.
 * Validates field-level inputs and coordinates registration via Model.
 */
class RegisterPresenter {
    private var view: RegisterView? = null
    private val model = RegisterModel()

    /** Called by View to start receiving updates. */
    fun attachView(view: RegisterView) {
        this.view = view
    }

    /** Called by View on destroy to avoid leaks. */
    fun detachView() {
        this.view = null
    }

    /**
     * Main registration flow:
     * 1) Clear previous errors
     * 2) Validate presence and consistency
     * 3) Field-specific validation (name/email/password)
     * 4) Mock registration
     * 5) Notify View and navigate
     */
    fun register(email: String, firstName: String, lastName: String, password: String, confirmPassword: String) {
        view?.clearErrors()
        
        if (!model.validateRegistrationData(firstName, lastName, email, password, confirmPassword)) {
            view?.showError("Please fill in all fields and ensure passwords match")
            return
        }
        
        if (!validateFirstName(firstName)) return
        if (!validateLastName(lastName)) return
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return
        
        view?.showProgress()
        
        if (model.registerUser(firstName.trim(), lastName.trim(), email.trim(), password)) {
            val userData = model.prepareUserData(firstName, lastName, email, password)
            view?.hideProgress()
            // Use email and password only for login
            view?.onRegisterSuccess(userData.second, userData.third)
        } else {
            view?.hideProgress()
            view?.onRegisterFailed("Registration failed")
        }
    }

    /** Validates first name and surfaces errors to the View. */
    private fun validateFirstName(firstName: String): Boolean {
        return when {
            firstName.isEmpty() -> {
                view?.showError("First name is required")
                view?.setFirstNameError("Please enter your first name")
                false
            }
            !model.checkNameFormat(firstName) -> {
                view?.showError("First name must be at least 2 letters")
                view?.setFirstNameError("Invalid first name")
                false
            }
            firstName.length > 50 -> {
                view?.showError("First name must be less than 50 characters")
                view?.setFirstNameError("First name too long")
                false
            }
            else -> true
        }
    }

    /** Validates last name and surfaces errors to the View. */
    private fun validateLastName(lastName: String): Boolean {
        return when {
            lastName.isEmpty() -> {
                view?.showError("Last name is required")
                view?.setLastNameError("Please enter your last name")
                false
            }
            !model.checkNameFormat(lastName) -> {
                view?.showError("Last name must be at least 2 letters")
                view?.setLastNameError("Invalid last name")
                false
            }
            lastName.length > 50 -> {
                view?.showError("Last name must be less than 50 characters")
                view?.setLastNameError("Last name too long")
                false
            }
            else -> true
        }
    }

    /** Validates email and surfaces errors to the View. */
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

    /** Validates password and surfaces errors to the View. */
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

    /** Validates confirm-password and surfaces errors to the View. */
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

    /** Instructs View to navigate back to Login. */
    fun goToLogin() {
        view?.navigateToLogin()
    }
}


