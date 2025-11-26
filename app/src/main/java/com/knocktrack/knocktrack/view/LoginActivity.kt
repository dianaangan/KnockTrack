package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.presenter.LoginPresenter

/**
 * Login screen (View in MVP).
 * Handles UI rendering and user input; delegates logic to the Presenter.
 */
class LoginActivity : Activity(), LoginView {

    private lateinit var presenter: LoginPresenter
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var ivPasswordToggle: ImageView
    private var isPasswordVisible = false

    /** Sets up UI, presenter, listeners, and restores any passed state. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initPresenter()
        setupListeners()
        
        // If coming from logout, we may pre-fill the email
        val emailFromLogout = intent.getStringExtra("user_email")
        if (!emailFromLogout.isNullOrEmpty()) {
            etEmail.setText(emailFromLogout)
        }
    }

    /** Binds all required view references. */
    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)
    }

    /** Creates presenter and attaches this Activity as the View. */
    private fun initPresenter() {
        presenter = LoginPresenter()
        presenter.attachView(this)
    }

    /**
     * Routes UI interactions to Presenter methods.
     * - Login button -> presenter.login
     * - Register link -> presenter.goToRegister
     * - Password toggle icon shows/hides password
     */
    private fun setupListeners() {
        btnLogin.setOnClickListener {
            presenter.login(etEmail.text.toString(), etPassword.text.toString())
        }

        tvRegister.setOnClickListener {
            presenter.goToRegister()
        }

        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    /** Toggles password visibility for the password field. */
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_open)
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_closed)
        }
        // Move cursor to end
        etPassword.setSelection(etPassword.text.length)
    }

    /** Shows progress while the Presenter performs work. */
    override fun showProgress() {
        progressBar.visibility = View.VISIBLE
        btnLogin.text = ""  // Hide button text to show loading indicator
        btnLogin.isEnabled = false
    }

    /** Hides progress after the Presenter completes work. */
    override fun hideProgress() {
        progressBar.visibility = View.GONE
        btnLogin.text = "LOGIN"  // Restore button text
        btnLogin.isEnabled = true
    }

    /** Called by Presenter when login succeeds; navigates to Home. */
    override fun onLoginSuccess(name: String, email: String, password: String) {
        Toast.makeText(this, "Login successful! Welcome back!", Toast.LENGTH_SHORT).show()
        navigateToHome(name, email, password)
    }

    /** Presents a login failure message. */
    override fun onLoginFailed(message: String) {
        showError(message)
    }

    /** Generic error presenter for toasts. */
    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Clears any field-level validation errors. */
    override fun clearErrors() {
        etEmail.error = null
        etPassword.error = null
    }

    /** Highlights email field with a specific error. */
    override fun setEmailError(error: String) {
        etEmail.error = error
        etEmail.requestFocus()
    }

    /** Highlights password field with a specific error. */
    override fun setPasswordError(error: String) {
        etPassword.error = error
        etPassword.requestFocus()
    }

    /** Navigates to the registration screen. */
    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    /** Navigates to the home screen with user context. */
    override fun navigateToHome(name: String, email: String, password: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("user_name", name)
        intent.putExtra("user_email", email)
        intent.putExtra("user_password", password)
        startActivity(intent)
        finish()
    }

    /** Detaches presenter to prevent leaks. */
    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    /**
     * Disable back navigation to previous screens once we are on the login screen
     * (e.g., after logout). Keeps the user on Login until they authenticate again.
     */
    override fun onBackPressed() {
        // Do nothing to keep user on Login screen
    }
}


