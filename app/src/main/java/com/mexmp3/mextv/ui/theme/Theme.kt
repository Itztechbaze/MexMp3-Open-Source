package com.mexmp3.mextv.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Shared neutrals ────────────────────────────────────────────────────────
val PureBlack  = Color(0xFF000000)
val White      = Color(0xFFFFFFFF)
val White70    = Color(0xB3FFFFFF)
val White40    = Color(0x66FFFFFF)

// ════════════════════════════════════════════════════════════════════════════
// 1. MILITARY DARK  — olive / burnt-orange  (default)
// ════════════════════════════════════════════════════════════════════════════
private val MilitaryDarkColors = darkColorScheme(
    primary            = Color(0xFF4B5320),
    onPrimary          = White,
    primaryContainer   = Color(0xFF2B3300),
    onPrimaryContainer = White,
    secondary          = Color(0xFFCC5500),
    onSecondary        = White,
    secondaryContainer = Color(0xFF3D1800),
    background         = Color(0xFF000000),
    onBackground       = White,
    surface            = Color(0xFF121212),
    onSurface          = White,
    surfaceVariant     = Color(0xFF1E1E1E),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF3A3A3A),
    error              = Color(0xFFCF6679),
)

// ════════════════════════════════════════════════════════════════════════════
// 2. PURE BLACK MINIMAL  — cool graphite
// ════════════════════════════════════════════════════════════════════════════
private val PureBlackColors = darkColorScheme(
    primary            = Color(0xFF909090),
    onPrimary          = Color(0xFF000000),
    primaryContainer   = Color(0xFF222222),
    onPrimaryContainer = White,
    secondary          = Color(0xFF555555),
    onSecondary        = White,
    secondaryContainer = Color(0xFF111111),
    background         = Color(0xFF000000),
    onBackground       = White,
    surface            = Color(0xFF0A0A0A),
    onSurface          = White,
    surfaceVariant     = Color(0xFF161616),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF2E2E2E),
)

// ════════════════════════════════════════════════════════════════════════════
// 3. DEEP FOREST  — emerald / amber
// ════════════════════════════════════════════════════════════════════════════
private val DeepForestColors = darkColorScheme(
    primary            = Color(0xFF1B5E20),
    onPrimary          = White,
    primaryContainer   = Color(0xFF0A2A0C),
    onPrimaryContainer = White,
    secondary          = Color(0xFFFF6F00),
    onSecondary        = White,
    secondaryContainer = Color(0xFF3E1C00),
    background         = Color(0xFF050F05),
    onBackground       = White,
    surface            = Color(0xFF0C1A0C),
    onSurface          = White,
    surfaceVariant     = Color(0xFF112211),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF2E4B2E),
)

// ════════════════════════════════════════════════════════════════════════════
// 4. ORANGE NIGHT  — deep ember / golden
// ════════════════════════════════════════════════════════════════════════════
private val OrangeNightColors = darkColorScheme(
    primary            = Color(0xFFE65100),
    onPrimary          = White,
    primaryContainer   = Color(0xFF3E1700),
    onPrimaryContainer = White,
    secondary          = Color(0xFFFFD600),
    onSecondary        = Color(0xFF1A1100),
    secondaryContainer = Color(0xFF2A2000),
    background         = Color(0xFF0A0500),
    onBackground       = White,
    surface            = Color(0xFF130B00),
    onSurface          = White,
    surfaceVariant     = Color(0xFF201200),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF4A2800),
)

// ════════════════════════════════════════════════════════════════════════════
// 5. MONOCHROME ELITE  — silver / pure white
// ════════════════════════════════════════════════════════════════════════════
private val MonochromeColors = darkColorScheme(
    primary            = Color(0xFFBDBDBD),
    onPrimary          = Color(0xFF000000),
    primaryContainer   = Color(0xFF333333),
    onPrimaryContainer = White,
    secondary          = Color(0xFF757575),
    onSecondary        = White,
    secondaryContainer = Color(0xFF222222),
    background         = Color(0xFF000000),
    onBackground       = White,
    surface            = Color(0xFF111111),
    onSurface          = White,
    surfaceVariant     = Color(0xFF1C1C1C),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF3C3C3C),
)

// ════════════════════════════════════════════════════════════════════════════
// 6. NEON ABYSS  — electric violet / cyan  (new)
// ════════════════════════════════════════════════════════════════════════════
private val NeonAbyssColors = darkColorScheme(
    primary            = Color(0xFF9B30FF),   // vivid violet
    onPrimary          = White,
    primaryContainer   = Color(0xFF2A0A4A),
    onPrimaryContainer = White,
    secondary          = Color(0xFF00E5FF),   // electric cyan
    onSecondary        = Color(0xFF001A1F),
    secondaryContainer = Color(0xFF002A33),
    background         = Color(0xFF050008),
    onBackground       = White,
    surface            = Color(0xFF0D0015),
    onSurface          = White,
    surfaceVariant     = Color(0xFF160022),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF3D1A6B),
    error              = Color(0xFFFF4081),
)

// ════════════════════════════════════════════════════════════════════════════
// 7. BLOOD ROSE  — deep crimson / dusty rose  (new)
// ════════════════════════════════════════════════════════════════════════════
private val BloodRoseColors = darkColorScheme(
    primary            = Color(0xFFB71C1C),   // deep crimson
    onPrimary          = White,
    primaryContainer   = Color(0xFF3B0000),
    onPrimaryContainer = White,
    secondary          = Color(0xFFE8899A),   // dusty rose
    onSecondary        = Color(0xFF1F0008),
    secondaryContainer = Color(0xFF3D0015),
    background         = Color(0xFF08000A),
    onBackground       = White,
    surface            = Color(0xFF110008),
    onSurface          = White,
    surfaceVariant     = Color(0xFF1C0010),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF5C0020),
    error              = Color(0xFFFF6090),
)

