package com.knocktrack.knocktrack.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Service class for Firebase Authentication and Database operations.
 * Handles user registration, login, and profile management.
 */
class FirebaseAuthService {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    
    /**
     * Registers a new user with Firebase Authentication.
     * @param email User's email address
     * @param password User's password
     * @param firstName User's first name
     * @param lastName User's last name
     * @return Result containing success status and user data or error message
     */
    suspend fun registerUser(
        email: String, 
        password: String, 
        firstName: String, 
        lastName: String
    ): Result<FirebaseUser> {
        return try {
            // For development/testing, you can disable reCAPTCHA in Firebase Console
            // Go to Authentication > Settings > Authorized domains
            // Add your domain or disable reCAPTCHA verification
            
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                // Save additional user data to Realtime Database
                val userData = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )
                
                database.child("users").child(user.uid).setValue(userData).await()
                Result.success(user)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            // Provide more helpful error messages
            val errorMessage = when {
                e.message?.contains("API key not valid") == true -> 
                    "Firebase configuration error. Please check your google-services.json file."
                e.message?.contains("network") == true -> 
                    "Network error. Please check your internet connection."
                e.message?.contains("email address is already in use") == true -> 
                    "This email is already registered."
                else -> e.message ?: "Registration failed"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Signs in an existing user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing success status and user data or error message
     */
    suspend fun signInUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Gets the currently signed-in user.
     * @return Current FirebaseUser or null if not signed in
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Checks if a user is currently signed in.
     * @return true if user is signed in, false otherwise
     */
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Gets user profile data from the database.
     * @param userId User's UID
     * @return Result containing user data or error message
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val snapshot = database.child("users").child(userId).get().await()
            val userData = snapshot.value as? Map<String, Any>
            
            if (userData != null) {
                Result.success(userData)
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
