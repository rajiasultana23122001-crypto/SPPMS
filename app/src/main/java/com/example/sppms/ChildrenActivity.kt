package com.example.sppms

import android.os.Bundle
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
        val parentEmail = currentUser.email?.trim() ?: return

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { result ->
                childListContainer.removeAllViews()

                if (result.isEmpty) {
                    val tv = TextView(this)
                    tv.text = "No child linked yet"
                    tv.textSize = 16f
                    tv.setPadding(16, 16, 16, 16)
                    tv.setBackgroundColor(android.graphics.Color.WHITE)
                    childListContainer.addView(tv)
                } else {
                    for (document in result) {
                        val childName = document.getString("name") ?: "Unknown"
                        val childAge = document.getString("childAge") ?: "N/A"
                        val childEmail = document.getString("email") ?: ""

                        val tv = TextView(this)
                        tv.text = "Name: $childName\nAge: $childAge\nEmail: $childEmail"
                        tv.textSize = 16f
                        tv.setPadding(16, 16, 16, 16)
                        tv.setBackgroundColor(android.graphics.Color.WHITE)

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.bottomMargin = 16
                        tv.layoutParams = params

                        childListContainer.addView(tv)
                    }
                }
            }
    }
}