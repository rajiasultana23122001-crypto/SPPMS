package com.example.sppms

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AppUsageActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var containerLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_usage)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        containerLayout = findViewById(R.id.containerLayout)

        loadYouTubeHistory()
    }

    private fun loadYouTubeHistory() {
        val parentEmail = auth.currentUser?.email ?: return

        db.collection("users")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->

                containerLayout.removeAllViews()

                val title = TextView(this)
                title.text = "YouTube History"
                title.textSize = 24f
                title.setTextColor(Color.BLACK)
                title.setPadding(16, 16, 16, 24)
                containerLayout.addView(title)

                if (documents.isEmpty) {
                    val empty = TextView(this)
                    empty.text = "No child data found"
                    empty.textSize = 18f
                    containerLayout.addView(empty)
                    return@addOnSuccessListener
                }

                for (doc in documents) {

                    val childName = doc.getString("childName") ?: "Unknown"
                    val videoTitle = doc.getString("videoTitle") ?: "Unknown"
                    val category = doc.getString("category") ?: "Unknown"
                    val watchTime = doc.getString("watchTime") ?: "0 sec"
                    val timestamp = doc.getLong("lastUpdated") ?: 0L

                    val timeText = if (timestamp > 0) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        sdf.format(Date(timestamp))
                    } else {
                        "Unknown time"
                    }

                    // 🎴 Card Layout
                    val card = LinearLayout(this)
                    card.orientation = LinearLayout.VERTICAL
                    card.setBackgroundColor(Color.WHITE)
                    card.setPadding(20, 20, 20, 20)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(16, 0, 16, 20)
                    card.layoutParams = params

                    val tv = TextView(this)
                    tv.text = """
                        👤 Child: $childName
                        
                        ▶️ Video: $videoTitle
                        📂 Category: $category
                        ⏱️ Watch Time: $watchTime
                        🕒 Time: $timeText
                    """.trimIndent()

                    tv.textSize = 18f
                    tv.setTextColor(Color.BLACK)

                    card.addView(tv)
                    containerLayout.addView(card)
                }
            }
            .addOnFailureListener {
                containerLayout.removeAllViews()

                val error = TextView(this)
                error.text = "Failed to load data"
                error.textSize = 18f
                containerLayout.addView(error)
            }
    }
}