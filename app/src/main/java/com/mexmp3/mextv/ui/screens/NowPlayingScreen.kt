package com.mexmp3.mextv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.data.albumArtUri
import com.mexmp3.mextv.ui.components.WaveformVisualizer
import com.mexmp3.mextv.ui.viewmodel.MainViewModel
import com.mexmp3.mextv.util.Constants
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val song       by vm.currentSong.collectAsState()
    val isPlaying  by vm.isPlaying.collectAsState()
    val position   by vm.currentPosition.collectAsState()
    val dur        by vm.duration.collectAsState()
    val shuffle    by vm.shuffleOn.collectAsState()
    val repeat     by vm.repeatMode.collectAsState()

    var showLyrics by remember { mutableStateOf(false) }
    var lyricsFullScreen by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }

    // Auto-dismiss if nothing is playing
    if (song == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val currentSong = song!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            swipeOffset >  220f -> vm.skipPrev()
                            swipeOffset < -220f -> vm.skipNext()
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f }
                ) { _, delta -> swipeOffset += delta }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.player_now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Rotating album art ─────────────────────────────────────────
            RotatingAlbumArt(
                albumArtUri = currentSong.albumArtUri(),
                isPlaying   = isPlaying,
                modifier    = Modifier.size(272.dp)
            )

            Spacer(Modifier.height(32.dp))

            // ── Song info ──────────────────────────────────────────────────
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${currentSong.artist} — ${currentSong.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // ── Fix 3: Audio quality badge + Fix 5: Bluetooth indicator ───
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Audio quality badge (bitrate / format)
                AudioQualityBadge(songPath = currentSong.path)

                Spacer(Modifier.width(10.dp))

                // Bluetooth connected device indicator
                BluetoothIndicator()
            }

            Spacer(Modifier.height(24.dp))

            // ── Waveform ───────────────────────────────────────────────────
            WaveformVisualizer(
                isPlaying = isPlaying,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            // ── Seek bar ───────────────────────────────────────────────────
            PlayerSeekBar(
                position = position,
                duration = dur,
                onSeek   = { vm.seekTo(it) }
            )

            Spacer(Modifier.height(28.dp))

            // ── Controls ───────────────────────────────────────────────────
            PlayerControls(
                isPlaying  = isPlaying,
                shuffleOn  = shuffle,
                repeatMode = repeat,
                onPlayPause = { vm.togglePlayPause() },
                onNext      = { vm.skipNext() },
                onPrev      = { vm.skipPrev() },
                onShuffle   = { vm.toggleShuffle() },
                onRepeat    = { vm.cycleRepeat() }
            )

            Spacer(Modifier.height(24.dp))

            // ── Lyrics (collapsible) ───────────────────────────────────────
            AnimatedVisibility(
                visible = showLyrics,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                LyricsSection(
                    song         = currentSong,
                    positionMs   = position,
                    onFullScreen = { lyricsFullScreen = true }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ── Full-screen lyrics overlay ─────────────────────────────────────────
    if (lyricsFullScreen) {
        LyricsFullScreenOverlay(
            song       = currentSong,
            positionMs = position,
            onDismiss  = { lyricsFullScreen = false }
        )
    }
}

// ── Album Art Display ─────────────────────────────────────────────────────────

@Composable
private fun RotatingAlbumArt(
    albumArtUri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // One 360° rotation every 12 seconds, loop while playing
            while (true) {
                rotation.animateTo(
                    targetValue   = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 12_000, easing = LinearEasing)
                )
                // Keep value from growing unbounded
                rotation.snapTo(rotation.value % 360f)
            }
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = modifier
            .rotate(rotation.value)
            .shadow(
                elevation    = if (isPlaying) 24.dp else 8.dp,
                shape        = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                spotColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (albumArtUri != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                modifier     = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(80.dp))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                            modifier = Modifier.size(80.dp))
                    }
                }
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                modifier = Modifier.size(80.dp))
        }
        // Vinyl centre hole
        Box(modifier = Modifier.size(72.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)))
        Box(modifier = Modifier.size(14.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.background))
    }
}

