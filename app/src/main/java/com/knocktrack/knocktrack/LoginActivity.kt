package com.knocktrack.knocktrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
class LoginActivity : Activity(), LoginView {

    private lateinit var presenter: LoginPresenter
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initPresenter()
        setupListeners()
        
        val emailFromLogout = intent.getStringExtra("user_email")
        if (!emailFromLogout.isNullOrEmpty()) {
            etEmail.setText(emailFromLogout)
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initPresenter() {
        presenter = LoginPresenter()
        presenter.attachView(this)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            presenter.login(etEmail.text.toString(), etPassword.text.toString())
        }

        tvRegister.setOnClickListener {
            presenter.goToRegister()
        }
    }

    // LoginView interface implementation
    override fun showProgress() {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
    }

    override fun hideProgress() {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
    }

    override fun onLoginSuccess(name: String, email: String, password: String) {
        Toast.makeText(this, "Login successful! Welcome back!", Toast.LENGTH_SHORT).show()
        navigateToHome(name, email, password)
    }

    override fun onLoginFailed(message: String) {
        showError(message)
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun clearErrors() {
        etEmail.error = null
        etPassword.error = null
    }

    override fun setEmailError(error: String) {
        etEmail.error = error
        etEmail.requestFocus()
    }

    override fun setPasswordError(error: String) {
        etPassword.error = error
        etPassword.requestFocus()
    }

    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    override fun navigateToHome(name: String, email: String, password: String) {
        val intent = Intent(this, HomeActivity::class.java)
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