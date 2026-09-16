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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AdVolService created")
        isRunning = true
        registerSpotifyReceiver()

        // Mirror the coordinator's status into the persistent notification. The coordinator
        // keeps working while this service is down, so this also catches up on restart.
        val app = application as? AdVolApplication
        if (app != null) {
            serviceScope.launch {
                app.adMuteCoordinator.statusText.collect { text ->
                    if (isForeground) updateNotification(text)
                }
            }
        }
    }

    private var isForeground = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_PAUSE -> {
                serviceScope.launch {
                    val app = application as AdVolApplication
                    app.preferencesManager.setServiceEnabled(false)
                    app.volumeController.restore()
                    app.adMuteCoordinator.setStatus("Monitoring paused")
                }
            }
            ACTION_RESUME -> {
                serviceScope.launch {
                    val app = application as AdVolApplication
                    app.preferencesManager.setServiceEnabled(true)
                    app.adMuteCoordinator.setStatus(getString(R.string.status_monitoring))
                }
            }
            ACTION_STOP -> {
                isForeground = false
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
        val status = (application as? AdVolApplication)?.adMuteCoordinator?.statusText?.value
            ?: getString(R.string.status_monitoring)
        val notification = buildNotification(status)
        isForeground = true
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
        isRunning = false
        isForeground = false
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

        /** True between onCreate and onDestroy of the live instance. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AdVolService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Starts the foreground service if it is not running and monitoring is enabled.
         * Safe to call from the notification listener / broadcast receiver: on Android 12+ a
         * background start may be refused, in which case we log and carry on (the coordinator
         * still mutes without the service as long as this process is alive).
         */
        fun ensureRunning(context: Context) {
            if (isRunning) return
            val app = context.applicationContext as? AdVolApplication ?: return
            app.appScope.launch {
                if (!app.preferencesManager.isServiceEnabled.first()) return@launch
                if (isRunning) return@launch
                try {
                    Log.d(TAG, "Service not running while Spotify is active; starting it")
                    start(app)
                } catch (e: Exception) {
                    // ForegroundServiceStartNotAllowedException (API 31+) is an IllegalStateException
                    Log.w(TAG, "Could not start foreground service from background: ${e.message}")
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdVolService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun onPlaybackStateReceived(context: Context, state: PlaybackState) {
            val app = context.applicationContext as? AdVolApplication ?: return
            // Decide and act immediately, whether or not the service is up...
            app.adMuteCoordinator.onPlaybackState(state)
            // ...and bring the service back so the dynamic receiver and notification return.
            ensureRunning(context)
        }
    }
}
