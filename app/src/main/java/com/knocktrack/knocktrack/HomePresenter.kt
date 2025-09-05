package com.knocktrack.knocktrack

class HomePresenter {
    private var view: HomeView? = null
    private val model = HomeModel()
    var currentUserName: String? = null
    var currentUserEmail: String? = null
    var currentUserPassword: String? = null

    fun attachView(view: HomeView) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun processUserData(name: String, email: String, password: String) {
        if (model.validateUserData(name, email, password)) {
            val displayName = model.getUserDisplayName(name)
            val displayEmail = model.getUserDisplayEmail(email)
            view?.showUserData(displayName, displayEmail)
        } else {
            view?.navigateToLogin()
        }
    }

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