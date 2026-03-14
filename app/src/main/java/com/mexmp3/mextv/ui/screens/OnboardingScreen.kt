package com.mexmp3.mextv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mexmp3.mextv.R
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun OnboardingScreen(
    onScanClicked: () -> Unit,
    onSkip: () -> Unit
) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(100); phase = 1
        delay(600); phase = 2
        delay(500); phase = 3
        delay(700); phase = 4
    }

    val inf = rememberInfiniteTransition(label = "ob")
    // Only animate once the UI has entered view (phase >= 1) — avoids running
    // heavy GPU animations on low-end devices before the screen is even visible
    val wavePhase by inf.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "wv"
    )
    val pulseScale by if (phase >= 1) {
        inf.animateFloat(
            initialValue = 0.93f, targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "ps"
        )
    } else remember { mutableFloatStateOf(1f) }
    val glowAlpha by if (phase >= 1) {
        inf.animateFloat(
            initialValue = 0.2f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "ga"
        )
    } else remember { mutableFloatStateOf(0.2f) }

    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bg        = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Ambient background canvas — full bleed behind everything
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(primary.copy(alpha = glowAlpha * 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.25f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.5f, size.height * 0.25f)
            )
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(secondary.copy(alpha = glowAlpha * 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.8f),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.8f)
            )
            drawAmbientWaveBars(wavePhase, primary, secondary, size.width, size.height)
        }

        // Scrollable content — NOTHING can be cropped on any device
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Skip button top-right
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AnimatedVisibility(phase >= 4, enter = fadeIn(tween(400))) {
                    TextButton(onClick = onSkip) {
                        Text(
                            stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── App logo orb ───────────────────────────────────────────────
            AnimatedVisibility(
                phase >= 1,
                enter = scaleIn(spring(dampingRatio = 0.42f, stiffness = 170f)) + fadeIn(tween(700))
            ) {
                Box(modifier = Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                    // Pulsing outer glow ring
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                Brush.sweepGradient(listOf(
                                    primary.copy(0.75f), Color.Transparent,
                                    secondary.copy(0.5f), Color.Transparent,
                                    primary.copy(0.75f)
                                )),
                                CircleShape
                            )
                    )
                    // Mid glow
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(
                                    primary.copy(0.2f), primary.copy(0.05f), Color.Transparent
                                ))
                            )
                    )
                    // Inner circle with REAL app launcher icon
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, primary.copy(0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher),
                            contentDescription = "MexMp3",
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Title ──────────────────────────────────────────────────────
            AnimatedVisibility(
                phase >= 2,
                enter = fadeIn(tween(700)) + slideInVertically(tween(600)) { 24 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "MexMp3",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp).height(2.5.dp).clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(Color.Transparent, primary, secondary, Color.Transparent))
                            )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.onboarding_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Feature cards ──────────────────────────────────────────────
            AnimatedVisibility(phase >= 3, enter = fadeIn(tween(500))) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnboardingFeatureCard(Icons.Rounded.Album,        "Stunning Visuals",  stringResource(R.string.onboarding_feature_1), primary,   0)
                    OnboardingFeatureCard(Icons.Rounded.Tune,         "Studio Equalizer",  stringResource(R.string.onboarding_feature_2), secondary, 100)
                    OnboardingFeatureCard(Icons.Rounded.Timer,        "Smart Features",    stringResource(R.string.onboarding_feature_3), primary,   200)
                    OnboardingFeatureCard(Icons.Rounded.Lyrics,       "Live Lyrics",       "Synced lyrics fetched automatically for every track.", secondary, 300)
                    OnboardingFeatureCard(Icons.Rounded.Palette,      "12 Bespoke Themes", "Craft your perfect look with ultra-sleek skins.", primary,   400)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── CTA buttons ────────────────────────────────────────────────
            AnimatedVisibility(
                phase >= 4,
                enter = fadeIn(tween(500)) + slideInVertically(tween(600)) { 50 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary gradient CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(Brush.horizontalGradient(listOf(primary, secondary.copy(0.88f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onScanClicked,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(29.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor   = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(Icons.Rounded.LibraryMusic, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.onboarding_scan_btn),
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Secondary outline button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .border(
                                1.dp,
                                Brush.horizontalGradient(listOf(primary.copy(0.45f), secondary.copy(0.3f))),
                                RoundedCornerShape(25.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick  = onSkip,
                            modifier = Modifier.fillMaxSize(),
                            shape    = RoundedCornerShape(25.dp)
                        ) {
                            Text(
                                stringResource(R.string.onboarding_get_started),
                                color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
                                fontWeight = FontWeight.Medium,
                                fontSize   = 15.sp
                            )
                        }
                    }
                }
            }

            // Generous bottom breathing room — nothing ever gets clipped
            Spacer(Modifier.height(48.dp))
        }
    }
}

// ── Feature card ──────────────────────────────────────────────────────────────
@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector, title: String, description: String,
    accentColor: Color, delay: Int
) {
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delay.toLong()); vis = true }

    AnimatedVisibility(vis, enter = fadeIn(tween(450)) + slideInHorizontally(tween(450)) { -36 }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(listOf(
                        accentColor.copy(0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(0.55f)
                    ))
                )
                .border(
                    0.5.dp,
                    Brush.horizontalGradient(listOf(accentColor.copy(0.35f), Color.Transparent)),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp)
                Spacer(Modifier.height(3.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f), lineHeight = 16.sp)
            }
        }
    }
}

// ── Dual-colour ambient waveform bars ─────────────────────────────────────────
private fun DrawScope.drawAmbientWaveBars(
    phase: Float, primary: Color, secondary: Color, width: Float, height: Float
) {
    val barCount = 34
    val barW     = 2.6.dp.toPx()
    val spacing  = width / barCount
    val baseline = height * 0.66f
    for (i in 0 until barCount) {
        val x     = spacing * i + spacing / 2f
        val noise = sin(phase + i * 0.5f) * 0.5f + cos(phase * 0.6f + i * 0.27f) * 0.5f
        val barH  = (16f + noise * 22f).dp.toPx()
        val t     = i.toFloat() / barCount
        val color = if (t < 0.5f) primary else secondary
        val alpha = 0.03f + abs(noise) * 0.045f
        drawLine(color.copy(alpha), Offset(x, baseline - barH), Offset(x, baseline + barH * 0.22f), barW, StrokeCap.Round)
    }
}
