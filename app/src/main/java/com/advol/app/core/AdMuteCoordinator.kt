package com.advol.app.core

import android.util.Log
import com.advol.app.AdVolApplication
import com.advol.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Decides what to do with every playback update (mute, duck, restore, count statistics).
 *
 * This lives at application level on purpose: detection can arrive from the notification
 * listener or the broadcast receiver while the foreground service is not (yet) running,
 * e.g. right after the system re-created the process to bind the listener, or after a reboot.
 * Previously the decision only happened inside a live service instance, which is one reason
 * the first ad after such a restart went unmuted.
 */
class AdMuteCoordinator(private val app: AdVolApplication) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var adStartTime: Long = 0L

    private val _statusText = MutableStateFlow(app.getString(R.string.status_monitoring))
    /** Human-readable status for the foreground notification and UI. */
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun setStatus(text: String) {
        _statusText.value = text
    }

    fun onPlaybackState(state: PlaybackState) {
        scope.launch {
            val prefs = app.preferencesManager
            val isEnabled = prefs.isServiceEnabled.first()
            if (!isEnabled) {
                Log.d(TAG, "Muting disabled by user, skipping volume change")
                return@launch
            }

            val muteLevel = prefs.muteLevelPercent.first()
            val smoothFade = prefs.isSmoothFadeEnabled.first()
            val fadeDuration = prefs.fadeDurationMs.first()
            val duckWhenLocked = prefs.isDuckWhenVolumeLockedEnabled.first()

            if (state.isAd && state.isPlaying) {
                Log.d(TAG, "Action: Muting Spotify Advertisement (source=${state.source})")
                if (adStartTime == 0L) {
                    adStartTime = System.currentTimeMillis()
                }
                app.volumeController.mute(
                    targetPercent = muteLevel,
                    smoothFade = smoothFade,
                    fadeDurationMs = fadeDuration,
                    allowDucking = duckWhenLocked
                ) {
                    // Called once the controller knows which mechanism works on this route.
                    _statusText.value = when (app.volumeController.activeStrategy.value) {
                        MuteStrategy.AUDIO_FOCUS_DUCK -> app.getString(R.string.status_ad_ducked)
                        MuteStrategy.UNAVAILABLE -> app.getString(R.string.status_ad_volume_locked)
                        else -> app.getString(R.string.status_ad_muted)
                    }
                }
            } else {
                Log.d(TAG, "Action: Restoring volume for normal playback / pause")
                if (adStartTime > 0L) {
                    val durationSec = (System.currentTimeMillis() - adStartTime) / 1000
                    prefs.incrementAdsMuted(durationSec)
                    adStartTime = 0L
                }
                app.volumeController.restore(
                    smoothFade = smoothFade,
                    fadeDurationMs = fadeDuration
                )

                _statusText.value = if (!state.trackName.isNullOrBlank()) {
                    "${state.trackName} - ${state.artistName.orEmpty()}"
                } else {
                    app.getString(R.string.status_monitoring)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AdMuteCoordinator"
    }
}
