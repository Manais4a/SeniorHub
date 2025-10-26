package com.seniorhub.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.seniorhub.R

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        // Use MaterialButton instead of Button since that's what's in your XML
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        val btnCreateAccount = findViewById<MaterialButton>(R.id.btnCreateAccount)

        btnSignIn.setOnClickListener {
            val intent = Intent(this@LandingActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this@LandingActivity, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}