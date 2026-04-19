package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

                val titleText = TextView(this)
                titleText.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                titleText.text = "Child Activity & Location"
                titleText.textSize = 24f
                titleText.setPadding(12, 12, 12, 24)
                titleText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                container.addView(titleText)

                if (documents.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No child data found"
                    emptyText.textSize = 18f
                    emptyText.setPadding(16, 16, 16, 16)
                    container.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val name = doc.getString("name") ?: "Unknown"
                    val email = doc.getString("email") ?: "No email"
                    val appName = doc.getString("appUsage") ?: "No app data"

                    val contentType = doc.getString("contentType") ?: "video"
                    val videoTitle = doc.getString("videoTitle") ?: "Unknown title"
                    val shortTitle = doc.getString("shortTitle") ?: "Unknown short"
                    val watchTime = doc.getString("watchTime") ?: "0 sec"
                    val shortWatchTime = doc.getString("shortWatchTime") ?: "0 sec"

                    val shownTitle = if (contentType == "short") shortTitle else videoTitle
                    val shownTime = if (contentType == "short") shortWatchTime else watchTime

                    val firestoreCategory = doc.getString("category") ?: ""
                    val finalCategory = if (firestoreCategory.isNotBlank()) {
                        firestoreCategory
                    } else {
                        classifyVideo(shownTitle)
                    }

                    val watchSeconds = extractSeconds(shownTime)
                    val latitude = doc.getDouble("latitude")
                    val longitude = doc.getDouble("longitude")

                    val alertMessage = when {
                        finalCategory.equals("Entertainment", ignoreCase = true) ->
                            "Warning: Entertainment content detected"

                        watchSeconds >= 120 ->
                            "Warning: High screen usage detected"

                        finalCategory.equals("Academic", ignoreCase = true) ->
                            "Safe: Educational content detected"

                        else ->
                            "Info: Unable to classify content"
                    }

                    val cardLayout = LinearLayout(this)
                    cardLayout.orientation = LinearLayout.VERTICAL
                    cardLayout.setBackgroundColor(Color.WHITE)
                    cardLayout.setPadding(16, 16, 16, 16)

                    val cardParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    cardParams.bottomMargin = 24
                    cardLayout.layoutParams = cardParams

                    val infoText = TextView(this)
                    infoText.text = """
                        Name: $name
                        Email: $email
                        App: $appName
                        Type: $contentType
                        Title: $shownTitle
                        Category: $finalCategory
                        Time: $shownTime
                        Location: ${if (latitude != null && longitude != null) "$latitude, $longitude" else "Not available"}
                        $alertMessage
                    """.trimIndent()
                    infoText.textSize = 16f
                    infoText.setTextColor(Color.BLACK)

                    val btnViewLocation = Button(this)
                    btnViewLocation.text = "View Location"
                    btnViewLocation.setOnClickListener {
                        if (latitude != null && longitude != null) {
                            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($name)")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")

                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            } else {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
                                )
                                startActivity(browserIntent)
                            }
                        } else {
                            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                        }
                    }

                    cardLayout.addView(infoText)
                    cardLayout.addView(btnViewLocation)
                    container.addView(cardLayout)
                }
            }
            .addOnFailureListener {
                container.removeAllViews()

                val errorText = TextView(this)
                errorText.text = "Failed to load data"
                errorText.textSize = 18f
                errorText.setPadding(16, 16, 16, 16)
                container.addView(errorText)
            }
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
            title.equals("Unknown title", ignoreCase = true) -> "Unknown"
            academicKeywords.any { t.contains(it) } -> "Academic"
            entertainmentKeywords.any { t.contains(it) } -> "Entertainment"
            else -> "Unknown"
        }
    }

    private fun extractSeconds(watchTime: String): Int {
        val clean = watchTime.lowercase().trim()

        return when {
            clean.contains("minute") -> {
                val number = clean
                    .replace("minutes", "")
                    .replace("minute", "")
                    .trim()
                    .toIntOrNull() ?: 0
                number * 60
            }

            clean.contains("sec") -> {
                clean.replace("sec", "").trim().toIntOrNull() ?: 0
            }

            else -> clean.toIntOrNull() ?: 0
        }
    }
}