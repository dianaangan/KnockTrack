package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.presenter.HomePresenter

/**
 * Home screen of the app (View in MVP).
 *
 * Responsibilities:
 * - Inflate and bind UI elements
 * - Forward user actions to the Presenter
 * - Render state received from the Presenter
 * - Perform navigation as instructed by the Presenter
 */
class HomeActivity : Activity(), HomeView {

    private lateinit var presenter: HomePresenter
    private lateinit var tvWelcome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    /**
     * Android lifecycle: initializes the UI and wires MVP components.
     * - Loads user data from Firebase Authentication
     * - Displays user information or navigates to login if not authenticated
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        initPresenter()
        setupListeners()

        // Load user data from Firebase instead of intent extras
        presenter.loadUserData()
    }

    /** Binds views from the layout. */
    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogout = findViewById(R.id.btnLogout)
    }

    /** Creates the Presenter and attaches this Activity as its View. */
    private fun initPresenter() {
        presenter = HomePresenter()
        presenter.attachView(this)
    }

    /** Wires user interactions to Presenter actions. */
    private fun setupListeners() {
        btnLogout.setOnClickListener {
            presenter.logout()
        }
    }

    /** Renders the formatted user data from the Presenter. */
    override fun showUserData(name: String, email: String) {
        tvWelcome.text = "Welcome, $name!"
        tvEmail.text = "Email: $email"
    }

    override fun onLogoutSuccess() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onLogoutFailed(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Navigates to Login screen. */
    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    /** Detaches Presenter to avoid memory leaks. */
    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }


}


