package com.example.sppms

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.usage.UsageStatsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChildDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val handler = Handler(Looper.getMainLooper())

    private var lastApp = ""
    private var startTime = 0L

    private lateinit var tvCurrentStatus: TextView
    private lateinit var btnPermission: Button
    private lateinit var btnNotificationAccess: Button

    private val autoTracker = object : Runnable {
        override fun run() {
            val user = auth.currentUser

            if (user != null) {
                val currentPackage = getLastUsedTrackedPackage()
                val currentApp = getReadableAppName(currentPackage)
                val currentTime = System.currentTimeMillis()

                if (currentApp == lastApp) {
                    val duration = (currentTime - startTime) / 1000
                    saveTrackingData(currentApp, currentPackage, duration)
                } else {
                    lastApp = currentApp
                    startTime = currentTime
                    saveTrackingData(currentApp, currentPackage, 0)
                }
            }

            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        btnPermission = findViewById(R.id.btnOpenPermission)
        btnNotificationAccess = findViewById(R.id.btnNotificationAccess)

        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnNotificationAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        handler.postDelayed(autoTracker, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoTracker)
    }

    private fun saveTrackingData(currentApp: String, currentPackage: String, durationSec: Long) {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val childName = document.getString("name") ?: "Unknown Child"
                val childEmail = document.getString("email") ?: user.email.orEmpty()

                val videoTitle = detectCurrentVideoTitle(currentPackage)
                val category = classifyVideo(videoTitle)

                val data = hashMapOf(
                    "appUsage" to currentApp,
                    "packageName" to currentPackage,
                    "watchTime" to "${durationSec} sec",
                    "videoTitle" to videoTitle,
                    "category" to category,
                    "childName" to childName,
                    "childEmail" to childEmail,
                    "lastUpdated" to System.currentTimeMillis()
                )

                db.collection("users").document(user.uid)
                    .update(data as Map<String, Any>)
                    .addOnSuccessListener {
                        tvCurrentStatus.text = """
                            Current App: $currentApp
                            Video Title: $videoTitle
                            Category: $category
                            Time: ${durationSec} sec
                        """.trimIndent()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Tracking update failed", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun detectCurrentVideoTitle(currentPackage: String): String {
        val mediaTitle = getMediaSessionTitle(currentPackage)
        if (mediaTitle.isNotBlank() && !isSystemText(mediaTitle)) {
            Log.d("SPPMS_TITLE", "From MediaSession: $mediaTitle")
            return mediaTitle
        }

        val notifPackage = MyNotificationListener.latestPackage ?: ""
        val notifTitle = MyNotificationListener.latestTitle ?: ""
        val notifText = MyNotificationListener.latestText ?: ""

        if (notifPackage == currentPackage) {
            if (notifTitle.isNotBlank() && !isSystemText(notifTitle)) {
                Log.d("SPPMS_TITLE", "From Notification Title: $notifTitle")
                return notifTitle
            }

            if (notifText.isNotBlank() && !isSystemText(notifText)) {
                Log.d("SPPMS_TITLE", "From Notification Text: $notifText")
                return notifText
            }
        }

        return "Unknown title"
    }

    private fun getMediaSessionTitle(currentPackage: String): String {
        return try {
            val mediaSessionManager =
                getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

            val componentName = ComponentName(this, MyNotificationListener::class.java)
            val controllers: List<MediaController> =
                mediaSessionManager.getActiveSessions(componentName)

            for (controller in controllers) {
                if (controller.packageName == currentPackage) {
                    val metadata = controller.metadata
                    if (metadata != null) {
                        val title =
                            metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)

                        if (!title.isNullOrBlank()) {
                            return title
                        }
                    }
                }
            }

            ""
        } catch (e: Exception) {
            Log.e("SPPMS_MEDIA", "Media session error: ${e.message}")
            ""
        }
    }

    private fun isSystemText(text: String): Boolean {
        val value = text.lowercase()

        return value.contains("android system") ||
                value.contains("running in background") ||
                value.contains("usb debugging") ||
                value.contains("charging") ||
                value.contains("battery") ||
                value.contains("messenger chat heads") ||
                value.contains("new message")
    }

    private fun classifyVideo(title: String): String {
        val t = title.lowercase()

        val academicKeywords = listOf(
            "math", "science", "physics", "chemistry", "biology",
            "lecture", "class", "tutorial", "education", "learn",
            "grammar", "english", "programming", "coding", "history",
            "geography", "ict", "exam", "assignment", "course",
            "chapter", "lesson", "school", "college", "university"
        )

        val entertainmentKeywords = listOf(
            "review", "vlog", "funny", "prank", "music", "song",
            "movie", "drama", "splitsvilla", "reaction", "gaming",
            "shorts", "episode", "ep ", "show", "trailer", "dance"
        )

        return when {
            title == "Unknown title" -> "Unknown"
            academicKeywords.any { t.contains(it) } -> "Academic"
            entertainmentKeywords.any { t.contains(it) } -> "Entertainment"
            else -> "Unknown"
        }
    }

    private fun getLastUsedTrackedPackage(): String {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 60 * 10,
            time
        )

        if (stats.isNullOrEmpty()) return "unknown"

        val sortedStats = stats.sortedByDescending { it.lastTimeUsed }

        for (app in sortedStats) {
            val pkg = app.packageName.lowercase()

            if (
                pkg.contains("youtube") ||
                pkg.contains("chrome") ||
                pkg.contains("facebook") ||
                pkg.contains("instagram") ||
                pkg.contains("messenger") ||
                pkg.contains("snapchat") ||
                pkg.contains("netflix") ||
                pkg.contains("primevideo")
            ) {
                return app.packageName
            }
        }

        return "other.app"
    }

    private fun getReadableAppName(packageName: String): String {
        val pkg = packageName.lowercase()

        return when {
            pkg.contains("youtube") -> "YouTube"
            pkg.contains("chrome") -> "Chrome"
            pkg.contains("facebook") -> "Facebook"
            pkg.contains("instagram") -> "Instagram"
            pkg.contains("messenger") -> "Messenger"
            pkg.contains("snapchat") -> "Snapchat"
            pkg.contains("netflix") -> "Netflix"
            pkg.contains("primevideo") -> "Prime Video"
            pkg == "unknown" -> "Unknown"
            else -> "Other App"
        }
    }
}