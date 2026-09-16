package com.advol.app.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "advol_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_MUTE_LEVEL_PERCENT = intPreferencesKey("mute_level_percent")
        val KEY_SMOOTH_FADE = booleanPreferencesKey("smooth_fade")
        val KEY_FADE_DURATION_MS = intPreferencesKey("fade_duration_ms")
        val KEY_TOTAL_ADS_MUTED = intPreferencesKey("total_ads_muted")
        val KEY_TOTAL_TIME_MUTED_SECONDS = longPreferencesKey("total_time_muted_sec")
        val KEY_DUCK_WHEN_VOLUME_LOCKED = booleanPreferencesKey("duck_when_volume_locked")

        const val DEFAULT_MUTE_LEVEL = 0 // 0% is full mute
        const val DEFAULT_FADE_DURATION = 250 // ms
    }

    val isServiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SERVICE_ENABLED] ?: true
    }

    val muteLevelPercent: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MUTE_LEVEL_PERCENT] ?: DEFAULT_MUTE_LEVEL
    }

    val isSmoothFadeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SMOOTH_FADE] ?: true
    }

    val fadeDurationMs: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_FADE_DURATION_MS] ?: DEFAULT_FADE_DURATION
    }

    /**
     * When the media volume index is locked by the platform (Android Auto, casting), fall back to
     * audio-focus ducking so the ad is at least lowered by the player itself.
     */
    val isDuckWhenVolumeLockedEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DUCK_WHEN_VOLUME_LOCKED] ?: true
    }

    val totalAdsMuted: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_ADS_MUTED] ?: 0
    }

    val totalTimeMutedSeconds: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_TIME_MUTED_SECONDS] ?: 0L
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setMuteLevelPercent(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MUTE_LEVEL_PERCENT] = percent.coerceIn(0, 50)
        }
    }

    suspend fun setSmoothFadeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SMOOTH_FADE] = enabled
        }
    }

    suspend fun setFadeDurationMs(durationMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FADE_DURATION_MS] = durationMs.coerceIn(50, 1000)
        }
    }

    suspend fun setDuckWhenVolumeLockedEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DUCK_WHEN_VOLUME_LOCKED] = enabled
        }
    }

    suspend fun incrementAdsMuted(seconds: Long = 0) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_TOTAL_ADS_MUTED] ?: 0
            preferences[KEY_TOTAL_ADS_MUTED] = current + 1
            if (seconds > 0) {
                val currentTime = preferences[KEY_TOTAL_TIME_MUTED_SECONDS] ?: 0L
                preferences[KEY_TOTAL_TIME_MUTED_SECONDS] = currentTime + seconds
            }
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOTAL_ADS_MUTED] = 0
            preferences[KEY_TOTAL_TIME_MUTED_SECONDS] = 0L
        }
    }
}
