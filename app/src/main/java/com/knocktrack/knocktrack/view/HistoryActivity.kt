package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.knocktrack.knocktrack.R

class HistoryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        initViews()
    }
    
    private fun initViews() {
        // Back button
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
        
        // Clear history button
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)
        btnClearHistory.setOnClickListener {
            // TODO: Implement clear history functionality
            Toast.makeText(this, "Clear history feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Bottom navigation
        val navHome = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(0) as LinearLayout
        val navHistory = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(1) as LinearLayout
        val navSettings = findViewById<LinearLayout>(R.id.bottomNavigation).getChildAt(2) as LinearLayout
        
        navHome.setOnClickListener {
            navigateToHome()
        }
        
        navHistory.setOnClickListener {
            // Already on history, do nothing
        }
        
        navSettings.setOnClickListener {
            navigateToSettings()
        }
    }
    
    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
    
    private fun navigateToSettings() {
        startActivity(Intent(this, SettingActivity::class.java))
    }
}