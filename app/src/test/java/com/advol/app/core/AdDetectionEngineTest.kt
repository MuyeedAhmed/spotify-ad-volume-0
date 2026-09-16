package com.advol.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdDetectionEngineTest {

    @Test
    fun testExplicitAdUriIsDetected() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = "spotify:ad:0000000000000000000000",
            trackName = "Some Brand Offer",
            artistName = "Advertiser",
            albumName = "Promo",
            trackLengthMs = 30000
        )
        assertTrue("Ad with spotify:ad URI should be detected as ad", isAd)
    }

    @Test
    fun testTrackTitleAdvertisementIsDetected() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = null,
            trackName = "Advertisement",
            artistName = "Spotify",
            albumName = null,
            trackLengthMs = 15000
        )
        assertTrue("Track titled 'Advertisement' should be detected as ad", isAd)
    }

    @Test
    fun testCaseInsensitiveAdvertisementIsDetected() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = "spotify:track:sample",
            trackName = "ADVERTISEMENT - Discount Deal",
            artistName = "Promo Brand",
            albumName = "",
            trackLengthMs = 30000
        )
        assertTrue("Track containing case-insensitive ADVERTISEMENT should be detected", isAd)
    }

    @Test
    fun testSpotifySelfPromoIsDetected() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = null,
            trackName = "Spotify",
            artistName = "Spotify",
            albumName = null,
            trackLengthMs = 20000
        )
        assertTrue("Spotify promo track should be detected as ad", isAd)
    }

    @Test
    fun testNormalSongIsNotDetectedAsAd() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = "spotify:track:4cOdK2wGLETKBW3PvgPWqT",
            trackName = "Bohemian Rhapsody",
            artistName = "Queen",
            albumName = "A Night at the Opera",
            trackLengthMs = 354000
        )
        assertFalse("Real song must not be detected as ad", isAd)
    }

    @Test
    fun testPodcastEpisodeIsNotDetectedAsAd() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = "spotify:episode:7makk4oTQel546v09Zl9pX",
            trackName = "Deep Dive on Tech",
            artistName = "The Tech Show",
            albumName = "Season 3",
            trackLengthMs = 1800000
        )
        assertFalse("Podcast episode must not be detected as ad", isAd)
    }

    @Test
    fun testLocalFileIsNotDetectedAsAd() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = "spotify:local:Artist:Album:Song:200",
            trackName = "My Recorded Track",
            artistName = "My Band",
            albumName = "Demo",
            trackLengthMs = 200000
        )
        assertFalse("Local file must not be detected as ad", isAd)
    }

    @Test
    fun testNotificationWithAdvertisementIsDetected() {
        val isAdTitle = AdDetectionEngine.isAdFromNotification(
            title = "Advertisement",
            text = "Listen without interruptions",
            subText = null
        )
        assertTrue("Notification titled 'Advertisement' must be detected as ad", isAdTitle)

        val isAdText = AdDetectionEngine.isAdFromNotification(
            title = "Brand Sponsor",
            text = "Advertisement",
            subText = null
        )
        assertTrue("Notification with text 'Advertisement' must be detected as ad", isAdText)
    }

    @Test
    fun testNormalNotificationIsNotDetectedAsAd() {
        val isAd = AdDetectionEngine.isAdFromNotification(
            title = "Starboy",
            text = "The Weeknd, Daft Punk",
            subText = "Starboy"
        )
        assertFalse("Normal song notification must not be detected as ad", isAd)
    }

    @Test
    fun testBlankOrLoadingNotificationIsNotDetectedAsAd() {
        val isAdBlank = AdDetectionEngine.isAdFromNotification(
            title = "",
            text = "",
            subText = null
        )
        assertFalse("Empty notification must not be detected as ad", isAdBlank)

        val isAdSpotifyTitleOnly = AdDetectionEngine.isAdFromNotification(
            title = "Spotify",
            text = "",
            subText = null
        )
        assertFalse("Notification with just Spotify title during track load must not be detected as ad", isAdSpotifyTitleOnly)
    }

    @Test
    fun testEmptyPlaybackStateChangeBroadcastIsNotDetectedAsAd() {
        val isAd = AdDetectionEngine.isAdFromBroadcast(
            trackId = null,
            trackName = null,
            artistName = null,
            albumName = null,
            trackLengthMs = 0
        )
        assertFalse("Empty broadcast from playback state change must not be detected as ad", isAd)
    }
}
