package com.knocktrack.knocktrack.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.service.FirebaseAuthService

class LandingPageActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        val firebaseAuthService = FirebaseAuthService()
        if (firebaseAuthService.isUserSignedIn()) {
            // User is already logged in, navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // User is not logged in, show landing page
        setContentView(R.layout.activity_landing_page)

        val getStartedButton = findViewById<Button>(R.id.getStartedButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        getStartedButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
