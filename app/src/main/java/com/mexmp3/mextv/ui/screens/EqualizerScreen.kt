package com.mexmp3.mextv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.mexmp3.mextv.util.Constants
import kotlin.math.*

// ── EQ band labels ────────────────────────────────────────────────────────────
private val BAND_LABELS  = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
private val BAND_FREQS   = listOf("Bass", "Low-Mid", "Mid", "High-Mid", "Treble")
private const val DB_MAX = 12f
private const val DB_MIN = -12f

@Composable
fun EqualizerScreen(
    eqEnabled: Boolean,
    currentPreset: String,
    bandValues: List<Float>,               // -12f..+12f per band
    onEnabledChange: (Boolean) -> Unit,
    onPresetChange: (String) -> Unit,
    onBandChange: (bandIndex: Int, dB: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = Constants.EQ_PRESETS.keys.toList()

    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    // Glow pulse — only animate when EQ is actually enabled to save CPU/battery
    val infiniteTransition = rememberInfiniteTransition(label = "eqGlow")
    val glowAlpha by if (eqEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f, targetValue = 0.75f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glow"
        )
    } else {
        remember { mutableFloatStateOf(0.15f) }
    }
    val activeGlow = if (eqEnabled) glowAlpha else 0.15f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Equalizer", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)

            // Enable toggle pill
            EqEnablePill(enabled = eqEnabled, glowAlpha = activeGlow, primary = primary, onToggle = onEnabledChange)
        }

        // ── Spectrum curve display ─────────────────────────────────────────
        EqSpectrumDisplay(
            bandValues = bandValues,
            enabled    = eqEnabled,
            primary    = primary,
            secondary  = secondary,
            glowAlpha  = activeGlow,
            modifier   = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // ── Vertical band sliders ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 12.dp)
        ) {
            // dB grid lines + labels
            EqGridLines(primary = primary, modifier = Modifier.fillMaxSize())

            // Band sliders
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bandValues.forEachIndexed { idx, db ->
                    EqBandSlider(
                        bandLabel = BAND_LABELS[idx],
                        freqLabel = BAND_FREQS[idx],
                        dB        = db,
                        enabled   = eqEnabled,
                        primary   = primary,
                        secondary = secondary,
                        modifier  = Modifier.weight(1f).fillMaxHeight(),
                        onValueChange = { onBandChange(idx, it) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Preset chips ──────────────────────────────────────────────────
        Text(
            "PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                EqPresetChip(
                    label    = preset,
                    selected = currentPreset == preset,
                    enabled  = eqEnabled,
                    primary  = primary,
                    onClick  = { onPresetChange(preset) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Bass / Treble quick knobs ──────────────────────────────────────
        Text(
            "QUICK ADJUST",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAdjustBar(
                label    = "Bass Boost",
                icon     = Icons.Rounded.GraphicEq,
                value    = bandValues.firstOrNull() ?: 0f,
                enabled  = eqEnabled,
                primary  = primary,
                modifier = Modifier.weight(1f),
                onChange = { onBandChange(0, it) }
            )
            QuickAdjustBar(
                label    = "Treble",
                icon     = Icons.Rounded.Tune,
                value    = bandValues.lastOrNull() ?: 0f,
                enabled  = eqEnabled,
                primary  = secondary,
                modifier = Modifier.weight(1f),
                onChange = { onBandChange(bandValues.lastIndex, it) }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Enable pill toggle ────────────────────────────────────────────────────────

@Composable
private fun EqEnablePill(enabled: Boolean, glowAlpha: Float, primary: Color, onToggle: (Boolean) -> Unit) {
    val bgColor by animateColorAsState(
        if (enabled) primary.copy(alpha = 0.2f) else Color.Transparent,
        tween(300), label = "pillBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (enabled) primary.copy(alpha = glowAlpha) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                .background(if (enabled) primary else MaterialTheme.colorScheme.outline.copy(0.4f)))
            Text(if (enabled) "ON" else "OFF",
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp)
        }
    }
}

// ── Spectrum curve display ────────────────────────────────────────────────────

@Composable
private fun EqSpectrumDisplay(
    bandValues: List<Float>,
    enabled: Boolean,
    primary: Color,
    secondary: Color,
    glowAlpha: Float,
    modifier: Modifier = Modifier
) {
    val animatedBands = bandValues.map { db ->
        animateFloatAsState(if (enabled) db else 0f, spring(dampingRatio = 0.6f, stiffness = 120f), label = "band").value
    }

    Canvas(modifier = modifier.clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .border(0.5.dp, primary.copy(alpha = glowAlpha * 0.4f), RoundedCornerShape(16.dp))
    ) {
        val w = size.width
        val h = size.height
        val midY = h * 0.5f
        val padding = w * 0.05f

        // Grid mid-line
        drawLine(primary.copy(alpha = 0.12f), Offset(padding, midY), Offset(w - padding, midY), 1.dp.toPx())

        if (animatedBands.size < 2) return@Canvas

        // Build curve points (cubic spline through each band)
        val xStep = (w - padding * 2) / (animatedBands.size - 1)
        val points = animatedBands.mapIndexed { i, db ->
            val x = padding + i * xStep
            val y = midY - (db / DB_MAX) * (midY * 0.78f)
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val cp1x = (points[i].x + points[i+1].x) / 2f
                cubicTo(cp1x, points[i].y, cp1x, points[i+1].y, points[i+1].x, points[i+1].y)
            }
        }

        // Filled gradient under curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(
            listOf(primary.copy(alpha = glowAlpha * 0.3f), Color.Transparent),
            startY = 0f, endY = h
        ))

        // Curve stroke
        drawPath(path, Brush.linearGradient(
            listOf(secondary.copy(alpha = 0.9f), primary),
            start = Offset(0f, 0f), end = Offset(w, 0f)
        ), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Band dot markers
        points.forEach { pt ->
            drawCircle(primary, radius = 4.dp.toPx(), center = pt)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = pt)
        }
    }
}

// ── dB grid lines ─────────────────────────────────────────────────────────────

@Composable
private fun EqGridLines(primary: Color, modifier: Modifier = Modifier) {
    val lineAlpha = 0.08f
    val dbMarks = listOf(12, 6, 0, -6, -12)
    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        dbMarks.forEach { db ->
            val y = h * 0.5f - (db / DB_MAX) * (h * 0.46f)
            drawLine(primary.copy(alpha = lineAlpha), Offset(0f, y), Offset(w, y), 1.dp.toPx())
        }
    }
}

// ── Vertical band slider ──────────────────────────────────────────────────────

@Composable
private fun EqBandSlider(
    bandLabel: String,
    freqLabel: String,
    dB: Float,
    enabled: Boolean,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var trackHeightPx by remember { mutableFloatStateOf(200f) }

    // rememberUpdatedState — the drag lambda always reads the latest dB value
    // even though pointerInput only recomposes when its keys change
    val currentDb by rememberUpdatedState(dB)

    val animatedDb by animateFloatAsState(
        if (enabled) dB else 0f,
        spring(dampingRatio = 0.6f, stiffness = 120f), label = "db"
    )

    val thumbColor by animateColorAsState(
        if (!enabled) MaterialTheme.colorScheme.outline.copy(0.4f)
        else if (dB > 0) primary else secondary,
        tween(300), label = "thumb"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (dB == 0f) "0" else if (dB > 0) "+${dB.toInt()}" else "${dB.toInt()}",
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f) else thumbColor,
            fontSize = 10.sp, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .width(44.dp)
                .onSizeChanged { trackHeightPx = it.height.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        // Snap band to wherever the finger touches down
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tapFraction = 1f - (down.position.y / trackHeightPx).coerceIn(0f, 1f)
                        onValueChange((DB_MIN + tapFraction * (DB_MAX - DB_MIN)).coerceIn(DB_MIN, DB_MAX))
                        // Then track drag — use currentDb (rememberUpdatedState) not captured dB
                        var lastY = down.position.y
                        var pointer = down
                        while (pointer.pressed) {
                            val event = awaitPointerEvent()
                            pointer = event.changes.firstOrNull() ?: break
                            pointer.consume()
                            val delta = pointer.position.y - lastY
                            lastY = pointer.position.y
                            val newDb = (currentDb - delta * (DB_MAX - DB_MIN) / trackHeightPx)
                                .coerceIn(DB_MIN, DB_MAX)
                            onValueChange(newDb)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track background
            Box(
                modifier = Modifier.fillMaxHeight().width(3.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Active fill — animated for smooth visual
            val fillFraction = (animatedDb - DB_MIN) / (DB_MAX - DB_MIN)
            val midFraction  = (-DB_MIN) / (DB_MAX - DB_MIN)
            Canvas(modifier = Modifier.fillMaxHeight().width(3.dp)) {
                val midY   = size.height * (1f - midFraction)
                val thumbY = size.height * (1f - fillFraction)
                val top    = minOf(midY, thumbY)
                val bot    = maxOf(midY, thumbY)
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (animatedDb >= 0) primary.copy(alpha = if (enabled) 0.85f else 0.2f)
                            else secondary.copy(alpha = if (enabled) 0.85f else 0.2f),
                            if (animatedDb >= 0) primary.copy(alpha = if (enabled) 0.4f else 0.1f)
                            else secondary.copy(alpha = if (enabled) 0.4f else 0.1f)
                        ), startY = top, endY = bot
                    ),
                    topLeft = Offset(0f, top),
                    size    = androidx.compose.ui.geometry.Size(size.width, bot - top)
                )
            }

            // Thumb — raw dB fraction, no animation lag
            val rawFraction = (dB - DB_MIN) / (DB_MAX - DB_MIN)
            val thumbOffsetPx = ((0.5f - rawFraction) * trackHeightPx * 0.92f)
                .coerceIn(-trackHeightPx * 0.46f, trackHeightPx * 0.46f)
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                modifier = Modifier
                    .offset(y = with(density) { thumbOffsetPx.toDp() })
                    .size(26.dp)
                    .shadow(if (enabled) 6.dp else 0.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.95f), thumbColor.copy(alpha = 0.85f))
                    ))
                    .border(1.5.dp, thumbColor.copy(alpha = 0.6f), CircleShape)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(bandLabel, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.9f else 0.35f),
            textAlign = TextAlign.Center)
        Text(freqLabel, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f),
            textAlign = TextAlign.Center)
    }
}

