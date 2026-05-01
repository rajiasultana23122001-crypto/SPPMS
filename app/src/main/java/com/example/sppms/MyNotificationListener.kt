package com.example.sppms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListener : NotificationListenerService() {

    companion object {
        var latestTitle: String? = null
        var latestText: String? = null
        var latestPackage: String? = null
        var latestBigText: String? = null
        var latestSubText: String? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification.extras

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

            latestPackage = sbn.packageName
            latestTitle = title
            latestText = text
            latestBigText = bigText
            latestSubText = subText

            Log.d("PARENTRAL_NOTIF", "Package: ${sbn.packageName}")
            Log.d("PARENTRAL_NOTIF", "Title: $title")
            Log.d("PARENTRAL_NOTIF", "Text: $text")
            Log.d("PARENTRAL_NOTIF", "BigText: $bigText")
            Log.d("PARENTRAL_NOTIF", "SubText: $subText")
        } catch (e: Exception) {
            Log.e("PARENTRAL_NOTIF", "Notification read error: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (latestPackage == sbn.packageName) {
            latestTitle = null
            latestText = null
            latestBigText = null
            latestSubText = null
            latestPackage = null
        }
    }
}