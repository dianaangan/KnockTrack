package com.knocktrack.knocktrack

class HomePresenter {
    private var view: HomeActivity? = null
    private var userName: String = ""
    private var userEmail: String = ""
    private var userPassword: String = ""

    fun attachView(view: HomeActivity) {
        this.view = view
        userName = view.intent.getStringExtra("user_name") ?: "User"
        userEmail = view.intent.getStringExtra("user_email") ?: "user@example.com"
        userPassword = view.intent.getStringExtra("user_password") ?: ""
    }

    fun detachView() {
        this.view = null
    }

    fun loadUserData() {
        if (validateUserData()) {
            view?.showUserData(userName, userEmail)
        } else {
            view?.navigateToLogin()
        }
    }

    private fun validateUserData(): Boolean {
        return userName.isNotEmpty() && userEmail.isNotEmpty() && userPassword.isNotEmpty()
    }

    fun logout() {
        if (validateUserData()) {
            view?.navigateToLogin(userName, userEmail, userPassword)
        } else {
            view?.navigateToLogin()
        }
    }
}