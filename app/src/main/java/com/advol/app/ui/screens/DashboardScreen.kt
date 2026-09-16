package com.advol.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.advol.app.core.MuteStrategy
import com.advol.app.core.PlaybackState
import com.advol.app.ui.components.PermissionSetupCard
import com.advol.app.ui.components.StatusCard
import com.advol.app.ui.theme.AdOrange
import com.advol.app.ui.theme.DarkBackground
import com.advol.app.ui.theme.DarkSurface
import com.advol.app.ui.theme.DarkSurfaceVariant
import com.advol.app.ui.theme.SpotifyGreen
import com.advol.app.ui.theme.TextMuted
import com.advol.app.ui.theme.TextPrimary
import com.advol.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    isServiceEnabled: Boolean,
    playbackState: PlaybackState,
    totalAdsMuted: Int,
    totalTimeMutedSeconds: Long,
    onToggleService: (Boolean) -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    isVolumeLocked: Boolean = false,
    activeStrategy: MuteStrategy = MuteStrategy.NONE,
    isDuckWhenLocked: Boolean = true
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Title & Hero Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AdVol",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isServiceEnabled) "Active background muting" else "Muter paused",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isServiceEnabled) SpotifyGreen else TextMuted
                )
            }

            Switch(
                checked = isServiceEnabled,
                onCheckedChange = onToggleService,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = SpotifyGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }

        // Live Spotify Status Card
        StatusCard(
            playbackState = playbackState,
            isServiceEnabled = isServiceEnabled,
            activeStrategy = activeStrategy
        )

        // Android Auto / locked-volume notice
        if (isVolumeLocked) {
            CarModeBanner(isDuckWhenLocked = isDuckWhenLocked)
        }

        // Statistics Grid
        Text(
            text = "STATISTICS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatItem(
                title = "Ads Muted",
                value = "$totalAdsMuted",
                icon = Icons.Default.Block,
                modifier = Modifier.weight(1f)
            )

            val formattedTime = formatSeconds(totalTimeMutedSeconds)
            StatItem(
                title = "Quiet Time",
                value = formattedTime,
                icon = Icons.Default.HourglassEmpty,
                modifier = Modifier.weight(1f)
            )
        }

        // Configuration & Checklist
        Text(
            text = "SETUP CHECKLIST",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        PermissionSetupCard(
            onOpenGuide = onOpenGuide
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CarModeBanner(isDuckWhenLocked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdOrange.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .border(1.dp, AdOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = AdOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Android Auto / car mode detected",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDuckWhenLocked) {
                    "The car controls the volume, so Android ignores most phone volume changes. " +
                        "AdVol will fully mute ads where your Android version still allows it; " +
                        "otherwise it asks Spotify to lower its own volume (quieter, not silent)."
                } else {
                    "The car controls the volume, so Android ignores most phone volume changes. " +
                        "Enable \"Lower ads in car mode\" in Settings so AdVol can still quieten ads " +
                        "when a full mute is refused."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun formatSeconds(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
