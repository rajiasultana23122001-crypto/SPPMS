package com.example.sppms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
        val btnLogin = findViewById<Button>(R.id.btnLoginSubmit)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val userId = auth.currentUser?.uid

                        if (userId == null) {
                            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->

                                if (document.exists()) {
                                    val role = document.getString("role")

                                    if (role == "parent") {
                                        Toast.makeText(this, "Parent Login Successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, ParentDashboardActivity::class.java))
                                        finish()
                                    } else if (role == "child") {
                                        Toast.makeText(this, "Child Login Successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, ChildDashboardActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "User data not found in Firestore", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Login Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}