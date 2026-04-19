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

        val cardScreenTime = findViewById<LinearLayout>(R.id.cardScreenTime)
        val cardLocation = findViewById<LinearLayout>(R.id.cardLocation)
        val cardAppUsage = findViewById<LinearLayout>(R.id.cardAppUsage)
        val cardAlerts = findViewById<LinearLayout>(R.id.cardAlerts)
        val cardChildren = findViewById<LinearLayout>(R.id.cardChildren)
        val cardLast24History = findViewById<LinearLayout>(R.id.cardLast24History)
        val tvLogout = findViewById<TextView>(R.id.tvLogout)

        cardScreenTime.setOnClickListener {
            startActivity(Intent(this, ScreenTimeActivity::class.java))
        }

        cardLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        cardAppUsage.setOnClickListener {
            startActivity(Intent(this, AppUsageActivity::class.java))
        }

        cardAlerts.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        cardChildren.setOnClickListener {
            startActivity(Intent(this, ChildrenActivity::class.java))
        }

        cardLast24History.setOnClickListener {
            startActivity(Intent(this, Last24HourHistoryActivity::class.java))
        }

        tvLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}