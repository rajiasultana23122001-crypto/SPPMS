package com.example.sppms

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ParentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        auth = FirebaseAuth.getInstance()

        val tvLogout = findViewById<TextView>(R.id.tvLogout)

        val cardScreenTime = findViewById<LinearLayout>(R.id.cardScreenTime)
        val cardLocation = findViewById<LinearLayout>(R.id.cardLocation)
        val cardAppUsage = findViewById<LinearLayout>(R.id.cardAppUsage)
        val cardAlerts = findViewById<LinearLayout>(R.id.cardAlerts)
        val cardChildren = findViewById<LinearLayout>(R.id.cardChildren)
        val cardSettings = findViewById<LinearLayout>(R.id.cardSettings)

        tvLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        cardScreenTime.setOnClickListener {
            startActivity(Intent(this, ScreenTimeActivity::class.java))
        }

        cardLocation.setOnClickListener {
            // later
        }

        cardAppUsage.setOnClickListener {
            startActivity(Intent(this, AppUsageActivity::class.java))
        }

        cardAlerts.setOnClickListener {
            // later
        }

        cardChildren.setOnClickListener {
            // later
        }

        cardChildren.setOnClickListener {
            startActivity(Intent(this, ChildrenActivity::class.java))
        }
    }
}