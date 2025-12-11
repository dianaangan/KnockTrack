package com.knocktrack.knocktrack.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.presenter.SettingPresenter

/**
 * Settings screen of the app (View in MVP).
 *
 * Responsibilities:
 * - Inflate and bind UI elements
 * - Forward user actions to the Presenter
 * - Render state received from the Presenter
 * - Perform navigation as instructed by the Presenter
 */
class SettingActivity : BaseActivity(), SettingView {
    
    private lateinit var presenter: SettingPresenter
    private lateinit var etDeviceId: EditText
    private lateinit var etAuthKey: EditText
    private lateinit var btnSaveConfig: Button
    private lateinit var btnReset: Button
    private lateinit var tvDeviceStatus: TextView
    
    /**
     * Android lifecycle: initializes the UI and wires MVP components.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        
        initViews()
        initPresenter()
        setupListeners()
        
        // Load device configuration
        presenter.loadDeviceConfiguration()
    }
    
    /** Binds views from the layout. */
    private fun initViews() {
        etDeviceId = findViewById(R.id.etDeviceId)
        etAuthKey = findViewById(R.id.etAuthKey)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)
        btnReset = findViewById(R.id.btnReset)
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus)
    }
    
    /** Creates the Presenter and attaches this Activity as its View. */
    private fun initPresenter() {
        presenter = SettingPresenter()
        presenter.attachView(this, this)
    }
    
    /** Wires user interactions to Presenter actions. */
    private fun setupListeners() {
        // Back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
        
        // Save configuration button
        btnSaveConfig.setOnClickListener {
            val deviceId = etDeviceId.text.toString().trim()
            val authKey = etAuthKey.text.toString().trim()
            presenter.saveConfiguration(deviceId, authKey)
        }
        
        // Reset button
        btnReset.setOnClickListener {
            presenter.resetConfiguration()
        }
        
        // Bottom navigation
        val bottomNav = findViewById<LinearLayout>(R.id.bottomNavigation)
        val navHome = bottomNav.getChildAt(0) as LinearLayout
        val navHistory = bottomNav.getChildAt(1) as LinearLayout
        val navSettings = bottomNav.getChildAt(2) as LinearLayout
        
        navHome.setOnClickListener {
            navigateToHome()
        }
        
        navHistory.setOnClickListener {
            navigateToHistory()
        }
        
        navSettings.setOnClickListener {
            // Already on settings, do nothing
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Force refresh the listener to ensure it's working properly
        com.knocktrack.knocktrack.utils.GlobalFirebaseListener.forceRefresh(this)
        
        // Add test gesture for Settings screen
        // Long press anywhere on the screen to test alerts
        findViewById<android.view.View>(android.R.id.content).setOnLongClickListener {
            testDoorbellAlert()
            true
        }
        
        // Reload configuration when resuming
        presenter.loadDeviceConfiguration()
    }
    
    /** Detaches Presenter to avoid memory leaks. */
    override fun onDestroy() {
        presenter.cleanup()
        presenter.detachView()
        super.onDestroy()
    }
    
    /** Override from BaseActivity - handles global alert navigation */
    override fun navigateToHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }
    
    // SettingView implementation
    
    override fun showLoading(show: Boolean) {
        btnSaveConfig.isEnabled = !show
        btnSaveConfig.text = if (show) "Validating..." else "Connect to Doorbell"
        btnSaveConfig.alpha = if (show) 0.6f else 1.0f
    }
    
    override fun showDeviceConfiguration(deviceId: String, authKey: String, isConnected: Boolean) {
        etDeviceId.setText(deviceId)
        etAuthKey.setText(authKey)
        showConnectionStatus(isConnected)
        enableFields(!isConnected)
        
        // Update button text based on connection status
        btnSaveConfig.text = if (isConnected) "Connected" else "Connect to Doorbell"
        btnReset.visibility = if (isConnected) android.view.View.VISIBLE else android.view.View.GONE
    }
    
    override fun showConnectionStatus(connected: Boolean) {
        if (connected) {
            tvDeviceStatus.text = "Connected ✓"
            tvDeviceStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        } else {
            tvDeviceStatus.text = "Not Connected"
            tvDeviceStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        }
    }
    
    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun enableFields(enabled: Boolean) {
        val textColor = if (enabled) {
            resources.getColor(android.R.color.black)
        } else {
            resources.getColor(android.R.color.darker_gray)
        }
        
        val backgroundRes = if (enabled) {
            R.drawable.edittext_border
        } else {
            R.drawable.edittext_border_grey
        }
        
        etDeviceId.isEnabled = enabled
        etDeviceId.isFocusable = enabled
        etDeviceId.isFocusableInTouchMode = enabled
        etDeviceId.isClickable = enabled
        etDeviceId.setTextColor(textColor)
        etDeviceId.setBackgroundResource(backgroundRes)
        
        etAuthKey.isEnabled = enabled
        etAuthKey.isFocusable = enabled
        etAuthKey.isFocusableInTouchMode = enabled
        etAuthKey.isClickable = enabled
        etAuthKey.setTextColor(textColor)
        etAuthKey.setBackgroundResource(backgroundRes)
        
        btnSaveConfig.isEnabled = enabled
        btnSaveConfig.alpha = if (enabled) 1.0f else 0.6f
    }
    
    override fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}



