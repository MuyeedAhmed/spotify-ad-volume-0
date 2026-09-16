package com.advol.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.advol.app.AdVolApplication
import com.advol.app.core.AdDetectionEngine
import com.advol.app.core.PlaybackState
import com.advol.app.service.AdVolService

class SpotifyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        Log.d(TAG, "Received broadcast action: $action")

        val app = context.applicationContext as? AdVolApplication
        val prevState = app?.currentPlaybackState?.value ?: PlaybackState()

        val state = when (action) {
            ACTION_METADATA_CHANGED -> {
                val trackId = intent.getStringExtra("id")
                val trackName = intent.getStringExtra("track")
                val artistName = intent.getStringExtra("artist")
                val albumName = intent.getStringExtra("album")
                val trackLength = intent.getIntExtra("length", 0)

                val isAd = AdDetectionEngine.isAdFromBroadcast(
                    trackId = trackId,
                    trackName = trackName,
                    artistName = artistName,
                    albumName = albumName,
                    trackLengthMs = trackLength
                )

                val explicitPlaying = if (intent.hasExtra("playing")) intent.getBooleanExtra("playing", false) else null
                val isPlaying = PlaybackState.resolveMetadataIsPlaying(explicitPlaying, prevState, isAd)

                Log.d(TAG, "METADATA_CHANGED -> isAd: $isAd, track: '$trackName', artist: '$artistName', id: '$trackId', isPlaying: $isPlaying")

                PlaybackState(
                    trackId = trackId,
                    trackName = trackName,
                    artistName = artistName,
                    albumName = albumName,
                    trackLengthMs = trackLength,
                    isPlaying = isPlaying,
                    isAd = isAd,
                    source = PlaybackState.Source.BROADCAST
                )
            }
            ACTION_PLAYBACK_STATE_CHANGED -> {
                val isPlaying = intent.getBooleanExtra("playing", false)
                Log.d(TAG, "PLAYBACK_STATE_CHANGED -> isPlaying: $isPlaying (retaining isAd=${prevState.isAd}, track='${prevState.trackName}')")

                // Retain current track info and ad status. Never reset isAd to false on playback state changes!
                prevState.copy(
                    isPlaying = isPlaying,
                    source = PlaybackState.Source.BROADCAST
                )
            }
            ACTION_QUEUE_CHANGED -> {
                // Queue changes do not indicate track transitions
                Log.d(TAG, "QUEUE_CHANGED ignored")
                return
            }
            else -> return
        }

        app?.updatePlaybackState(state)

        // Notify running foreground service
        AdVolService.onPlaybackStateReceived(context, state)
    }

    companion object {
        private const val TAG = "SpotifyBroadcastRcvr"
        const val ACTION_METADATA_CHANGED = "com.spotify.music.metadatachanged"
        const val ACTION_PLAYBACK_STATE_CHANGED = "com.spotify.music.playbackstatechanged"
        const val ACTION_QUEUE_CHANGED = "com.spotify.music.queuechanged"
    }
}
