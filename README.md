# Spotify-Ad-Volume-0

**AdVol** is an Android background app designed to automatically mute your media volume whenever **Spotify** plays advertisements, and restore media volume the instant songs resume.

Built with **Android (Kotlin & Jetpack Compose)**.

---

## Setup & Installation

### 1. One-Time Spotify Setup
For ad detection without battery impact:
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

## Building the Project

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
