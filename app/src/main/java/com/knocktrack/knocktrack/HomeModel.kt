package com.knocktrack.knocktrack

class HomeModel {
    
    fun validateUserData(name: String, email: String, password: String): Boolean {
        return name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
    }
    
    fun formatUserName(name: String): String {
        return name.trim().replaceFirstChar { it.uppercase() }
    }
    
    fun formatUserEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    fun getUserDisplayName(name: String): String {
        return "Welcome, ${formatUserName(name)}!"
    }
    
    fun getUserDisplayEmail(email: String): String {
        return "Email: ${formatUserEmail(email)}"
    }
    
    fun prepareLogoutData(name: String, email: String, password: String): Triple<String, String, String> {
        return Triple(
            formatUserName(name),
            formatUserEmail(email),
            password
        )
    }
}
