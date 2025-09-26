package com.knocktrack.knocktrack.presenter

import com.knocktrack.knocktrack.model.HomeModel
import com.knocktrack.knocktrack.view.HomeView

/**
 * Presenter for the Home screen.
 * Coordinates between View and Model:
 * - Validates and formats data via Model
 * - Pushes UI updates and navigation commands to View
 */
class HomePresenter {
    private var view: HomeView? = null
    private val model = HomeModel()
    var currentUserName: String? = null
    var currentUserEmail: String? = null
    var currentUserPassword: String? = null

    /** The View calls this when it's ready to receive updates. */
    fun attachView(view: HomeView) {
        this.view = view
    }

    /** The View calls this on destroy to avoid leaks. */
    fun detachView() {
        this.view = null
    }

    /**
     * Processes incoming user data:
     * - If valid, formats display values and updates the View
     * - If invalid, instructs View to navigate back to Login
     */
    fun processUserData(name: String, email: String, password: String) {
        if (model.validateUserData(name, email, password)) {
            val displayName = model.getUserDisplayName(name)
            val displayEmail = model.getUserDisplayEmail(email)
            view?.showUserData(displayName, displayEmail)
        } else {
            view?.navigateToLogin()
        }
    }

    /** Handles logout, forwarding pre-filled data to Login when available. */
    fun logout() {
        if (currentUserName != null && currentUserEmail != null && currentUserPassword != null) {
            val logoutData = model.prepareLogoutData(
                currentUserName!!, 
                currentUserEmail!!, 
                currentUserPassword!!
            )
            view?.onLogoutSuccess()
            view?.navigateToLogin(logoutData.first, logoutData.second, logoutData.third)
        } else {
            view?.navigateToLogin()
        }
    }
}


