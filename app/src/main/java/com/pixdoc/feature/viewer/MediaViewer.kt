package com.pixdoc.feature.viewer

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixdoc.core.utils.FileUtils
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun MediaViewer(file: File) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Setup player
    DisposableEffect(file) {
        val player = MediaPlayer()
        try {
            player.setDataSource(context, Uri.fromFile(file))
            player.prepare()
            duration = player.duration
            player.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
            }
            mediaPlayer = player
        } catch (e: Exception) {
            errorMessage = "Unable to play audio: ${e.message}"
        }

        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    // Polling player position
    LaunchedEffect(isPlaying, isUserScrubbing) {
        while (isPlaying && !isUserScrubbing) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentPosition = it.currentPosition
                }
            }
            delay(250)
        }
    }

    // Vinyl spinning animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    if (errorMessage != null) {
        UnsupportedDocFallback(file, message = errorMessage ?: "Audio playback error")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A237E),
                        Color(0xFF121212)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated vinyl disc
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .rotate(if (isPlaying) rotation else 0f)
                    .background(Color(0xFF212121), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Outer grooves
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .background(Color(0xFF2C2C2C), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Center label
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(0xFFD81B60), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // File Name & Info
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${file.extension.uppercase()} Audio  •  ${FileUtils.formatFileSize(file.length())}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Progress Slider
            val maxDuration = duration.coerceAtLeast(1).toFloat()
            val sliderValue = if (isUserScrubbing) scrubPosition else currentPosition.toFloat()

            Slider(
                value = sliderValue.coerceIn(0f, maxDuration),
                onValueChange = {
                    isUserScrubbing = true
                    scrubPosition = it
                },
                onValueChangeFinished = {
                    mediaPlayer?.seekTo(scrubPosition.toInt())
                    currentPosition = scrubPosition.toInt()
                    isUserScrubbing = false
                },
                valueRange = 0f..maxDuration,
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(if (isUserScrubbing) scrubPosition.toInt() else currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Player Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Rewind 10s
                IconButton(onClick = {
                    val target = (currentPosition - 10000).coerceAtLeast(0)
                    mediaPlayer?.seekTo(target)
                    currentPosition = target
                }) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause FAB
                FloatingActionButton(
                    onClick = {
                        val player = mediaPlayer ?: return@FloatingActionButton
                        if (player.isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.start()
                            isPlaying = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("media_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Fast Forward 10s
                IconButton(onClick = {
                    val target = (currentPosition + 10000).coerceAtMost(duration)
                    mediaPlayer?.seekTo(target)
                    currentPosition = target
                }) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
