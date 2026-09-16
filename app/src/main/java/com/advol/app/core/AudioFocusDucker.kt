package com.advol.app.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Fallback "mute" for routes where the phone's volume index is locked (Android Auto).
 *
 * Holding transient MAY_DUCK audio focus makes the playing app lower its own output level
 * (Spotify does this for navigation prompts), and for apps that ignore the callback the
 * framework ducks the player automatically. Because the attenuation happens inside the
 * player, before the audio is captured for the car, it survives the volume lock.
 *
 * Trade-off: ducking is a reduction (roughly -14 dB), not silence, and it cannot be made
 * stronger from outside the player.
 */
class AudioFocusDucker(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var request: AudioFocusRequest? = null

    val isActive: Boolean
        get() = request != null

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        Log.d(TAG, "Focus change while ducking: $change")
        if (change == AudioManager.AUDIOFOCUS_LOSS) {
            // Someone else took focus permanently (e.g. a phone call); our request is void.
            request = null
        }
    }

    /** Starts ducking. Returns true if focus was granted (or already held). */
    fun start(): Boolean {
        if (request != null) return true

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setWillPauseWhenDucked(false)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(listener, mainHandler)
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)
        return if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            request = focusRequest
            Log.d(TAG, "Ducking focus granted")
            true
        } else {
            Log.w(TAG, "Ducking focus request failed: result=$result")
            false
        }
    }

    /** Releases focus so the player restores its normal level. */
    fun stop() {
        val current = request ?: return
        request = null
        val result = audioManager.abandonAudioFocusRequest(current)
        Log.d(TAG, "Ducking focus abandoned: result=$result")
    }

    companion object {
        private const val TAG = "AudioFocusDucker"
    }
}
