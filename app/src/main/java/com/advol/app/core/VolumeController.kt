package com.advol.app.core

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

/**
 * Controller managing device media volume, mute state preservation,
 * and smooth fade transitions during Spotify advertisements.
 */
class VolumeController(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutex = Mutex()

    private var savedVolume: Int? = null
    private var lastKnownMusicVolume: Int = (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.7f).roundToInt().coerceAtLeast(5)
    private var fadeJob: Job? = null

    private val _isMutedState = MutableStateFlow(false)
    val isMutedState: StateFlow<Boolean> = _isMutedState.asStateFlow()

    private val _currentVolume = MutableStateFlow(getCurrentVolume())
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    fun getMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    fun getMinVolume(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
    } else {
        0
    }

    fun getCurrentVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    /**
     * Mutes or lowers the media volume when an advertisement begins.
     * Preserves the original volume if not already muted.
     */
    fun mute(
        targetPercent: Int = 0,
        smoothFade: Boolean = true,
        fadeDurationMs: Int = 250,
        onMuted: (() -> Unit)? = null
    ) {
        scope.launch {
            mutex.withLock {
                fadeJob?.cancel()

                val max = getMaxVolume()
                val min = getMinVolume()
                val targetVol = (max * (targetPercent / 100f)).roundToInt().coerceIn(min, max)

                // Preserve original volume only on initial mute
                if (!_isMutedState.value) {
                    val current = getCurrentVolume()
                    // CRITICAL: Never save 0, 1, or targetVol as original volume!
                    // If current volume is too low, it's a mid-transition artifact, so use lastKnownMusicVolume.
                    savedVolume = if (current > targetVol + 1 && current > 2) {
                        current
                    } else if (lastKnownMusicVolume > 2) {
                        lastKnownMusicVolume
                    } else {
                        (max * 0.7f).roundToInt()
                    }
                    lastKnownMusicVolume = savedVolume!!
                    Log.d(TAG, "Preserving original volume: $savedVolume (current was: $current, lastKnown: $lastKnownMusicVolume)")
                }

                _isMutedState.value = true

                if (smoothFade && fadeDurationMs > 0) {
                    fadeJob = launch {
                        smoothTransition(from = getCurrentVolume(), to = targetVol, durationMs = fadeDurationMs)
                        onMuted?.invoke()
                    }
                } else {
                    setStreamVolume(targetVol)
                    onMuted?.invoke()
                }
            }
        }
    }

    /**
     * Restores media volume to the level prior to ad mute.
     */
    fun restore(
        smoothFade: Boolean = true,
        fadeDurationMs: Int = 200,
        onRestored: (() -> Unit)? = null
    ) {
        scope.launch {
            mutex.withLock {
                if (!_isMutedState.value) {
                    val current = getCurrentVolume()
                    if (current > 2) {
                        lastKnownMusicVolume = current
                    }
                    return@withLock
                }

                fadeJob?.cancel()

                val max = getMaxVolume()
                // CRITICAL: Ensure restored volume is a genuine listening volume, NEVER 1!
                val targetToRestore = when {
                    savedVolume != null && savedVolume!! > 2 -> savedVolume!!
                    lastKnownMusicVolume > 2 -> lastKnownMusicVolume
                    else -> (max * 0.7f).roundToInt()
                }
                Log.d(TAG, "Restoring volume to: $targetToRestore (savedVolume: $savedVolume, lastKnown: $lastKnownMusicVolume)")

                _isMutedState.value = false
                savedVolume = null
                lastKnownMusicVolume = targetToRestore

                if (smoothFade && fadeDurationMs > 0) {
                    fadeJob = launch {
                        smoothTransition(from = getCurrentVolume(), to = targetToRestore, durationMs = fadeDurationMs)
                        onRestored?.invoke()
                    }
                } else {
                    setStreamVolume(targetToRestore)
                    onRestored?.invoke()
                }
            }
        }
    }

    /**
     * Smoothly steps volume between start and target values.
     */
    private suspend fun smoothTransition(from: Int, to: Int, durationMs: Int) {
        if (from == to) {
            setStreamVolume(to)
            return
        }

        val stepCount = kotlin.math.abs(to - from).coerceAtLeast(1)
        val stepInterval = (durationMs / stepCount).coerceAtLeast(15).toLong()
        val stepDirection = if (to > from) 1 else -1

        var current = from
        try {
            while (current != to) {
                current += stepDirection
                setStreamVolume(current)
                delay(stepInterval)
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    private fun setStreamVolume(volume: Int) {
        try {
            val clamped = volume.coerceIn(getMinVolume(), getMaxVolume())
            // Flag 0 prevents the on-screen volume overlay HUD from appearing
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
            _currentVolume.value = clamped
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "VolumeController"
    }
}
