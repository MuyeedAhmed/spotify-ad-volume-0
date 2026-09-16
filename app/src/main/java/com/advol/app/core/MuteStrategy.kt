package com.advol.app.core

/**
 * How AdVol is (or should be) reducing the audibility of the current advertisement.
 */
enum class MuteStrategy {
    /** Nothing is being applied right now. */
    NONE,

    /** Classic behaviour: the phone's media stream index is lowered/muted. */
    STREAM_VOLUME,

    /**
     * Fallback used when the media volume index is locked by the platform (Android Auto,
     * screen casting, other projection routes). AdVol holds transient "may duck" audio focus
     * so the player itself lowers its output. Quieter, not silent.
     */
    AUDIO_FOCUS_DUCK,

    /** Volume is locked and the ducking fallback is disabled: nothing can be done. */
    UNAVAILABLE;

    companion object {
        /**
         * Pure decision function, unit-tested.
         *
         * @param routeLocked  the current media output route is known to ignore volume writes
         *                     (e.g. Android Auto's remote submix).
         * @param writeAccepted whether the most recent stream-volume write was actually applied
         *                      (null = no write attempted yet).
         * @param duckEnabled  user preference allowing the audio-focus fallback.
         */
        fun resolve(routeLocked: Boolean, writeAccepted: Boolean?, duckEnabled: Boolean): MuteStrategy {
            val locked = routeLocked || writeAccepted == false
            return when {
                !locked -> STREAM_VOLUME
                duckEnabled -> AUDIO_FOCUS_DUCK
                else -> UNAVAILABLE
            }
        }
    }
}
