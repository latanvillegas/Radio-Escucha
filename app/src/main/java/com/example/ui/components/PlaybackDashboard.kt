package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.util.ConnectivityObserver

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackDashboard(
    currentStation: RadioStation?,
    currentTrackTitle: String? = null,
    currentTrackArtist: String? = null,
    currentTrackAlbum: String? = null,
    currentTrackArtworkUrl: String? = null,
    playbackStatus: PlaybackStatus,
    volume: Float,
    errorMessage: String? = null,
    sleepSecondsLeft: Int? = null,
    networkStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onPreviousStation: () -> Unit = {},
    onNextStation: () -> Unit = {},
    onRandomStation: () -> Unit = {},
    onVolumeChange: (Float) -> Unit,
    onCancelSleepTimer: () -> Unit = {},
    onShare: (RadioStation) -> Unit = {},
    onOpenFullScreen: () -> Unit = {}
) {
    val isPlaying = playbackStatus == PlaybackStatus.PLAYING
    val isBuffering = playbackStatus == PlaybackStatus.BUFFERING

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "turntable_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header / Network / Sleep timer banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live status tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (playbackStatus) {
                        PlaybackStatus.PLAYING -> Color(0xFF1B5E20).copy(alpha = 0.3f)
                        PlaybackStatus.BUFFERING -> Color(0xFFE65100).copy(alpha = 0.3f)
                        PlaybackStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (playbackStatus) {
                                        PlaybackStatus.PLAYING -> Color(0xFF4CAF50)
                                        PlaybackStatus.BUFFERING -> Color(0xFFFF9800)
                                        PlaybackStatus.ERROR -> MaterialTheme.colorScheme.error
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Text(
                            text = when (playbackStatus) {
                                PlaybackStatus.PLAYING -> "EN VIVO"
                                PlaybackStatus.BUFFERING -> "CONECTANDO..."
                                PlaybackStatus.ERROR -> "ERROR"
                                else -> "LISTO"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (playbackStatus) {
                                PlaybackStatus.PLAYING -> Color(0xFF81C784)
                                PlaybackStatus.BUFFERING -> Color(0xFFFFB74D)
                                PlaybackStatus.ERROR -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (sleepSecondsLeft != null && sleepSecondsLeft > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onCancelSleepTimer() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = "Temporizador",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formatTimeLeft(sleepSecondsLeft),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (currentStation != null) {
                        IconButton(
                            onClick = { onShare(currentStation) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenFullScreen,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Pantalla completa",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Main Info: Logo + Station + Song, Artist & Album
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = currentStation != null) { onOpenFullScreen() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    if (currentStation != null) {
                        StationLogo(
                            stationName = currentStation.name,
                            stationUrl = currentStation.url,
                            faviconUrl = currentTrackArtworkUrl ?: currentStation.faviconUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isPlaying) Modifier.rotate(rotationAngle) else Modifier)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = currentStation?.name ?: "Selecciona una emisora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )

                    if (currentStation != null) {
                        // Canción
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Canción",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentTrackTitle?.takeIf { it.isNotBlank() } ?: "Transmisión en directo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        // Artista
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Artista",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            val artistLabel = currentTrackArtist?.takeIf { it.isNotBlank() }
                                ?: currentStation.genre.ifBlank { currentStation.country }
                            Text(
                                text = "Artista: $artistLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Álbum
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "Álbum",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            val albumLabel = currentTrackAlbum?.takeIf { it.isNotBlank() }
                                ?: currentStation.country.ifBlank { "Radio en Vivo" }
                            Text(
                                text = "Álbum: $albumLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "Toca cualquier estación para sintonizar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isPlaying) {
                    AudioVisualizer(
                        isPlaying = true,
                        modifier = Modifier
                            .width(36.dp)
                            .height(28.dp)
                    )
                }
            }

            // Error display if present
            if (!errorMessage.isNullOrBlank() && playbackStatus == PlaybackStatus.ERROR) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRandomStation,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatoria",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPreviousStation,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Big Main Play/Pause Button
                FloatingActionButton(
                    onClick = onTogglePlay,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNextStation,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onStopPlayback,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Detener",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Volume Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeDown,
                    contentDescription = "Volumen bajo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onVolumeChange(0f) }
                )

                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volumen alto",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onVolumeChange(1f) }
                )
            }
        }
    }
}
