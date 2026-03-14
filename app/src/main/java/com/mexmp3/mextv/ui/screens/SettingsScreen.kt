package com.mexmp3.mextv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mexmp3.mextv.R
import com.mexmp3.mextv.ui.viewmodel.MainViewModel
import com.mexmp3.mextv.util.Constants

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val currentTheme    by vm.themeName.collectAsState()
    val eqEnabled       by vm.prefs.eqEnabled.collectAsState(initial = false)
    val eqPreset        by vm.prefs.eqPreset.collectAsState(initial = "Flat")
    val eqCustomBandsRaw by vm.eqCustomBandsRaw.collectAsState()
    val sleepTimer      by vm.prefs.sleepTimerMinutes.collectAsState(initial = 0)
    val gapless         by vm.prefs.gaplessEnabled.collectAsState(initial = true)

    var showThemePicker by remember { mutableStateOf(false) }
    var showEqDialog    by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    // Fix 2: Live EQ band state hoisted here (must never be inside an if-block —
    // Compose rules forbid remember/state calls inside conditionals).
    // Reseeds itself whenever the active preset or persisted custom bands change.
    val liveBands = remember(eqPreset, eqCustomBandsRaw) {
        val bands = if (eqPreset == "Custom") {
            val raw = eqCustomBandsRaw.split(",").mapNotNull { it.trim().toFloatOrNull() }
            if (raw.size == 5) raw else List(5) { 0f }
        } else {
            (Constants.EQ_PRESETS[eqPreset] ?: Constants.EQ_PRESETS["Flat"]!!).map { it / 100f }
        }
        mutableStateOf(bands)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 140.dp), // 140dp = mini player (72dp) + nav bar + breathing room
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = stringResource(R.string.settings_title),
            style    = MaterialTheme.typography.headlineMedium,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingCard(
            icon     = Icons.Rounded.Palette,
            title    = stringResource(R.string.settings_theme),
            subtitle = themeDisplayName(currentTheme),
            onClick  = { showThemePicker = true }
        )

        SettingCard(
            icon     = Icons.Rounded.Equalizer,
            title    = stringResource(R.string.settings_equalizer),
            subtitle = if (eqEnabled) "On \u2022 $eqPreset" else "Off",
            onClick  = { showEqDialog = true }
        )

        SettingCard(
            icon     = Icons.Rounded.Bedtime,
            title    = stringResource(R.string.sleep_timer_title),
            subtitle = if (sleepTimer > 0) "$sleepTimer min" else stringResource(R.string.sleep_timer_off),
            onClick  = { showSleepDialog = true }
        )

        // Gapless toggle (inline, no dialog)
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.GraphicEq, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_gapless), style = MaterialTheme.typography.bodyLarge)
                    Text("Continuous playback between tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked         = gapless,
                    onCheckedChange = { vm.setGapless(it) }
                )
            }
        }

        SettingCard(
            icon     = Icons.Rounded.Refresh,
            title    = stringResource(R.string.settings_rescan),
            subtitle = "Scan for new music files",
            onClick  = { vm.scanLibrary() }
        )

        // About card
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("MexMp3", style = MaterialTheme.typography.titleMedium)
                    Text("Version 2.0.6 \u2022 MexTech Limited",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // ── Theme Picker ──────────────────────────────────────────────────────────
    if (showThemePicker) {
        // Each entry: key, display name, primary colour, secondary colour
        val themes = listOf(
            Triple(Constants.THEME_MILITARY,   "Military Dark",      Pair(Color(0xFF4B5320), Color(0xFFCC5500))),
            Triple(Constants.THEME_BLACK,      "Pure Black",         Pair(Color(0xFF909090), Color(0xFF555555))),
            Triple(Constants.THEME_FOREST,     "Deep Forest",        Pair(Color(0xFF1B5E20), Color(0xFFFF6F00))),
            Triple(Constants.THEME_ORANGE,     "Orange Night",       Pair(Color(0xFFE65100), Color(0xFFFFD600))),
            Triple(Constants.THEME_MONO,       "Monochrome Elite",   Pair(Color(0xFFBDBDBD), Color(0xFF757575))),
            Triple(Constants.THEME_NEON_ABYSS, "Neon Abyss",         Pair(Color(0xFF9B30FF), Color(0xFF00E5FF))),
            Triple(Constants.THEME_BLOOD_ROSE, "Blood Rose",         Pair(Color(0xFFB71C1C), Color(0xFFE8899A))),
            Triple(Constants.THEME_ARCTIC,     "Arctic Frost",       Pair(Color(0xFF4DD0E1), Color(0xFF80DEEA))),
            Triple(Constants.THEME_SOLAR,      "Solar Flare",        Pair(Color(0xFFFFC107), Color(0xFFFF7043))),
            Triple(Constants.THEME_PHANTOM,    "Phantom Noir",       Pair(Color(0xFFE0E0E0), Color(0xFF9E9E9E))),
            Triple(Constants.THEME_COSMIC,     "Cosmic Dusk",        Pair(Color(0xFF7C4DFF), Color(0xFFCE93D8))),
            Triple(Constants.THEME_JUNGLE,     "Jungle Shadow",      Pair(Color(0xFF00695C), Color(0xFFCDDC39))),
        )
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            title = {
                Text("Choose Theme", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 480.dp)
                ) {
                    items(themes) { (key, label, colours) ->
                        val isSelected = currentTheme == key
                        val (pri, sec) = colours
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected)
                                        Brush.verticalGradient(listOf(pri.copy(alpha = 0.25f), Color(0xFF111111)))
                                    else
                                        Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D)))
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    brush = if (isSelected)
                                        Brush.linearGradient(listOf(pri, sec))
                                    else
                                        Brush.linearGradient(listOf(Color(0xFF2E2E2E), Color(0xFF2E2E2E))),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { vm.setTheme(key); showThemePicker = false }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Colour swatch
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(pri, sec)))
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check, null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                label,
                                style     = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color     = if (isSelected) pri else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                maxLines  = 2
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    // ── Equalizer (full-screen overlay) ──────────────────────────────────────
    if (showEqDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // Fix 1: Consume ALL pointer events so nothing bleeds through to Settings below
                .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }
        ) {
            EqualizerScreen(
                eqEnabled       = eqEnabled,
                currentPreset   = eqPreset,
                bandValues      = liveBands.value,
                onEnabledChange = { enabled -> vm.setEqualizerEnabled(enabled, eqPreset) },
                onPresetChange  = { preset ->
                    if (preset == "Custom") {
                        // Keep current band values as-is and switch the label to Custom
                        val currentBands = liveBands.value
                        vm.saveCustomEqBands(currentBands)
                    } else {
                        val presetBands = (Constants.EQ_PRESETS[preset] ?: Constants.EQ_PRESETS["Flat"]!!)
                            .map { it / 100f }
                        liveBands.value = presetBands
                        vm.applyEqPreset(preset)
                    }
                },
                onBandChange    = { idx, db ->
                    val updated = liveBands.value.toMutableList().also { it[idx] = db }
                    liveBands.value = updated
                    vm.saveCustomEqBands(updated)
                },
                onDismiss = { showEqDialog = false }
            )
        }
    }

    // ── Sleep Timer Dialog ─────────────────────────────────────────────────────
    if (showSleepDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimer,
            onDismiss      = { showSleepDialog = false },
            onSet          = { min -> vm.setSleepTimer(min); showSleepDialog = false }
        )
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(16.dp),
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SleepTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit
) {
    val options = listOf(0, 5, 10, 15, 30, 45, 60)
    val labels  = listOf("Off", "5 min", "10 min", "15 min", "30 min", "45 min", "1 hour")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer_title)) },
        text = {
            Column {
                options.zip(labels).forEach { (min, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentMinutes == min, onClick = { onSet(min) })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}

private fun themeDisplayName(key: String) = when (key) {
    "MilitaryDark"     -> "Military Dark"
    "PureBlackMinimal" -> "Pure Black"
    "DeepForest"       -> "Deep Forest"
    "OrangeNight"      -> "Orange Night"
    "MonochromeElite"  -> "Monochrome Elite"
    "NeonAbyss"        -> "Neon Abyss"
    "BloodRose"        -> "Blood Rose"
    "ArcticFrost"      -> "Arctic Frost"
    "SolarFlare"       -> "Solar Flare"
    "PhantomNoir"      -> "Phantom Noir"
    "CosmicDusk"       -> "Cosmic Dusk"
    "JungleShadow"     -> "Jungle Shadow"
    else               -> key
}
