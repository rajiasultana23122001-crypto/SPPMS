package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ParentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var rootScroll: ScrollView
    private lateinit var tvDashboardTitle: TextView
    private lateinit var tvLogout: TextView

    private lateinit var cards: List<LinearLayout>
    private lateinit var cardTexts: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        auth = FirebaseAuth.getInstance()

        rootScroll = findViewById(R.id.rootScroll)
        tvDashboardTitle = findViewById(R.id.tvDashboardTitle)
        tvLogout = findViewById(R.id.tvLogout)

        val cardScreenTime = findViewById<LinearLayout>(R.id.cardScreenTime)
        val cardLocation = findViewById<LinearLayout>(R.id.cardLocation)
        val cardAppUsage = findViewById<LinearLayout>(R.id.cardAppUsage)
        val cardReels = findViewById<LinearLayout>(R.id.cardReels)
        val cardChildren = findViewById<LinearLayout>(R.id.cardChildren)
        val cardLast24History = findViewById<LinearLayout>(R.id.cardLast24History)
        val cardOthersApp = findViewById<LinearLayout>(R.id.cardOthersApp)
        val cardSettings = findViewById<LinearLayout>(R.id.cardSettings)

        cards = listOf(
            cardScreenTime,
            cardLocation,
            cardAppUsage,
            cardReels,
            cardChildren,
            cardLast24History,
            cardOthersApp,
            cardSettings
        )

        cardTexts = cards.map { card ->
            card.getChildAt(1) as TextView
        }

        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        applyDashboardTheme(isDarkMode)

        cardScreenTime.setOnClickListener {
            startActivity(Intent(this, ScreenTimeActivity::class.java))
        }

        cardLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        cardAppUsage.setOnClickListener {
            startActivity(Intent(this, AppUsageActivity::class.java))
        }

        cardReels.setOnClickListener {
            startActivity(Intent(this, ReelsActivity::class.java))
        }

        cardChildren.setOnClickListener {
            startActivity(Intent(this, ChildrenActivity::class.java))
        }

        cardLast24History.setOnClickListener {
            startActivity(Intent(this, Last24HourHistoryActivity::class.java))
        }

        cardOthersApp.setOnClickListener {
            startActivity(Intent(this, OthersAppActivity::class.java))
        }

        cardSettings.setOnClickListener {
            val currentMode = sharedPreferences.getBoolean("dark_mode", false)
            val newMode = !currentMode

            sharedPreferences.edit().putBoolean("dark_mode", newMode).apply()
            applyDashboardTheme(newMode)
        }

        tvLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun applyDashboardTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            rootScroll.setBackgroundColor(Color.parseColor("#121212"))

            for (card in cards) {
                card.setBackgroundColor(Color.parseColor("#1E1E1E"))
            }

            for (text in cardTexts) {
                text.setTextColor(Color.WHITE)
            }

            tvDashboardTitle.setTextColor(Color.WHITE)
            tvLogout.setTextColor(Color.WHITE)
        } else {
            rootScroll.setBackgroundColor(Color.parseColor("#ECECEC"))

            for (card in cards) {
                card.setBackgroundColor(Color.WHITE)
            }

            for (text in cardTexts) {
                text.setTextColor(Color.parseColor("#C8C1D1"))
            }

            tvDashboardTitle.setTextColor(Color.parseColor("#F8F5D7"))
            tvLogout.setTextColor(Color.WHITE)
        }
    }
}