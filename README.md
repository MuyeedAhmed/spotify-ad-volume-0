# AdVol 🎵🔇

**AdVol** is an Android background application designed to automatically manipulate and mute/lower your media volume whenever **Spotify** plays advertisements, and smoothly restore your music volume the instant your songs resume.

Built with **Modern Android (Kotlin & Jetpack Compose)**, following Material 3 guidelines and background execution standards for Android 8.0 through Android 15.

---

## ✨ Features

- **Dual-Layer Ad Detection**:
  - **Primary**: Ultra low-latency Spotify Broadcast Receiver (`com.spotify.music.metadatachanged`, `com.spotify.music.playbackstatechanged`).
  - **Secondary (Fallback)**: `NotificationListenerService` that inspects Spotify notifications even if broadcast settings are disabled.
- **Intelligent Volume Management**:
  - Automatically captures and preserves your pre-ad volume level.
  - Prevents volume corruption during consecutive back-to-back ads.
  - **Adjustable Mute Level**: Choose complete silence (0%) or a lowered background level (e.g. 5%–15%) so you still know playback is continuing.
  - **Smooth Audio Fading**: Coroutine-based volume ramp down (~250ms) and ramp up (~300ms) to eliminate sudden audio pop/click artifacts.
  - Silent volume manipulation (uses flag `0` so the system on-screen volume HUD never interrupts you).
- **Background Reliability**:
  - Android 14/15-ready **Foreground Service** (`mediaPlayback` type) with persistent status notification and quick Pause/Resume actions.
  - **Quick Settings Tile**: Toggle AdVol on or off with a single tap directly from your Android notification shade.
  - Battery optimization exclusion guidance for uninterrupted background operation.
- **Clean Jetpack Compose UI**:
  - Live "Now Playing" card showing real-time Spotify status (Playing, Paused, or Muted Ad).
  - Statistics dashboard: total ads muted and quiet time saved.
  - Built-in visual setup guide for Spotify.

---

## 🏗️ Architecture

```
advol0/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/advol/app/
│   │   │   │   ├── AdVolApplication.kt          # Global state & notification channels
│   │   │   │   ├── MainActivity.kt              # Compose root & permissions
│   │   │   │   ├── core/
│   │   │   │   │   ├── AdDetectionEngine.kt     # Pure ad evaluation heuristics
│   │   │   │   │   ├── PlaybackState.kt         # Data model for Spotify metadata
│   │   │   │   │   ├── PreferencesManager.kt    # DataStore settings & statistics
│   │   │   │   │   └── VolumeController.kt      # AudioManager & smooth fade transitions
│   │   │   │   ├── receiver/
│   │   │   │   │   └── SpotifyBroadcastReceiver.kt # Broadcast receiver for Spotify intents
│   │   │   │   ├── service/
│   │   │   │   │   ├── AdVolService.kt          # Foreground monitoring service
│   │   │   │   │   ├── AdVolNotificationListener.kt # Fallback NotificationListenerService
│   │   │   │   │   └── AdVolTileService.kt      # Quick Settings dropdown tile
│   │   │   │   └── ui/
│   │   │   │       ├── components/              # StatusCard, PermissionBanner
│   │   │   │       ├── screens/                 # DashboardScreen, SettingsScreen, SetupGuideScreen
│   │   │   │       └── theme/                   # Theme, Color, Type
│   │   │   └── res/                             # Vectors, icons, strings, colors
│   │   └── test/
│   │       └── java/com/advol/app/core/
│   │           └── AdDetectionEngineTest.kt     # Unit test suite
├── gradle/
│   └── libs.versions.toml                       # Modern Version Catalog
└── build.gradle.kts / settings.gradle.kts
```

---

## 🚀 Setup & Installation

### 1. One-Time Spotify Setup
For the most instantaneous ad detection without battery impact:
1. Open the **Spotify** app.
2. Tap your **Profile picture / Settings (Gear icon)**.
3. Scroll to the **"Privacy & Social"** (or **"Social"**) section.
4. Toggle **"Device Broadcast Status"** to **ON**.

*(Note: If you skip this, AdVol's built-in `NotificationListenerService` will still detect ads as long as you grant Notification Access!)*

### 2. Android Permissions
- **Notification Permission (Android 13+)**: Allows AdVol to show the ongoing foreground monitoring notification.
- **Notification Access (Optional, Recommended)**: Enables the secondary ad detection layer.
- **Battery Optimization (Optional)**: Whitelist AdVol to ensure OEM battery managers don't terminate background monitoring.

---

## 🛠️ Building the Project

### Prerequisites
- **Android Studio** (Koala / Ladybug or newer recommended)
- **JDK 17** or newer
- Android SDK with API 35 installed

### Open in Android Studio
1. Open Android Studio.
2. Select **Open** and select the `advol0` folder.
3. Allow Gradle to sync dependencies.
4. Connect an Android phone or launch an emulator, then click **Run (Shift + F10)**.

### Build via Command Line
```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testing Ad Detection
Unit tests are included in [`AdDetectionEngineTest.kt`](app/src/test/java/com/advol/app/core/AdDetectionEngineTest.kt) covering:
- Explicit ad URI detection (`spotify:ad:...`)
- Track title heuristic (`Advertisement`)
- Self-promotional Spotify ads (`Spotify` artist/title combinations)
- Protection against false positives for songs, podcast episodes (`spotify:episode:...`), and local files (`spotify:local:...`)
- Notification title & text matching

---

## 📄 License
This project is open-source and free to customize under the MIT License.