// ════════════════════════════════════════════════════════════════════════════
// 8. ARCTIC FROST  — ice blue / glacier white  (new)
// ════════════════════════════════════════════════════════════════════════════
private val ArcticFrostColors = darkColorScheme(
    primary            = Color(0xFF4DD0E1),   // ice teal
    onPrimary          = Color(0xFF001F24),
    primaryContainer   = Color(0xFF002B33),
    onPrimaryContainer = White,
    secondary          = Color(0xFF80DEEA),   // glacier
    onSecondary        = Color(0xFF001419),
    secondaryContainer = Color(0xFF001E24),
    background         = Color(0xFF020B0E),
    onBackground       = White,
    surface            = Color(0xFF051318),
    onSurface          = White,
    surfaceVariant     = Color(0xFF0A1E24),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF1A3D46),
    error              = Color(0xFFFF7043),
)

// ════════════════════════════════════════════════════════════════════════════
// 9. SOLAR FLARE  — molten gold / solar orange  (new)
// ════════════════════════════════════════════════════════════════════════════
private val SolarFlareColors = darkColorScheme(
    primary            = Color(0xFFFFC107),   // molten gold
    onPrimary          = Color(0xFF1A1000),
    primaryContainer   = Color(0xFF2E1F00),
    onPrimaryContainer = White,
    secondary          = Color(0xFFFF7043),   // solar orange
    onSecondary        = Color(0xFF1A0800),
    secondaryContainer = Color(0xFF2A0F00),
    background         = Color(0xFF080500),
    onBackground       = White,
    surface            = Color(0xFF110A00),
    onSurface          = White,
    surfaceVariant     = Color(0xFF1C1200),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF4A3000),
    error              = Color(0xFFFF5252),
)

// ════════════════════════════════════════════════════════════════════════════
// 10. PHANTOM NOIR  — oil-slick black / platinum  (new)
// ════════════════════════════════════════════════════════════════════════════
private val PhantomNoirColors = darkColorScheme(
    primary            = Color(0xFFE0E0E0),   // near-white platinum
    onPrimary          = Color(0xFF000000),
    primaryContainer   = Color(0xFF1A1A1A),
    onPrimaryContainer = White,
    secondary          = Color(0xFF9E9E9E),   // cool grey
    onSecondary        = Color(0xFF000000),
    secondaryContainer = Color(0xFF0F0F0F),
    background         = Color(0xFF000000),
    onBackground       = White,
    surface            = Color(0xFF080808),
    onSurface          = White,
    surfaceVariant     = Color(0xFF121212),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF252525),
    error              = Color(0xFFCF6679),
)

// ════════════════════════════════════════════════════════════════════════════
// 11. COSMIC DUSK  — deep indigo / soft lavender  (new)
// ════════════════════════════════════════════════════════════════════════════
private val CosmicDuskColors = darkColorScheme(
    primary            = Color(0xFF7C4DFF),   // deep indigo
    onPrimary          = White,
    primaryContainer   = Color(0xFF1A0050),
    onPrimaryContainer = White,
    secondary          = Color(0xFFCE93D8),   // soft lavender
    onSecondary        = Color(0xFF1A001F),
    secondaryContainer = Color(0xFF2A0035),
    background         = Color(0xFF04000E),
    onBackground       = White,
    surface            = Color(0xFF0A0018),
    onSurface          = White,
    surfaceVariant     = Color(0xFF140028),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF3A1870),
    error              = Color(0xFFFF4081),
)

// ════════════════════════════════════════════════════════════════════════════
// 12. JUNGLE SHADOW  — dark teal / lime strike  (new)
// ════════════════════════════════════════════════════════════════════════════
private val JungleShadowColors = darkColorScheme(
    primary            = Color(0xFF00695C),   // deep teal
    onPrimary          = White,
    primaryContainer   = Color(0xFF00201C),
    onPrimaryContainer = White,
    secondary          = Color(0xFFCDDC39),   // electric lime
    onSecondary        = Color(0xFF141400),
    secondaryContainer = Color(0xFF202000),
    background         = Color(0xFF010A08),
    onBackground       = White,
    surface            = Color(0xFF041210),
    onSurface          = White,
    surfaceVariant     = Color(0xFF081C18),
    onSurfaceVariant   = White70,
    outline            = Color(0xFF1A3D35),
    error              = Color(0xFFFF5252),
)

// ════════════════════════════════════════════════════════════════════════════
// Router
// ════════════════════════════════════════════════════════════════════════════
fun themeColorScheme(themeName: String): ColorScheme = when (themeName) {
    "PureBlackMinimal" -> PureBlackColors
    "DeepForest"       -> DeepForestColors
    "OrangeNight"      -> OrangeNightColors
    "MonochromeElite"  -> MonochromeColors
    "NeonAbyss"        -> NeonAbyssColors
    "BloodRose"        -> BloodRoseColors
    "ArcticFrost"      -> ArcticFrostColors
    "SolarFlare"       -> SolarFlareColors
    "PhantomNoir"      -> PhantomNoirColors
    "CosmicDusk"       -> CosmicDuskColors
    "JungleShadow"     -> JungleShadowColors
    else               -> MilitaryDarkColors   // "MilitaryDark" default
}

@Composable
fun MexMp3Theme(
    themeName: String = "MilitaryDark",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = themeColorScheme(themeName),
        typography  = MexTypography,
        content     = content
    )
}
