package com.example.sppms

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        container = findViewById(R.id.locationContainer)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        loadChildrenProfiles()
    }

    private fun loadChildrenProfiles() {
        val parentEmail = auth.currentUser?.email?.lowercase()?.trim() ?: return

        container.removeAllViews()
        showStatusMessage("Loading profiles...")

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                container.removeAllViews()

                if (documents.isEmpty) {
                    showStatusMessage("No children linked yet.")
                    return@addOnSuccessListener
                }

                val title = TextView(this).apply {
                    text = "Select a child to track"
                    textSize = 18f
                    setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.primary_dark_purple))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 24.toPx())
                }
                container.addView(title)

                for (doc in documents) {
                    val name = doc.getString("name") ?: "Child"
                    val email = doc.getString("email") ?: ""
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    val updated = doc.getLong("locationUpdatedAt") ?: 0L
                    
                    addProfileCard(name, email, lat, lng, updated)
                }
            }
    }

    private fun addProfileCard(name: String, email: String, lat: Double?, lng: Double?, updated: Long) {
        val density = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.card_background)
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            elevation = 4 * density
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, (16 * density).toInt())
            layoutParams = params
            setOnClickListener { showLocationDetail(name, email, lat, lng, updated) }
        }

        val iconTv = TextView(this).apply {
            text = name.take(1).uppercase()
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_circle_purple_light)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this@LocationActivity, R.color.primary_purple))
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val nameTv = TextView(this@LocationActivity).apply {
                text = name
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.text_dark))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val subTv = TextView(this@LocationActivity).apply {
                text = if (lat != null && lng != null && lat != 0.0) "📍 Location available" else "📍 No location yet"
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.text_gray_light))
            }
            addView(nameTv)
            addView(subTv)
        }

        card.addView(iconTv)
        card.addView(textLayout)
        container.addView(card)
    }

    private fun showLocationDetail(name: String, email: String, lat: Double?, lng: Double?, updated: Long) {
        container.removeAllViews()

        val header = TextView(this).apply {
            text = "Location: $name"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.primary_dark_purple))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24.toPx())
        }
        container.addView(header)

        val detailCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_background)
            setPadding(24.toPx(), 24.toPx(), 24.toPx(), 24.toPx())
            elevation = 4 * resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val tvEmail = TextView(this).apply {
            text = "✉️ $email"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.text_dark))
            setPadding(0, 0, 0, 12.toPx())
        }
        detailCard.addView(tvEmail)

        val locationText = if (lat != null && lng != null && lat != 0.0) {
            "📍 Coordinates: $lat, $lng"
        } else {
            "📍 Location not available yet"
        }
        val tvLoc = TextView(this).apply {
            text = locationText
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.text_dark))
            setPadding(0, 0, 0, 8.toPx())
        }
        detailCard.addView(tvLoc)

        if (updated > 0) {
            val minutesAgo = (System.currentTimeMillis() - updated) / 1000 / 60
            val tvUpdated = TextView(this).apply {
                text = "🕒 Updated $minutesAgo min ago"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.text_gray_light))
                setPadding(0, 0, 0, 16.toPx())
            }
            detailCard.addView(tvUpdated)
        }

        if (lat != null && lng != null && lat != 0.0) {
            val btnViewMap = Button(this).apply {
                text = "View on Map"
                setBackgroundColor(ContextCompat.getColor(this@LocationActivity, R.color.primary_purple))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    val intent = Intent(this@LocationActivity, ChildLocationMapActivity::class.java)
                    intent.putExtra("childName", name)
                    intent.putExtra("latitude", lat)
                    intent.putExtra("longitude", lng)
                    startActivity(intent)
                }
            }
            detailCard.addView(btnViewMap)

            val btnGoogleMaps = Button(this).apply {
                text = "Open in Google Maps"
                setBackgroundColor(Color.parseColor("#43A047")) // Keep green for maps if desired, or use a resource
                setTextColor(Color.WHITE)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 12.toPx(), 0, 0)
                layoutParams = params
                setOnClickListener {
                    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                }
            }
            detailCard.addView(btnGoogleMaps)
        }

        container.addView(detailCard)

        val btnBack = TextView(this).apply {
            text = "← Back to Profiles"
            setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.primary_purple))
            gravity = Gravity.CENTER
            setPadding(0, 40.toPx(), 0, 40.toPx())
            setOnClickListener { loadChildrenProfiles() }
        }
        container.addView(btnBack)
    }

    private fun showStatusMessage(msg: String) {
        val tv = TextView(this).apply {
            text = msg
            gravity = Gravity.CENTER
            setPadding(0, 100.toPx(), 0, 0)
            setTextColor(ContextCompat.getColor(this@LocationActivity, R.color.text_gray_light))
        }
        container.addView(tv)
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}
