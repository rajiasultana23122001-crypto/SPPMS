package com.example.sppms

import android.graphics.Color
import android.os.Bundle
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
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_last24_hour_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        barChart = findViewById(R.id.barChart)

        loadGraphData()
    }

    private fun loadGraphData() {
        val parentEmail = auth.currentUser?.email ?: return

        db.collection("users")
            .whereEqualTo("parentEmail", parentEmail)
            .get()
            .addOnSuccessListener { childDocs ->

                if (childDocs.isEmpty) {
                    Toast.makeText(this, "No linked children found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val childEmails = childDocs.documents.mapNotNull { it.getString("email") }
                val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

                db.collection("usage_logs")
                    .whereGreaterThanOrEqualTo("timestamp", last24Hours)
                    .get()
                    .addOnSuccessListener { logDocs ->

                        val hourBuckets = MutableList(24) { 0f }

                        for (doc in logDocs) {
                            val email = doc.getString("childEmail") ?: continue
                            val app = doc.getString("appUsage") ?: continue
                            val durationSec = doc.getLong("durationSec") ?: 0L
                            val timestamp = doc.getLong("timestamp") ?: continue

                            if (email in childEmails && app.equals("YouTube", ignoreCase = true)) {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = timestamp
                                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                                hourBuckets[hour] += durationSec.toFloat() / 60f
                            }
                        }

                        showBarChart(hourBuckets)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to load log data", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load children", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showBarChart(hourBuckets: List<Float>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        for (i in 0 until 24) {
            entries.add(BarEntry(i.toFloat(), hourBuckets[i]))
            labels.add(i.toString())
        }

        val dataSet = BarDataSet(entries, "YouTube usage (minutes)")
        dataSet.valueTextColor = Color.BLACK
        dataSet.color = Color.parseColor("#8E7CC3")

        val data = BarData(dataSet)
        data.barWidth = 0.9f

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.legend.textColor = Color.BLACK

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = 0f
        xAxis.textColor = Color.BLACK

        barChart.axisLeft.textColor = Color.BLACK
        barChart.axisRight.isEnabled = false
        barChart.invalidate()
    }
}
