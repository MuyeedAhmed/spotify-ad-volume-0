package com.advol.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MuteStrategyTest {

    @Test
    fun normalRouteUsesStreamVolume() {
        assertEquals(
            MuteStrategy.STREAM_VOLUME,
            MuteStrategy.resolve(routeLocked = false, writeAccepted = null, duckEnabled = true)
        )
        assertEquals(
            MuteStrategy.STREAM_VOLUME,
            MuteStrategy.resolve(routeLocked = false, writeAccepted = true, duckEnabled = false)
        )
    }

    @Test
    fun androidAutoRouteFallsBackToDucking() {
        assertEquals(
            "Android Auto remote submix route must switch to audio-focus ducking",
            MuteStrategy.AUDIO_FOCUS_DUCK,
            MuteStrategy.resolve(routeLocked = true, writeAccepted = null, duckEnabled = true)
        )
    }

    @Test
    fun rejectedVolumeWriteFallsBackToDucking() {
        assertEquals(
            "A volume write that did not take effect must switch to ducking even if the route looked normal",
            MuteStrategy.AUDIO_FOCUS_DUCK,
            MuteStrategy.resolve(routeLocked = false, writeAccepted = false, duckEnabled = true)
        )
    }

    @Test
    fun lockedRouteWithDuckingDisabledIsUnavailable() {
        assertEquals(
            MuteStrategy.UNAVAILABLE,
            MuteStrategy.resolve(routeLocked = true, writeAccepted = null, duckEnabled = false)
        )
        assertEquals(
            MuteStrategy.UNAVAILABLE,
            MuteStrategy.resolve(routeLocked = false, writeAccepted = false, duckEnabled = false)
        )
    }
}
