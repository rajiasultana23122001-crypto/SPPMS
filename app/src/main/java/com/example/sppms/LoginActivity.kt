package com.example.sppms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)
        val tvLoginMessage = findViewById<TextView>(R.id.tvLoginMessage)

        val fromRegister = intent.getBooleanExtra("fromRegister", false)
        if (fromRegister) {
            tvLoginMessage.visibility = View.VISIBLE
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            val role = document.getString("role")

                            if (role == "parent") {
                                startActivity(Intent(this, ParentDashboardActivity::class.java))
                            } else {
                                startActivity(Intent(this, ChildDashboardActivity::class.java))
                            }

                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to load user role", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}