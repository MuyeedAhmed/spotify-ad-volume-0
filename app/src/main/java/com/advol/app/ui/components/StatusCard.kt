package com.advol.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.advol.app.core.PlaybackState
import com.advol.app.ui.theme.AdOrange
import com.advol.app.ui.theme.AdRed
import com.advol.app.ui.theme.DarkSurface
import com.advol.app.ui.theme.DarkSurfaceVariant
import com.advol.app.ui.theme.SpotifyGreen
import com.advol.app.ui.theme.TextMuted
import com.advol.app.ui.theme.TextPrimary
import com.advol.app.ui.theme.TextSecondary

@Composable
fun StatusCard(
    playbackState: PlaybackState,
    isServiceEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isAd = playbackState.isAd && playbackState.isPlaying && isServiceEnabled

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            !isServiceEnabled -> Color.Transparent
            isAd -> AdRed.copy(alpha = 0.8f)
            playbackState.isPlaying -> SpotifyGreen.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "cardBorderColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, cardBorderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE SPOTIFY STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.5.sp
                )

                // State Badge Pill
                val badgeColor = when {
                    !isServiceEnabled -> DarkSurfaceVariant
                    isAd -> AdOrange
                    playbackState.isPlaying -> SpotifyGreen
                    else -> DarkSurfaceVariant
                }

                val badgeText = when {
                    !isServiceEnabled -> "PAUSED"
                    isAd -> "MUTED (AD)"
                    playbackState.isPlaying -> "PLAYING"
                    else -> "IDLE"
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(if (isAd) pulseAlpha else 1f)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Info Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon avatar
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isAd) AdRed.copy(alpha = 0.2f) else DarkSurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isAd -> Icons.Default.VolumeOff
                            playbackState.isPlaying -> Icons.Default.VolumeUp
                            else -> Icons.Default.MusicNote
                        },
                        contentDescription = null,
                        tint = if (isAd) AdRed else if (playbackState.isPlaying) SpotifyGreen else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isAd -> "Spotify Advertisement"
                            !playbackState.trackName.isNullOrBlank() -> playbackState.trackName
                            else -> "No track detected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            isAd -> "Volume is automatically reduced"
                            !playbackState.artistName.isNullOrBlank() -> playbackState.artistName
                            else -> "Start Spotify to begin monitoring"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAd) AdOrange else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
