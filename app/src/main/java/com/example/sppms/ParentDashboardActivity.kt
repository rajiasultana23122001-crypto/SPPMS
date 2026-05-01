package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ParentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupProfileHeader()
        setupClickListeners()
        loadChildrenCount()
    }

    private fun setupProfileHeader() {
        val tvProfileInitial = findViewById<TextView>(R.id.tvProfileInitial)
        val tvLogout = findViewById<TextView>(R.id.tvLogout)

        val user = auth.currentUser
        if (user != null) {
            val name = user.displayName ?: "Parent"
            tvProfileInitial.text = if (name.isNotEmpty()) name.take(1).uppercase() else "P"
        }

        tvLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadChildrenCount() {
        val parentEmail = auth.currentUser?.email?.lowercase()?.trim() ?: return
        val tvChildrenCount = findViewById<TextView>(R.id.tvChildrenCount)

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                tvChildrenCount.text = "${documents.size()} children linked"
            }
    }

    private fun setupClickListeners() {
        // Updated to use correct IDs from redesigned dashboard
        findViewById<View>(R.id.cardScreenTime)?.setOnClickListener {
            startActivity(Intent(this, ScreenTimeActivity::class.java))
        }

        findViewById<View>(R.id.cardLocation)?.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        findViewById<View>(R.id.cardAppUsage)?.setOnClickListener {
            startActivity(Intent(this, AppUsageActivity::class.java))
        }

        findViewById<View>(R.id.cardReels)?.setOnClickListener {
            startActivity(Intent(this, ReelsActivity::class.java))
        }

        findViewById<View>(R.id.cardAlerts)?.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        findViewById<View>(R.id.cardOthersApp)?.setOnClickListener {
            startActivity(Intent(this, OthersAppActivity::class.java))
        }

        findViewById<View>(R.id.cardLast24History)?.setOnClickListener {
            startActivity(Intent(this, Last24HourHistoryActivity::class.java))
        }

        findViewById<View>(R.id.cardChildren)?.setOnClickListener {
            startActivity(Intent(this, ChildrenActivity::class.java))
        }
    }
}
