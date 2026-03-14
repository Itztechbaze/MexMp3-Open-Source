package com.mexmp3.mextv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mexmp3.mextv.R
import com.mexmp3.mextv.data.Song
import com.mexmp3.mextv.data.albumArtUri
import com.mexmp3.mextv.data.durationFormatted
import kotlin.math.abs

// ── Album Art ─────────────────────────────────────────────────────────────────

/**
 * Loads album art via Coil. Shows a music-note placeholder while loading or on error.
 * Only one AsyncImage request — no double-render bug.
 */
@Composable
fun AlbumArtImage(
    albumArtUri: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (albumArtUri != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ── Song Card ─────────────────────────────────────────────────────────────────

@Composable
fun SongCard(
    song: Song,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Only animate when this card is actually playing — avoids running
    // hundreds of infinite animations simultaneously which crashes on low-end devices
    val pulseAlpha by if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue  = 0.3f,
            animationSpec = infiniteRepeatable(
                animation  = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AlbumArtImage(albumArtUri = song.albumArtUri(), modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.width(12.dp))

            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Playing indicator dot
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                )
                Spacer(Modifier.width(8.dp))
            }

            // Duration
            Text(
                text = song.durationFormatted(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Mini Player ───────────────────────────────────────────────────────────────

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -200f) onNext()
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f }
                ) { _, dragAmount -> offsetX += dragAmount }
            }
            .clickable(onClick = onExpand)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        MaterialTheme.colorScheme.surface
                    )
                ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AlbumArtImage(albumArtUri = song.albumArtUri(), modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
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

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.ctrl_pause)
                                         else stringResource(R.string.ctrl_play),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.ctrl_next),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Empty / Error / Loading States ────────────────────────────────────────────

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.MusicOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Animated Waveform Visualizer ──────────────────────────────────────────────

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // Pre-generate stable random durations per bar
    val durations = remember { (0 until barCount).map { (350..850).random() } }

    // Only run infinite animations when actually playing — avoids burning CPU/GPU
    // on low-end devices (e.g. Vivo Y20) which causes ANR crashes during scroll
    val heights: List<State<Float>> = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveform")
        durations.mapIndexed { i, dur ->
            infiniteTransition.animateFloat(
                initialValue = 0.08f,
                targetValue  = 0.92f,
                animationSpec = infiniteRepeatable(
                    animation    = tween(dur, easing = FastOutSlowInEasing),
                    repeatMode   = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 25)
                ),
                label = "bar_$i"
            )
        }
    } else {
        // Static idle heights — zero animation cost
        remember { (0 until barCount).map { mutableFloatStateOf(0.06f) } }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val totalGap = size.width * 0.35f
        val barWidth = (size.width - totalGap) / barCount
        val gap      = totalGap / (barCount - 1).coerceAtLeast(1)

        heights.forEachIndexed { i, heightState ->
            val h    = heightState.value * size.height
            val x    = i * (barWidth + gap)
            val top  = (size.height - h) / 2f
            drawRoundRect(
                color        = color,
                topLeft      = Offset(x, top),
                size         = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
                alpha        = if (isPlaying) 0.85f else 0.25f
            )
        }
    }
}
