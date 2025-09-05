package com.knocktrack.knocktrack

class RegisterModel {
    
    fun registerUser(name: String, email: String, password: String): Boolean {
        // Simple mock registration - in real app, this would save to database
        return true
    }
    
    fun validateRegistrationData(name: String, email: String, password: String, confirmPassword: String): Boolean {
        return name.isNotEmpty() && 
               email.isNotEmpty() && 
               password.isNotEmpty() && 
               confirmPassword.isNotEmpty() &&
               password == confirmPassword
    }
    
    fun formatUserName(name: String): String {
        return name.trim().replaceFirstChar { it.uppercase() }
    }
    
    fun formatUserEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    fun prepareUserData(name: String, email: String, password: String): Triple<String, String, String> {
        return Triple(
            formatUserName(name),
            formatUserEmail(email),
            password
        )
    }
    
    fun checkEmailFormat(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun checkPasswordStrength(password: String): Boolean {
        return password.length >= 6
    }
    
    fun checkNameFormat(name: String): Boolean {
        return name.trim().length >= 2 && name.matches(Regex("^[a-zA-Z\\s]+$"))
    }
}
