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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

/**
 * Controller managing device media volume, mute state preservation,
 * and smooth fade transitions during Spotify advertisements.
 *
 * Strategies (see [MuteStrategy]):
 *  - STREAM_VOLUME: lower the phone's media volume index. Every write is verified by reading
 *    the index back, because on some routes AudioService drops the write silently.
 *  - AUDIO_FOCUS_DUCK: fallback when the write did not take effect. This is the Android Auto
 *    case: while projecting, media goes through a remote submix that Android Auto marks as a
 *    full-volume device, so index changes are ignored and the car owns the volume. Holding
 *    transient "may duck" audio focus makes Spotify lower its own output instead.
 *
 * On Android Auto the exact behaviour depends on the OS version: recent releases still honour a
 * write of index 0 (it flips the stream mute flag, which is applied to the car), older ones
 * ignore it. The controller therefore always tries the real write first, including a plain
 * full mute when the requested level is rejected, and ducks only if nothing took effect.
 */
class VolumeController(
    context: Context,
    private val routeMonitor: AudioRouteMonitor,
    private val ducker: AudioFocusDucker,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutex = Mutex()

    private var savedVolume: Int? = null
    private var lastKnownMusicVolume: Int = (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.7f).roundToInt().coerceAtLeast(5)
    private var fadeJob: Job? = null
    private var verifyJob: Job? = null

    private val _isMutedState = MutableStateFlow(false)
    /** True while AdVol is actively suppressing an ad by any strategy. */
    val isMutedState: StateFlow<Boolean> = _isMutedState.asStateFlow()

    private val _activeStrategy = MutableStateFlow(MuteStrategy.NONE)
    /** Which mechanism is currently applied (or [MuteStrategy.UNAVAILABLE] if none could be). */
    val activeStrategy: StateFlow<MuteStrategy> = _activeStrategy.asStateFlow()

    private val _currentVolume = MutableStateFlow(getCurrentVolume())
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    /** Set when the platform ignored our most recent stream-volume write. */
    private val _lastWriteRejected = MutableStateFlow(false)

    /**
     * True while the media route ignores volume index changes: either the route monitor
     * recognises it (Android Auto remote submix, fixed-volume outputs) or a write was rejected.
     */
    val isVolumeLocked: StateFlow<Boolean> =
        combine(routeMonitor.isVolumeLocked, _lastWriteRejected) { route, rejected -> route || rejected }
            .stateIn(scope, SharingStarted.Eagerly, routeMonitor.isVolumeLocked.value)

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
     *
     * @param allowDucking whether the audio-focus fallback may be used when the volume index is
     *                     locked (Android Auto). [onApplied] is always invoked once the decision
     *                     has been made and the first step applied; read [activeStrategy] there.
     */
    fun mute(
        targetPercent: Int = 0,
        smoothFade: Boolean = true,
        fadeDurationMs: Int = 250,
        allowDucking: Boolean = true,
        onApplied: (() -> Unit)? = null
    ) {
        scope.launch {
            mutex.withLock {
                fadeJob?.cancel()
                verifyJob?.cancel()

                val max = getMaxVolume()
                val min = getMinVolume()
                val targetVol = (max * (targetPercent / 100f)).roundToInt().coerceIn(min, max)

                val routeLocked = routeMonitor.isVolumeLockedNow()
                val alreadyMutedByStream = _isMutedState.value && _activeStrategy.value == MuteStrategy.STREAM_VOLUME
                val current = getCurrentVolume()

                // Preserve original volume only on initial mute
                if (!alreadyMutedByStream) {
                    savedVolume = when {
                        // On a locked route getStreamVolume() reports a fake max: keep the last
                        // real phone volume instead so a later restore off the car is sane.
                        routeLocked -> if (lastKnownMusicVolume > 2) lastKnownMusicVolume else (max * 0.7f).roundToInt()
                        // CRITICAL: Never save 0, 1, or targetVol as original volume!
                        // If current volume is too low, it's a mid-transition artifact, so use lastKnownMusicVolume.
                        current > targetVol + 1 && current > 2 -> current
                        lastKnownMusicVolume > 2 -> lastKnownMusicVolume
                        else -> (max * 0.7f).roundToInt()
                    }
                    lastKnownMusicVolume = savedVolume!!
                    Log.d(TAG, "Preserving original volume: $savedVolume (current was: $current, lastKnown: $lastKnownMusicVolume, routeLocked: $routeLocked)")
                }

                // --- Attempt 1: the real stream write, verified by read-back. -------------------
                // No fade on a locked route: intermediate steps would all be dropped.
                val useFade = smoothFade && fadeDurationMs > 0 && !routeLocked
                var appliedVol = if (useFade) stepToward(current, targetVol) else targetVol
                var accepted = setStreamVolume(appliedVol)

                // --- Attempt 2: a plain full mute. Locked routes accept index 0 on recent
                // Android versions (it sets the stream mute flag) even though other indexes are
                // ignored, so this still gives silence in the car where the OS allows it.
                if (!accepted && targetVol != min) {
                    Log.w(TAG, "Level $targetVol rejected by the platform, trying a full mute")
                    appliedVol = min
                    accepted = setStreamVolume(min)
                }

                if (accepted) {
                    ducker.stop() // in case a route change left the fallback active
                    _isMutedState.value = true
                    _activeStrategy.value = MuteStrategy.STREAM_VOLUME

                    if (useFade && appliedVol != targetVol) {
                        fadeJob = launch {
                            smoothTransition(from = appliedVol, to = targetVol, durationMs = fadeDurationMs)
                        }
                    }
                    if (routeLocked && allowDucking) {
                        // The projection app may undo our mute a moment later; re-check and
                        // add ducking on top if the stream is audible again.
                        verifyJob = launch { verifyMuteStillApplied(appliedVol) }
                    }
                    onApplied?.invoke()
                    return@withLock
                }

                // --- Attempt 3: audio-focus ducking. ------------------------------------------
                Log.w(TAG, "Stream volume writes are ignored on this route (Android Auto / casting). Falling back.")
                if (!alreadyMutedByStream) savedVolume = null
                val strategy = MuteStrategy.resolve(
                    routeLocked = true,
                    writeAccepted = false,
                    duckEnabled = allowDucking
                )
                when (strategy) {
                    MuteStrategy.AUDIO_FOCUS_DUCK -> {
                        val granted = ducker.start()
                        _isMutedState.value = granted
                        _activeStrategy.value = if (granted) MuteStrategy.AUDIO_FOCUS_DUCK else MuteStrategy.UNAVAILABLE
                        Log.d(TAG, "Ducking fallback ${if (granted) "active" else "NOT granted"}")
                    }
                    else -> {
                        _isMutedState.value = false
                        _activeStrategy.value = MuteStrategy.UNAVAILABLE
                        Log.w(TAG, "Volume is locked and ducking is disabled: cannot suppress this ad")
                    }
                }
                onApplied?.invoke()
            }
        }
    }

    /**
     * Restores media volume to the level prior to ad mute (or releases the ducking focus).
     */
    fun restore(
        smoothFade: Boolean = true,
        fadeDurationMs: Int = 200,
        onRestored: (() -> Unit)? = null
    ) {
        scope.launch {
            mutex.withLock {
                verifyJob?.cancel()
                val previousStrategy = _activeStrategy.value
                val routeLocked = routeMonitor.isVolumeLocked.value

                if (!_isMutedState.value) {
                    // Keep lastKnownMusicVolume fresh, but never learn the fake "max" reported
                    // while the route is locked.
                    val current = getCurrentVolume()
                    if (current > 2 && !routeLocked) {
                        lastKnownMusicVolume = current
                    }
                    _activeStrategy.value = MuteStrategy.NONE
                    return@withLock
                }

                fadeJob?.cancel()
                _isMutedState.value = false
                _activeStrategy.value = MuteStrategy.NONE

                // Always release ducking focus; it is a no-op when not held.
                ducker.stop()
                if (previousStrategy == MuteStrategy.AUDIO_FOCUS_DUCK) {
                    savedVolume = null
                    Log.d(TAG, "Released ducking focus")
                    onRestored?.invoke()
                    return@withLock
                }

                val max = getMaxVolume()
                // CRITICAL: Ensure restored volume is a genuine listening volume, NEVER 1!
                val targetToRestore = when {
                    savedVolume != null && savedVolume!! > 2 -> savedVolume!!
                    lastKnownMusicVolume > 2 -> lastKnownMusicVolume
                    else -> (max * 0.7f).roundToInt()
                }
                Log.d(TAG, "Restoring volume to: $targetToRestore (savedVolume: $savedVolume, lastKnown: $lastKnownMusicVolume, routeLocked: $routeLocked)")

                savedVolume = null
                lastKnownMusicVolume = targetToRestore

                if (smoothFade && fadeDurationMs > 0 && !routeLocked) {
                    fadeJob = launch {
                        smoothTransition(from = getCurrentVolume(), to = targetToRestore, durationMs = fadeDurationMs)
                        onRestored?.invoke()
                    }
                } else {
                    // On a locked route any non-zero index clears the mute flag; the index itself
                    // is ignored there, so a single write is all that is needed.
                    setStreamVolume(targetToRestore)
                    onRestored?.invoke()
                }
            }
        }
    }

    private suspend fun verifyMuteStillApplied(expectedVol: Int) {
        delay(VERIFY_DELAY_MS)
        mutex.withLock {
            if (!_isMutedState.value || _activeStrategy.value != MuteStrategy.STREAM_VOLUME) return@withLock
            val actual = getCurrentVolume()
            if (actual != expectedVol) {
                Log.w(TAG, "Mute was undone by the platform (stream at $actual, expected $expectedVol); adding ducking")
                _lastWriteRejected.value = true
                if (ducker.start()) {
                    _activeStrategy.value = MuteStrategy.AUDIO_FOCUS_DUCK
                }
            }
        }
    }

    private fun stepToward(from: Int, to: Int): Int = when {
        to > from -> from + 1
        to < from -> from - 1
        else -> to
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
                if (!setStreamVolume(current)) {
                    Log.w(TAG, "Volume write rejected mid-fade; stopping fade")
                    return
                }
                delay(stepInterval)
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    /**
     * Sets the media volume index and returns whether the platform actually applied it.
     * On locked routes (Android Auto's remote submix, fixed-volume outputs) AudioService drops
     * the write silently and keeps reporting max, which is how we detect the lock.
     */
    private fun setStreamVolume(volume: Int): Boolean {
        return try {
            val clamped = volume.coerceIn(getMinVolume(), getMaxVolume())
            // Flag 0 prevents the on-screen volume overlay HUD from appearing
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
            val actual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            _currentVolume.value = actual
            val accepted = actual == clamped
            _lastWriteRejected.value = !accepted
            if (!accepted) {
                Log.w(TAG, "setStreamVolume($clamped) ignored: stream still at $actual")
            }
            accepted
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume: ${e.message}")
            _lastWriteRejected.value = true
            false
        }
    }

    companion object {
        private const val TAG = "VolumeController"
        private const val VERIFY_DELAY_MS = 1200L
    }
}
