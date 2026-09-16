package com.advol.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.advol.app.core.PlaybackState
import com.advol.app.core.PreferencesManager
import com.advol.app.core.VolumeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdVolApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var volumeController: VolumeController
        private set

    private val _currentPlaybackState = MutableStateFlow(PlaybackState())
    val currentPlaybackState: StateFlow<PlaybackState> = _currentPlaybackState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        volumeController = VolumeController(this)

        createNotificationChannel()
    }

    fun updatePlaybackState(state: PlaybackState) {
        _currentPlaybackState.value = state
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "advol_monitoring_channel"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: AdVolApplication
            private set
    }
}
