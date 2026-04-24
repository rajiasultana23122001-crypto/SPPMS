package com.example.sppms

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class OthersAppActivity : AppCompatActivity() {

    private val appPackages = mapOf(
        "Facebook" to "com.facebook.katana",
        "Instagram" to "com.instagram.android",
        "Twitter" to "com.twitter.android",
        "Snapchat" to "com.snapchat.android",
        "Messenger" to "com.facebook.orca",
        "Telegram" to "org.telegram.messenger",
        "WhatsApp" to "com.whatsapp"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_others_app)

        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please allow Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadUsageData()
        }
    }

    private fun loadUsageData() {
        val usageMap = getTodayUsageMinutes()
        val maxValue = (usageMap.values.maxOrNull() ?: 1).coerceAtLeast(1)

        setUsage(R.id.tvFacebook, R.id.barFacebook, "Facebook", usageMap["Facebook"] ?: 0, maxValue)
        setUsage(R.id.tvInstagram, R.id.barInstagram, "Instagram", usageMap["Instagram"] ?: 0, maxValue)
        setUsage(R.id.tvTwitter, R.id.barTwitter, "Twitter", usageMap["Twitter"] ?: 0, maxValue)
        setUsage(R.id.tvSnapchat, R.id.barSnapchat, "Snapchat", usageMap["Snapchat"] ?: 0, maxValue)
        setUsage(R.id.tvMessenger, R.id.barMessenger, "Messenger", usageMap["Messenger"] ?: 0, maxValue)
        setUsage(R.id.tvTelegram, R.id.barTelegram, "Telegram", usageMap["Telegram"] ?: 0, maxValue)
        setUsage(R.id.tvWhatsapp, R.id.barWhatsapp, "WhatsApp", usageMap["WhatsApp"] ?: 0, maxValue)
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

    private fun getTodayUsageMinutes(): Map<String, Int> {
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

        val result = mutableMapOf<String, Int>()

        for ((appName, packageName) in appPackages) {
            val usage = stats
                .filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }

            result[appName] = (usage / 1000 / 60).toInt()
        }

        return result
    }

    private fun setUsage(
        textViewId: Int,
        progressBarId: Int,
        appName: String,
        minutes: Int,
        maxValue: Int
    ) {
        val textView = findViewById<TextView>(textViewId)
        val progressBar = findViewById<ProgressBar>(progressBarId)

        textView.text = "$appName: $minutes minutes"
        progressBar.max = maxValue
        progressBar.progress = minutes
    }
}