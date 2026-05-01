package com.example.sppms

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SppmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
