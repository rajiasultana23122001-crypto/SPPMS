package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AlertsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var containerLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        containerLayout = findViewById(R.id.containerLayout)

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

                for (doc in documents) {
                    val childName = doc.getString("name") ?: "Child"
                    val childEmail = doc.getString("email") ?: ""
                    addProfileCard(childName, childEmail)
                }
            }
            .addOnFailureListener { e ->
                showStatusMessage("Error: ${e.message}")
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
            setOnClickListener { loadAlertsForChild(name, email) }
        }

        // Avatar Initial Circle
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
            val nameTv = TextView(this@AlertsActivity).apply {
                text = name
                textSize = 18f
                setTextColor(Color.parseColor("#1A1A2E"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val subTv = TextView(this@AlertsActivity).apply {
                text = "Tap to view alerts"
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
        containerLayout.addView(card)
    }

    private fun loadAlertsForChild(name: String, email: String) {
        containerLayout.removeAllViews()
        
        val headerTv = TextView(this).apply {
            text = "Alerts for $name"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#7E57C2"))
            setPadding(0, 0, 0, (20 * resources.displayMetrics.density).toInt())
        }
        containerLayout.addView(headerTv)

        // For now, alerts are tied to reels limits and general usage
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { query ->
                val doc = query.documents.firstOrNull()
                if (doc != null) {
                    val todaySec = doc.getLong("todayReelsSeconds") ?: 0L
                    val limit = doc.getLong("reelsDailyLimitMin") ?: 30L
                    val limitExceeded = (todaySec / 60) >= limit

                    if (limitExceeded) {
                        addAlertItemCard("⚠️ Reels Limit Exceeded", "Exceeded daily limit of $limit minutes today.")
                    } else {
                        addAlertItemCard("✅ Within Limits", "Reels usage is within the daily limit.")
                    }
                } else {
                    showStatusMessage("User data not found.")
                }

                val btnBackToProfiles = TextView(this).apply {
                    text = "← Back to Profiles"
                    setTextColor(Color.parseColor("#7E57C2"))
                    setPadding(0, (40 * resources.displayMetrics.density).toInt(), 0, (40 * resources.displayMetrics.density).toInt())
                    gravity = Gravity.CENTER
                    setOnClickListener { loadChildrenProfiles() }
                }
                containerLayout.addView(btnBackToProfiles)
            }
    }

    private fun addAlertItemCard(title: String, msg: String) {
        val density = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_background) 
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            elevation = 2 * density
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, (12 * density).toInt())
            layoutParams = params
        }

        val titleTv = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val msgTv = TextView(this).apply {
            text = msg
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, 8, 0, 0)
        }

        card.addView(titleTv)
        card.addView(msgTv)
        containerLayout.addView(card)
    }

    private fun showStatusMessage(msg: String) {
        val empty = TextView(this).apply {
            text = msg
            textSize = 16f
            setPadding(32, (40 * resources.displayMetrics.density).toInt(), 32, (40 * resources.displayMetrics.density).toInt())
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        containerLayout.addView(empty)
    }
}
