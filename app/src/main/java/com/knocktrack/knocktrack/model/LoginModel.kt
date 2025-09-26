package com.knocktrack.knocktrack.model

import com.knocktrack.knocktrack.service.FirebaseAuthService
import com.google.firebase.auth.FirebaseUser

/**
 * Model for Login domain logic.
 * Encapsulates validation and coordinates with Firebase Auth service.
 */
class LoginModel {
    
    private val firebaseAuthService = FirebaseAuthService()
    
    /**
     * Authenticates user with Firebase Authentication.
     * @param email User's email address
     * @param password User's password
     * @return Result containing FirebaseUser on success or Exception on failure
     */
    suspend fun authenticate(email: String, password: String): Result<FirebaseUser> {
        return firebaseAuthService.signInUser(email, password)
    }
    
    fun validateLoginData(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }
    
    fun formatUserEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    fun prepareUserData(email: String, password: String): Pair<String, String> {
        return Pair(
            formatUserEmail(email),
            password
        )
    }
    
    fun checkEmailFormat(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun checkPasswordNotEmpty(password: String): Boolean {
        return password.isNotEmpty()
    }
    
    fun validateCredentials(email: String, password: String): Boolean {
        return checkEmailFormat(email) && checkPasswordNotEmpty(password)
    }
}


