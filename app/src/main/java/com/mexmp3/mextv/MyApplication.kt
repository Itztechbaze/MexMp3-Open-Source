package com.mexmp3.mextv

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.FirebaseApp
import com.mexmp3.mextv.util.Constants

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val playbackChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(playbackChannel)
    }
}
