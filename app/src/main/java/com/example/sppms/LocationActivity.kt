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

class LocationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var containerLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        containerLayout = findViewById(R.id.containerLayout)

        loadChildrenLocations()
    }

    private fun loadChildrenLocations() {
        val parentEmail = auth.currentUser?.email ?: return

        db.collection("users")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                containerLayout.removeAllViews()

                val titleText = TextView(this)
                titleText.text = "Children Location"
                titleText.textSize = 24f
                titleText.setTextColor(Color.BLACK)
                titleText.setPadding(16, 16, 16, 24)
                containerLayout.addView(titleText)

                if (documents.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No child location found"
                    emptyText.textSize = 18f
                    emptyText.setPadding(16, 16, 16, 16)
                    containerLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val name = doc.getString("name") ?: "Unknown"
                    val email = doc.getString("email") ?: "No email"
                    val latitude = doc.getDouble("latitude")
                    val longitude = doc.getDouble("longitude")

                    val locationText = if (latitude != null && longitude != null) {
                        "$latitude, $longitude"
                    } else {
                        "Not available"
                    }

                    val childLayout = LinearLayout(this)
                    childLayout.orientation = LinearLayout.VERTICAL
                    childLayout.setBackgroundColor(Color.WHITE)
                    childLayout.setPadding(20, 20, 20, 20)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(16, 0, 16, 20)
                    childLayout.layoutParams = params

                    val tvInfo = TextView(this)
                    tvInfo.text = """
                        Name: $name
                        Email: $email
                        Location: $locationText
                    """.trimIndent()
                    tvInfo.textSize = 18f
                    tvInfo.setTextColor(Color.BLACK)

                    val btnViewLocation = Button(this)
                    btnViewLocation.text = "View Location"
                    btnViewLocation.setTextColor(Color.WHITE)
                    btnViewLocation.setBackgroundColor(Color.parseColor("#6D6D6D"))

                    btnViewLocation.setOnClickListener {
                        if (latitude != null && longitude != null) {
                            val intent = Intent(this, ChildLocationMapActivity::class.java)
                            intent.putExtra("childName", name)
                            intent.putExtra("latitude", latitude)
                            intent.putExtra("longitude", longitude)
                            startActivity(intent)
                        }
                    }

                    childLayout.addView(tvInfo)
                    childLayout.addView(btnViewLocation)

                    containerLayout.addView(childLayout)
                }
            }
            .addOnFailureListener {
                containerLayout.removeAllViews()

                val errorText = TextView(this)
                errorText.text = "Failed to load location"
                errorText.textSize = 18f
                errorText.setPadding(16, 16, 16, 16)
                containerLayout.addView(errorText)
            }
    }
}