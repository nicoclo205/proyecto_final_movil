package com.example.projectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class InfoActivity : AppCompatActivity() {

    lateinit var homeButton: ImageButton
    lateinit var orderButton: ImageButton
    lateinit var calculatorButton: ImageButton
    lateinit var reminderButton: ImageButton
    lateinit var buttonProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        homeButton = findViewById(R.id.homeButton)
        orderButton = findViewById(R.id.orderButton)
        calculatorButton = findViewById(R.id.calculatorButton)
        reminderButton = findViewById(R.id.recordsButton)
        buttonProfile = findViewById(R.id.buttonProfile)


        setupButtonListeners()

        }

    private fun setupButtonListeners() {

        homeButton.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
            finish()
        }

        orderButton.setOnClickListener {
            startActivity(Intent(this, OrderActivity::class.java))
            finish()
        }

        calculatorButton.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
            finish()
        }

        reminderButton.setOnClickListener {
            startActivity(Intent(this, ReminderActivity::class.java))
            finish()
        }
        
        buttonProfile.setOnClickListener {
            // Navegar a la actividad de perfil
            startActivity(Intent(this, ProfileActivity::class.java))
        }

    }

    }

