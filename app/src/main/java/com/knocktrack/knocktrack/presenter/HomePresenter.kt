package com.knocktrack.knocktrack.presenter

import com.knocktrack.knocktrack.model.HomeModel
import com.knocktrack.knocktrack.view.HomeView
import com.google.firebase.auth.FirebaseUser

/**
 * Presenter for the Home screen.
 * Coordinates between View and Model:
 * - Gets user data from Firebase Auth
 * - Pushes UI updates and navigation commands to View
 */
class HomePresenter {
    private var view: HomeView? = null
    private val model = HomeModel()

    /** The View calls this when it's ready to receive updates. */
    fun attachView(view: HomeView) {
        this.view = view
    }

    /** The View calls this on destroy to avoid leaks. */
    fun detachView() {
        this.view = null
    }

    /**
     * Loads and displays current user data from authentication service.
     * - Checks if user is signed in
     * - Gets user profile from database
     * - Updates the View with user information
     */
    fun loadUserData() {
        val currentUser = model.getCurrentUser()
        
        if (currentUser == null || !model.isUserSignedIn()) {
            // User not signed in, navigate to login
            view?.navigateToLogin()
            return
        }
        
        // Get user ID and email from Firebase user
        val userId = currentUser.uid
        val userEmail = currentUser.email ?: ""
        val userDisplayName = currentUser.displayName ?: "User"
        
        // Get user profile data from database
        model.getUserProfile(userId) { result ->
            result.fold(
                onSuccess = { userData ->
                    // Extract user data from database
                    val firstName = userData["firstName"] as? String ?: ""
                    val lastName = userData["lastName"] as? String ?: ""
                    val email = userData["email"] as? String ?: userEmail
                    
                    // Format display data
                    val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        model.formatDisplayName(firstName, lastName)
                    } else {
                        userDisplayName
                    }
                    val welcomeMessage = model.getWelcomeMessage(displayName)
                    val emailDisplay = model.getEmailDisplay(email)
                    
                    // Update the view
                    view?.showUserData(welcomeMessage, emailDisplay)
                },
                onFailure = { exception ->
                    // If we can't get profile data, use basic user data
                    val welcomeMessage = model.getWelcomeMessage(userDisplayName)
                    val emailDisplay = model.getEmailDisplay(userEmail)
                    
                    view?.showUserData(welcomeMessage, emailDisplay)
                }
            )
        }
    }

    /**
     * Handles user logout.
     * - Signs out from authentication service
     * - Navigates to login screen
     */
    fun logout() {
        try {
            model.signOut()
            view?.onLogoutSuccess()
            view?.navigateToLogin()
        } catch (e: Exception) {
            view?.onLogoutFailed("Logout failed: ${e.message ?: "Unknown error"}")
        }
    }
}


