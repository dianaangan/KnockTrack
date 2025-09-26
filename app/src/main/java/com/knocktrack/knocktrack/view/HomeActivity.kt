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
     * - Reads intent extras for the logged-in user
     * - Hands data to Presenter for processing
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    //jkhk
        initViews()
        initPresenter()
        setupListeners()

        // Read user data provided by previous screen (e.g., Login)
        val userName = intent.getStringExtra("user_name") ?: "User"
        val userEmail = intent.getStringExtra("user_email") ?: "user@example.com"
        val userPassword = intent.getStringExtra("user_password") ?: ""
        
        // Store in presenter so it can handle logout without re-reading intent
        presenter.currentUserName = userName
        presenter.currentUserEmail = userEmail
        presenter.currentUserPassword = userPassword
        
        // Ask Presenter to validate and format data, and update the View
        presenter.processUserData(userName, userEmail, userPassword)
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

    /** Navigates to Login without payload. */
    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    /** Navigates to Login and pre-fills known credentials. */
    override fun navigateToLogin(name: String, email: String, password: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("user_name", name)
        intent.putExtra("user_email", email)
        intent.putExtra("user_password", password)
        startActivity(intent)
        finish()
    }

    /** Detaches Presenter to avoid memory leaks. */
    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}


