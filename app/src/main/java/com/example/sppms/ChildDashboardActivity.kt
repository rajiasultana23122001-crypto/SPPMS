
package com.example.sppms

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChildDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())

    private var lastApp = ""
    private var startTime = 0L

    private val autoTracker = object : Runnable {
        override fun run() {
            val user = auth.currentUser

            if (user != null) {
                val currentApp = getLastUsedTrackedApp()
                val currentTime = System.currentTimeMillis()

                if (currentApp == lastApp) {
                    val duration = (currentTime - startTime) / 1000

                    db.collection("users").document(user.uid)
                        .update(
                            mapOf(
                                "appUsage" to currentApp,
                                "watchTime" to "${duration} sec"
                            )
                        )
                } else {
                    lastApp = currentApp
                    startTime = currentTime

                    db.collection("users").document(user.uid)
                        .update(
                            mapOf(
                                "appUsage" to currentApp,
                                "watchTime" to "0 sec"
                            )
                        )
                }
            }

            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etVideoTitle = findViewById<EditText>(R.id.etVideoTitle)
        val btnSend = findViewById<Button>(R.id.btnSendActivity)
        val btnPermission = findViewById<Button>(R.id.btnOpenPermission)

        btnSend.setOnClickListener {
            val user = auth.currentUser

            if (user != null) {
                val currentApp = getLastUsedTrackedApp()
                val videoTitle = etVideoTitle.text.toString().trim()

                val finalTitle = if (videoTitle.isEmpty()) "No title entered" else videoTitle

                val userRef = db.collection("users").document(user.uid)

                userRef.update(
                    mapOf(
                        "appUsage" to currentApp,
                        "youtubeTitle" to finalTitle
                    )
                ).addOnSuccessListener {
                    Toast.makeText(this, "Video title sent", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to send title", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        btnPermission.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        handler.postDelayed(autoTracker, 10000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoTracker)
    }

    private fun getLastUsedTrackedApp(): String {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 60 * 10,
            time
        )

        if (stats.isNullOrEmpty()) return "Unknown"

        val sortedStats = stats.sortedByDescending { it.lastTimeUsed }

        for (app in sortedStats) {
            val pkg = app.packageName.lowercase()

            when {
                pkg.contains("youtube") -> return "YouTube"
                pkg.contains("facebook.katana") -> return "Facebook"
                pkg.contains("instagram") -> return "Instagram"
                pkg.contains("messenger") -> return "Messenger"
                pkg.contains("snapchat") -> return "Snapchat"
            }
        }

        return "Other App"
    }
}