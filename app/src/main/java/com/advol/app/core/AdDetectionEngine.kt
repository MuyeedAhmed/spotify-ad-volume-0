package com.advol.app.core

/**
 * Pure engine responsible for evaluating Spotify metadata and determining
 * whether the current audio output is an advertisement.
 */
object AdDetectionEngine {

    private const val SCHEME_TRACK = "spotify:track:"
    private const val SCHEME_EPISODE = "spotify:episode:"
    private const val SCHEME_LOCAL = "spotify:local:"
    private const val SCHEME_AD = "spotify:ad:"

    private const val AD_KEYWORD = "advertisement"
    private const val SPOTIFY_KEYWORD = "spotify"

    /**
     * Determines whether the given broadcast metadata represents an advertisement.
     */
    fun isAdFromBroadcast(
        trackId: String?,
        trackName: String?,
        artistName: String?,
        albumName: String?,
        trackLengthMs: Int
    ): Boolean {
        val cleanId = trackId?.trim().orEmpty()
        val cleanTrack = trackName?.trim().orEmpty()
        val cleanArtist = artistName?.trim().orEmpty()

        // 1. Explicit ad URI scheme
        if (cleanId.startsWith(SCHEME_AD, ignoreCase = true) || cleanId.contains(":ad:", ignoreCase = true)) {
            return true
        }

        // 2. Safe check: Valid song/podcast/local schemes are NOT ads unless specifically matching ad keywords
        val isRecognizedTrackOrEpisode = cleanId.startsWith(SCHEME_TRACK, ignoreCase = true) ||
                cleanId.startsWith(SCHEME_EPISODE, ignoreCase = true) ||
                cleanId.startsWith(SCHEME_LOCAL, ignoreCase = true)

        // 3. Track name contains "Advertisement"
        if (cleanTrack.equals(AD_KEYWORD, ignoreCase = true) || cleanTrack.contains(AD_KEYWORD, ignoreCase = true)) {
            return true
        }

        // 4. "Spotify" as track name and artist name (common for Spotify promo ads)
        if (cleanTrack.equals(SPOTIFY_KEYWORD, ignoreCase = true) &&
            (cleanArtist.equals(SPOTIFY_KEYWORD, ignoreCase = true) || cleanArtist.isBlank())
        ) {
            return true
        }

        // 5. If not recognized as a track or episode and ID is atypical, check metadata
        if (!isRecognizedTrackOrEpisode && cleanId.isNotBlank()) {
            if (cleanTrack.equals(SPOTIFY_KEYWORD, ignoreCase = true) && cleanArtist.equals(SPOTIFY_KEYWORD, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    /**
     * Determines whether an active notification from Spotify represents an advertisement.
     */
    fun isAdFromNotification(
        title: String?,
        text: String?,
        subText: String?
    ): Boolean {
        val cleanTitle = title?.trim().orEmpty()
        val cleanText = text?.trim().orEmpty()

        // Ignore empty / loading notifications
        if (cleanTitle.isBlank() && cleanText.isBlank()) {
            return false
        }

        if (cleanTitle.equals(AD_KEYWORD, ignoreCase = true) || cleanTitle.contains(AD_KEYWORD, ignoreCase = true)) {
            return true
        }

        if (cleanText.equals(AD_KEYWORD, ignoreCase = true) || cleanText.contains(AD_KEYWORD, ignoreCase = true)) {
            return true
        }

        // Only classify as ad if BOTH title and text say "Spotify" (promo audio ad)
        if (cleanTitle.equals(SPOTIFY_KEYWORD, ignoreCase = true) &&
            cleanText.equals(SPOTIFY_KEYWORD, ignoreCase = true)
        ) {
            return true
        }

        return false
    }
}
