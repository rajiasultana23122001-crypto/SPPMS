package com.example.sppms

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class ScreenTimeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var childrenContainer: LinearLayout
    private lateinit var tvStatusInfo: TextView
    private lateinit var tvTotalScreenTime: TextView
    private lateinit var tvChildNameLabel: TextView
    private lateinit var tvIcon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_time)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        tvStatusInfo = findViewById(R.id.tvStatusInfo)
        childrenContainer = findViewById(R.id.childrenContainer)
        tvTotalScreenTime = findViewById(R.id.tvTotalScreenTime)
        tvChildNameLabel = findViewById(R.id.tvChildNameLabel)
        tvIcon = findViewById(R.id.tvIcon)

        loadChildrenProfiles()
    }

    private fun loadChildrenProfiles() {
        val currentUserEmail = auth.currentUser?.email ?: return
        val parentEmailLower = currentUserEmail.lowercase().trim()

        tvStatusInfo.text = "Loading profiles..."
        tvStatusInfo.visibility = View.VISIBLE
        childrenContainer.removeAllViews()

        db.collection("users")
            .whereEqualTo("role", "child")
            .get()
            .addOnSuccessListener { documents ->
                val linkedChildren = documents.filter { 
                    val pEmail = it.getString("parentEmail")?.lowercase()?.trim() ?: ""
                    pEmail == parentEmailLower
                }

                if (linkedChildren.isEmpty()) {
                    tvStatusInfo.text = "No children linked."
                    tvStatusInfo.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                tvStatusInfo.visibility = View.GONE

                for (doc in linkedChildren) {
                    val childName = doc.getString("name") ?: "Child"
                    val childEmail = doc.getString("email") ?: ""
                    val currentWatchTimeStr = doc.getString("watchTime") ?: "0 sec"
                    
                    addProfileCard(childName, childEmail, currentWatchTimeStr)
                }
            }
            .addOnFailureListener {
                tvStatusInfo.text = "Error loading children."
            }
    }

    private fun addProfileCard(name: String, email: String, liveSessionStr: String) {
        val density = resources.displayMetrics.density
        
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.setBackgroundResource(R.drawable.card_background)
        card.setPadding((20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt(), (20 * density).toInt())
        card.elevation = 4 * density
        card.gravity = android.view.Gravity.CENTER_VERTICAL
        
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, (20 * density).toInt())
        card.layoutParams = params

        // Profile Initial Circle
        val initialTv = TextView(this)
        initialTv.text = if (name.isNotEmpty()) name.take(1).uppercase() else "?"
        initialTv.setTextColor(Color.WHITE)
        initialTv.textSize = 18f
        initialTv.setTypeface(null, android.graphics.Typeface.BOLD)
        initialTv.gravity = android.view.Gravity.CENTER
        initialTv.setBackgroundResource(R.drawable.bg_circle_purple_light)
        initialTv.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7E57C2"))
        
        val iconParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
        initialTv.layoutParams = iconParams
        
        // Name Layout
        val nameLayout = LinearLayout(this)
        nameLayout.orientation = LinearLayout.VERTICAL
        nameLayout.setPadding((16 * density).toInt(), 0, 0, 0)
        nameLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val nameTv = TextView(this)
        nameTv.text = name
        nameTv.textSize = 18f
        nameTv.setTextColor(Color.parseColor("#1A1A2E"))
        nameTv.setTypeface(null, android.graphics.Typeface.BOLD)

        val subTv = TextView(this)
        subTv.text = "Tap to view today's usage"
        subTv.textSize = 13f
        subTv.setTextColor(Color.GRAY)

        nameLayout.addView(nameTv)
        nameLayout.addView(subTv)

        val arrowTv = TextView(this)
        arrowTv.text = ">"
        arrowTv.textSize = 20f
        arrowTv.setTextColor(Color.parseColor("#7E57C2"))

        card.addView(initialTv)
        card.addView(nameLayout)
        card.addView(arrowTv)

        card.setOnClickListener {
            calculateAndShowTime(name, email, liveSessionStr)
        }

        childrenContainer.addView(card)
    }

    private fun calculateAndShowTime(name: String, email: String, liveSessionStr: String) {
        tvStatusInfo.text = "Calculating..."
        tvStatusInfo.visibility = View.VISIBLE
        
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
                var totalSeconds = 0L
                for (doc in logs) {
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    if (timestamp >= todayStart) {
                        totalSeconds += doc.getLong("durationSec") ?: 0L
                    }
                }
                
                // Add current live session
                val currentSec = liveSessionStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
                totalSeconds += currentSec

                val minutes = totalSeconds / 60
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                
                val timeText = String.format(Locale.US, "%dh %dm", hours, remainingMinutes)
                
                // Update Top UI
                tvTotalScreenTime.text = "$timeText / 12h used"
                tvChildNameLabel.text = name
                tvIcon.text = "📈"
                
                tvStatusInfo.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch logs", Toast.LENGTH_SHORT).show()
                tvStatusInfo.visibility = View.GONE
            }
    }
}
