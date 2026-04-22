package com.example.sppms

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReelsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var containerLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reels)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        containerLayout = findViewById(R.id.containerLayout)

        loadReelsData()
    }

    private fun loadReelsData() {
        val parentEmail = auth.currentUser?.email ?: return

        db.collection("users")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                containerLayout.removeAllViews()

                val titleText = TextView(this)
                titleText.text = "Reels Control"
                titleText.textSize = 24f
                titleText.setTextColor(Color.BLACK)
                titleText.setPadding(16, 16, 16, 24)
                containerLayout.addView(titleText)

                if (documents.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No child data found"
                    emptyText.textSize = 18f
                    emptyText.setPadding(16, 16, 16, 16)
                    containerLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val childUid = doc.id
                    val name = doc.getString("name") ?: "Unknown"
                    val email = doc.getString("email") ?: "No email"

                    val reelsStatus = doc.getString("reelsStatus") ?: "Stopped"
                    val reelsApp = doc.getString("reelsApp") ?: "Not detected"
                    val startedAtMillis = doc.getLong("reelsStartedAt") ?: 0L
                    val todayReelsSeconds = doc.getLong("todayReelsSeconds") ?: 0L
                    val reelsDailyLimitMin = doc.getLong("reelsDailyLimitMin") ?: 30L

                    val startedAtText = if (startedAtMillis > 0) {
                        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        formatter.format(Date(startedAtMillis))
                    } else {
                        "Not started"
                    }

                    val todayMinutes = todayReelsSeconds / 60
                    val limitExceeded = todayMinutes >= reelsDailyLimitMin

                    val cardLayout = LinearLayout(this)
                    cardLayout.orientation = LinearLayout.VERTICAL
                    cardLayout.setBackgroundColor(Color.WHITE)
                    cardLayout.setPadding(20, 20, 20, 20)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(16, 0, 16, 20)
                    cardLayout.layoutParams = params

                    val tvInfo = TextView(this)
                    tvInfo.text = """
                        Name: $name
                        Email: $email
                        
                        1. Reels Status
                        Status: $reelsStatus
                        App: $reelsApp
                        Started at: $startedAtText
                        
                        2. Time Limitation
                        Current daily limit: $reelsDailyLimitMin min
                        ${if (limitExceeded) "Warning: Today's limit exceeded" else "Within daily limit"}
                        
                        3. Today's Reels Time
                        Today watched: $todayMinutes min
                    """.trimIndent()
                    tvInfo.textSize = 18f
                    tvInfo.setTextColor(Color.BLACK)

                    val etLimit = EditText(this)
                    etLimit.hint = "Set daily reels limit (minutes)"
                    etLimit.inputType = InputType.TYPE_CLASS_NUMBER
                    etLimit.setText(reelsDailyLimitMin.toString())

                    val btnSaveLimit = Button(this)
                    btnSaveLimit.text = "Save Limit"
                    btnSaveLimit.setTextColor(Color.WHITE)
                    btnSaveLimit.setBackgroundColor(Color.parseColor("#6D6D6D"))

                    btnSaveLimit.setOnClickListener {
                        val newLimit = etLimit.text.toString().trim().toLongOrNull()
                        if (newLimit == null || newLimit <= 0) {
                            Toast.makeText(this, "Enter valid minutes", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        db.collection("users")
                            .document(childUid)
                            .update("reelsDailyLimitMin", newLimit)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Limit saved", Toast.LENGTH_SHORT).show()
                                loadReelsData()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save limit", Toast.LENGTH_SHORT).show()
                            }
                    }

                    cardLayout.addView(tvInfo)
                    cardLayout.addView(etLimit)
                    cardLayout.addView(btnSaveLimit)

                    containerLayout.addView(cardLayout)
                }
            }
            .addOnFailureListener {
                containerLayout.removeAllViews()

                val errorText = TextView(this)
                errorText.text = "Failed to load reels data"
                errorText.textSize = 18f
                errorText.setPadding(16, 16, 16, 16)
                containerLayout.addView(errorText)
            }
    }
}