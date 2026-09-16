package com.advol.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.advol.app.ui.theme.DarkBackground
import com.advol.app.ui.theme.DarkSurface
import com.advol.app.ui.theme.DarkSurfaceVariant
import com.advol.app.ui.theme.SpotifyGreen
import com.advol.app.ui.theme.TextMuted
import com.advol.app.ui.theme.TextPrimary
import com.advol.app.ui.theme.TextSecondary

@Composable
fun SetupGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Spotify Setup Guide",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "To allow AdVol to detect ads instantaneously without battery drain, enable 'Device Broadcast Status' inside Spotify.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        // Step 1
        StepCard(
            stepNumber = "1",
            title = "Open Spotify Settings",
            description = "Launch Spotify and tap on your Profile picture / Settings gear in the top corner."
        )

        // Step 2
        StepCard(
            stepNumber = "2",
            title = "Find 'Device Broadcast Status'",
            description = "Scroll down through the settings list until you reach the 'Privacy and Social' or 'Social' section."
        )

        // Step 3
        StepCard(
            stepNumber = "3",
            title = "Toggle Switch ON",
            description = "Switch 'Device Broadcast Status' to ON. This allows Spotify to notify AdVol of track and ad changes in real-time."
        )

        // Android Auto note
        Text(
            text = "USING ANDROID AUTO?",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Why ads may only be lowered in the car",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When Android Auto is connected, Android routes media to the car and locks the phone's " +
                        "media volume at maximum (you may have noticed the slider jumping to 100%). The car's own " +
                        "knob controls loudness and apps cannot change it.\n\n" +
                        "AdVol still tries a real mute first and checks whether Android accepted it (recent " +
                        "versions do). If it was refused, AdVol falls back to asking Spotify to duck (lower) its " +
                        "own output for the length of the ad, the same mechanism navigation prompts use. Ducked " +
                        "ads are noticeably quieter but not silent. Detection still needs 'Device Broadcast " +
                        "Status' or Notification Access as usual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Open Spotify Button
        Button(
            onClick = {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    // Open in Play Store if not installed
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music"))
                    context.startActivity(marketIntent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Launch,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Open Spotify",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
