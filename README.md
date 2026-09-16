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

## Android Auto (car mode)

While Android Auto is connected, Android routes media to the car and locks the phone's media volume at maximum (the slider jumps to 100% and stays there). Apps cannot change the volume the car hears: the system silently ignores `setStreamVolume()` and even `ADJUST_MUTE` for the media stream. There is no public API around this.

AdVol handles it like this:

1. It detects the locked route (remote-submix media output for `USAGE_MEDIA`, or a volume write that did not take effect when read back).
2. It still tries the real write first, then a plain full mute (index 0). Recent Android versions honour the full mute even in the car because it sets the stream's mute flag; older ones drop it.
3. If nothing took effect, it holds transient *may-duck* audio focus for the length of the ad, so Spotify lowers its own output, the same way it does for Google Maps prompts, and releases focus the moment the song resumes.

In the car, ads are therefore either **fully muted** (where the OS allows it) or **quieter, not silent** (ducking). The dashboard shows an "Android Auto / car mode detected" banner, the status card says "LOWERED (AD)" when ducking is in use, and the service notification says "Lowering ad volume". The ducking fallback can be turned off under **Settings → Android Auto & Casting**. Ad detection (Device Broadcast Status / Notification Access) works the same as on the phone.

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
