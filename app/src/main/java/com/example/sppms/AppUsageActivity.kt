package com.example.sppms

import android.graphics.Color
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
        val parentEmail = auth.currentUser?.email

        db.collection("users")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { docs ->

                container.removeAllViews()

                if (docs.isEmpty) {
                    val tv = TextView(this)
                    tv.text = "No child found"
                    tv.textSize = 18f
                    container.addView(tv)
                    return@addOnSuccessListener
                }

                for (doc in docs) {

                    val name = doc.getString("name") ?: "Unknown"
                    val email = doc.getString("email") ?: ""
                    val app = doc.getString("appUsage") ?: "Unknown"
                    val video = doc.getString("videoTitle") ?: "Unknown title"
                    val category = doc.getString("category") ?: "Unknown"
                    val time = doc.getString("watchTime") ?: "0 sec"

                    val tv = TextView(this)

                    tv.text = """
                        Name: $name
                        Email: $email
                        App: $app
                        Video Title: $video
                        Category: $category
                        Time: $time
                    """.trimIndent()

                    tv.setPadding(16,16,16,16)
                    tv.setBackgroundColor(Color.WHITE)

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