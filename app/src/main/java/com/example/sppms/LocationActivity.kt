package com.example.sppms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LocationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        val tvChildName = findViewById<TextView>(R.id.tvChildName)
        val tvChildEmail = findViewById<TextView>(R.id.tvChildEmail)
        val tvLocation = findViewById<TextView>(R.id.tvLocation)
        val btnViewLocation = findViewById<Button>(R.id.btnViewLocation)

        val childName = "Ethan Ibne Ferdous"
        val childEmail = "ehan@gmail.com"
        val locationText = "23.8100877, 90.3556386"

        tvChildName.text = "Name: $childName"
        tvChildEmail.text = "Email: $childEmail"
        tvLocation.text = "Location: $locationText"

        btnViewLocation.setOnClickListener {
            if (locationText.isNotBlank() && locationText.contains(",")) {
                val parts = locationText.split(",")
                val lat = parts[0].trim()
                val lng = parts[1].trim()

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
