package com.knocktrack.knocktrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
class HomeActivity : Activity(), HomeView {

    private lateinit var presenter: HomePresenter
    private lateinit var tvWelcome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        // dasdasd
        initViews()
        initPresenter()
        setupListeners()

        val userName = intent.getStringExtra("user_name") ?: "User"
        val userEmail = intent.getStringExtra("user_email") ?: "user@example.com"
        val userPassword = intent.getStringExtra("user_password") ?: ""
        
        // Store data for logout
        presenter.currentUserName = userName
        presenter.currentUserEmail = userEmail
        presenter.currentUserPassword = userPassword
        
        // Process user data through presenter and model
        presenter.processUserData(userName, userEmail, userPassword)
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun initPresenter() {
        presenter = HomePresenter()
        presenter.attachView(this)
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            presenter.logout()
        }
    }

    // HomeView interface implementation
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

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun navigateToLogin(name: String, email: String, password: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("user_name", name)
        intent.putExtra("user_email", email)
        intent.putExtra("user_password", password)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}
