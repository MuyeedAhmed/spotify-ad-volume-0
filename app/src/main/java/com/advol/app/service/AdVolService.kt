package com.advol.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.advol.app.AdVolApplication
import com.advol.app.MainActivity
import com.advol.app.R
import com.advol.app.core.MuteStrategy
import com.advol.app.core.PlaybackState
import com.advol.app.receiver.SpotifyBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps AdVol active in the background, monitors Spotify
 * playback via dynamic broadcast receiver, and coordinates volume ducking/muting.
 */
class AdVolService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var broadcastReceiver: SpotifyBroadcastReceiver? = null
    private var adStartTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AdVolService created")
        registerSpotifyReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_PAUSE -> {
                serviceScope.launch {
                    val app = application as AdVolApplication
                    app.preferencesManager.setServiceEnabled(false)
                    app.volumeController.restore()
                    updateNotification("Monitoring paused")
                }
            }
            ACTION_RESUME -> {
                serviceScope.launch {
                    val app = application as AdVolApplication
                    app.preferencesManager.setServiceEnabled(true)
                    updateNotification("Monitoring Spotify...")
                }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startInForeground()
            }
        }

        return START_STICKY
    }

    private fun startInForeground() {
        val notification = buildNotification(getString(R.string.status_monitoring))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
            }
            startForeground(AdVolApplication.NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(AdVolApplication.NOTIFICATION_ID, notification)
        }
    }

    private fun registerSpotifyReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = SpotifyBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction(SpotifyBroadcastReceiver.ACTION_METADATA_CHANGED)
                addAction(SpotifyBroadcastReceiver.ACTION_PLAYBACK_STATE_CHANGED)
                addAction(SpotifyBroadcastReceiver.ACTION_QUEUE_CHANGED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(broadcastReceiver, filter)
            }
            Log.d(TAG, "Dynamic SpotifyBroadcastReceiver registered")
        }
    }

    private fun unregisterSpotifyReceiver() {
        broadcastReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d(TAG, "Dynamic SpotifyBroadcastReceiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver: ${e.message}")
            }
            broadcastReceiver = null
        }
    }

    private fun handlePlaybackUpdate(state: PlaybackState) {
        serviceScope.launch {
            val app = application as? AdVolApplication ?: return@launch
            val isEnabled = app.preferencesManager.isServiceEnabled.first()

            if (!isEnabled) {
                Log.d(TAG, "Service is disabled by user, skipping volume change")
                return@launch
            }

            val muteLevel = app.preferencesManager.muteLevelPercent.first()
            val smoothFade = app.preferencesManager.isSmoothFadeEnabled.first()
            val fadeDuration = app.preferencesManager.fadeDurationMs.first()
            val duckWhenLocked = app.preferencesManager.isDuckWhenVolumeLockedEnabled.first()

            if (state.isAd && state.isPlaying) {
                Log.d(TAG, "Action: Muting Spotify Advertisement")
                if (adStartTime == 0L) {
                    adStartTime = System.currentTimeMillis()
                }
                app.volumeController.mute(
                    targetPercent = muteLevel,
                    smoothFade = smoothFade,
                    fadeDurationMs = fadeDuration,
                    allowDucking = duckWhenLocked
                ) {
                    // Called once the controller has decided which mechanism works on the
                    // current route (phone volume vs. Android Auto ducking fallback).
                    val text = when (app.volumeController.activeStrategy.value) {
                        MuteStrategy.AUDIO_FOCUS_DUCK -> getString(R.string.status_ad_ducked)
                        MuteStrategy.UNAVAILABLE -> getString(R.string.status_ad_volume_locked)
                        else -> getString(R.string.status_ad_muted)
                    }
                    updateNotification(text)
                }
            } else {
                Log.d(TAG, "Action: Restoring volume for normal playback / pause")
                if (adStartTime > 0L) {
                    val durationSec = (System.currentTimeMillis() - adStartTime) / 1000
                    app.preferencesManager.incrementAdsMuted(durationSec)
                    adStartTime = 0L
                }
                app.volumeController.restore(
                    smoothFade = smoothFade,
                    fadeDurationMs = fadeDuration
                )

                val displayText = if (!state.trackName.isNullOrBlank()) {
                    "${state.trackName} - ${state.artistName.orEmpty()}"
                } else {
                    getString(R.string.status_monitoring)
                }
                updateNotification(displayText)
            }
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, AdVolService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pendingPause = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val resumeIntent = Intent(this, AdVolService::class.java).apply {
            action = ACTION_RESUME
        }
        val pendingResume = PendingIntent.getService(
            this,
            2,
            resumeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AdVolApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .addAction(R.drawable.ic_tile, getString(R.string.action_pause), pendingPause)
            .addAction(R.drawable.ic_tile, getString(R.string.action_resume), pendingResume)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(AdVolApplication.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSpotifyReceiver()
        serviceScope.cancel()
        Log.d(TAG, "AdVolService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AdVolService"
        const val ACTION_START = "com.advol.app.action.START"
        const val ACTION_STOP = "com.advol.app.action.STOP"
        const val ACTION_PAUSE = "com.advol.app.action.PAUSE"
        const val ACTION_RESUME = "com.advol.app.action.RESUME"

        private var activeServiceInstance: AdVolService? = null

        fun start(context: Context) {
            val intent = Intent(context, AdVolService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdVolService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun onPlaybackStateReceived(context: Context, state: PlaybackState) {
            activeServiceInstance?.handlePlaybackUpdate(state)
        }
    }

    init {
        activeServiceInstance = this
    }
}
