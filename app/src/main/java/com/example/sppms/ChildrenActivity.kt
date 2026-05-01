package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChildrenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var childListContainer: LinearLayout
    private lateinit var tvChildrenCount: TextView
    private lateinit var tvProfileInitial: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_children)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        childListContainer = findViewById(R.id.childListContainer)
        tvChildrenCount = findViewById(R.id.tvChildrenCount)
        tvProfileInitial = findViewById(R.id.tvProfileInitial)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        setupHeader()
        loadChildren()
    }

    private fun setupHeader() {
        val user = auth.currentUser
        if (user != null) {
            // Try to get name from Firestore or Auth display name
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: user.displayName ?: "Parent"
                    tvProfileInitial.text = if (name.isNotEmpty()) name.take(1).uppercase() else "P"
                }
        }
    }

    private fun loadChildren() {
        val currentUser = auth.currentUser ?: return
        val parentEmail = currentUser.email?.trim()?.lowercase() ?: return

        childListContainer.removeAllViews()
        tvChildrenCount.text = "Fetching accounts..."

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { result ->
                childListContainer.removeAllViews()
                val count = result.size()
                tvChildrenCount.text = "$count accounts linked"

                if (result.isEmpty) {
                    showEmptyMessage("No children linked yet.\nRegister a child account with your email as Parent Email.")
                } else {
                    for (document in result) {
                        val childName = document.getString("name") ?: "Unknown"
                        val childAge = document.getString("childAge") ?: "N/A"
                        val childEmail = document.getString("email") ?: ""
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")

                        addChildCard(childName, childAge, childEmail, lat, lng)
                    }
                }
            }
            .addOnFailureListener { e ->
                tvChildrenCount.text = "Error loading children"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addChildCard(name: String, age: String, email: String, lat: Double?, lng: Double?) {
        val density = resources.displayMetrics.density
        
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_background)
            setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
            elevation = 4 * density
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, (20 * density).toInt())
            layoutParams = params
        }

        // Top Row: Icon + Name/Age
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val initialTv = TextView(this).apply {
            text = name.take(1).uppercase()
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_circle_purple_light)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7E57C2"))
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
        }

        val nameLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            
            val nameTv = TextView(this@ChildrenActivity).apply {
                text = name
                textSize = 18f
                setTextColor(Color.parseColor("#1A1A2E"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val ageTv = TextView(this@ChildrenActivity).apply {
                text = "Age: $age years"
                textSize = 13f
                setTextColor(Color.GRAY)
            }
            addView(nameTv)
            addView(ageTv)
        }

        topRow.addView(initialTv)
        topRow.addView(nameLayout)
        card.addView(topRow)

        // Email
        val emailTv = TextView(this).apply {
            text = "✉️ $email"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, (16 * density).toInt(), 0, 0)
        }
        card.addView(emailTv)

        // Location Info and Map Button
        if (lat != null && lng != null && lat != 0.0) {
            val locTv = TextView(this).apply {
                text = "📍 Last location available"
                textSize = 14f
                setTextColor(Color.parseColor("#7E57C2"))
                setPadding(0, (8 * density).toInt(), 0, (12 * density).toInt())
            }
            card.addView(locTv)

            val btnViewMap = Button(this).apply {
                text = "View Location on Map"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#7E57C2"))
                setOnClickListener {
                    val intent = Intent(this@ChildrenActivity, ChildLocationMapActivity::class.java)
                    intent.putExtra("childName", name)
                    intent.putExtra("latitude", lat)
                    intent.putExtra("longitude", lng)
                    startActivity(intent)
                }
            }
            card.addView(btnViewMap)
        } else {
            val locTv = TextView(this).apply {
                text = "📍 Location not yet shared"
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(0, (12 * density).toInt(), 0, 0)
            }
            card.addView(locTv)
        }

        childListContainer.addView(card)
    }

    private fun showEmptyMessage(msg: String) {
        val tv = TextView(this).apply {
            text = msg
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(32, 100, 32, 0)
        }
        childListContainer.addView(tv)
    }
}
