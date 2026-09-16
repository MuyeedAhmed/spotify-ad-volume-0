package com.advol.app.core

/**
 * Represents the current playback and metadata state captured from Spotify.
 */
data class PlaybackState(
    val trackId: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val trackLengthMs: Int = 0,
    val isPlaying: Boolean = false,
    val isAd: Boolean = false,
    val source: Source = Source.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Source {
        BROADCAST,
        NOTIFICATION,
        UNKNOWN
    }

    companion object {
        /**
         * Spotify's `metadatachanged` broadcast does not carry a `playing` extra; only
         * `playbackstatechanged` does. Falling back to the previous state's flag meant the first
         * ad after a process start (previous state = default, not playing) was treated as paused
         * and never muted.
         *
         * @param explicit the `playing` extra if the broadcast carried one.
         */
        fun resolveMetadataIsPlaying(explicit: Boolean?, previous: PlaybackState, isAd: Boolean): Boolean {
            if (explicit != null) return explicit
            return when {
                // Ad metadata is only announced when the ad actually starts playing.
                isAd -> true
                // No playback state seen yet in this process: metadata changes happen during playback.
                previous.source == Source.UNKNOWN -> true
                else -> previous.isPlaying
            }
        }
    }

    val displayTitle: String
        get() = when {
            isAd -> "Advertisement"
            !trackName.isNullOrBlank() -> trackName
            else -> "No active track"
        }

    val displaySubtitle: String
        get() = when {
            isAd -> "Muting in progress"
            !artistName.isNullOrBlank() -> artistName
            else -> "Waiting for Spotify..."
        }
}
