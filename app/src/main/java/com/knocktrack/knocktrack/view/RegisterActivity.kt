package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.presenter.RegisterPresenter

/**
 * Registration screen (View in MVP).
 * Displays input fields and delegates validation and submission to Presenter.
 */
class RegisterActivity : Activity(), RegisterView {

    private lateinit var presenter: RegisterPresenter
    private lateinit var etEmail: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    /** Initializes UI, presenter, and interaction handlers. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initPresenter()
        setupListeners()
    }

    /** Binds view references. */
    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    /** Creates presenter and attaches this Activity as View. */
    private fun initPresenter() {
        presenter = RegisterPresenter()
        presenter.attachView(this)
    }

    /**
     * Wires clicks to Presenter actions:
     * - Register button submits the form
     * - Login link navigates back to Login
     */
    private fun setupListeners() {
        btnRegister.setOnClickListener {
            presenter.register(
                etEmail.text.toString(),
                etFirstName.text.toString(),
                etLastName.text.toString(),
                etPassword.text.toString(),
                etConfirmPassword.text.toString()
            )
        }

        tvLogin.setOnClickListener {
            presenter.goToLogin()
        }
    }

    /** Shows loading during registration. */
    override fun showProgress() {
        progressBar.visibility = View.VISIBLE
        btnRegister.text = ""  // Hide button text to show loading indicator
        btnRegister.isEnabled = false
    }

    /** Hides loading after registration completes. */
    override fun hideProgress() {
        progressBar.visibility = View.GONE
        btnRegister.text = "Register"  // Restore button text
        btnRegister.isEnabled = true
    }

    /** Called by Presenter on success; navigates to Login with prefill (email only). */
    override fun onRegisterSuccess(email: String, password: String) {
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
        navigateToLogin(email, password)
    }

    /** Presents a failure message. */
    override fun onRegisterFailed(message: String) {
        showError(message)
    }

    /** Generic error toast. */
    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Clears all field-level errors. */
    override fun clearErrors() {
        etFirstName.error = null
        etLastName.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null
    }

    /** Highlights first name field error. */
    override fun setFirstNameError(error: String) {
        etFirstName.error = error
        etFirstName.requestFocus()
    }

    /** Highlights last name field error. */
    override fun setLastNameError(error: String) {
        etLastName.error = error
        etLastName.requestFocus()
    }

    /** Highlights email field error. */
    override fun setEmailError(error: String) {
        etEmail.error = error
        etEmail.requestFocus()
    }

    /** Highlights password field error. */
    override fun setPasswordError(error: String) {
        etPassword.error = error
        etPassword.requestFocus()
    }

    /** Highlights confirm password field error. */
    override fun setConfirmPasswordError(error: String) {
        etConfirmPassword.error = error
        etConfirmPassword.requestFocus()
    }

    /** Navigates back to Login without data. */
    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    /** Navigates to Login, pre-filling successful registration details (email). */
    override fun navigateToLogin(email: String, password: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("user_email", email)
        intent.putExtra("user_password", password)
        startActivity(intent)
        finish()
    }

    /** Detach presenter to avoid leaks. */
    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}


