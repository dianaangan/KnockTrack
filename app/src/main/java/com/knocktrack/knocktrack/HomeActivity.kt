package com.knocktrack.knocktrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*

class HomeActivity : Activity() {

    private lateinit var presenter: HomePresenter
    private lateinit var tvWelcome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        initPresenter()
        setupListeners()

        presenter.loadUserData()
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

    fun showUserData(name: String, email: String) {
        tvWelcome.text = "Welcome, $name!"
        tvEmail.text = "Email: $email"
    }

    fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun navigateToLogin(name: String, email: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("user_email", email)
        intent.putExtra("user_name", name)
        startActivity(intent)
        finish()
    }

    fun navigateToLogin(name: String, email: String, password: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("user_email", email)
        intent.putExtra("user_name", name)
        intent.putExtra("user_password", password)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}
