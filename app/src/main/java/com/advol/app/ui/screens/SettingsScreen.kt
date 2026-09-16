package com.advol.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.advol.app.ui.theme.AdRed
import com.advol.app.ui.theme.DarkBackground
import com.advol.app.ui.theme.DarkSurface
import com.advol.app.ui.theme.DarkSurfaceVariant
import com.advol.app.ui.theme.SpotifyGreen
import com.advol.app.ui.theme.TextMuted
import com.advol.app.ui.theme.TextPrimary
import com.advol.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    muteLevelPercent: Int,
    isSmoothFade: Boolean,
    fadeDurationMs: Int,
    isDuckWhenLocked: Boolean,
    onDuckWhenLockedChanged: (Boolean) -> Unit,
    onMuteLevelChanged: (Int) -> Unit,
    onSmoothFadeChanged: (Boolean) -> Unit,
    onFadeDurationChanged: (Int) -> Unit,
    onResetStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        // Mute Level Section
        Text(
            text = "VOLUME REDUCTION",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target Volume During Ads",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (muteLevelPercent == 0) "Complete silence (0%)" else "Reduced to $muteLevelPercent%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = "$muteLevelPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        color = SpotifyGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = muteLevelPercent.toFloat(),
                    onValueChange = { onMuteLevelChanged(it.roundToInt()) },
                    valueRange = 0f..40f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyGreen,
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        // Smooth Transition Section
        Text(
            text = "AUDIO FADE TRANSITIONS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smooth Audio Fade",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Gently ramp volume up and down to prevent audio clicks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isSmoothFade,
                        onCheckedChange = onSmoothFadeChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = SpotifyGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                if (isSmoothFade) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fade Duration",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${fadeDurationMs}ms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpotifyGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = fadeDurationMs.toFloat(),
                        onValueChange = { onFadeDurationChanged(it.roundToInt()) },
                        valueRange = 100f..600f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = SpotifyGreen,
                            activeTrackColor = SpotifyGreen,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }

        // Car / Android Auto fallback
        Text(
            text = "ANDROID AUTO & CASTING",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lower ads in car mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "While Android Auto (or casting) is active the car owns the volume and Android " +
                                "ignores phone volume changes. AdVol always tries a real mute first; if the system " +
                                "refuses it, this option lets AdVol ask Spotify to duck its own output during " +
                                "ads instead. Ducked ads are quieter, not silent.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = isDuckWhenLocked,
                        onCheckedChange = onDuckWhenLockedChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = SpotifyGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }

        // Data & Statistics
        Text(
            text = "DATA MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AdRed)
        ) {
            Text("Reset Ad Statistics", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Statistics?") },
            text = { Text("This will reset the total ads muted count and quiet time counters to zero.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetStats()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = AdRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
