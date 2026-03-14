package com.mexmp3.mextv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mexmp3.mextv.service.MusicService

/**
 * Restarts MusicService after device reboot so the app is ready for playback
 * immediately. Required on Vivo Funtouch OS and other aggressive OEM ROMs
 * that kill all services on reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val serviceIntent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
