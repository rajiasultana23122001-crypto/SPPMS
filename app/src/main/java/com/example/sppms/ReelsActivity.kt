package com.example.sppms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReelsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var containerLayout: LinearLayout

    private val notificationPermissionCode = 2001
    private val alertedChildren = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reels)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        containerLayout = findViewById(R.id.containerLayout)

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        loadChildrenProfiles()
    }

    private fun loadChildrenProfiles() {
        val parentEmail = auth.currentUser?.email?.lowercase()?.trim() ?: return

        containerLayout.removeAllViews()
        showStatusMessage("Loading profiles...")

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                containerLayout.removeAllViews()

                if (documents.isEmpty) {
                    showStatusMessage("No children linked yet.")
                    return@addOnSuccessListener
                }

                // Show limit exceeded banner if any child exceeded
                var anyExceeded = false
                for (doc in documents) {
                    val todayReelsSeconds = doc.getLong("todayReelsSeconds") ?: 0L
                    val reelsDailyLimitMin = doc.getLong("reelsDailyLimitMin") ?: 30L
                    if (todayReelsSeconds / 60 >= reelsDailyLimitMin) {
                        anyExceeded = true
                        break
                    }
                }

                if (anyExceeded) {
                    addLimitExceededBanner()
                }

                for (doc in documents) {
                    addProfileCard(doc)
                }
            }
            .addOnFailureListener { e ->
                showStatusMessage("Error: ${e.message}")
            }
    }

    private fun addLimitExceededBanner() {
        val bannerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FFF3F3"))
            setPadding(16, 16, 16, 16)
            val bannerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            bannerParams.setMargins(0, 0, 0, 16)
            layoutParams = bannerParams
        }

        val iconTv = TextView(this).apply {
            text = "⚠️"
            textSize = 20f
            setPadding(0, 0, 12, 0)
        }

        val msgLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val tvTitle = TextView(this@ReelsActivity).apply {
                text = "Reels limit exceeded"
                textSize = 14f
                setTextColor(Color.parseColor("#C62828"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvMsg = TextView(this@ReelsActivity).apply {
                text = "One or more children have exceeded their limit."
                textSize = 12f
                setTextColor(Color.parseColor("#C62828"))
            }
            addView(tvTitle)
            addView(tvMsg)
        }

        bannerLayout.addView(iconTv)
        bannerLayout.addView(msgLayout)
        containerLayout.addView(bannerLayout)
    }

    private fun addProfileCard(doc: DocumentSnapshot) {
        val name = doc.getString("name") ?: "Child"
        val email = doc.getString("email") ?: ""
        val todayReelsSeconds = doc.getLong("todayReelsSeconds") ?: 0L
        val reelsDailyLimitMin = doc.getLong("reelsDailyLimitMin") ?: 30L
        val limitExceeded = (todayReelsSeconds / 60) >= reelsDailyLimitMin

        val density = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.card_background)
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            elevation = 4 * density
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, (16 * density).toInt())
            layoutParams = params
            setOnClickListener { showReelsDetailForChild(doc.id) }
        }

        // Avatar Initial Circle
        val iconTv = TextView(this).apply {
            text = name.take(1).uppercase()
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_circle_purple_light)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (limitExceeded) Color.parseColor("#C62828") else Color.parseColor("#7E57C2")
            )
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val nameTv = TextView(this@ReelsActivity).apply {
                text = name
                textSize = 18f
                setTextColor(Color.parseColor("#1A1A2E"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val subTv = TextView(this@ReelsActivity).apply {
                text = if (limitExceeded) "⚠️ Limit Exceeded" else "Tap to view Reels status"
                textSize = 13f
                setTextColor(if (limitExceeded) Color.parseColor("#C62828") else Color.GRAY)
            }
            addView(nameTv)
            addView(subTv)
        }

        val arrowTv = TextView(this).apply {
            text = ">"
            textSize = 20f
            setTextColor(Color.parseColor("#7E57C2"))
        }

        card.addView(iconTv)
        card.addView(textLayout)
        card.addView(arrowTv)
        containerLayout.addView(card)
    }

    private fun showReelsDetailForChild(childUid: String) {
        containerLayout.removeAllViews()
        showStatusMessage("Fetching details...")

        db.collection("users").document(childUid).get()
            .addOnSuccessListener { doc ->
                containerLayout.removeAllViews()
                if (!doc.exists()) {
                    showStatusMessage("User data not found.")
                    return@addOnSuccessListener
                }

                val name = doc.getString("name") ?: "Unknown"
                val email = doc.getString("email") ?: "No email"
                val reelsStatus = doc.getString("reelsStatus") ?: "Stopped"
                val reelsApp = doc.getString("reelsApp") ?: "Not detected"
                val startedAtMillis = doc.getLong("reelsStartedAt") ?: 0L
                val todayReelsSeconds = doc.getLong("todayReelsSeconds") ?: 0L
                val reelsDailyLimitMin = doc.getLong("reelsDailyLimitMin") ?: 30L

                val startedAtText = if (startedAtMillis > 0) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startedAtMillis))
                } else "Not started"

                val todayMinutes = todayReelsSeconds / 60
                val limitExceeded = todayMinutes >= reelsDailyLimitMin
                val isWatching = reelsStatus == "Watching Reels"

                val header = TextView(this).apply {
                    text = "Reels Status: $name"
                    textSize = 20f
                    setTextColor(Color.parseColor("#7E57C2"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 20)
                }
                containerLayout.addView(header)

                val cardLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.WHITE)
                    setPadding(20, 20, 20, 20)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 20)
                    layoutParams = params
                }

                // Header row: status badge
                val headerRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val tvEmail = TextView(this).apply {
                    text = email
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvBadge = TextView(this).apply {
                    text = if (isWatching) "▶ Watching" else "■ Stopped"
                    textSize = 12f
                    setPadding(12, 6, 12, 6)
                    setTextColor(Color.WHITE)
                    setBackgroundColor(
                        if (isWatching) Color.parseColor("#FF8F00") else Color.parseColor("#43A047")
                    )
                }

                headerRow.addView(tvEmail)
                headerRow.addView(tvBadge)
                cardLayout.addView(headerRow)

                fun addRow(icon: String, label: String, value: String, valueColor: Int = Color.parseColor("#1A1A2E")) {
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        val rowParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        rowParams.setMargins(0, 10, 0, 10)
                        layoutParams = rowParams
                    }
                    row.addView(TextView(this).apply { text = icon; textSize = 16f; setPadding(0, 0, 10, 0) })
                    row.addView(TextView(this).apply {
                        text = label
                        textSize = 14f
                        setTextColor(Color.GRAY)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    row.addView(TextView(this).apply {
                        text = value
                        textSize = 14f
                        setTextColor(valueColor)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    })
                    cardLayout.addView(row)
                }

                addRow("📱", "App", reelsApp)
                addRow("🕒", "Started at", startedAtText)
                addRow("⏱", "Current daily limit", "$reelsDailyLimitMin min")
                addRow("👁", "Today's watched", "$todayMinutes min")
                addRow("⚠️", "Warning",
                    if (limitExceeded) "Limit exceeded" else "Within limit",
                    if (limitExceeded) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
                )

                val tvLimitLabel = TextView(this).apply {
                    text = "Set new daily limit (minutes)"
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    setPadding(0, 16, 0, 8)
                }
                cardLayout.addView(tvLimitLabel)

                val limitRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val etLimit = EditText(this).apply {
                    hint = "Minutes"
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(reelsDailyLimitMin.toString())
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                    setPadding(12, 12, 12, 12)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, 12, 0)
                    }
                }

                val btnSaveLimit = Button(this).apply {
                    text = "Save"
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#5C35C4"))
                    setOnClickListener {
                        val newLimit = etLimit.text.toString().trim().toLongOrNull()
                        if (newLimit == null || newLimit <= 0) {
                            Toast.makeText(this@ReelsActivity, "Enter valid minutes", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        db.collection("users").document(childUid)
                            .update("reelsDailyLimitMin", newLimit)
                            .addOnSuccessListener {
                                Toast.makeText(this@ReelsActivity, "Limit saved", Toast.LENGTH_SHORT).show()
                                showReelsDetailForChild(childUid)
                            }
                    }
                }

                limitRow.addView(etLimit)
                limitRow.addView(btnSaveLimit)
                cardLayout.addView(limitRow)

                containerLayout.addView(cardLayout)

                val btnBack = TextView(this).apply {
                    text = "← Back to Profiles"
                    setTextColor(Color.parseColor("#7E57C2"))
                    gravity = Gravity.CENTER
                    setPadding(0, 40, 0, 40)
                    setOnClickListener { loadChildrenProfiles() }
                }
                containerLayout.addView(btnBack)
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reels_alert_channel", "Reels Limit Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when child exceeds reels limit"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showLimitNotification(childName: String, usedMinutes: Long, limitMinutes: Long) {
        val builder = NotificationCompat.Builder(this, "reels_alert_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Reels Limit Exceeded")
            .setContentText("$childName used $usedMinutes min. Limit: $limitMinutes min.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(childName.hashCode(), builder.build())
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionCode
                )
            }
        }
    }

    private fun showStatusMessage(msg: String) {
        val empty = TextView(this).apply {
            text = msg
            textSize = 16f
            setPadding(32, 40, 32, 40)
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        containerLayout.addView(empty)
    }
}
