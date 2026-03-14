package com.mexmp3.mextv.service

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.*
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mexmp3.mextv.MainActivity
import com.mexmp3.mextv.EXTRA_OPEN_NOW_PLAYING
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.data.albumArtUri
import com.mexmp3.mextv.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class MusicService : LifecycleService() {

    companion object {
        val isPlaying    = MutableStateFlow(false)
        val currentSong  = MutableStateFlow<Song?>(null)
        val currentPosition = MutableStateFlow(0L)
        val duration     = MutableStateFlow(0L)
        val queue        = MutableStateFlow<List<Song>>(emptyList())
        val queueIndex   = MutableStateFlow(0)
        val shuffleOn    = MutableStateFlow(false)
        val repeatMode   = MutableStateFlow(Constants.REPEAT_NONE)
        /** Original unshuffled queue — restored when shuffle is turned off */
        val originalQueue = MutableStateFlow<List<Song>>(emptyList())
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private lateinit var mainHandler: Handler
    private var audioFocusRequest: AudioFocusRequest? = null
    private var equalizer: Equalizer? = null
    private var sleepTimerJob: Job? = null
    private var positionJob: Job? = null
    private var hasAudioFocus = false
    private var noisyReceiverRegistered = false
    private var controlReceiverRegistered = false

    // ── WakeLock — keeps CPU alive when screen is off so playback never stops ──
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    // Internal noisy receiver — registered dynamically (not exported)
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pausePlayback()
            }
        }
    }

    // Notification / transport controls receiver
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_PLAY_PAUSE -> togglePlayPause()
                Constants.ACTION_NEXT       -> skipToNext()
                Constants.ACTION_PREV       -> skipToPrevious()
                Constants.ACTION_CLOSE      -> {
                    // Stop playback, clear state, remove notification and kill service
                    pausePlayback()
                    isPlaying.value    = false
                    currentSong.value  = null
                    queue.value        = emptyList()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mainHandler = Handler(Looper.getMainLooper())
        setupPlayer()
        setupMediaSession()
        setupAudioFocus()
        registerNoisyReceiver()
        registerControlReceiver()
        startPositionUpdater()
        acquireWakeLock()
        // Call startForeground immediately in onCreate — on Vivo Funtouch OS and
        // other aggressive OEM ROMs, if startForeground() is only called later in
        // onStartCommand the OS can kill the service in the gap. Calling it here
        // in onCreate guarantees it is always a foreground service from birth.
        runCatching { startForeground(Constants.NOTIFICATION_ID, buildNotification()) }
    }

    private fun acquireWakeLock() {
        // PARTIAL_WAKE_LOCK keeps the CPU running when screen is off — essential
        // on budget devices (Vivo Y20 etc.) that aggressively kill background processes.
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MexMp3::PlaybackWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(/* 12 hours max safety timeout */ 12 * 60 * 60 * 1000L)
        }
        // WifiLock prevents the OS from turning off Wi-Fi during playback.
        // WIFI_MODE_FULL_LOW_LATENCY is the recommended replacement on API 29+.
        // WIFI_MODE_FULL_HIGH_PERF is deprecated but kept as fallback for API 26-28.
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        else
            @Suppress("DEPRECATION") android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
        wifiLock = wm.createWifiLock(wifiLockMode, "MexMp3::PlaybackWifiLock")
            .apply { acquire() }
    }

    private fun releaseWakeLocks() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        runCatching { if (wifiLock?.isHeld == true) wifiLock?.release() }
    }

    // ── Player ─────────────────────────────────────────────────────────────────

    // Guard flag — true while playSong() is actively setting up a new track.
    // Prevents onPlaybackStateChanged from firing handleSongEnd() during the
    // clearMediaItems() / prepare() calls which also trigger STATE_ENDED/IDLE.
    @Volatile private var isChangingSong = false

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(false)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        // Never handle song end while we are actively loading a new song —
                        // clearMediaItems() + prepare() both fire STATE_ENDED/STATE_IDLE
                        // which would re-trigger skipToNext() and cause infinite skipping.
                        if (state == Player.STATE_ENDED && !isChangingSong && !player.hasNextMediaItem()) {
                            mainHandler.post { handleSongEnd() }
                        }
                        mainHandler.post { updatePlaybackState() }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        MusicService.isPlaying.value = playing
                        mainHandler.post {
                            updatePlaybackState()
                            updateOrPostNotification()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        MusicService.isPlaying.value = false
                        isChangingSong = false
                    }
                })
            }
    }

    private var gaplessEnabled: Boolean = false // reserved for future re-implementation

    fun setGapless(enabled: Boolean) {
        gaplessEnabled = enabled
    }

    // ── MediaSession ───────────────────────────────────────────────────────────

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MexMp3Session").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay()                  = resumePlayback()
                override fun onPause()                 = pausePlayback()
                override fun onSkipToNext()            = skipToNext()
                override fun onSkipToPrevious()        = skipToPrevious()
                override fun onSeekTo(pos: Long)       = seekTo(pos)
                override fun onStop() {
                    pausePlayback()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    // ── Audio focus ────────────────────────────────────────────────────────────

    private fun setupAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        player.volume = 1.0f
                        if (hasAudioFocus && !player.isPlaying) resumePlayback()
                        hasAudioFocus = true
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        hasAudioFocus = false
                        pausePlayback()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.volume = 0.3f
                }
            }
            .build()
    }

    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    // ── Receivers ─────────────────────────────────────────────────────────────

    private fun registerNoisyReceiver() {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        // NOT_EXPORTED — only this app should trigger it
        registerReceiver(noisyReceiver, filter, RECEIVER_NOT_EXPORTED)
        noisyReceiverRegistered = true
    }

    private fun registerControlReceiver() {
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_PLAY_PAUSE)
            addAction(Constants.ACTION_NEXT)
            addAction(Constants.ACTION_PREV)
            addAction(Constants.ACTION_CLOSE)
        }
        registerReceiver(controlReceiver, filter, RECEIVER_NOT_EXPORTED)
        controlReceiverRegistered = true
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun playSong(song: Song) {
        if (!requestAudioFocus()) return
        isChangingSong = true          // block STATE_ENDED callbacks during setup
        currentSong.value = song
        duration.value = song.duration
        currentPosition.value = 0L
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(song.path))
        player.prepare()
        player.play()
        isChangingSong = false         // safe to handle STATE_ENDED again
        updateMediaMetadata(song)
        loadAlbumArtAsync(song)
        startOrUpdateForeground()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        originalQueue.value = songs          // always save the original order
        queue.value = songs
        queueIndex.value = startIndex.coerceIn(0, songs.lastIndex)
        playSong(songs[queueIndex.value])
    }

    /** Insert [song] immediately after the currently playing track */
    fun playNext(song: Song) {
        val q = queue.value.toMutableList()
        val insertAt = (queueIndex.value + 1).coerceIn(0, q.size)
        q.add(insertAt, song)
        queue.value = q
    }

    /** Append [song] to the end of the current queue */
    fun addToQueue(song: Song) {
        val q = queue.value.toMutableList()
        q.add(song)
        queue.value = q
    }

    fun togglePlayPause() {
        if (player.isPlaying) pausePlayback() else resumePlayback()
    }

    fun resumePlayback() {
        if (!requestAudioFocus()) return
        if (currentSong.value != null) {
            // Re-acquire wake lock when resuming after pause
            if (wakeLock?.isHeld == false) {
                runCatching { wakeLock?.acquire(12 * 60 * 60 * 1000L) }
            }
            player.play()
        }
    }

    fun pausePlayback() {
        player.pause()
        // Release wake lock while paused — no need to keep CPU awake
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
    }

    fun skipToNext() {
        val q = queue.value
        if (q.isEmpty()) return
        val nextIdx = when {
            shuffleOn.value -> {
                // Pick a random index that isn't the current one (if queue has >1 song)
                if (q.size > 1) {
                    var idx: Int
                    do { idx = q.indices.random() } while (idx == queueIndex.value)
                    idx
                } else 0
            }
            queueIndex.value < q.lastIndex -> queueIndex.value + 1
            repeatMode.value == Constants.REPEAT_ALL -> 0
            else                     -> return
        }
        queueIndex.value = nextIdx
        playSong(q[nextIdx])
    }

    fun skipToPrevious() {
        // If >3 s into track, restart; otherwise go to previous
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            return
        }
        val q = queue.value
        if (q.isEmpty()) return
        val prevIdx = when {
            queueIndex.value > 0                    -> queueIndex.value - 1
            repeatMode.value == Constants.REPEAT_ALL -> q.lastIndex
            else                                    -> 0
        }
        queueIndex.value = prevIdx
        playSong(q[prevIdx])
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        currentPosition.value = positionMs
    }

    fun setEqualizerEnabled(enabled: Boolean, preset: String = "Flat") {
        if (enabled) {
            val sessionId = runCatching { player.audioSessionId }.getOrNull() ?: return
            if (sessionId == 0) return
            if (equalizer == null || equalizer?.enabled == false) {
                runCatching {
                    equalizer?.release()
                    equalizer = Equalizer(0, sessionId).apply { this.enabled = true }
                }
            }
            applyEqPreset(preset)
        } else {
            equalizer?.enabled = false
        }
    }

    fun applyEqPreset(presetName: String) {
        // audioSessionId is 0 until ExoPlayer has prepared media — skip if not ready
        val sessionId = runCatching { player.audioSessionId }.getOrNull() ?: return
        if (sessionId == 0) return

        // Ensure EQ exists — create it lazily if the caller never called setEqualizerEnabled
        if (equalizer == null) {
            runCatching {
                equalizer = Equalizer(0, sessionId).apply { this.enabled = true }
            }
        }
        val eq = equalizer ?: return
        if (!eq.enabled) eq.enabled = true

        val gains = Constants.EQ_PRESETS[presetName] ?: Constants.EQ_PRESETS["Flat"] ?: return
        val bandCount = eq.numberOfBands.toInt()
        val range = eq.getBandLevelRange()
        for (i in 0 until minOf(bandCount, gains.size)) {
            // gains[] are in millibels (100 = 1 dB); range[] is also in millibels
            val clamped = gains[i].toShort().coerceIn(range[0], range[1])
            runCatching { eq.setBandLevel(i.toShort(), clamped) }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = lifecycleScope.launch {
                delay(minutes * 60_000L)
                pausePlayback()
            }
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private fun handleSongEnd() {
        when (repeatMode.value) {
            Constants.REPEAT_ONE -> { player.seekTo(0L); player.play() }
            else                 -> skipToNext()
        }
    }

    private fun startPositionUpdater() {
        positionJob = lifecycleScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    currentPosition.value = player.currentPosition
                }
                delay(500L)
            }
        }
    }

    private fun updateMediaMetadata(song: Song) {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,  song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,  song.duration)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.albumArtUri())
                .build()
        )
    }

    private fun updatePlaybackState() {
        val state = if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, player.currentPosition, 1.0f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY              or
                    PlaybackStateCompat.ACTION_PAUSE             or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT      or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS  or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
    }

    // ── Notification album art cache — updated off main thread ────────────────
    @Volatile private var cachedAlbumArt: android.graphics.Bitmap? = null
    @Volatile private var cachedAlbumArtSongId: Long = -1L

    /** Load album art on an IO thread and post a notification update when done */
    private fun loadAlbumArtAsync(song: Song) {
        if (cachedAlbumArtSongId == song.id) return // already cached for this song
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bmp = runCatching {
                val uri = android.net.Uri.parse(song.albumArtUri())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.loadThumbnail(uri, android.util.Size(256, 256), null)
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
            }.getOrNull()
            cachedAlbumArt = bmp
            cachedAlbumArtSongId = song.id
            // Re-post notification with the loaded art (main thread)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                updateOrPostNotification()
            }
        }
    }

    // ── Notification ───────────────────────────────────────────────────────────

    private fun startOrUpdateForeground() {
        startForeground(Constants.NOTIFICATION_ID, buildNotification())
    }

    /** Must only be called from main thread */
    private fun updateOrPostNotification() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(Constants.NOTIFICATION_ID, buildNotification())
        } else {
            mainHandler.post {
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(Constants.NOTIFICATION_ID, buildNotification())
            }
        }
    }

    private fun buildNotification(): Notification {
        val song    = currentSong.value
        val playing = isPlaying.value

        // ── Tap → open Now Playing screen ─────────────────────────────────
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_NOW_PLAYING, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionPi(action: String, reqCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                this, reqCode,
                Intent(action).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val prevPi  = actionPi(Constants.ACTION_PREV,       10)
        val playPi  = actionPi(Constants.ACTION_PLAY_PAUSE, 11)
        val nextPi  = actionPi(Constants.ACTION_NEXT,       12)
        val closePi = actionPi(Constants.ACTION_CLOSE,      13)

        // ── Album art bitmap — use cached value loaded asynchronously ──────
        val albumArt: android.graphics.Bitmap? =
            if (song != null && cachedAlbumArtSongId == song.id) cachedAlbumArt else null

        // ── Build notification ─────────────────────────────────────────────
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            // Small status-bar icon — must be monochrome/white vector
            .setSmallIcon(R.drawable.ic_notif_logo)
            // Large album art square
            .setLargeIcon(albumArt)
            .setContentTitle(song?.title  ?: getString(R.string.player_unknown_song))
            .setContentText(song?.artist  ?: getString(R.string.player_unknown_artist))
            .setSubText(song?.album)
            .setContentIntent(contentIntent)
            // Make it look great on lock screen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setSilent(true)
            // Custom clean action icons
            .addAction(R.drawable.ic_notif_prev,
                getString(R.string.notif_action_prev), prevPi)
            .addAction(
                if (playing) R.drawable.ic_notif_pause else R.drawable.ic_notif_play,
                if (playing) getString(R.string.notif_action_pause) else getString(R.string.notif_action_play),
                playPi)
            .addAction(R.drawable.ic_notif_next,
                getString(R.string.notif_action_next), nextPi)
            .addAction(R.drawable.ic_notif_close,
                getString(R.string.notif_action_close), closePi)
            // MediaStyle — shows album art as background, prev/play/next in compact view
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)  // prev, play/pause, next
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(closePi)
            )
            .build()
    }

    // ── Service lifecycle ──────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // startForeground MUST be called immediately — Android 12+ kills the service
        // with ForegroundServiceDidNotStartInTimeException if this isn't done within 5 seconds.
        // This also handles the OS-restart case (intent == null, START_STICKY) where
        // playSong() is never called so startForeground() would otherwise never execute.
        runCatching { startForeground(Constants.NOTIFICATION_ID, buildNotification()) }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return MusicBinder()
    }

    /**
     * Called when the user swipes the app away from the recents/task list.
     * Music MUST keep playing — only the ✕ notification button should stop it.
     *
     * Strategy:
     * 1. Call startService() explicitly — this promotes the service from
     *    "bound-only" to "started", so it survives the activity unbinding.
     * 2. Re-post startForeground() so the OS sees an active foreground service
     *    and won't kill it along with the task.
     * 3. AlarmManager restart as a belt-and-suspenders fallback for aggressive
     *    OEM battery managers (Vivo, Xiaomi, Oppo, Samsung One UI, etc.).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Explicitly re-start as a foreground service so the OS treats it as
        // a started service — not just a bound one. This is critical: once the
        // activity is swiped away the ViewModel unbinds, and if the service was
        // never started independently Android is free to kill it immediately.
        // startForegroundService() is required (not startService()) because
        // on API 26+ a background start would be blocked.
        runCatching {
            val intent = Intent(applicationContext, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        }

        // Re-post the foreground notification immediately so Android doesn't
        // remove it when the task is cleared from recents
        runCatching { startForeground(Constants.NOTIFICATION_ID, buildNotification()) }

        // AlarmManager belt-and-suspenders for aggressive OEM killers
        // (Redmi, Vivo, Oppo, Samsung One UI aggressive battery modes)
        val restartIntent = Intent(applicationContext, MusicService::class.java).also {
            it.setPackage(packageName)
        }
        val pending = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        alarm.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 500,
            pending
        )
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        positionJob?.cancel()
        runCatching { player.release() }
        runCatching { mediaSession.release() }
        runCatching { equalizer?.release() }
        runCatching { audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) } }
        if (noisyReceiverRegistered)   { unregisterReceiver(noisyReceiver);   noisyReceiverRegistered   = false }
        if (controlReceiverRegistered) { unregisterReceiver(controlReceiver); controlReceiverRegistered = false }
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseWakeLocks()
        super.onDestroy()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}
