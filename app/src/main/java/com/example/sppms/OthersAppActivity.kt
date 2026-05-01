package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class OthersAppActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var appItemsContainer: LinearLayout

    private val appsToTrack = listOf(
        AppInfo("Facebook", "🔵"),
        AppInfo("Instagram", "📸"),
        AppInfo("Twitter", "🐦"),
        AppInfo("Snapchat", "🟡"),
        AppInfo("Messenger", "💬"),
        AppInfo("Telegram", "✈️"),
        AppInfo("WhatsApp", "🟢")
    )

    data class AppInfo(val name: String, val iconEmoji: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_others_app)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        appItemsContainer = findViewById(R.id.appItemsContainer)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvLogout)?.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadChildrenProfiles()
    }

    private fun loadChildrenProfiles() {
        val parentEmail = auth.currentUser?.email?.lowercase()?.trim() ?: return

        appItemsContainer.removeAllViews()
        showStatusMessage("Loading profiles...")

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                appItemsContainer.removeAllViews()
                if (documents.isEmpty) {
                    showStatusMessage("No children linked yet.")
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val childName = doc.getString("name") ?: "Child"
                    val childEmail = doc.getString("email") ?: ""
                    addProfileCard(childName, childEmail)
                }
            }
    }

    private fun addProfileCard(name: String, email: String) {
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
            setOnClickListener { loadUsageForChild(name, email) }
        }

        val iconTv = TextView(this).apply {
            text = name.take(1).uppercase()
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_circle_purple_light)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7E57C2"))
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val nameTv = TextView(this@OthersAppActivity).apply {
                text = name
                textSize = 18f
                setTextColor(Color.parseColor("#1A1A2E"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val subTv = TextView(this@OthersAppActivity).apply {
                text = "Tap to view social media usage"
                textSize = 13f
                setTextColor(Color.GRAY)
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
        appItemsContainer.addView(card)
    }

    private fun loadUsageForChild(name: String, email: String) {
        appItemsContainer.removeAllViews()
        
        val headerTv = TextView(this).apply {
            text = "App Usage: $name"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#7E57C2"))
            setPadding(0, 0, 0, (20 * density).toInt())
        }
        appItemsContainer.addView(headerTv)

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        db.collection("usage_logs")
            .whereEqualTo("childEmail", email)
            .get()
            .addOnSuccessListener { logs ->
                val usageMap = mutableMapOf<String, Long>()
                for (app in appsToTrack) usageMap[app.name] = 0L

                for (log in logs) {
                    val timestamp = log.getLong("timestamp") ?: 0L
                    if (timestamp >= todayStart) {
                        val appName = log.getString("appUsage") ?: ""
                        if (usageMap.containsKey(appName)) {
                            usageMap[appName] = (usageMap[appName] ?: 0L) + (log.getLong("durationSec") ?: 0L)
                        }
                    }
                }

                for (app in appsToTrack) {
                    addAppUsageRow(app, usageMap[app.name] ?: 0L)
                }

                val btnBack = TextView(this).apply {
                    text = "← Back to Profiles"
                    setTextColor(Color.parseColor("#7E57C2"))
                    gravity = Gravity.CENTER
                    setPadding(0, (40 * density).toInt(), 0, (40 * density).toInt())
                    setOnClickListener { loadChildrenProfiles() }
                }
                appItemsContainer.addView(btnBack)
            }
    }

    private fun addAppUsageRow(app: AppInfo, totalSeconds: Long) {
        val minutes = totalSeconds / 60
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconTv = TextView(this).apply { text = app.iconEmoji; textSize = 24f; setPadding(0, 0, 16, 0) }
        val nameTv = TextView(this).apply { 
            text = app.name; textSize = 17f; setTextColor(Color.parseColor("#1A1A2E"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val minTv = TextView(this).apply { 
            text = "$minutes minutes"; textSize = 15f; setTextColor(Color.parseColor("#7E57C2"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        topRow.addView(iconTv); topRow.addView(nameTv); topRow.addView(minTv)
        
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (8 * density).toInt())
            params.setMargins((40 * density).toInt(), (12 * density).toInt(), 0, 0)
            layoutParams = params
            max = 120 // 2 hour max scale
            progress = minutes.toInt().coerceAtMost(120)
            progressDrawable.setTint(Color.parseColor("#7E57C2"))
        }

        rowLayout.addView(topRow); rowLayout.addView(progressBar)
        appItemsContainer.addView(rowLayout)
    }

    private val density get() = resources.displayMetrics.density
    private fun showStatusMessage(msg: String) {
        val tv = TextView(this).apply { text = msg; gravity = Gravity.CENTER; setPadding(0, 100, 0, 0); setTextColor(Color.GRAY) }
        appItemsContainer.addView(tv)
    }
}
