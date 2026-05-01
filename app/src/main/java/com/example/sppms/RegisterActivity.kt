package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

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
                Toast.makeText(this, "Google register failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
        val btnGoogleRegister = findViewById<View>(R.id.btnGoogleRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)

        // Initial state for role buttons
        rbParent.setBackgroundResource(R.drawable.bg_chip_purple)
        rbChild.setBackgroundResource(R.drawable.bg_chip_purple)

        val primaryColor = ContextCompat.getColor(this, R.color.primary_purple)
        val lightBgColor = ContextCompat.getColor(this, R.color.bg_purple_light)
        val textColorDark = ContextCompat.getColor(this, R.color.text_dark)

        radioGroupRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == rbParent.id) {
                parentLayout.visibility = View.VISIBLE
                childLayout.visibility = View.GONE
                rbParent.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                rbParent.setTextColor(Color.WHITE)
                rbChild.backgroundTintList = android.content.res.ColorStateList.valueOf(lightBgColor)
                rbChild.setTextColor(textColorDark)
            } else if (checkedId == rbChild.id) {
                parentLayout.visibility = View.GONE
                childLayout.visibility = View.VISIBLE
                rbChild.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                rbChild.setTextColor(Color.WHITE)
                rbParent.backgroundTintList = android.content.res.ColorStateList.valueOf(lightBgColor)
                rbParent.setTextColor(textColorDark)
            }
        }

        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnGoogleRegister.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val nidNumber = etNidNumber.text.toString().trim()
            val childAge = etChildAge.text.toString().trim()
            val parentEmailInput = etParentEmail.text.toString().trim().lowercase()

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
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
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
            if (role == "child" && parentEmailInput.isEmpty()) {
                Toast.makeText(this, "Enter Parent Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Creating Account..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser!!.uid
                        saveUserProfile(userId, name, email, role, nidNumber, childAge, parentEmailInput)
                    } else {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Join Parental Care"
                        val errorMsg = when (task.exception) {
                            is FirebaseAuthUserCollisionException -> "Email already exists."
                            is FirebaseAuthWeakPasswordException -> "Password too weak."
                            else -> task.exception?.message ?: "Registration failed."
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                                val role = doc.getString("role")
                                if (role == "parent") {
                                    startActivity(Intent(this, ParentDashboardActivity::class.java))
                                } else {
                                    startActivity(Intent(this, ChildDashboardActivity::class.java))
                                }
                                finish()
                            } else {
                                findViewById<EditText>(R.id.etName).setText(user.displayName)
                                findViewById<EditText>(R.id.etEmail).setText(user.email)
                                Toast.makeText(this, "Please select role and complete profile", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
    }

    private fun saveUserProfile(uid: String, name: String, email: String, role: String, nid: String, age: String, pEmail: String) {
        val userMap = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )
        if (role == "parent") userMap["nidNumber"] = nid
        if (role == "child") {
            userMap["childAge"] = age
            userMap["parentEmail"] = pEmail
        }

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                auth.signOut() 
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    putExtra("fromRegister", true)
                    putExtra("registeredEmail", email)
                })
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                findViewById<Button>(R.id.btnRegisterNow).isEnabled = true
            }
    }
}
