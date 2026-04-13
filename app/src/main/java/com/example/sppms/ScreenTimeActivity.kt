package com.example.sppms

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ScreenTimeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_time)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val container = findViewById<LinearLayout>(R.id.containerLayout)
        val currentUserEmail = auth.currentUser?.email

        db.collection("users")
            .whereEqualTo("parentEmail", currentUserEmail)
            .get()
            .addOnSuccessListener { documents ->

                for (doc in documents) {

                    val name = doc.getString("name") ?: "Unknown"
                    val appName = doc.getString("appUsage") ?: "No app data"
                    val watchTime = doc.getString("watchTime") ?: "0 sec"

                    val tv = TextView(this)
                    tv.text = "Name: $name\nApp: $appName\nScreen Time: $watchTime"
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