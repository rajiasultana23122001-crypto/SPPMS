package com.example.sppms

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Last24HourHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var containerLayout: LinearLayout
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_last24_hour_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Find the root layout or the chart
        barChart = findViewById(R.id.barChart)
        barChart.visibility = View.GONE
        
        // We'll use the parent of the chart as our container
        containerLayout = barChart.parent as LinearLayout

        loadChildrenProfiles()
    }

    private fun loadChildrenProfiles() {
        val parentEmail = auth.currentUser?.email?.lowercase()?.trim() ?: return

        containerLayout.removeAllViews()
        barChart.visibility = View.GONE
        
        showStatusMessage("Select a child to view graph")

        db.collection("users")
            .whereEqualTo("role", "child")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { documents ->
                containerLayout.removeAllViews()
                if (documents.isEmpty) {
                    showStatusMessage("No children linked yet.")
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val name = doc.getString("name") ?: "Child"
                    val email = doc.getString("email") ?: ""
                    addProfileCard(name, email)
                }
            }
    }

    private fun addProfileCard(name: String, email: String) {
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
            setOnClickListener { loadGraphForChild(name, email) }
        }

        val iconTv = TextView(this).apply {
            text = name.take(1).uppercase()
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_circle_purple_light)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7E57C2"))
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
        }

        val nameTv = TextView(this).apply {
            text = name
            textSize = 18f
            setPadding((16 * density).toInt(), 0, 0, 0)
            setTextColor(Color.parseColor("#1A1A2E"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val arrowTv = TextView(this).apply { text = ">"; textSize = 20f; setTextColor(Color.parseColor("#7E57C2")) }

        card.addView(iconTv); card.addView(nameTv); card.addView(arrowTv)
        containerLayout.addView(card)
    }

    private fun loadGraphForChild(name: String, email: String) {
        containerLayout.removeAllViews()
        containerLayout.addView(barChart)
        barChart.visibility = View.VISIBLE

        val hourBuckets = MutableList(24) { 0f }
        val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        db.collection("usage_logs")
            .whereEqualTo("childEmail", email)
            .get()
            .addOnSuccessListener { logs ->
                for (doc in logs) {
                    val app = doc.getString("appUsage") ?: ""
                    val durationSec = doc.getLong("durationSec") ?: 0L
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    if (timestamp >= last24Hours && app.equals("YouTube", ignoreCase = true)) {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = timestamp
                        val hour = calendar.get(Calendar.HOUR_OF_DAY)
                        hourBuckets[hour] += durationSec.toFloat() / 60f
                    }
                }
                showBarChart(hourBuckets, name)

                val btnBack = TextView(this).apply {
                    text = "← Back to Profiles"
                    setTextColor(Color.parseColor("#7E57C2"))
                    gravity = Gravity.CENTER
                    setPadding(0, 40, 0, 40)
                    setOnClickListener { loadChildrenProfiles() }
                }
                containerLayout.addView(btnBack)
            }
    }

    private fun showBarChart(hourBuckets: List<Float>, name: String) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        for (i in 0 until 24) {
            entries.add(BarEntry(i.toFloat(), hourBuckets[i]))
            val hourLabel = if (i == 0) "12a" else if (i < 12) "${i}a" else if (i == 12) "12p" else "${i - 12}p"
            labels.add(hourLabel)
        }

        val dataSet = BarDataSet(entries, "YouTube usage (min) for $name")
        dataSet.color = Color.parseColor("#7C4DFF")
        dataSet.valueTextColor = Color.BLACK

        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.axisRight.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun showStatusMessage(msg: String) {
        val tv = TextView(this).apply { text = msg; gravity = Gravity.CENTER; setPadding(0, 40, 0, 40); textSize = 16f }
        containerLayout.addView(tv)
    }
}
