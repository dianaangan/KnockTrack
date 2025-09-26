package com.knocktrack.knocktrack.model

/**
 * Model for Login domain logic.
 * Encapsulates validation and simple authentication checks.
 */
class LoginModel {
    
    fun authenticate(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
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


