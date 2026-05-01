package com.example.sppms

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChildDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val handler = Handler(Looper.getMainLooper())
    private val locationPermissionCode = 1001

    private var lastApp = ""
    private var lastVideoTitle = ""
    private var lastCategory = ""
    private var startTime = 0L

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private var reelsStartedAt: Long = 0L
    private var lastReelsDateKey: String = ""
    private var todayReelsSeconds: Long = 0L
    private var isDataInitialized = false

    private lateinit var tvCurrentStatus: TextView
    private lateinit var btnPermission: Button
    private lateinit var btnNotificationAccess: Button
    private lateinit var btnAccessibility: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationUpdateRequested = false

    private val autoTracker = object : Runnable {
        override fun run() {
            val user = auth.currentUser
            if (user != null && isDataInitialized) {
                val currentPackage = getLastUsedTrackedPackage()
                val currentApp = getReadableAppName(currentPackage)
                val videoTitle = detectCurrentVideoTitle(currentPackage)
                val category = classifyVideo(videoTitle)
                val currentTime = System.currentTimeMillis()

                if (currentApp != lastApp || (currentApp == "YouTube" && videoTitle != lastVideoTitle && lastVideoTitle.isNotEmpty())) {
                    if (lastApp.isNotEmpty() && lastApp != "Other App" && lastApp != "Unknown") {
                        val duration = (currentTime - startTime) / 1000
                        if (duration > 3) {
                            saveUsageLog(lastApp, duration, lastVideoTitle, lastCategory)
                        }
                    }
                    lastApp = currentApp
                    lastVideoTitle = videoTitle
                    lastCategory = category
                    startTime = currentTime
                }

                val durationSinceStart = (currentTime - startTime) / 1000
                saveTrackingData(currentApp, currentPackage, videoTitle, category, durationSinceStart)
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
        btnAccessibility = findViewById(R.id.btnAccessibility)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                saveLocationToFirestore(location.latitude, location.longitude)
            }
        }

        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnNotificationAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        initializeChildData()
        checkLocationPermissionAndFetch()
    }

    private fun initializeChildData() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    todayReelsSeconds = doc.getLong("todayReelsSeconds") ?: 0L
                    lastReelsDateKey = getTodayDateKey()
                    lastKnownName = doc.getString("name") ?: "Child"
                }
                isDataInitialized = true
                handler.postDelayed(autoTracker, 1000)
            }
            .addOnFailureListener {
                isDataInitialized = true
                handler.postDelayed(autoTracker, 1000)
            }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoTracker)
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        if (locationUpdateRequested) return
        locationUpdateRequested = true

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        if (locationUpdateRequested) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdateRequested = false
        }
    }

    private var lastKnownName: String = "Child"

    private fun saveUsageLog(appName: String, durationSec: Long, title: String, cat: String) {
        val user = auth.currentUser ?: return
        val logData = hashMapOf<String, Any>(
            "childEmail" to (user.email?.lowercase()?.trim() ?: ""),
            "childName" to lastKnownName,
            "appUsage" to appName,
            "durationSec" to durationSec,
            "videoTitle" to title,
            "category" to cat,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("usage_logs").add(logData)
    }

    private fun saveTrackingData(currentApp: String, currentPackage: String, videoTitle: String, category: String, durationSec: Long) {
        val user = auth.currentUser ?: return
        val todayKey = getTodayDateKey()

        if (lastReelsDateKey != todayKey) {
            lastReelsDateKey = todayKey
            todayReelsSeconds = 0L
            reelsStartedAt = 0L
        }

        val reelsNow = isReelsContent(currentPackage, videoTitle)
        var reelsStatus = "Stopped"
        var reelsApp = "Not detected"

        if (reelsNow) {
            reelsStatus = "Watching Reels"
            reelsApp = currentApp
            if (reelsStartedAt == 0L) reelsStartedAt = System.currentTimeMillis()
            todayReelsSeconds += 5
        } else {
            reelsStartedAt = 0L
        }

        val data = hashMapOf<String, Any>(
            "appUsage" to currentApp,
            "packageName" to currentPackage,
            "watchTime" to "${durationSec} sec",
            "videoTitle" to videoTitle,
            "category" to category,
            "lastUpdated" to System.currentTimeMillis(),
            "reelsStatus" to reelsStatus,
            "reelsApp" to reelsApp,
            "reelsStartedAt" to reelsStartedAt,
            "todayReelsSeconds" to todayReelsSeconds
        )

        currentLatitude?.let { data["latitude"] = it }
        currentLongitude?.let { data["longitude"] = it }

        db.collection("users").document(user.uid).update(data)
            .addOnSuccessListener {
                updateStatusUI(currentApp, videoTitle, category, durationSec, reelsStatus, reelsApp)
            }
            .addOnFailureListener {
                db.collection("users").document(user.uid).set(data, SetOptions.merge())
            }
    }
    
    private fun updateStatusUI(app: String, title: String, cat: String, dur: Long, status: String, reelsApp: String) {
        val lat = currentLatitude ?: 0.0
        val lng = currentLongitude ?: 0.0
        val loc = if (currentLatitude != null) String.format(Locale.US, "%.4f, %.4f", lat, lng) else "Waiting..."
        val todayMin = todayReelsSeconds / 60

        tvCurrentStatus.text = """
            Current App: $app
            Video: $title ($cat)
            Session: ${dur}s | Today Reels: ${todayMin}m
            Location: $loc
            Status: $status
        """.trimIndent()
    }

    private fun isReelsContent(pkg: String, title: String): Boolean {
        val combined = (title + (MyNotificationListener.latestTitle ?: "")).lowercase()
        return when {
            pkg.contains("youtube") -> combined.contains("shorts") || title == "Unknown title"
            pkg.contains("instagram") || pkg.contains("facebook") -> combined.contains("reel")
            else -> false
        }
    }

    private fun getTodayDateKey() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun detectCurrentVideoTitle(currentPackage: String): String {
        val mediaTitle = getMediaSessionTitle(currentPackage)
        if (mediaTitle.isNotBlank() && !isSystemText(mediaTitle)) return mediaTitle
        val notifTitle = MyNotificationListener.latestTitle ?: ""
        if (MyNotificationListener.latestPackage == currentPackage && notifTitle.isNotBlank() && !isSystemText(notifTitle)) return notifTitle
        return "Unknown title"
    }

    private fun getMediaSessionTitle(pkg: String): String {
        return try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = msm.getActiveSessions(ComponentName(this, MyNotificationListener::class.java))
            controllers.find { it.packageName == pkg }?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        } catch (e: Exception) { "" }
    }

    private fun isSystemText(text: String) = text.lowercase().let { it.contains("android system") || it.contains("charging") || it.contains("battery") }

    private fun classifyVideo(title: String): String {
        val t = title.lowercase()
        return when {
            title == "Unknown title" -> "Unknown"
            listOf("math","science","learn","edu","coding").any { t.contains(it) } -> "Academic"
            listOf("music","vlog","game","movie").any { t.contains(it) } -> "Entertainment"
            else -> "Unknown"
        }
    }

    private fun getLastUsedTrackedPackage(): String {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 60000, time) ?: return "unknown"
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: "unknown"
    }

    private fun getReadableAppName(pkg: String) = when {
        pkg.contains("youtube") -> "YouTube"
        pkg.contains("facebook.katana") -> "Facebook"
        pkg.contains("instagram") -> "Instagram"
        pkg.contains("twitter") || pkg.contains("x.android") -> "Twitter"
        pkg.contains("snapchat") -> "Snapchat"
        pkg.contains("facebook.orca") -> "Messenger"
        pkg.contains("telegram") -> "Telegram"
        pkg.contains("whatsapp") -> "WhatsApp"
        pkg.contains("chrome") -> "Chrome"
        else -> "Other App"
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        } else {
            startLocationUpdates()
        }
    }

    private fun saveLocationToFirestore(lat: Double, lng: Double) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).update(mapOf("latitude" to lat, "longitude" to lng, "locationUpdatedAt" to System.currentTimeMillis()))
    }
}
