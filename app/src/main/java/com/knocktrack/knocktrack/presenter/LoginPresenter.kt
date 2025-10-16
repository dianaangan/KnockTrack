package com.knocktrack.knocktrack.presenter

import com.knocktrack.knocktrack.model.LoginModel
import com.knocktrack.knocktrack.view.LoginView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Presenter for the Login screen.
 * Orchestrates validation and authentication using Model; updates View.
 */
class LoginPresenter {
    private var view: LoginView? = null
    private val model = LoginModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /** Called by View to start receiving updates. */
    fun attachView(view: LoginView) {
        this.view = view
    }

    /** Called by View on teardown to avoid leaks. */
    fun detachView() {
        this.view = null
    }

    /**
     * Main login flow:
     * 1) Clear previous errors
     * 2) Validate presence of fields
     * 3) Validate specific field formats
     * 4) Firebase authentication
     * 5) Notify View of success/failure
     */
    fun login(email: String, password: String) {
        view?.clearErrors()
        
        if (!model.validateLoginData(email, password)) {
            view?.showError("Please enter both email and password")
            return
        }
        
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        
        view?.showProgress()
        
        // Perform Firebase authentication asynchronously
        coroutineScope.launch {
            try {
                val result = model.authenticate(email.trim(), password)
                
                withContext(Dispatchers.Main) {
                    view?.hideProgress()
                    
                    result.fold(
                        onSuccess = { firebaseUser ->
                            // Login successful
                            val userData = model.prepareUserData(email, password)
                            view?.onLoginSuccess("User", userData.first, userData.second)
                        },
                        onFailure = { exception ->
                            // Login failed
                            val errorMessage = when {
                                exception.message?.contains("user not found") == true -> 
                                    "No account found with this email. Please check your email or register."
                                exception.message?.contains("wrong password") == true -> 
                                    "Incorrect password. Please try again."
                                exception.message?.contains("network") == true -> 
                                    "Network error. Please check your internet connection."
                                else -> 
                                    "Login failed: ${exception.message ?: "Invalid email or password"}"
                            }
                            view?.onLoginFailed(errorMessage)
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view?.hideProgress()
                    view?.onLoginFailed("Login failed: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    /** Validates email and reports user-friendly errors to the View. */
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

    /** Validates password presence and reports errors to the View. */
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

    /** Instructs View to navigate to Registration. */
    fun goToRegister() {
        view?.navigateToRegister()
    }
}


