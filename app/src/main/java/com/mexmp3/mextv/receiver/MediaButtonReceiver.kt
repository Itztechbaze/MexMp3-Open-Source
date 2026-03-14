package com.mexmp3.mextv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import com.mexmp3.mextv.util.Constants

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return
        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return
        if (event.action != KeyEvent.ACTION_DOWN) return

        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> Constants.ACTION_PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_NEXT -> Constants.ACTION_NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Constants.ACTION_PREV
            else -> return
        }
        context.sendBroadcast(Intent(action).setPackage(context.packageName))
    }
}
