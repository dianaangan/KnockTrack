package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.knocktrack.knocktrack.R

class SettingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        
        initViews()
    }
    
    private fun initViews() {
        // Back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
        
        // Notification settings
        val cbEnableNotifications = findViewById<CheckBox>(R.id.cbEnableNotifications)
        val rgNotificationType = findViewById<RadioGroup>(R.id.rgNotificationType)
        val rbSound = findViewById<RadioButton>(R.id.rbSound)
        val rbVibration = findViewById<RadioButton>(R.id.rbVibration)
        val rbBoth = findViewById<RadioButton>(R.id.rbBoth)
        
        cbEnableNotifications.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement notification enable/disable functionality
            if (isChecked) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        rgNotificationType.setOnCheckedChangeListener { _, checkedId ->
            // TODO: Implement notification type change functionality
            when (checkedId) {
                R.id.rbSound -> Toast.makeText(this, "Sound notifications selected", Toast.LENGTH_SHORT).show()
                R.id.rbVibration -> Toast.makeText(this, "Vibration notifications selected", Toast.LENGTH_SHORT).show()
                R.id.rbBoth -> Toast.makeText(this, "Both sound and vibration selected", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Bottom navigation
        val navHome = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(0) as LinearLayout
        val navHistory = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(1) as LinearLayout
        val navSettings = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(2) as LinearLayout
        
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
    
    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
    
    private fun navigateToHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }
}