package com.advol.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.advol.app.AdVolApplication
import com.advol.app.core.AdDetectionEngine
import com.advol.app.core.PlaybackState

/**
 * Secondary / fallback detection engine using Android's NotificationListenerService.
 * Catches Spotify ads even when the user hasn't turned on "Device Broadcast Status" in Spotify settings.
 */
class AdVolNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName != SPOTIFY_PACKAGE) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return
        if (title.equals("Spotify", ignoreCase = true) && text.isNullOrBlank()) return

        Log.d(TAG, "Spotify notification posted: title='$title', text='$text', subText='$subText'")

        val app = applicationContext as? AdVolApplication
        val currentState = app?.currentPlaybackState?.value

        // If broadcast receiver recently confirmed a valid song playing, do not let a lagging notification trigger an ad mute
        if (currentState != null &&
            currentState.source == PlaybackState.Source.BROADCAST &&
            !currentState.isAd &&
            currentState.isPlaying &&
            System.currentTimeMillis() - currentState.timestamp < 2000L
        ) {
            Log.d(TAG, "Ignoring notification update because broadcast recently confirmed song: '${currentState.trackName}'")
            return
        }

        val isAd = AdDetectionEngine.isAdFromNotification(title, text, subText)

        val state = PlaybackState(
            trackName = title,
            artistName = text,
            albumName = subText,
            isPlaying = true,
            isAd = isAd,
            source = PlaybackState.Source.NOTIFICATION
        )

        app?.updatePlaybackState(state)

        AdVolService.onPlaybackStateReceived(applicationContext, state)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName != SPOTIFY_PACKAGE) return
        Log.d(TAG, "Spotify notification removed. Playback stopped.")

        // When Spotify notification is removed, playback has halted
        val state = PlaybackState(
            isPlaying = false,
            isAd = false,
            source = PlaybackState.Source.NOTIFICATION
        )
        val app = applicationContext as? AdVolApplication
        app?.updatePlaybackState(state)
        AdVolService.onPlaybackStateReceived(applicationContext, state)
    }

    companion object {
        private const val TAG = "AdVolNotifListener"
        const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
}
