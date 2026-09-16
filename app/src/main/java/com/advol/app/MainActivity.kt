package com.advol.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.advol.app.core.MuteStrategy
import com.advol.app.service.AdVolService
import com.advol.app.ui.screens.DashboardScreen
import com.advol.app.ui.screens.SettingsScreen
import com.advol.app.ui.screens.SetupGuideScreen
import com.advol.app.ui.theme.AdVolTheme
import com.advol.app.ui.theme.DarkBackground
import com.advol.app.ui.theme.DarkSurface
import com.advol.app.ui.theme.SpotifyGreen
import com.advol.app.ui.theme.TextMuted
import com.advol.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission response handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            AdVolTheme {
                val app = application as AdVolApplication
                val scope = rememberCoroutineScope()

                val isServiceEnabled by app.preferencesManager.isServiceEnabled.collectAsState(initial = true)
                val muteLevelPercent by app.preferencesManager.muteLevelPercent.collectAsState(initial = 0)
                val isSmoothFade by app.preferencesManager.isSmoothFadeEnabled.collectAsState(initial = true)
                val fadeDurationMs by app.preferencesManager.fadeDurationMs.collectAsState(initial = 250)
                val totalAdsMuted by app.preferencesManager.totalAdsMuted.collectAsState(initial = 0)
                val totalTimeMutedSeconds by app.preferencesManager.totalTimeMutedSeconds.collectAsState(initial = 0L)
                val playbackState by app.currentPlaybackState.collectAsState()
                val isDuckWhenLocked by app.preferencesManager.isDuckWhenVolumeLockedEnabled.collectAsState(initial = true)
                val isVolumeLocked by app.volumeController.isVolumeLocked.collectAsState()
                val activeStrategy by app.volumeController.activeStrategy.collectAsState()

                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.DASHBOARD,
                                onClick = { currentTab = ScreenTab.DASHBOARD },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyGreen,
                                    selectedTextColor = SpotifyGreen,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = DarkBackground
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.SETTINGS,
                                onClick = { currentTab = ScreenTab.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyGreen,
                                    selectedTextColor = SpotifyGreen,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = DarkBackground
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.GUIDE,
                                onClick = { currentTab = ScreenTab.GUIDE },
                                icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Guide") },
                                label = { Text("Setup Guide") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyGreen,
                                    selectedTextColor = SpotifyGreen,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = DarkBackground
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        ScreenTab.DASHBOARD -> DashboardScreen(
                            isServiceEnabled = isServiceEnabled,
                            playbackState = playbackState,
                            totalAdsMuted = totalAdsMuted,
                            totalTimeMutedSeconds = totalTimeMutedSeconds,
                            isVolumeLocked = isVolumeLocked,
                            activeStrategy = activeStrategy,
                            isDuckWhenLocked = isDuckWhenLocked,
                            onToggleService = { enabled ->
                                scope.launch {
                                    app.preferencesManager.setServiceEnabled(enabled)
                                    if (enabled) {
                                        AdVolService.start(this@MainActivity)
                                    } else {
                                        app.volumeController.restore()
                                    }
                                }
                            },
                            onOpenGuide = { currentTab = ScreenTab.GUIDE },
                            modifier = Modifier.padding(innerPadding)
                        )

                        ScreenTab.SETTINGS -> SettingsScreen(
                            muteLevelPercent = muteLevelPercent,
                            isSmoothFade = isSmoothFade,
                            fadeDurationMs = fadeDurationMs,
                            isDuckWhenLocked = isDuckWhenLocked,
                            onDuckWhenLockedChanged = { scope.launch { app.preferencesManager.setDuckWhenVolumeLockedEnabled(it) } },
                            onMuteLevelChanged = { scope.launch { app.preferencesManager.setMuteLevelPercent(it) } },
                            onSmoothFadeChanged = { scope.launch { app.preferencesManager.setSmoothFadeEnabled(it) } },
                            onFadeDurationChanged = { scope.launch { app.preferencesManager.setFadeDurationMs(it) } },
                            onResetStats = { scope.launch { app.preferencesManager.resetStats() } },
                            modifier = Modifier.padding(innerPadding)
                        )

                        ScreenTab.GUIDE -> SetupGuideScreen(
                            onBack = { currentTab = ScreenTab.DASHBOARD },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }

        // Auto-start service if enabled
        AdVolService.start(this)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

enum class ScreenTab {
    DASHBOARD,
    SETTINGS,
    GUIDE
}
