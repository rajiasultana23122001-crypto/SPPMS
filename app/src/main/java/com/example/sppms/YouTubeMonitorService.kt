package com.example.sppms

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class YouTubeMonitorService : AccessibilityService() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != "com.google.android.youtube") return

        val user = auth.currentUser ?: return
        val root = rootInActiveWindow ?: return

        val allTexts = mutableListOf<String>()
        collectAllTexts(root, allTexts)

        for (text in allTexts) {
            Log.d("YT_DEBUG", "TEXT = $text")
        }

        val candidate = allTexts
            .map { it.trim() }
            .filter { it.length > 10 }
            .distinct()
            .firstOrNull() ?: return

        val now = System.currentTimeMillis()

        db.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "youtubeTitle" to candidate,
                    "lastYoutubeUpdate" to now
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d("YT_DEBUG", "Saved to Firestore")
            }
            .addOnFailureListener {
                Log.e("YT_DEBUG", "Firestore save failed", it)
            }
    }

    override fun onInterrupt() {}

    private fun collectAllTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            out.add(text)
        }

        val desc = node.contentDescription?.toString()?.trim()
        if (!desc.isNullOrBlank()) {
            out.add(desc)
        }

        for (i in 0 until node.childCount) {
            collectAllTexts(node.getChild(i), out)
        }
    }
}