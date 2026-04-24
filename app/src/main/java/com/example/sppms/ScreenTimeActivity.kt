package com.example.sppms

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class ScreenTimeActivity : AppCompatActivity() {

    private lateinit var tvScreenTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_time)

        tvScreenTime = findViewById(R.id.tvScreenTime)

        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please allow Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            showTodayScreenTime()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::tvScreenTime.isInitialized && hasUsageAccessPermission()) {
            showTodayScreenTime()
        }
    }

    private fun showTodayScreenTime() {
        val totalMinutes = getTodayTotalScreenTimeMinutes()
        tvScreenTime.text = formatScreenTime(totalMinutes)
    }

    private fun getTodayTotalScreenTimeMinutes(): Int {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val totalMillis = stats.sumOf { it.totalTimeInForeground }

        return (totalMillis / 1000 / 60).toInt()
    }

    private fun formatScreenTime(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "$hours hr $minutes min"
            hours > 0 -> "$hours hr"
            else -> "$minutes min"
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}