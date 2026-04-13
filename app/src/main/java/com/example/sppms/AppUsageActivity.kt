package com.example.sppms

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppUsageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_usage)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val container = findViewById<LinearLayout>(R.id.containerLayout)
        val currentUserEmail = auth.currentUser?.email

        db.collection("users")
            .whereEqualTo("parentEmail", currentUserEmail)
            .get()
            .addOnSuccessListener { documents ->

                container.removeAllViews()

                // title
                val titleText = TextView(this)
                titleText.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                titleText.text = "App Usage"
                titleText.textSize = 24f
                titleText.setPadding(12, 12, 12, 24)
                titleText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                container.addView(titleText)

                for (doc in documents) {

                    val name = doc.getString("name") ?: "Unknown"
                    val email = doc.getString("email") ?: "No email"
                    val appName = doc.getString("appUsage") ?: "No app data"
                    val youtubeTitle = doc.getString("youtubeTitle") ?: "No video data yet"
                    val watchTime = doc.getString("watchTime") ?: "No watch time yet"

                    val category = if (
                        youtubeTitle.lowercase().contains("math") ||
                        youtubeTitle.lowercase().contains("class") ||
                        youtubeTitle.lowercase().contains("science")
                    ) {
                        "Academic 📚"
                    } else {
                        "Entertainment 🎮"
                    }

                    val watchSeconds = watchTime
                        .replace("sec", "")
                        .replace("minutes", "")
                        .trim()
                        .toIntOrNull() ?: 0

                    val alertMessage = when {
                        category.contains("Entertainment") -> "Alert: Entertainment content"
                        watchSeconds >= 120 -> "Alert: Long screen time"
                        else -> "Alert: Safe usage"
                    }

                    val tv = TextView(this)
                    tv.text =
                        "Name: $name\n" +
                                "Email: $email\n" +
                                "App: $appName\n" +
                                "Video: $youtubeTitle\n" +
                                "Category: $category\n" +
                                "Time: $watchTime\n" +
                                "$alertMessage"

                    tv.textSize = 16f
                    tv.setPadding(16, 16, 16, 16)
                    tv.setBackgroundColor(android.graphics.Color.WHITE)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.bottomMargin = 20
                    tv.layoutParams = params

                    container.addView(tv)
                }
            }
    }
}