// ── Seek Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun PlayerSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column {
        Slider(
            value       = progress,
            onValueChange = { onSeek((it * duration).toLong()) },
            modifier    = Modifier.fillMaxWidth(),
            colors      = SliderDefaults.colors(
                thumbColor        = MaterialTheme.colorScheme.primary,
                activeTrackColor  = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs(position), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMs(duration), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Player Controls ───────────────────────────────────────────────────────────

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    shuffleOn: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.ctrl_shuffle),
                tint = if (shuffleOn) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }

        // Previous
        IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.ctrl_previous),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(38.dp)
            )
        }

        // Play / Pause (large circular button)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.ctrl_pause)
                                     else stringResource(R.string.ctrl_play),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        // Next
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.ctrl_next),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(38.dp)
            )
        }

        // Repeat
        IconButton(onClick = onRepeat) {
            val (icon, tint) = when (repeatMode) {
                Constants.REPEAT_ONE -> Icons.Rounded.RepeatOne to MaterialTheme.colorScheme.primary
                Constants.REPEAT_ALL -> Icons.Rounded.Repeat    to MaterialTheme.colorScheme.primary
                else                 -> Icons.Rounded.Repeat    to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(icon, contentDescription = stringResource(R.string.ctrl_repeat),
                tint = tint, modifier = Modifier.size(26.dp))
        }
    }
}

// ── Audio Quality Badge (Fix 3) ───────────────────────────────────────────────

