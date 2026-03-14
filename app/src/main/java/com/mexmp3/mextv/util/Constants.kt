package com.mexmp3.mextv.util

object Constants {
    const val NOTIFICATION_CHANNEL_ID = "mexmp3_playback"
    const val NOTIFICATION_ID = 1001

    // DataStore keys
    const val PREFS_NAME = "mexmp3_prefs"
    const val KEY_ONBOARDING_DONE = "onboarding_done"
    const val KEY_THEME = "theme_name"
    const val KEY_SHUFFLE = "shuffle_enabled"
    const val KEY_REPEAT_MODE = "repeat_mode"
    const val KEY_CURRENT_SONG_ID = "current_song_id"
    const val KEY_QUEUE = "queue_json"
    const val KEY_GAPLESS = "gapless_enabled"
    const val KEY_SLEEP_TIMER_MINUTES = "sleep_timer_min"
    const val KEY_EQ_ENABLED      = "eq_enabled"
    const val KEY_EQ_PRESET       = "eq_preset"
    const val KEY_EQ_CUSTOM_BANDS = "eq_custom_bands"   // comma-separated floats (dB)

    // Theme names
    const val THEME_MILITARY   = "MilitaryDark"
    const val THEME_BLACK      = "PureBlackMinimal"
    const val THEME_FOREST     = "DeepForest"
    const val THEME_ORANGE     = "OrangeNight"
    const val THEME_MONO       = "MonochromeElite"
    const val THEME_NEON_ABYSS = "NeonAbyss"
    const val THEME_BLOOD_ROSE = "BloodRose"
    const val THEME_ARCTIC     = "ArcticFrost"
    const val THEME_SOLAR      = "SolarFlare"
    const val THEME_PHANTOM    = "PhantomNoir"
    const val THEME_COSMIC     = "CosmicDusk"
    const val THEME_JUNGLE     = "JungleShadow"

    // Repeat modes
    const val REPEAT_NONE = 0
    const val REPEAT_ALL = 1
    const val REPEAT_ONE = 2

    // Intent actions
    const val ACTION_PLAY_PAUSE = "com.mexmp3.mextv.PLAY_PAUSE"
    const val ACTION_NEXT = "com.mexmp3.mextv.NEXT"
    const val ACTION_PREV = "com.mexmp3.mextv.PREV"
    const val ACTION_CLOSE = "com.mexmp3.mextv.CLOSE"
    const val ACTION_SEEK = "com.mexmp3.mextv.SEEK"

    // EQ presets — values in millibels (100 = 1 dB), range -1200..+1200
    // Stored as a mutable map so the "Custom" entry can be updated live from the EQ screen
    val EQ_PRESETS: MutableMap<String, IntArray> = mutableMapOf(
        "Flat"       to intArrayOf(    0,    0,    0,    0,    0),
        "Bass Boost" to intArrayOf(  900,  600,  100, -100, -200),
        "Rock"       to intArrayOf(  500,  200, -300,  300,  600),
        "Jazz"       to intArrayOf(  400,  300,  100,  300,  400),
        "Pop"        to intArrayOf( -100,  300,  500,  300, -100),
        "Classical"  to intArrayOf(  500,  300,  -50,  200,  400),
        "Hip-Hop"    to intArrayOf(  700,  500,  100, -200, -100),
        "Electronic" to intArrayOf(  600,  200, -300,  200,  700),
        "Vocal"      to intArrayOf( -200,  100,  600,  400, -100),
        "Custom"     to intArrayOf(    0,    0,    0,    0,    0)
    )
}
