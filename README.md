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
  <img src="https://img.shields.io/badge/version-2.0.6-blue"/>
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
15. [Known Limitations / TODOs](#known-limitations--todos)
16. [Changelog](#changelog)

---

## Features

| Feature | Status |
|---|---|
| MediaStore auto-scan (audio files) | ✅ |
| Auto-scan on launch when permission already granted | ✅ |
| Gapless ExoPlayer playback | ✅ |
| **Spinning vinyl disk animation (playing/paused with exact angle hold)** | ✅ |
| Animated waveform visualiser | ✅ |
| Swipe left/right on player to skip | ✅ |
| **Synced + plain LRC lyrics (local file + LRCLib API)** | ✅ |
| **Lyrics full-screen mode with song title & artist on top** | ✅ |
| **Expanded lyrics panel (wider display, more lines visible)** | ✅ |
| Persistent foreground service | ✅ |
| Lock-screen + notification controls (MediaStyle) | ✅ |
| Notification tap → opens Now Playing screen directly | ✅ |
| MediaSession (Bluetooth / headset buttons) | ✅ |
| Audio focus handling + ducking | ✅ |
| BecomingNoisy receiver (headset unplug) | ✅ |
| **Premium Poweramp-style 5-band Equalizer** | ✅ |
| **10 EQ presets + fully functional Custom mode** | ✅ |
| Sleep timer | ✅ |
| **Shuffle (true random, no repeat; restores sequential order when turned off)** | ✅ |
| Repeat (none / all / one) | ✅ |
| **Song list sort: Title A–Z / Recently Added / Most Played** | ✅ |
| **12 bespoke ultra-sleek app themes** | ✅ |
| Visual swatch grid theme picker | ✅ |
| Playlist CRUD (Room DB) | ✅ |
| Songs / Albums / Artists / Playlists / Folders / Recent screens | ✅ |
| **Recent screen shows recently played songs (DB history)** | ✅ |
| Global search (title, artist, album) | ✅ |
| Mini-player (always visible above bottom nav) | ✅ |
| Full Now Playing screen | ✅ |
| **Song long-press options: Play Next / Add to Queue / Add to Playlist / Edit Info / Delete** | ✅ |
| Metadata editor sheet | ✅ |
| **Delete song from device (with confirmation dialog)** | ✅ |
| **Larger song cards (bigger art thumbnail, taller rows)** | ✅ |
| **Removed excess black margin above song list** | ✅ |
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
- **DataStore** for all preferences (theme, EQ, shuffle, repeat, sleep timer, gapless)
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
               │     ├── Equalizer  (Poweramp-style full-screen, Custom fully tunable)
               │     ├── Sleep Timer
               │     ├── Gapless Playback toggle
               │     └── Rescan Library
               └── Now Playing  (mini-player tap OR notification tap)
                     ├── Spinning vinyl disk (pauses/resumes at exact angle)
                     ├── Waveform visualiser
                     ├── Seek bar
                     ├── Transport controls (shuffle restores sequential order when off)
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

UI features: live spectrum curve (Canvas), 5 vertical drag sliders, dB readout per band, grid lines at ±6 dB / ±12 dB, scrollable preset chips, Bass + Treble quick-adjust sliders, ON/OFF pill toggle.

**Presets** (millibels — 100 = 1 dB):

| Preset | 60Hz | 230Hz | 910Hz | 4kHz | 14kHz |
|---|---|---|---|---|---|
| Flat | 0 | 0 | 0 | 0 | 0 |
| Bass Boost | +900 | +600 | +100 | −100 | −200 |
| Rock | +500 | +200 | −300 | +300 | +600 |
| Jazz | +400 | +300 | +100 | +300 | +400 |
| Pop | −100 | +300 | +500 | +300 | −100 |
| Classical | +500 | +300 | −50 | +200 | +400 |
| Hip-Hop | +700 | +500 | +100 | −200 | −100 |
| Electronic | +600 | +200 | −300 | +200 | +700 |
| Vocal | −200 | +100 | +600 | +400 | −100 |
| Custom | user | user | user | user | user |

The **Custom** preset preserves whatever band positions you drag to — selecting it does not reset values to zero. Any band drag while on any preset automatically switches to Custom and saves the configuration.

---

## Lyrics System

Two-stage lookup, fully on `Dispatchers.IO`:

### Stage 1 — Local LRC file
Looks for a `.lrc` file alongside the audio file:
```
/Music/Davido - Unavailable.mp3
/Music/Davido - Unavailable.lrc   ← auto-detected
```
LRC time tags (`[00:12.34]`) are stripped to plain text if synced data is malformed.

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

All dangerous permissions use runtime requests with rationale dialogs.
Battery optimisation exemption is requested automatically after onboarding completes.

---

## Known Limitations / TODOs

- **Metadata writing** — editor UI present; saving to disk is a no-op pending `MANAGE_MEDIA` implementation
- **True gapless playback** — toggle exists; requires `onMediaItemTransition` architecture (planned for v2.1)
- **Home-screen widget** — planned
- **Crossfade** — DataStore key reserved; implementation pending

---

## Changelog

### v2.0.6 — Bug Fix Release

#### Bug Fix

- **Notification tap not opening Now Playing screen** — Tapping the notification player while the app was already open did nothing — the app stayed on whatever screen was visible. Root cause: `openNowPlaying` was a plain `var` on the Activity, so when `onNewIntent()` fired Compose had no way to observe the change and `AppRoot` never recomposed. Fixed by changing `openNowPlaying` to `by mutableStateOf(false)` so Compose observes it as State. Added an `onNowPlayingConsumed` callback so the composable resets the flag after consuming it, preventing double-navigation. The `LaunchedEffect` now fires every time the notification is tapped regardless of which screen the user is on, navigating directly to the Now Playing screen with `launchSingleTop = true`.

---

### v2.0.5 — Bug Fix Release

#### Bug Fixes

- **Delete song not working — song stays on list after confirmation** — On Android 11+ (API 30+), apps cannot directly delete media files they did not create. The previous implementation called `ContentResolver.delete()` directly which silently returned 0 (deleted nothing) without throwing any error, so the song stayed on the list and no feedback was shown. Fixed with a two-path approach: on Android 11+ the system `MediaStore.createDeleteRequest()` is used, which shows the native OS "Allow deletion?" permission dialog — after the user confirms, the library rescans and a "Song deleted successfully" snackbar appears. On Android 10 and below the direct delete path is used with explicit success/failure feedback via snackbar. A `SnackbarHost` was also added to the main `Scaffold` so feedback toasts display correctly throughout the app.

---

### v2.0.4 — Bug Fix Release

#### Bug Fixes

- **Album art spinning too fast** — `animateTo` was being called with `infiniteRepeatable` as its `animationSpec`, which is invalid — `animateTo` runs to a single target value and `infiniteRepeatable` caused it to jump to the target instantly. Replaced with a `while(true)` loop that calls `animateTo(+360°)` with a 12-second `tween`, then `snapTo(angle % 360)` to prevent the value growing unbounded. The disk now rotates at a natural one revolution per 12 seconds and pauses/resumes at the exact angle when playback is toggled.

- **Custom EQ bands still not sliding correctly** — The drag lambda inside `pointerInput` was capturing `dB` at composition time — because `pointerInput` only recomposes when its keys change, the lambda was reading a stale value on every drag event, causing the thumb to freeze or jump erratically. Fixed by wrapping `dB` in `rememberUpdatedState` so the lambda always reads the latest live value regardless of recomposition. The thumb now follows the finger smoothly with zero lag.

- **App closing when swiped from recents on Vivo Y20** — Three-part fix: (1) `startForeground()` is now called immediately inside `onCreate()` rather than waiting for `onStartCommand()` — on Vivo Funtouch OS the gap between these two calls is enough for the OS to kill the service. (2) `MainViewModel.onCleared()` no longer calls `unbindService()` while music is playing — removing the last bound client while the service isn't yet self-started was the primary kill trigger. (3) Added `BootReceiver` (`RECEIVE_BOOT_COMPLETED` + `QUICKBOOT_POWERON`) to restart the service after device reboot, handling Vivo's aggressive post-reboot service cleanup.

- **Lyrics sync lag and stutter** — `activeIndex` was computed inside `remember(state, positionMs)` — this caused the entire lyrics `Column` (all lines, all animations) to recompose on every position tick (every 500ms). Replaced with `derivedStateOf` so only `activeIndex` is recomputed on position change, never the lyrics list. Scroll animation also tightened from `tween(400)` to `tween(300, LinearOutSlowInEasing)` for crisper line tracking.

---

### v2.0.3 — Bug Fix Release

#### Bug Fixes

- **Music stops when app swiped from recents (Redmi / budget devices)** — The previous fix used `startService()` which Android silently blocks on API 26+ when called from the background. Changed to `startForegroundService()` on API 26+ (with `startService()` fallback for older). This correctly re-promotes the service to a self-started foreground service the moment the app is swiped away, so even after the ViewModel unbinds there is no reason for the OS to kill it. Notification player remains visible and music keeps playing — only the ✕ button stops everything.

- **EQ band sliders lagging / not following finger** — Three root causes fixed: (1) Thumb position was driven by the spring-animated `animatedDb` value instead of the raw `dB`, causing the thumb to visually lag behind the finger. Fixed to use raw `dB` for thumb position so it follows the finger with zero lag. (2) Thumb offset calculation was incorrectly treating pixels as dp (`thumbOffset.dp / 3.5f`), making it move the wrong distance. Fixed using `LocalDensity` for correct px→dp conversion. (3) Replaced `detectVerticalDragGestures` with `awaitEachGesture` + `awaitFirstDown` + `awaitPointerEvent` loop — more reliable on budget devices, also snaps the band value immediately on touch-down so tapping anywhere on the track jumps to that position instantly. Touch hit area widened from 28 dp to 44 dp for easier finger targeting.

---

### v2.0.2 — UX Polish & Bug Fix Release

#### Bug Fixes

- **Shuffle stuck — could not return to sequential order** — Once shuffle was enabled, turning it off had no effect; the queue remained in its shuffled state and the player kept picking random tracks. Fixed by storing the original unshuffled queue (`originalQueue` StateFlow) when `playQueue()` is called. Toggling shuffle off now restores the original order and re-positions the queue index to the currently playing song so playback continues seamlessly.

- **Shuffle picking same song repeatedly** — `skipToNext()` in shuffle mode used `q.indices.random()` which could return the current index. Fixed to exclude the current index when the queue has more than one song.

- **Custom EQ bands resetting to zero** — Selecting the "Custom" preset chip was re-seeding all five band sliders to 0 dB from `Constants.EQ_PRESETS["Custom"]`. Fixed so that tapping "Custom" keeps the current band positions as-is and saves them as the new custom configuration, making the Custom preset a true "edit from here" mode.

- **Recent screen showing all songs** — The Recent tab was showing all library songs sorted by `dateAdded` instead of songs the user had actually played. Fixed to read from the Room `recentDao` play-history ordered most-recent-first, with a fallback to recently-added sort only if play history is empty.

#### Improvements

- **Spinning vinyl disk animation** — The Now Playing album art now rotates continuously like a vinyl record while playing. When paused, the disk freezes at its exact current angle using `Animatable`. Resuming playback continues spinning from that angle with no jump.

- **Larger song list rows** — `SongCard` album art thumbnail increased from 54 dp to 60 dp; row vertical padding increased from 12 dp to 14 dp. All song list screens benefit automatically.

- **Removed black margin above song list** — The `TopAppBar` inside `LibraryScaffold` had its `windowInsets` and `contentWindowInsets` zeroed, eliminating the excess black space pushing the song list down.

- **Full-screen lyrics mode** — The lyrics card now has an expand icon in its header. Tapping it opens a full-screen overlay showing the song title and artist at the top, with larger text, full karaoke sync highlighting, and auto-scroll.

- **Wider lyrics panel** — Inline lyrics card maximum height increased from 320 dp to 480 dp, showing significantly more lines before scrolling is needed.

- **Song list sort options** — Sort icon button added to the Songs screen header with three options: Title A–Z (default), Recently Added, Most Played. Active non-default sort highlights the icon in the primary theme colour.

- **Delete song from device** — "Delete from device" option added to the long-press song options sheet (shown in red). Tapping it shows a confirmation dialog. On confirmation the song is removed from MediaStore and the library rescans automatically.

---

### v2.0.1 — Bug Fix & Improvement Release

#### Bug Fixes

- **EQ page touch passing through to Settings** — Opening the Equalizer from Settings and touching anywhere on the EQ screen was also triggering items on the Settings screen underneath. Fixed by wrapping the EQ overlay in a full-screen `Box` with a `pointerInput` interceptor consuming all touch events.

- **Custom EQ bands unresponsive / stuck** — Dragging band sliders on the Custom preset had no visible effect. Fixed by introducing a `liveBands` local `mutableStateOf` that updates immediately on drag without waiting for a DataStore round-trip.

- **EQ presets have no audible effect** — Changing presets produced no change in audio. Fixed by lazily initialising the `Equalizer` inside `applyEqPreset()`, adding `runCatching {}` around `setBandLevel()` calls, and ensuring the EQ re-enables itself before applying.

- **Music stops when app swiped from recents** — Fixed by calling `startForeground()` immediately inside `onTaskRemoved()`.

#### Improvements

- Audio quality badge on Now Playing screen (320 kbps, FLAC, AAC, etc.)
- Bluetooth connected device indicator on Now Playing screen
- Album detail screen redesigned with larger art and song-count pill
- Playlist detail: Add Songs FAB with searchable bottom sheet

---

### v2.0.0 — Production Release

#### Critical Bug Fixes
- Infinite song skip bug fixed (`!player.hasNextMediaItem()` guard)
- Music stops when app swiped from recents — `onTaskRemoved()` + AlarmManager restart
- Release APK crashes — full R8/ProGuard rules added
- `ForegroundServiceDidNotStartInTimeException` on Android 12+ — `startForeground()` moved into `playSong()`
- Synchronous bitmap load ANR — async IO coroutine + bitmap cache
- App killed when screen off — `PARTIAL_WAKE_LOCK` properly acquired/released
- Battery optimisation / Doze mode killing service — exemption request after onboarding
- EQ QuickAdjustBar non-functional — `detectHorizontalDragGestures` fix
- `onStartCommand` crash on null intent — graceful handling added

---

## License

```
Copyright © 2026 MexTech Limited. All rights reserved.

MexMp3 is open source software released under the MIT License.
You are free to use, modify, and distribute this project with attribution.
```
