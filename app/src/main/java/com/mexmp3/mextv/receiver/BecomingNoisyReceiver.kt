package com.mexmp3.mextv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.mexmp3.mextv.util.Constants

class BecomingNoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            // Pause playback via broadcast to MusicService
            val pauseIntent = Intent(Constants.ACTION_PLAY_PAUSE).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(pauseIntent)
        }
    }
}
