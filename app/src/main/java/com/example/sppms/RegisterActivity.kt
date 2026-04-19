package com.example.sppms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val radioGroupRole = findViewById<RadioGroup>(R.id.radioGroupRole)
        val rbParent = findViewById<RadioButton>(R.id.rbParent)
        val rbChild = findViewById<RadioButton>(R.id.rbChild)
        val parentLayout = findViewById<LinearLayout>(R.id.parentLayout)
        val childLayout = findViewById<LinearLayout>(R.id.childLayout)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val etNidNumber = findViewById<EditText>(R.id.etNidNumber)
        val etChildAge = findViewById<EditText>(R.id.etChildAge)
        val etParentEmail = findViewById<EditText>(R.id.etParentEmail)
        val btnRegister = findViewById<Button>(R.id.btnRegisterNow)

        radioGroupRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == rbParent.id) {
                parentLayout.visibility = View.VISIBLE
                childLayout.visibility = View.GONE
            } else if (checkedId == rbChild.id) {
                parentLayout.visibility = View.GONE
                childLayout.visibility = View.VISIBLE
            }
        }

        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val nidNumber = etNidNumber.text.toString().trim()
            val childAge = etChildAge.text.toString().trim()
            val parentEmail = etParentEmail.text.toString().trim().lowercase()

            val role = when {
                rbParent.isChecked -> "parent"
                rbChild.isChecked -> "child"
                else -> ""
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role.isEmpty()) {
                Toast.makeText(this, "Select Parent or Child", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "parent" && nidNumber.isEmpty()) {
                Toast.makeText(this, "Enter NID Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "child" && childAge.isEmpty()) {
                Toast.makeText(this, "Enter Child Age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "child" && parentEmail.isEmpty()) {
                Toast.makeText(this, "Enter Parent Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val userId = auth.currentUser!!.uid

                        val userMap = hashMapOf<String, Any>(
                            "name" to name,
                            "email" to email,
                            "role" to role,
                            "nidNumber" to nidNumber,
                            "childAge" to childAge
                        )

                        if (role == "child") {
                            userMap["parentEmail"] = parentEmail
                        }

                        db.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Register successful", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this, LoginActivity::class.java)
                                intent.putExtra("fromRegister", true)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Registration failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}