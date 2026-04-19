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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        latestPackage = sbn.packageName
        latestTitle = title
        latestText = text

        Log.d("SPPMS_NOTIF", "Package: ${sbn.packageName}")
        Log.d("SPPMS_NOTIF", "Title: $title")
        Log.d("SPPMS_NOTIF", "Text: $text")
    }
}