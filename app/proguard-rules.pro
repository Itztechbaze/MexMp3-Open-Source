# ═══════════════════════════════════════════════════════════════════════════════
# MexMp3 ProGuard Rules
# Without these rules the release APK will crash because R8 strips classes that
# are accessed via reflection, JNI, or annotation processors at runtime.
# ═══════════════════════════════════════════════════════════════════════════════

# ── General Android / Kotlin ────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep Kotlin metadata so reflection-based libraries work
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Room (SQLite ORM) ──────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-dontwarn androidx.room.**

# ── DataStore ──────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── ExoPlayer / Media3 ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn androidx.media3.**
-dontwarn com.google.android.exoplayer2.**

# ── MediaSession / MediaCompat ─────────────────────────────────────────────────
-keep class android.support.v4.media.** { *; }
-keep class androidx.media.** { *; }
-dontwarn android.support.v4.media.**
-dontwarn androidx.media.**

# ── Gson ──────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Compose ───────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── App model and DB classes ──────────────────────────────────────────────────
-keep class com.mexmp3.mextv.data.** { *; }
-keep class com.mexmp3.mextv.data.db.** { *; }

# ── Keep original names in crash stack traces ─────────────────────────────────
-renamesourcefileattribute SourceFile

# ── Strip debug logs from release build ───────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
