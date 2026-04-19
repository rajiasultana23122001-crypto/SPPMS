package com.example.sppms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}