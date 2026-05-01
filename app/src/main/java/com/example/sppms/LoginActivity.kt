package com.example.sppms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)
        val btnGoogleLogin = findViewById<View>(R.id.btnGoogleLogin)

        val fromRegister = intent.getBooleanExtra("fromRegister", false)
        val registeredEmail = intent.getStringExtra("registeredEmail") ?: ""

        if (fromRegister && registeredEmail.isNotEmpty()) {
            etEmail.setText(registeredEmail)
            etPassword.requestFocus()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnGoogleLogin.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: run {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Login"
                        Toast.makeText(this, "Login failed. Try again.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    navigateByRole(uid)
                }
                .addOnFailureListener { exception ->
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    val errorMsg = when (exception) {
                        is FirebaseAuthInvalidUserException ->
                            "No account found with this email. Please register first."
                        is FirebaseAuthInvalidCredentialsException ->
                            "Wrong password. Please try again."
                        else -> "Login failed: ${exception.message}"
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    checkUserRegistration(user.uid, user.email ?: "", user.displayName ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserRegistration(uid: String, email: String, name: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    navigateByRole(uid)
                } else {
                    Toast.makeText(this, "User profile not found. Please register.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, RegisterActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("name", name)
                    startActivity(intent)
                    auth.signOut()
                }
            }
    }

    private fun navigateByRole(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    return@addOnSuccessListener
                }
                val role = document.getString("role")
                if (role == "parent") {
                    startActivity(Intent(this, ParentDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, ChildDashboardActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user profile.", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
    }
}
