# MexMp3 — Premium Offline Music Player

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="MexMp3 Logo"/>
</p>

<p align="center">
  <strong>A premium, fully offline Android music player built with Kotlin + Jetpack Compose.</strong><br/>
  Developed by <strong>MexTech Limited</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2026%2B-green?logo=android"/>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-blue?logo=kotlin"/>
  <img src="https://img.shields.io/badge/Compose-BOM%202026.02.01-brightgreen"/>
  <img src="https://img.shields.io/badge/ExoPlayer-Media3%201.6.0-orange"/>
  <img src="https://img.shields.io/badge/AGP-8.9.0-lightgrey"/>
  <img src="https://img.shields.io/badge/version-2.2.0__patch-blue"/>
</p>

---

## Table of Contents
1. [Features](#features)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Tech Stack](#tech-stack)
5. [Getting Started](#getting-started)
6. [Build Variants](#build-variants)
7. [Firebase Setup](#firebase-setup)
8. [Codemagic CI/CD](#codemagic-cicd)
9. [Signing Configuration](#signing-configuration)
10. [Screens & Navigation](#screens--navigation)
11. [Theming System](#theming-system)
12. [Equalizer & Audio Effects](#equalizer--audio-effects)
13. [Lyrics System](#lyrics-system)
14. [Permissions](#permissions)
15. [Changelog](#changelog)

---

## Features

| Feature | Status |
|---|---|
| MediaStore auto-scan (audio files) | ✅ |
| Auto-scan on launch when permission already granted | ✅ |
| Gapless ExoPlayer playback | ✅ |
| Spinning vinyl disk animation (playing/paused with exact angle hold) | ✅ |
| Animated 32-bar waveform visualiser | ✅ |
| Swipe left/right on player to skip | ✅ |
| Synced + plain LRC lyrics (local file + LRCLib API) | ✅ |
| Lyrics full-screen mode with song title & artist on top | ✅ |
| Persistent foreground service | ✅ |
| Lock-screen + notification controls (MediaStyle) | ✅ |
| Notification tap → opens Now Playing screen directly | ✅ |
| MediaSession (Bluetooth / headset buttons) | ✅ |
| Audio focus handling + ducking | ✅ |
| BecomingNoisy receiver (headset unplug) | ✅ |
| **Studio Mastering 10-band Equalizer — complete rebuild** | ✅ |
| **Per-band color coding (5 unique colors per band)** | ✅ |
| **Multi-color animated spectrum curve with gradient fill** | ✅ |
| **Arc/knob quick-adjust dials (Bass, Low Mid, Treble)** | ✅ |
| **Pill-shaped band thumbs with glow shadow** | ✅ |
| **Power toggle button with pulse glow** | ✅ |
| **10 EQ presets + fully functional Custom mode** | ✅ |
| Sleep timer | ✅ |
| Shuffle (true random; restores sequential order when turned off) | ✅ |
| Repeat (none / all / one) | ✅ |
| Song list sort: Title A–Z / Recently Added / Most Played | ✅ |
| **12 bespoke ultra-sleek app themes** | ✅ |
| Visual swatch grid theme picker | ✅ |
| Playlist CRUD (Room DB) | ✅ |
| Songs / Albums / Artists / Playlists / Folders / Recent screens | ✅ |
| Recent screen shows recently played songs (DB history) | ✅ |
| Global search (title, artist, album) | ✅ |
| Mini-player (always visible above bottom nav) | ✅ |
| Full Now Playing screen | ✅ |
| Song long-press options: Play Next / Add to Queue / Add to Playlist / Edit Info / Delete | ✅ |
| Metadata editor sheet | ✅ |
| Delete song from device (with confirmation dialog) | ✅ |
| Runtime permission dialogs (rationale) | ✅ |
| Animated onboarding with real app logo | ✅ |
| Splash screen (AndroidX SplashScreen API) | ✅ |
| Empty / Loading / Error states | ✅ |
| Firebase Analytics + Crashlytics | ✅ |
| Dark theme enforced (brand overrides) | ✅ |
| Zero hardcoded strings (all in strings.xml) | ✅ |
| ProGuard / R8 release optimisation | ✅ |
| Edge-to-edge display with correct inset handling | ✅ |
| Audio quality badge (bitrate / format pill) | ✅ |
| Bluetooth connected device indicator on Now Playing | ✅ |
| Dynamic colour theming from album art | ✅ |
| Crossfade (0–12 seconds configurable) | ✅ |
| Home screen widget (prev / play-pause / next) | ✅ |
| Folder blacklist (exclude folders from library) | ✅ |

---

## Architecture

```
MVVM + StateFlow + Repository pattern
────────────────────────────────────────────────────
UI Layer        Jetpack Compose screens / components
    ↕
ViewModel       MainViewModel (AndroidViewModel)
    ↕                     ↕
Repository      MusicRepository (MediaStore + Room)
                PrefsRepository (DataStore)
    ↕
Service         MusicService (foreground, ExoPlayer)
```

- **Single ViewModel** — `MainViewModel` drives all UI state
- **MusicService** holds ExoPlayer state as `MutableStateFlow` in its `companion object` — the ViewModel observes it without holding a direct cross-lifecycle reference
- **Pending-action queue** drains once the service binds, eliminating fragile `delay()` hacks
- **Room** for playlists and recent-play history
- **DataStore** for all preferences (theme, EQ, shuffle, repeat, sleep timer, gapless, crossfade, folder blacklist, dynamic colour)
- **LRCLib API** for on-demand lyrics fetch — no API key required

---

## Project Structure

```
MexMp3v2/
├── app/
│   ├── google-services.json            ← Firebase config (keep secret)
│   ├── mexmp3keypro                    ← Release keystore (never commit to public repos)
│   ├── proguard-rules.pro
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mexmp3/mextv/
│       │   ├── MyApplication.kt        ← Firebase init + notification channel
│       │   ├── MainActivity.kt         ← NavHost, permissions, bottom nav,
│       │   │                              notification intent → NowPlaying routing
│       │   ├── data/
│       │   │   ├── Models.kt           ← Song, Album, Artist, Folder DTOs
│       │   │   ├── db/
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   └── Entities.kt     ← Room entities + DAOs
│       │   │   └── repository/
│       │   │       ├── MusicRepository.kt
│       │   │       └── PrefsRepository.kt
│       │   ├── receiver/
│       │   │   ├── BecomingNoisyReceiver.kt
│       │   │   ├── BootReceiver.kt
│       │   │   └── MediaButtonReceiver.kt
│       │   ├── service/
│       │   │   └── MusicService.kt
│       │   ├── ui/
│       │   │   ├── components/
│       │   │   │   └── Components.kt
│       │   │   ├── screens/
│       │   │   │   ├── OnboardingScreen.kt
│       │   │   │   ├── SongsScreen.kt
│       │   │   │   ├── AlbumsScreen.kt
│       │   │   │   ├── ArtistsScreen.kt
│       │   │   │   ├── PlaylistsScreen.kt
│       │   │   │   ├── FoldersAndRecentScreen.kt
│       │   │   │   ├── SearchScreen.kt
│       │   │   │   ├── NowPlayingScreen.kt
│       │   │   │   ├── EqualizerScreen.kt
│       │   │   │   ├── SettingsScreen.kt
│       │   │   │   ├── DetailScreen.kt
│       │   │   │   ├── SongOptionsSheet.kt
│       │   │   │   └── MetadataEditorSheet.kt
│       │   │   ├── theme/
│       │   │   │   ├── Theme.kt         ← 12 ColorSchemes + MexMp3Theme()
│       │   │   │   └── Typography.kt
│       │   │   └── viewmodel/
│       │   │       └── MainViewModel.kt
│       │   └── util/
│       │       ├── Constants.kt
│       │       └── PermissionUtils.kt
│       └── res/
│           ├── drawable/               ← Custom notification vector icons
│           ├── mipmap-{mdpi…xxxhdpi}/
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               └── file_paths.xml
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── codemagic.yaml
```

---

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 2.1.0 | Language |
| KSP | 2.1.0-1.0.29 | Annotation processing (replaces kapt) |
| Jetpack Compose BOM | 2026.02.01 | UI framework |
| Material 3 | via BOM | Design system |
| AndroidX Navigation Compose | 2.8.9 | Screen navigation |
| Media3 ExoPlayer | 1.6.0 | Audio playback |
| Media3 Session | 1.6.0 | MediaSession / lock screen |
| androidx.media | 1.7.0 | MediaStyle notification |
| Coil Compose | 2.7.0 | Album art image loading |
| Room | 2.6.1 | Playlist + recent play DB |
| DataStore Preferences | 1.1.3 | Settings persistence |
| Firebase BOM | 33.9.0 | Analytics + Crashlytics |
| AndroidX SplashScreen | 1.0.1 | Splash screen API |
| Lifecycle ViewModel Compose | 2.8.7 | ViewModel + Compose integration |
| Gson | 2.11.0 | JSON serialisation (queue) |
| LRCLib REST API | — | Free lyrics (no key required) |

**Build toolchain:**

| Tool | Version |
|---|---|
| Android Gradle Plugin | 8.9.0 |
| Gradle Wrapper | 8.11.1 (Codemagic) / 9.4.0 (local) |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |

---

## Getting Started

### Prerequisites
- Android Studio Meerkat (2024.3) or later
- JDK 17
- Android device or emulator on API 26+

### Clone & Open
```bash
git clone https://github.com/YOUR_ORG/mexmp3.git
```
Open **`MexMp3v2/`** in Android Studio.

### First-time Setup
1. Add your `google-services.json` to `app/` (required for Firebase).
2. Let Gradle sync.
3. Hit **Run** (`Shift+F10`).

> **Fully offline.** Lyrics fetching is the only network call and degrades gracefully with no connection.

### First Launch Flow
The animated onboarding screen appears with the real app logo. Tap **Scan My Music** to grant audio permission and populate the library. On subsequent launches the scan runs automatically if permission is already held.

---

## Build Variants

| Variant | Minify | Notes |
|---|---|---|
| `debug` | No | Logging enabled |
| `release` | Yes + shrink resources | Full R8/ProGuard; must be signed |

```bash
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # AAB for Play Store
```

---

## Firebase Setup

1. [Firebase Console](https://console.firebase.google.com/) → your project → download `google-services.json` → place in `app/`.
2. Enable **Analytics** and **Crashlytics**.
3. Crashes are reported automatically from signed release builds.

---

## Codemagic CI/CD

`codemagic.yaml` defines:
- Build on every push to `main`
- Gradle build, APK + AAB artifact upload
- Automatic keystore signing via environment variables

### Required Environment Variables

| Variable | Value |
|---|---|
| `CM_KEYSTORE` | Base64-encoded `.jks` file |
| `CM_KEYSTORE_PASSWORD` | Keystore password |
| `CM_KEY_ALIAS` | Key alias |
| `CM_KEY_PASSWORD` | Key password |

---

## Signing Configuration

Generate your keystore in Android Studio: **Build → Generate Signed Bundle/APK → Create new keystore**

Base64-encode for Codemagic:
```bash
base64 -w 0 mexmp3keypro.jks
```

---

## Screens & Navigation

```
Splash Screen
    └── Onboarding  (first launch — animated logo, feature cards, CTA buttons)
         └── Main App
               ├── Bottom Navigation
               │     ├── Songs  (sort: Title A-Z / Recently Added / Most Played)
               │     ├── Albums
               │     ├── Artists
               │     ├── Playlists
               │     ├── Folders
               │     └── Recent  (recently played from DB history)
               ├── Search  (top bar)
               ├── Settings  (top bar)
               │     ├── Theme Picker  (12-swatch visual grid)
               │     ├── Equalizer  (Studio Mastering 10-band, JBL Stereo, Custom fully tunable)
               │     ├── Crossfade  (0–12 seconds slider)
               │     ├── Gapless Playback toggle
               │     ├── Dynamic Colour toggle
               │     ├── Folder Blacklist
               │     ├── Sleep Timer
               │     └── Rescan Library
               └── Now Playing  (mini-player tap OR notification tap)
                     ├── Spinning vinyl disk (pauses/resumes at exact angle)
                     ├── 32-bar waveform visualiser
                     ├── Seek bar with glowing thumb
                     ├── Transport controls (glow-pulse play button)
                     ├── Audio quality badge + Bluetooth device indicator
                     └── Lyrics panel (expandable; full-screen with song title on top)
```

---

## Theming System

12 complete Material 3 dark colour schemes. Themes switch **instantly** at runtime — no restart needed.

| Key | Name | Primary | Secondary | Vibe |
|---|---|---|---|---|
| `MilitaryDark` *(default)* | Military Dark | `#4B5320` | `#CC5500` | Olive + burnt-orange |
| `PureBlackMinimal` | Pure Black | `#909090` | `#555555` | Cool graphite |
| `DeepForest` | Deep Forest | `#1B5E20` | `#FF6F00` | Emerald + amber |
| `OrangeNight` | Orange Night | `#E65100` | `#FFD600` | Deep ember + gold |
| `MonochromeElite` | Monochrome Elite | `#BDBDBD` | `#757575` | Silver platinum |
| `NeonAbyss` | Neon Abyss | `#9B30FF` | `#00E5FF` | Electric violet + cyan |
| `BloodRose` | Blood Rose | `#B71C1C` | `#E8899A` | Deep crimson + dusty rose |
| `ArcticFrost` | Arctic Frost | `#4DD0E1` | `#80DEEA` | Ice teal + glacier white |
| `SolarFlare` | Solar Flare | `#FFC107` | `#FF7043` | Molten gold + solar orange |
| `PhantomNoir` | Phantom Noir | `#E0E0E0` | `#9E9E9E` | Platinum on oil-slick black |
| `CosmicDusk` | Cosmic Dusk | `#7C4DFF` | `#CE93D8` | Deep indigo + soft lavender |
| `JungleShadow` | Jungle Shadow | `#00695C` | `#CDDC39` | Dark teal + electric lime |

The picker shows a 2-column gradient swatch grid with a checkmark on the active theme.

---

## Equalizer & Audio Effects

Uses `android.media.audiofx.Equalizer` bound to ExoPlayer's `audioSessionId`.

### Studio Mastering EQ — v2.2.0 (complete ground-up rebuild)

A **10-band professional equalizer** with a bespoke studio mastering UI — built entirely from scratch. Nothing from the previous EQ was retained.

#### UI & Visual Features
- **10-band slider array** — 32 Hz · 64 Hz · 125 Hz · 250 Hz · 500 Hz · 1kHz · 2kHz · 4kHz · 8kHz · 16kHz
- **Studio spectrum analyzer** — real-time Catmull-Rom spline curve with dual fill (curve + mirrored above) and an animated neon scanner line sweeping across when EQ is active
- **Per-band accent palette** — a continuous teal → electric-blue → violet gradient spectrum, each band uniquely colored; control nodes glow on the analyzer
- **Sharp studio thumb capsules** — 18×10 dp ultra-thin pill with white-to-accent gradient fill and accent glow shadow; clearly different from anything in the old EQ
- **dB badge** above each slider turns on/off and glows the band's accent color when non-zero
- **Stereo Enhancement Strip** — L/R animated bar-meter with phase-coherent wide-stereo field visualization, styled after professional mastering consoles
- **Studio Power Button** — sweep-gradient border ring with animated pulse; teal glow when active
- **Dark studio canvas** — `#070B11` background for the analyzer, giving a true hardware console feel
- **Preset chips** — Studio / Mastering / JBL Stereo chips have a dot indicator and a teal/blue dual-gradient highlight; all other presets cycle violet/indigo

#### Presets (10-band, millibels — 100 = 1 dB)

| Preset | 32 | 64 | 125 | 250 | 500 | 1k | 2k | 4k | 8k | 16k |
|---|---|---|---|---|---|---|---|---|---|---|
| Flat | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| Bass Boost | +900 | +800 | +600 | +300 | +50 | −50 | −100 | −200 | −200 | −200 |
| Treble Boost | −200 | −200 | −100 | 0 | +100 | +200 | +400 | +600 | +800 | +900 |
| Rock | +500 | +400 | +200 | −300 | 0 | +200 | +300 | +400 | +300 | +200 |
| Jazz | +400 | +350 | +200 | +100 | +100 | +200 | +300 | +300 | +200 | +100 |
| Pop | −100 | −100 | +100 | +400 | +500 | +400 | +300 | +200 | +100 | +50 |
| Classical | +500 | +400 | +300 | +100 | −50 | +50 | +100 | +300 | +350 | +400 |
| Hip-Hop | +700 | +600 | +500 | +300 | +100 | −100 | −200 | −200 | −100 | 0 |
| Electronic | +600 | +500 | +200 | −200 | −300 | −100 | +200 | +300 | +400 | +500 |
| Vocal | −200 | −300 | −200 | +200 | +600 | +700 | +500 | +300 | +100 | −50 |
| **Studio** | +300 | +200 | +100 | −50 | −100 | 0 | +100 | +200 | +300 | +400 |
| **Mastering** | +200 | +150 | +50 | −50 | −100 | −100 | +50 | +150 | +250 | +350 |
| **JBL Stereo** | +800 | +700 | +400 | +100 | −100 | +100 | +300 | +500 | +600 | +700 |
| Custom | user | user | user | user | user | user | user | user | user | user |

The **Custom** preset preserves whatever band positions you drag to — selecting it does not reset values to zero. 10-band values are averaged pairwise to 5-band when passed to the Android `audiofx.Equalizer` API for full hardware compatibility.

---

## Lyrics System

Two-stage lookup, fully on `Dispatchers.IO`:

### Stage 1 — Local LRC file
Looks for a `.lrc` file alongside the audio file:
```
/Music/Davido - Unavailable.mp3
/Music/Davido - Unavailable.lrc   ← auto-detected
```

### Stage 2 — LRCLib API (online fallback)
Queries [lrclib.net](https://lrclib.net) — free, no API key:
```
GET https://lrclib.net/api/get?artist_name=Davido&track_name=Unavailable
```
Synced lyrics are preferred; falls back to plain lyrics; shows a helpful message if neither source has data.

### Full-Screen Mode
Tap the expand icon in the lyrics card header to enter full-screen lyrics view. Song title and artist are displayed at the top. Synced karaoke highlighting and auto-scroll work identically to the inline panel.

---

## Permissions

| Permission | API Range | Purpose |
|---|---|---|
| `READ_MEDIA_AUDIO` | API 33+ | Scan music library |
| `READ_EXTERNAL_STORAGE` | API 26–32 | Scan music library (legacy) |
| `WRITE_EXTERNAL_STORAGE` | API 26–28 | Song deletion + ID3 tag editing (legacy) |
| `POST_NOTIFICATIONS` | API 33+ | Playback notification |
| `FOREGROUND_SERVICE` | all | Background playback |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | API 34+ | Media playback service type |
| `WAKE_LOCK` | all | Keep CPU alive during playback |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | API 23+ | Survive Doze mode / OEM battery killers |
| `MODIFY_AUDIO_SETTINGS` | all | Audio focus ducking |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | all | WifiLock during playback |
| `BLUETOOTH_CONNECT` | API 31+ | Bluetooth device name on Now Playing |
| `INTERNET` | all | Lyrics fetch via LRCLib |

All dangerous permissions use runtime requests with rationale dialogs. Battery optimisation exemption is requested automatically after onboarding completes.

---

## Changelog

### v2.2.0_patch

#### Bug Fixes & Improvements

- **Bluetooth crackle/stutter during downloads — fixed** (`MusicService.kt`):
  - Root cause: concurrent file downloads starve ExoPlayer's audio render thread of CPU time. Over Bluetooth (A2DP), the higher output latency amplifies any render-thread jitter into audible crackling and hanging.
  - Fix 1: Audio render thread priority raised to `THREAD_PRIORITY_AUDIO` so the OS scheduler pre-empts download/IO threads when audio needs to render.
  - Fix 2: `DefaultLoadControl` buffer tuned to 10 s min / 15 s max. The pre-filled buffer absorbs CPU starvation bursts from concurrent downloads without causing stutters.

- **EQ — theme-aware colours** (`EqualizerScreen.kt`): All hardcoded hex colours removed. Spectrum palette, per-band accents, canvas backgrounds, slider thumbs, preset chips, and the stereo strip now derive from `MaterialTheme.colorScheme` (primary, secondary, tertiary). The EQ automatically matches whichever app theme the user has selected.

- **Settings version string** (`SettingsScreen.kt`): Updated from `2.1.11` → `2.2.0_patch`.

- **Version** bumped to `2.2.0_patch` (versionCode 15).

---

### v2.2.0 — Studio Mastering Equalizer

#### New / Changed

- **Equalizer — complete ground-up rebuild** (nothing from the v2.1.11 EQ was kept):
  - Upgraded from 5-band to **10-band** professional studio layout: 32 Hz, 64 Hz, 125 Hz, 250 Hz, 500 Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
  - New teal/electric-blue/violet per-band accent palette — each of the 10 bands has a unique color drawn from a continuous spectrum gradient
  - Studio spectrum analyzer with Catmull-Rom cubic spline, dual fill (below and mirrored above centerline), animated neon scanner sweep line, and glowing control nodes
  - Ultra-thin 18×10 dp studio thumb capsules with white-to-accent gradient and accent glow shadow — sharp, hardware-console aesthetic
  - Stereo Enhancement Strip with animated L/R bar meters and phase-coherent wide-stereo field label — JBL-inspired stereo image
  - 14 presets including three new studio-grade presets: **Studio**, **Mastering**, and **JBL Stereo**
  - Power button uses a sweep-gradient border ring with animated pulse glow in teal
  - Dark `#070B11` analyzer canvas for authentic hardware console feel
  - Preset chips: Studio / Mastering / JBL Stereo have dot indicator + teal-blue dual-gradient highlight
  - 10-band → 5-band pairwise averaging for full `android.media.audiofx.Equalizer` API compatibility

- **Version** bumped to `2.2.0` (versionCode 14).

---

### v2.1.11 — UI Overhaul & Equalizer Rebuild

#### New / Changed

- **Complete UI redesign** — Every screen rebuilt with a new ultra-modern bespoke aesthetic: frosted-glass cards with drop shadows, gradient count badges, animated playing indicators, and consistent spacing throughout.

- **SongCard** — Now features a glow shadow on the album art thumbnail when the track is playing, an animated 3-bar playing indicator (replacing the old pulsing dot), a semi-transparent primary-tinted background tint, and the artist/album separator changed from `•` to `·` for cleaner rendering.

- **MiniPlayer** — Rebuilt with a top accent line in the primary theme color, a pill-shaped circular play/pause button with border, album art shadow glow when playing, and a `FastOutSlowInEasing` entrance.

- **AlbumsScreen** — Album cards now have a gradient overlay on the bottom half of the album art, and the song count is shown in a primary-colored pill overlaid directly on the art rather than below it.

- **ArtistsScreen** — Artist rows now display a gradient radial avatar circle with the artist's initial letter instead of a generic music note icon.

- **PlaylistsScreen** — Cards rebuilt with rounded icon boxes and gradient backgrounds; create/delete dialogs upgraded to use `Button` instead of `TextButton`; FAB is now circular with elevation shadow.

- **SearchScreen** — New gradient count badge on results, illustrated empty and no-results states with radial glow backdrop circles.

- **SettingsScreen** — All setting rows rebuilt with icon boxes (rounded square with primary tint background) and card drop shadows. Styled `Switch` with correct `onPrimary` thumb color. Sleep timer shows active selection in primary color.

- **NowPlayingScreen** — Play button now has an infinite radial glow pulse animation when playing. Prev/Next buttons wrapped in `surfaceVariant` circles. Shuffle and Repeat buttons tint their background circle when active. Seek bar has a custom shadow thumb; elapsed time shown in primary color.

- **Equalizer — complete rebuild** inspired by Poweramp:
  - 5 unique per-band colors: Bass (orange `#FF6B35`), Low Mid (amber `#FFD93D`), Mid (green `#6BCB77`), High Mid (blue `#4D96FF`), Treble (violet `#BB86FC`)
  - Multi-color animated spectrum curve — cubic spline with per-band horizontal gradient fill
  - Band sliders use pill-shaped thumbs (28×20 dp rounded rect) with glow shadow and gradient fill (white → band color)
  - dB axis labels (+12/+6/0/−6/−12) shown left of slider area
  - Power toggle is now a circular button with pulse glow when EQ is active
  - Three arc/knob quick-adjust dials (Bass, Low Mid, Treble) replace the old horizontal bars; each shows an animated arc sweep with a thumb dot and center dB readout
  - Band color indicator dots below each slider label

- **Last.fm removed entirely** — `LastFmRepository.kt` deleted; all scrobble calls, session key storage, username/password flow, and related DataStore keys removed from `MainViewModel`, `PrefsRepository`, `Constants`, `NowPlayingScreen`, and `SettingsScreen`. No network calls are made during playback.

- **Version** bumped to `2.1.11` (versionCode 13).

---

### v2.1.0 — Polish Release

#### Bug Fixes

- **Dynamic Colour not working** — Fixed palette swatch priority (`vibrantSwatch` first), thumbnail size increased to 256×256, background changed to a vertical gradient (accent top → near-black bottom) matching Spotify/Apple Music.

- **Last.fm developer note removed from login dialog** — Was shown to users; removed entirely.

---

### v2.0.9 — Fix & Polish Release

#### Bug Fixes

- **Crossfade and Gapless settings not persisting** — Both were instance variables defaulting to 0/false on every service start. Fixed by reading from DataStore in `onServiceConnected` and applying immediately on bind.

---

### v2.0.8 — Completion Release

#### Improvements

- **True gapless playback** — ExoPlayer native playlist. Next track buffered immediately; `onMediaItemTransition` updates metadata, notification, widget, and preloads the next-next song.
- **Metadata writing** — `updateSongMetadata()` writes to MediaStore via `ContentResolver.update()` on all API levels.
- **Home screen widget** — Song title, artist, and prev/play/next controls with live updates.
- **Crossfade** — Configurable 0–12 second crossfade via Settings → Audio → Crossfade.

---

### v2.0.7 — Premium Features Release

#### New Features

- **Crossfade** — 20-step volume fade between songs. Configurable 0–12 seconds.
- **Dynamic Colour Theming** — Now Playing background animates to album art dominant color using Palette API.
- **Home Screen Widget** — 4×2 widget with controls; updates instantly on playback change.
- **Folder Blacklist** — Exclude folders from library. Triggers auto-rescan on change.

#### Technical
- Added `androidx.palette:palette-ktx:1.0.0`
- `MexMp3Widget` registered as `AppWidgetProvider`
- All 4 features persist via DataStore

---

### v2.0.6 — Bug Fix Release

- **Notification tap not opening Now Playing** — `openNowPlaying` changed from `var` to `mutableStateOf` so Compose observes it correctly.

---

### v2.0.5 — Bug Fix Release

- **Delete song not working on Android 11+** — Two-path approach: `MediaStore.createDeleteRequest()` on API 30+ (shows native OS dialog); direct delete on API 29 and below.

---

### v2.0.4 — Bug Fix Release

- **Album art spinning too fast** — Fixed with `while(true)` loop + `snapTo(angle % 360)`.
- **Custom EQ bands not sliding** — Fixed with `rememberUpdatedState` so drag lambda reads live `dB` value.
- **App closing on Vivo Y20** — `startForeground()` moved to `onCreate()`; `unbindService()` not called while playing; `BootReceiver` added.
- **Lyrics sync stutter** — `activeIndex` moved to `derivedStateOf`; scroll animation tightened to `tween(300)`.

---

### v2.0.3 — Bug Fix Release

- **Music stops when swiped from recents** — Changed to `startForegroundService()` on API 26+.
- **EQ band sliders lagging** — Raw `dB` used for thumb position; `awaitEachGesture` for reliable touch; hit area widened to 44 dp.

---

### v2.0.2 — UX Polish & Bug Fix Release

- **Shuffle stuck** — `originalQueue` StateFlow added; turning shuffle off restores original order at current song.
- **Custom EQ resetting** — Selecting Custom no longer resets bands to zero.
- **Recent screen showing all songs** — Now reads from Room play-history.
- **Spinning vinyl disk** — Continuous rotation with exact angle hold on pause.
- **Delete song from device** — Added to long-press options sheet with confirmation.
- **Song list sort** — Title A–Z / Recently Added / Most Played.

---

### v2.0.1 — Bug Fix & Improvement Release

- **EQ touch passing through to Settings** — Full-screen `Box` with `pointerInput` interceptor.
- **Custom EQ unresponsive** — `liveBands` local `mutableStateOf` updates immediately on drag.
- **EQ presets no audible effect** — Lazy `Equalizer` init with `runCatching` around `setBandLevel()`.
- **Music stops when swiped from recents** — `startForeground()` in `onTaskRemoved()`.
- Audio quality badge and Bluetooth indicator added to Now Playing.

---

### v2.0.0 — Production Release

- Infinite song skip bug fixed
- Release APK crashes — full R8/ProGuard rules
- `ForegroundServiceDidNotStartInTimeException` on Android 12+ fixed
- ANR from synchronous bitmap load fixed
- `PARTIAL_WAKE_LOCK` properly managed
- Battery optimisation exemption requested after onboarding
- EQ QuickAdjustBar non-functional fixed
- `onStartCommand` null intent crash fixed

---

## License

```
Copyright © 2026 MexTech Limited. All rights reserved.

MexMp3 is open source software released under the MIT License.
You are free to use, modify, and distribute this project with attribution.
```
