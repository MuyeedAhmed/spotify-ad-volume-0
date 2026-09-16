package com.advol.app.core

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches where media audio is currently routed and whether the platform will honour
 * changes to the STREAM_MUSIC volume index.
 *
 * Background: when Android Auto (and screen-cast style projection) is active, the phone's media
 * output is captured through a "remote submix" device. The projection app forces that device
 * into AudioService's full/fixed-volume set, after which every setStreamVolume()/ADJUST_MUTE
 * call for STREAM_MUSIC is silently discarded and getStreamVolume() always reports max.
 * The car head unit owns the volume. AdVol therefore needs to know about that route so it can
 * switch to the audio-focus ducking fallback instead of doing nothing.
 */
class AudioRouteMonitor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isVolumeLocked = MutableStateFlow(false)
    /** True while the media route is one that ignores volume index changes. */
    val isVolumeLocked: StateFlow<Boolean> = _isVolumeLocked.asStateFlow()

    private val _isCarMode = MutableStateFlow(false)
    /** True while the phone reports the car UI mode (Android Auto projection). */
    val isCarMode: StateFlow<Boolean> = _isCarMode.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
    }

    private val carModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    private var started = false

    fun start() {
        if (started) return
        started = true
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        val filter = IntentFilter().apply {
            addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
            addAction(UiModeManager.ACTION_EXIT_CAR_MODE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Protected system broadcasts are delivered to non-exported receivers too.
            context.registerReceiver(carModeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(carModeReceiver, filter)
        }
        refresh()
    }

    fun stop() {
        if (!started) return
        started = false
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        try {
            context.unregisterReceiver(carModeReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    /** Re-evaluates the route synchronously and updates the flows. */
    fun refresh() {
        val carMode = computeCarMode()
        val locked = computeVolumeLocked()
        if (locked != _isVolumeLocked.value || carMode != _isCarMode.value) {
            Log.d(TAG, "Route changed -> volumeLocked=$locked, carMode=$carMode, mediaDevices=${describeMediaDevices()}")
        }
        _isCarMode.value = carMode
        _isVolumeLocked.value = locked
    }

    /**
     * Synchronous check used right before a mute so a route change that happened a moment
     * ago is not missed.
     */
    fun isVolumeLockedNow(): Boolean {
        refresh()
        return _isVolumeLocked.value
    }

    private fun computeCarMode(): Boolean {
        val byService = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR
        val byConfig = (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_CAR
        return byService || byConfig
    }

    private fun computeVolumeLocked(): Boolean {
        if (audioManager.isVolumeFixed) return true

        val mediaDevices = mediaOutputDevices()
        if (mediaDevices.isNotEmpty()) {
            // Precise answer (API 33+): the devices that USAGE_MEDIA is actually routed to.
            return mediaDevices.any { it.type in LOCKED_DEVICE_TYPES }
        }

        // Older devices: we cannot ask for the media route directly. A connected remote submix
        // output only exists while something is capturing media (projection/cast). Car mode on
        // its own is not enough (driving-mode apps enable it without projecting), so it is only
        // reported via [isCarMode]; a volume write that does not take effect is the safety net
        // (see VolumeController).
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.any { it.type == AudioDeviceInfo.TYPE_REMOTE_SUBMIX }
    }

    private fun mediaOutputDevices(): List<AudioDeviceInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return emptyList()
        return try {
            audioManager.getAudioDevicesForAttributes(MEDIA_ATTRIBUTES)
        } catch (e: Exception) {
            Log.w(TAG, "getAudioDevicesForAttributes failed: ${e.message}")
            emptyList()
        }
    }

    private fun describeMediaDevices(): String =
        mediaOutputDevices().joinToString { "${it.productName}(type=${it.type})" }

    companion object {
        private const val TAG = "AudioRouteMonitor"

        private val MEDIA_ATTRIBUTES = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        /** Output device types whose STREAM_MUSIC index is ignored by AudioService. */
        private val LOCKED_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX, // Android Auto / casting capture
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC,
            AudioDeviceInfo.TYPE_AUX_LINE,
            AudioDeviceInfo.TYPE_DOCK
        )
    }
}
