package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChildrenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var childListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_children)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        childListContainer = findViewById(R.id.childListContainer)

        loadChildren()
    }

    private fun loadChildren() {
        val currentUser = auth.currentUser ?: return
        val parentEmail = currentUser.email?.trim()?.lowercase() ?: return

        childListContainer.removeAllViews()

        val loadingTv = TextView(this)
        loadingTv.text = "Loading children..."
        loadingTv.textSize = 16f
        loadingTv.setPadding(16, 16, 16, 16)
        childListContainer.addView(loadingTv)

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { result ->
                childListContainer.removeAllViews()

                if (result.isEmpty) {
                    val tv = TextView(this)
                    tv.text = "No child linked yet.\n\nTo link a child:\n1. Register a Child account\n2. Enter your email ($parentEmail) as the Parent Email"
                    tv.textSize = 15f
                    tv.setPadding(16, 16, 16, 16)
                    tv.setTextColor(Color.parseColor("#424242"))
                    childListContainer.addView(tv)
                } else {
                    for (document in result) {
                        val childName = document.getString("name") ?: "Unknown"
                        val childAge = document.getString("childAge") ?: "N/A"
                        val childEmail = document.getString("email") ?: ""
                        val childUid = document.id
                        val latitude = document.getDouble("latitude")
                        val longitude = document.getDouble("longitude")

                        val card = LinearLayout(this)
                        card.orientation = LinearLayout.VERTICAL
                        card.setBackgroundColor(Color.WHITE)
                        card.setPadding(20, 20, 20, 20)

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.bottomMargin = 20
                        card.layoutParams = params

                        val tvInfo = TextView(this)
                        tvInfo.text = "👤 Name: $childName\n🎂 Age: $childAge\n✉️ Email: $childEmail"
                        tvInfo.textSize = 16f
                        tvInfo.setPadding(0, 0, 0, 12)
                        tvInfo.setTextColor(Color.parseColor("#1A1A2E"))
                        card.addView(tvInfo)

                        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
                            val tvLoc = TextView(this)
                            tvLoc.text = "📍 Location: $latitude, $longitude"
                            tvLoc.textSize = 14f
                            tvLoc.setTextColor(Color.parseColor("#5C35C4"))
                            tvLoc.setPadding(0, 0, 0, 12)
                            card.addView(tvLoc)

                            val btnViewMap = Button(this)
                            btnViewMap.text = "📍 View on Map"
                            btnViewMap.setTextColor(Color.WHITE)
                            btnViewMap.setBackgroundColor(Color.parseColor("#5C35C4"))
                            btnViewMap.setOnClickListener {
                                val intent = Intent(this, ChildLocationMapActivity::class.java)
                                intent.putExtra("childName", childName)
                                intent.putExtra("latitude", latitude)
                                intent.putExtra("longitude", longitude)
                                startActivity(intent)
                            }
                            card.addView(btnViewMap)
                        } else {
                            val tvNoLoc = TextView(this)
                            tvNoLoc.text = "📍 Location: Not available yet"
                            tvNoLoc.textSize = 14f
                            tvNoLoc.setTextColor(Color.GRAY)
                            card.addView(tvNoLoc)
                        }

                        childListContainer.addView(card)
                    }
                }
            }
            .addOnFailureListener { e ->
                childListContainer.removeAllViews()
                val tv = TextView(this)
                tv.text = "Failed to load children: ${e.message}"
                tv.textSize = 15f
                tv.setPadding(16, 16, 16, 16)
                childListContainer.addView(tv)
            }
    }
}
