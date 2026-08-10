package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.ui.theme.LocalIconScale
import com.example.util.ConnectivityObserver

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@Composable
fun PlaybackDashboard(
    currentStation: RadioStation?,
    currentTrackTitle: String? = null,
    currentTrackArtist: String? = null,
    currentTrackArtworkUrl: String? = null,
    playbackStatus: PlaybackStatus,
    volume: Float,
    errorMessage: String?,
    sleepSecondsLeft: Int?,
    networkStatus: ConnectivityObserver.Status,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onPreviousStation: () -> Unit = {},
    onNextStation: () -> Unit = {},
    onRandomStation: () -> Unit = {},
    onVolumeChange: (Float) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onShare: (RadioStation) -> Unit,
    onOpenFullScreen: () -> Unit = {}
) {
    val iconScale = LocalIconScale.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("playback_dashboard_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentStation == null) {
                // Standby Idle state
                Column(
                    modifier = Modifier.padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "SINTONIZAR RADIO",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Selecciona una estación de la lista para escuchar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Active player layout
                val isPlaying = playbackStatus == PlaybackStatus.PLAYING
                val activeStation = currentStation
                
                // Track spin rotation animated state
                val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "angle"
                )
                val animatedAngle = if (isPlaying) rotationAngle else 0f

                // Top state bar inside card
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColor = when (playbackStatus) {
                        PlaybackStatus.ERROR -> MaterialTheme.colorScheme.error
                        PlaybackStatus.BUFFERING -> MaterialTheme.colorScheme.tertiary
                        PlaybackStatus.PLAYING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val badgeContentColor = when (playbackStatus) {
                        PlaybackStatus.ERROR -> MaterialTheme.colorScheme.onError
                        PlaybackStatus.BUFFERING -> MaterialTheme.colorScheme.onTertiary
                        PlaybackStatus.PLAYING -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val badgeText = when (playbackStatus) {
                        PlaybackStatus.ERROR -> "CAÍDA ⚠️"
                        PlaybackStatus.BUFFERING -> "CARGANDO..."
                        PlaybackStatus.PLAYING -> "AL AIRE"
                        else -> "EN PAUSA"
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeColor, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeContentColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Connection Status Indicator
                    if (networkStatus == ConnectivityObserver.Status.MobileData) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DATOS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }

                    // Sleep indicator inside card if set
                    if (sleepSecondsLeft != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable { onCancelSleepTimer() }
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatTimeLeft(sleepSecondsLeft),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancelar temporizador",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Actions group: Fullscreen, Share & Stop
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenFullScreen,
                            modifier = Modifier
                                .testTag("open_fullscreen_player_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Pantalla completa",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp * iconScale)
                            )
                        }

                        IconButton(
                            onClick = { activeStation?.let { onShare(it) } },
                            modifier = Modifier
                                .testTag("share_active_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp * iconScale)
                            )
                        }

                        IconButton(
                            onClick = onStopPlayback,
                            modifier = Modifier
                                .testTag("stop_playback_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Detener",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Middle Station Metadata Row (Clickable to open FullScreen Player)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenFullScreen() }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!currentTrackArtworkUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = currentTrackArtworkUrl,
                                contentDescription = "Carátula de canción",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )

                            if (activeStation != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(1.dp)
                                ) {
                                    StationLogo(
                                        stationName = activeStation.name,
                                        stationUrl = activeStation.url,
                                        faviconUrl = activeStation.faviconUrl,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    } else if (activeStation != null) {
                        StationLogo(
                            stationName = activeStation.name,
                            stationUrl = activeStation.url,
                            faviconUrl = activeStation.faviconUrl,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer { rotationZ = animatedAngle }
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.onPrimary,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Radio,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    val hasTrackDetails = !currentTrackTitle.isNullOrBlank() || !currentTrackArtist.isNullOrBlank()

                    Column(modifier = Modifier.weight(1f)) {
                        if (hasTrackDetails) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = (currentTrackArtist ?: currentStation.genre).uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrackTitle ?: currentStation.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📻 ${currentStation.name}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = currentStation.genre.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentStation.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val geoParts = listOfNotNull(
                                currentStation.country.takeIf { it.isNotEmpty() },
                                currentStation.region.takeIf { it.isNotEmpty() },
                                currentStation.province.takeIf { it.isNotEmpty() },
                                currentStation.district.takeIf { it.isNotEmpty() }
                            ).filter { it.isNotBlank() }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (geoParts.isNotEmpty()) geoParts.joinToString(" • ") else if (currentStation.isCustom) "Stream personalizado" else "Transmisión en vivo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AudioVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage != null || playbackStatus == PlaybackStatus.ERROR) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage ?: "No se pudo conectar con la emisora.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onTogglePlay,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reintentar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onPreviousStation,
                            modifier = Modifier
                                .testTag("previous_station_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Estación Anterior",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp * iconScale)
                            )
                        }

                        when (playbackStatus) {
                            PlaybackStatus.BUFFERING -> {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            else -> {
                                val buttonBg = if (playbackStatus == PlaybackStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                val icon = when {
                                    playbackStatus == PlaybackStatus.ERROR -> Icons.Default.Refresh
                                    isPlaying -> Icons.Filled.Pause
                                    else -> Icons.Filled.PlayArrow
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("play_pause_button")
                                        .background(buttonBg, shape = RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onTogglePlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproducir / Reintentar",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp * iconScale)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onNextStation,
                            modifier = Modifier
                                .testTag("next_station_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Estación Siguiente",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp * iconScale)
                            )
                        }

                        IconButton(
                            onClick = onRandomStation,
                            modifier = Modifier
                                .testTag("random_station_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Estación Aleatoria",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp * iconScale)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isPlaying) Color.Green else Color.Gray, CircleShape)
                        )
                        Text(
                            text = "128kbps AAC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
