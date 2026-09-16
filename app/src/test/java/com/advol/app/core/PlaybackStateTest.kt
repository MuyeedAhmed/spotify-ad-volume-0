package com.advol.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {

    @Test
    fun explicitPlayingExtraAlwaysWins() {
        val paused = PlaybackState(isPlaying = false, source = PlaybackState.Source.BROADCAST)
        assertTrue(PlaybackState.resolveMetadataIsPlaying(explicit = true, previous = paused, isAd = false))
        val playing = PlaybackState(isPlaying = true, source = PlaybackState.Source.BROADCAST)
        assertFalse(PlaybackState.resolveMetadataIsPlaying(explicit = false, previous = playing, isAd = true))
    }

    @Test
    fun firstAdAfterProcessStartIsTreatedAsPlaying() {
        // Default state: nothing received yet in this process. This is the "first ad is not
        // muted" scenario: metadatachanged has no 'playing' extra and the old code inherited false.
        val initial = PlaybackState()
        assertTrue(
            "Ad metadata with no prior playback state must count as playing",
            PlaybackState.resolveMetadataIsPlaying(explicit = null, previous = initial, isAd = true)
        )
        assertTrue(
            "Song metadata with no prior playback state must count as playing",
            PlaybackState.resolveMetadataIsPlaying(explicit = null, previous = initial, isAd = false)
        )
    }

    @Test
    fun adMetadataWhilePreviouslyPausedIsTreatedAsPlaying() {
        val paused = PlaybackState(isPlaying = false, source = PlaybackState.Source.BROADCAST)
        assertTrue(PlaybackState.resolveMetadataIsPlaying(explicit = null, previous = paused, isAd = true))
    }

    @Test
    fun songMetadataInheritsKnownPausedState() {
        val paused = PlaybackState(isPlaying = false, source = PlaybackState.Source.BROADCAST)
        assertFalse(
            "Skipping tracks while paused must not be reported as playing",
            PlaybackState.resolveMetadataIsPlaying(explicit = null, previous = paused, isAd = false)
        )
        val playing = PlaybackState(isPlaying = true, source = PlaybackState.Source.BROADCAST)
        assertTrue(PlaybackState.resolveMetadataIsPlaying(explicit = null, previous = playing, isAd = false))
    }
}
