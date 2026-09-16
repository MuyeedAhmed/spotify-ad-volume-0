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
