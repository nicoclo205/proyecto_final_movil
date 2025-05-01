package com.example.projectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        
        // Initialize buttons
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        
        // Set click listeners
        buttonLogin.setOnClickListener {
            // Navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        
        buttonRegister.setOnClickListener {
            // Navigate to register screen
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}