// ── Preset chip ───────────────────────────────────────────────────────────────

@Composable
private fun EqPresetChip(label: String, selected: Boolean, enabled: Boolean, primary: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected && enabled) primary.copy(alpha = 0.22f)
        else if (selected) primary.copy(alpha = 0.10f)
        else Color.Transparent,
        tween(200), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        if (selected) primary.copy(alpha = if (enabled) 0.9f else 0.4f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        tween(200), label = "chipBorder"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected && enabled) primary
                    else if (selected) primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp
        )
    }
}

// ── Quick adjust horizontal bar ───────────────────────────────────────────────

@Composable
private fun QuickAdjustBar(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    enabled: Boolean,
    primary: Color,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit
) {
    val fraction = ((value - DB_MIN) / (DB_MAX - DB_MIN)).coerceIn(0f, 1f)
    val animFraction by animateFloatAsState(if (enabled) fraction else 0.5f, spring(stiffness = 120f), label = "qa")

    Column(modifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) primary else MaterialTheme.colorScheme.outline.copy(0.4f),
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f),
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (value == 0f) "0 dB" else if (value > 0) "+${String.format("%.1f", value)} dB" else "${String.format("%.1f", value)} dB",
                style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                color = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)
            )
        }
        Spacer(Modifier.height(8.dp))
        // Track
        var trackW by remember { mutableFloatStateOf(300f) }
        Box(
            modifier = Modifier.fillMaxWidth().height(20.dp)
                .onSizeChanged { trackW = it.width.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    // Must use detectHorizontalDragGestures — this is a horizontal bar.
                    // The previous code used detectVerticalDragGestures which never fires
                    // on a horizontal swipe, making the quick-adjust bars non-functional.
                    detectHorizontalDragGestures { change, _ ->
                        val newFraction = (change.position.x / trackW).coerceIn(0f, 1f)
                        onChange(DB_MIN + newFraction * (DB_MAX - DB_MIN))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(0.2f)))
            Box(modifier = Modifier.fillMaxWidth(animFraction).height(3.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(primary.copy(if (enabled) 0.5f else 0.15f), primary.copy(if (enabled) 0.9f else 0.2f)))))
            Box(modifier = Modifier
                .offset(x = (animFraction * (trackW / density - 16f)).dp)
                .size(16.dp).shadow(if (enabled) 4.dp else 0.dp, CircleShape).clip(CircleShape)
                .background(if (enabled) primary else MaterialTheme.colorScheme.outline.copy(0.35f)))
        }
    }
}
