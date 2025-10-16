package com.knocktrack.knocktrack.model

import com.knocktrack.knocktrack.service.FirebaseAuthService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Model for Home screen business logic and formatting.
 * Coordinates with Firebase Auth service for user data.
 */
class HomeModel {
    
    private val firebaseAuthService = FirebaseAuthService()
    
    /**
     * Gets the current Firebase user.
     * @return Current FirebaseUser or null if not signed in
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuthService.getCurrentUser()
    }
    
    /**
     * Checks if a user is currently signed in.
     * @return true if user is signed in, false otherwise
     */
    fun isUserSignedIn(): Boolean {
        return firebaseAuthService.isUserSignedIn()
    }
    
    /**
     * Gets user profile data from Firebase Database.
     * @param userId User's UID
     * @param callback Callback to handle the result
     */
    fun getUserProfile(userId: String, callback: (Result<Map<String, Any>>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = firebaseAuthService.getUserProfile(userId)
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
    
    /**
     * Signs out the current user.
     */
    fun signOut() {
        firebaseAuthService.signOut()
    }
    
    /**
     * Formats a display name from first and last name.
     * @param firstName User's first name
     * @param lastName User's last name
     * @return Formatted display name
     */
    fun formatDisplayName(firstName: String, lastName: String): String {
        val formattedFirst = firstName.trim().replaceFirstChar { it.uppercase() }
        val formattedLast = lastName.trim().replaceFirstChar { it.uppercase() }
        return "$formattedFirst $formattedLast"
    }
    
    /**
     * Formats email for display.
     * @param email User's email
     * @return Formatted email
     */
    fun formatUserEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    /**
     * Creates a welcome message with the user's name.
     * @param displayName User's formatted display name
     * @return Welcome message
     */
    fun getWelcomeMessage(displayName: String): String {
        return "Welcome, $displayName!"
    }
    
    /**
     * Creates an email display string.
     * @param email User's email
     * @return Email display string
     */
    fun getEmailDisplay(email: String): String {
        return "Email: ${formatUserEmail(email)}"
    }
}