@Composable
private fun AudioQualityBadge(songPath: String) {
    var label by remember(songPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(songPath) {
        label = withContext(Dispatchers.IO) {
            runCatching {
                val ext = songPath.substringAfterLast('.', "").lowercase()
                when (ext) {
                    "flac" -> "FLAC"
                    "wav"  -> "WAV"
                    "ogg"  -> "OGG"
                    "m4a"  -> "AAC"
                    "aac"  -> "AAC"
                    "opus" -> "OPUS"
                    else -> {
                        // Read bitrate via MediaExtractor for mp3 and others
                        val extractor = MediaExtractor()
                        try {
                            extractor.setDataSource(songPath)
                            var bitrate: Int? = null
                            for (i in 0 until extractor.trackCount) {
                                val fmt = extractor.getTrackFormat(i)
                                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                                if (mime.startsWith("audio/")) {
                                    if (fmt.containsKey(MediaFormat.KEY_BIT_RATE)) {
                                        bitrate = fmt.getInteger(MediaFormat.KEY_BIT_RATE)
                                    }
                                    break
                                }
                            }
                            when {
                                bitrate == null -> null
                                bitrate >= 320_000 -> "320 kbps"
                                bitrate >= 256_000 -> "256 kbps"
                                bitrate >= 192_000 -> "192 kbps"
                                bitrate >= 128_000 -> "128 kbps"
                                else -> "${bitrate / 1000} kbps"
                            }
                        } finally {
                            extractor.release()
                        }
                    }
                }
            }.getOrNull()
        }
    }

    label?.let { text ->
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            tonalElevation = 0.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ── Bluetooth Indicator (Fix 5) ───────────────────────────────────────────────

@Composable
private fun BluetoothIndicator() {
    val context = LocalContext.current
    val connectedDevice = remember {
        runCatching {
            // Runtime permission check — required on API 31+ (Android 12+)
            val permGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true // BLUETOOTH permission is a normal permission pre-API 31, always granted if in manifest
            }
            if (!permGranted) return@runCatching null

            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: return@runCatching null
            if (!adapter.isEnabled) return@runCatching null

            // Use BluetoothManager.getConnectedDevices(A2DP profile) to find actually-streaming devices
            @Suppress("MissingPermission")
            btManager.getConnectedDevices(android.bluetooth.BluetoothProfile.A2DP)
                .firstOrNull()
                ?.let { device ->
                    @Suppress("MissingPermission")
                    device.name
                }
        }.getOrNull()
    }

    connectedDevice?.let { name ->
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            tonalElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Lyrics data model ─────────────────────────────────────────────────────────

private data class LyricLine(val timeMs: Long, val text: String)

private sealed class LyricsState {
    data object Idle     : LyricsState()
    data object Loading  : LyricsState()
    data class  Synced(val lines: List<LyricLine>) : LyricsState()
    data class  Plain(val text: String)            : LyricsState()
    data object NotFound : LyricsState()
}

// ── Lyrics Section composable ─────────────────────────────────────────────────

@Composable
private fun LyricsSection(song: Song, positionMs: Long, onFullScreen: () -> Unit = {}) {
    var state by remember(song.id) { mutableStateOf<LyricsState>(LyricsState.Idle) }

    LaunchedEffect(song.id) {
        state = LyricsState.Loading
        // 1. Local .lrc file — IO thread
        val localParsed = withContext(Dispatchers.IO) {
            runCatching {
                val lrcFile = java.io.File(song.path.replaceAfterLast('.', "lrc"))
                if (lrcFile.exists()) parseLrcSynced(lrcFile.readText()) else null
            }.getOrNull()
        }
        if (localParsed != null) { state = localParsed; return@LaunchedEffect }

        // 2. LRCLib API — free, no key
        val remote = withContext(Dispatchers.IO) {
            runCatching {
                val artist = java.net.URLEncoder.encode(song.artist.trim(), "UTF-8")
                val title  = java.net.URLEncoder.encode(song.title.trim(), "UTF-8")
                val url    = java.net.URL("https://lrclib.net/api/get?artist_name=$artist&track_name=$title")
                val conn   = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8_000
                conn.readTimeout    = 8_000
                conn.setRequestProperty("User-Agent", "MexMp3/2.0")
                if (conn.responseCode == 200) {
                    val json   = conn.inputStream.bufferedReader().readText()
                    val synced = json.substringAfter("\"syncedLyrics\":\"").substringBefore("\"")
                        .replace("\\n", "\n").replace("\\r", "").trim()
                    val plain  = json.substringAfter("\"plainLyrics\":\"").substringBefore("\"")
                        .replace("\\n", "\n").replace("\\r", "").trim()
                    when {
                        synced.isNotBlank() -> parseLrcSynced(synced)
                        plain.isNotBlank()  -> LyricsState.Plain(plain)
                        else                -> null
                    }
                } else null
            }.getOrNull()
        }
        state = remote ?: LyricsState.NotFound
    }

    // derivedStateOf means position ticks only recompute activeIndex, never
    // recompose the entire lyrics column — eliminates the lag/stutter
    val activeIndex by remember(state) {
        derivedStateOf {
            val lines = (state as? LyricsState.Synced)?.lines ?: return@derivedStateOf -1
            var idx = 0
            for (i in lines.indices) {
                if (lines[i].timeMs <= positionMs) idx = i else break
            }
            idx
        }
    }

    val lyricsScrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(activeIndex) {
        if (activeIndex > 1) {
            val lineHeightPx = with(density) { 56.dp.roundToPx() }
            lyricsScrollState.animateScrollTo(
                (activeIndex - 1) * lineHeightPx,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
        } else if (activeIndex <= 1) {
            lyricsScrollState.animateScrollTo(0, animationSpec = tween(300))
        }
    }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lyrics, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lyrics", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                if (state is LyricsState.Synced) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text("SYNCED", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                // Full-screen button
                IconButton(onClick = onFullScreen, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = "Full screen lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(14.dp))

            when (val s = state) {
                is LyricsState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                is LyricsState.Synced -> {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(lyricsScrollState)
                    ) {
                        s.lines.forEachIndexed { idx, line ->
                            val isActive = idx == activeIndex
                            val textColor by animateColorAsState(
                                targetValue = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (idx < activeIndex) 0.4f else 0.75f),
                                animationSpec = tween(300), label = "lyricColor$idx"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isActive) 1.04f else 1.0f,
                                animationSpec = tween(280), label = "lyricScale$idx"
                            )
                            Text(
                                text  = line.text,
                                style = if (isActive)
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                else
                                    MaterialTheme.typography.bodyMedium,
                                color    = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) },
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
                            )
                        }
                    }
                }
                is LyricsState.Plain -> {
                    Text(
                        text = s.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.7f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                is LyricsState.NotFound, LyricsState.Idle -> {
                    Text(
                        "No lyrics found for this track.\n\nYou can add a .lrc file with the same name as your audio file in the same folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.6f
                    )
                }
            }
        }
    }
}

// ── Full-screen lyrics overlay ────────────────────────────────────────────────

@Composable
private fun LyricsFullScreenOverlay(
    song: Song,
    positionMs: Long,
    onDismiss: () -> Unit
) {
    var state by remember(song.id) { mutableStateOf<LyricsState>(LyricsState.Idle) }

    LaunchedEffect(song.id) {
        state = LyricsState.Loading
        val localParsed = withContext(Dispatchers.IO) {
            runCatching {
                val lrcFile = java.io.File(song.path.replaceAfterLast('.', "lrc"))
                if (lrcFile.exists()) parseLrcSynced(lrcFile.readText()) else null
            }.getOrNull()
        }
        if (localParsed != null) { state = localParsed; return@LaunchedEffect }
        val remote = withContext(Dispatchers.IO) {
            runCatching {
                val artist = java.net.URLEncoder.encode(song.artist.trim(), "UTF-8")
                val title  = java.net.URLEncoder.encode(song.title.trim(), "UTF-8")
                val url    = java.net.URL("https://lrclib.net/api/get?artist_name=$artist&track_name=$title")
                val conn   = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8_000; conn.readTimeout = 8_000
                conn.setRequestProperty("User-Agent", "MexMp3/2.0")
                if (conn.responseCode == 200) {
                    val json   = conn.inputStream.bufferedReader().readText()
                    val synced = json.substringAfter("\"syncedLyrics\":\"").substringBefore("\"")
                        .replace("\\n", "\n").replace("\\r", "").trim()
                    val plain  = json.substringAfter("\"plainLyrics\":\"").substringBefore("\"")
                        .replace("\\n", "\n").replace("\\r", "").trim()
                    when {
                        synced.isNotBlank() -> parseLrcSynced(synced)
                        plain.isNotBlank()  -> LyricsState.Plain(plain)
                        else                -> null
                    }
                } else null
            }.getOrNull()
        }
        state = remote ?: LyricsState.NotFound
    }

    val activeIndex = remember(state, positionMs) {
        val lines = (state as? LyricsState.Synced)?.lines ?: return@remember -1
        var idx = 0
        for (i in lines.indices) { if (lines[i].timeMs <= positionMs) idx = i else break }
        idx
    }

    val scrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(activeIndex) {
        if (activeIndex > 2) {
            val lineHeightPx = with(density) { 60.dp.roundToPx() }
            scrollState.animateScrollTo((activeIndex - 2) * lineHeightPx, animationSpec = tween(400))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top bar with song title ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit full screen",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state is LyricsState.Synced) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("SYNCED", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // ── Lyrics body ────────────────────────────────────────────────
            when (val s = state) {
                is LyricsState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is LyricsState.Synced -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        s.lines.forEachIndexed { idx, line ->
                            val isActive = idx == activeIndex
                            val textColor by animateColorAsState(
                                targetValue = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (idx < activeIndex) 0.4f else 0.75f),
                                animationSpec = tween(300), label = "fsLyricColor$idx"
                            )
                            val scale by animateFloatAsState(
                                targetValue = if (isActive) 1.05f else 1.0f,
                                animationSpec = tween(280), label = "fsLyricScale$idx"
                            )
                            Text(
                                text  = line.text,
                                style = if (isActive)
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                else
                                    MaterialTheme.typography.bodyLarge,
                                color    = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .graphicsLayer {
                                        scaleX = scale; scaleY = scale
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                    },
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
                            )
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
                is LyricsState.Plain -> {
                    Text(
                        text = s.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.7f,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
                else -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No lyrics found for this track.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── LRC parser ────────────────────────────────────────────────────────────────

private fun parseLrcSynced(content: String): LyricsState {
    val regex = Regex("""\[(\d{2}):(\d{2})[.:](\d{2,3})\]""")
    val lines = mutableListOf<LyricLine>()
    for (raw in content.lines()) {
        val m = regex.find(raw) ?: continue
        val min  = m.groupValues[1].toLongOrNull() ?: continue
        val sec  = m.groupValues[2].toLongOrNull() ?: continue
        val msRaw = m.groupValues[3]
        val ms   = if (msRaw.length == 2) (msRaw.toLongOrNull() ?: 0L) * 10L
                   else msRaw.toLongOrNull() ?: 0L
        val timeMs = min * 60_000L + sec * 1_000L + ms
        val text = regex.replace(raw, "").trim()
        if (text.isNotBlank()) lines.add(LyricLine(timeMs, text))
    }
    if (lines.isNotEmpty()) return LyricsState.Synced(lines.sortedBy { it.timeMs })
    val plain = content.lines().map { regex.replace(it, "").trim() }
        .filter { it.isNotBlank() }.joinToString("\n")
    return if (plain.isNotBlank()) LyricsState.Plain(plain) else LyricsState.NotFound
}

private fun formatMs(ms: Long): String {
    val total = ms / 1000L
    return "%d:%02d".format(total / 60, total % 60)
}
