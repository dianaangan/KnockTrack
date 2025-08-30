package com.knocktrack.knocktrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*

class RegisterActivity : Activity() {

    private lateinit var presenter: RegisterPresenter
    private lateinit var etEmail: EditText
    private lateinit var etName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

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
            navigateToLogin()
        }
    }

    fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun clearErrors() {
        etName.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null
    }

    fun setNameError(error: String) {
        etName.error = error
        etName.requestFocus()
    }

    fun setEmailError(error: String) {
        etEmail.error = error
        etEmail.requestFocus()
    }

    fun setPasswordError(error: String) {
        etPassword.error = error
        etPassword.requestFocus()
    }

    fun setConfirmPasswordError(error: String) {
        etConfirmPassword.error = error
        etConfirmPassword.requestFocus()
    }

    fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun navigateToLogin(email: String, name: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("user_email", email)
        intent.putExtra("user_name", name)
        startActivity(intent)
        finish()
    }

    fun navigateToLogin(email: String, name: String, password: String) {
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