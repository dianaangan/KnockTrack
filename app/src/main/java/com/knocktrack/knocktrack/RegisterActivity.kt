package com.knocktrack.knocktrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
class RegisterActivity : Activity(), RegisterView {

    private lateinit var presenter: RegisterPresenter
    private lateinit var etEmail: EditText
    private lateinit var etName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initPresenter()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etName = findViewById(R.id.etName)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initPresenter() {
        presenter = RegisterPresenter()
        presenter.attachView(this)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            presenter.register(
                etEmail.text.toString(),
                etName.text.toString(),
                etPassword.text.toString(),
                etConfirmPassword.text.toString()
            )
        }

        tvLogin.setOnClickListener {
            presenter.goToLogin()
        }
    }

    // RegisterView interface implementation
    override fun showProgress() {
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false
    }

    override fun hideProgress() {
        progressBar.visibility = View.GONE
        btnRegister.isEnabled = true
    }

    override fun onRegisterSuccess(name: String, email: String, password: String) {
        Toast.makeText(this, "Registration successful! Welcome ${name}!", Toast.LENGTH_LONG).show()
        navigateToLogin(name, email, password)
    }

    override fun onRegisterFailed(message: String) {
        showError(message)
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun clearErrors() {
        etName.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null
    }

    override fun setNameError(error: String) {
        etName.error = error
        etName.requestFocus()
    }

    override fun setEmailError(error: String) {
        etEmail.error = error
        etEmail.requestFocus()
    }

    override fun setPasswordError(error: String) {
        etPassword.error = error
        etPassword.requestFocus()
    }

    override fun setConfirmPasswordError(error: String) {
        etConfirmPassword.error = error
        etConfirmPassword.requestFocus()
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