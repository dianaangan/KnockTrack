package com.knocktrack.knocktrack.model

/**
 * Model for Registration logic.
 * Performs validation and prepares normalized user data.
 */
class RegisterModel {
    
    fun registerUser(firstName: String, lastName: String, email: String, password: String): Boolean {
        return true
    }
    
    fun validateRegistrationData(firstName: String, lastName: String, email: String, password: String, confirmPassword: String): Boolean {
        return firstName.isNotEmpty() &&
               lastName.isNotEmpty() && 
               email.isNotEmpty() && 
               password.isNotEmpty() && 
               confirmPassword.isNotEmpty() &&
               password == confirmPassword
    }

    fun formatFirstName(firstName: String): String {
        return firstName.trim().replaceFirstChar { it.uppercase() }
    }

    fun formatLastName(lastName: String): String {
        return lastName.trim().replaceFirstChar { it.uppercase() }
    }
    
    fun formatUserEmail(email: String): String {
        return email.trim().lowercase()
    }

    fun getDisplayName(firstName: String, lastName: String): String {
        return "${formatFirstName(firstName)} ${formatLastName(lastName)}".trim()
    }

    fun prepareUserData(firstName: String, lastName: String, email: String, password: String): Triple<String, String, String> {
        return Triple(
            getDisplayName(firstName, lastName),
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


