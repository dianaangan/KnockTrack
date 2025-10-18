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
        
        // Bottom navigation views
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navHistory = findViewById<LinearLayout>(R.id.navHistory)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)
        
        // Content buttons
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnMute = findViewById<Button>(R.id.btnMute)
        
        // Set up navigation listeners
        navHome.setOnClickListener {
            // Already on home, do nothing
        }
        
        navHistory.setOnClickListener {
            navigateToHistory()
        }
        
        navSettings.setOnClickListener {
            navigateToSettings()
        }
        
        btnHistory.setOnClickListener {
            navigateToHistory()
        }
        
        btnMute.setOnClickListener {
            // TODO: Implement mute functionality
            Toast.makeText(this, "Mute feature coming soon", Toast.LENGTH_SHORT).show()
        }
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
        // Extract name from welcome message (format: "Welcome, John Doe!")
        val nameOnly = name.replace("Welcome, ", "").replace("!", "").trim()
        val firstName = nameOnly.split(" ").firstOrNull() ?: "User"
        
        tvWelcome.text = "Welcome, $firstName"
        tvEmail.visibility = android.view.View.GONE
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
    
    /** Navigates to History screen. */
    private fun navigateToHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }
    
    /** Navigates to Settings screen. */
    private fun navigateToSettings() {
        startActivity(Intent(this, SettingActivity::class.java))
    }

    /** Detaches Presenter to avoid memory leaks. */
    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }


}


