package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.ui.theme.LocalIconScale
import com.example.util.ConnectivityObserver
import kotlin.math.roundToInt

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@Deprecated("Use FullPlayerScreen instead to avoid Dialog subwindow edge-to-edge limitations on custom vendor Android skins.")
@Composable
fun FullPlayerDialog(
    currentStation: RadioStation,
    currentTrackTitle: String?,
    currentTrackArtist: String?,
    currentTrackArtworkUrl: String?,
    playbackStatus: PlaybackStatus,
    volume: Float,
    sleepSecondsLeft: Int?,
    networkStatus: ConnectivityObserver.Status,
    isFavorite: Boolean,
    onToggleFavorite: (RadioStation) -> Unit,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onPreviousStation: () -> Unit,
    onNextStation: () -> Unit,
    onRandomStation: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onShare: (RadioStation) -> Unit,
    onDismiss: () -> Unit
) {
    FullPlayerScreen(
        currentStation = currentStation,
        currentTrackTitle = currentTrackTitle,
        currentTrackArtist = currentTrackArtist,
        currentTrackArtworkUrl = currentTrackArtworkUrl,
        playbackStatus = playbackStatus,
        volume = volume,
        sleepSecondsLeft = sleepSecondsLeft,
        networkStatus = networkStatus,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onTogglePlay = onTogglePlay,
        onStopPlayback = onStopPlayback,
        onPreviousStation = onPreviousStation,
        onNextStation = onNextStation,
        onRandomStation = onRandomStation,
        onVolumeChange = onVolumeChange,
        onOpenSleepTimer = onOpenSleepTimer,
        onShare = onShare,
        onDismiss = onDismiss
    )
}

@Composable
fun FullPlayerScreen(
    currentStation: RadioStation,
    currentTrackTitle: String?,
    currentTrackArtist: String?,
    currentTrackArtworkUrl: String?,
    playbackStatus: PlaybackStatus,
    volume: Float,
    sleepSecondsLeft: Int?,
    networkStatus: ConnectivityObserver.Status,
    isFavorite: Boolean,
    onToggleFavorite: (RadioStation) -> Unit,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onPreviousStation: () -> Unit,
    onNextStation: () -> Unit,
    onRandomStation: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onShare: (RadioStation) -> Unit,
    onDismiss: () -> Unit
) {
    val isPlaying = playbackStatus == PlaybackStatus.PLAYING
    val iconScale = LocalIconScale.current

    // Handle system back gesture / back button
    BackHandler(enabled = true) {
        onDismiss()
    }

    // Disc rotation
    val infiniteTransition = rememberInfiniteTransition(label = "full_disc_rotation")
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_player_surface"),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight && maxWidth > 560.dp
            val screenHeight = maxHeight
            val screenWidth = maxWidth

            // Ambient background gradient extending 100% full screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            if (isLandscape) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    FullPlayerTopBar(
                        playbackStatus = playbackStatus,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        currentStation = currentStation,
                        onDismiss = onDismiss,
                        onToggleFavorite = onToggleFavorite,
                        onShare = onShare
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column: Artwork + Metadata
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val artworkSize = (screenHeight * 0.42f).coerceIn(110.dp, 210.dp)
                            FullPlayerArtwork(
                                currentTrackArtworkUrl = currentTrackArtworkUrl,
                                currentStation = currentStation,
                                animatedAngle = animatedAngle,
                                modifier = Modifier.size(artworkSize)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FullPlayerMetadata(
                                currentStation = currentStation,
                                currentTrackTitle = currentTrackTitle,
                                currentTrackArtist = currentTrackArtist
                            )
                        }

                        // Right Column: Visualizer + Controls + Volume
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                        ) {
                            AudioVisualizer(
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            )

                            FullPlayerControls(
                                isPlaying = isPlaying,
                                playbackStatus = playbackStatus,
                                sleepSecondsLeft = sleepSecondsLeft,
                                onRandomStation = onRandomStation,
                                onPreviousStation = onPreviousStation,
                                onTogglePlay = onTogglePlay,
                                onNextStation = onNextStation,
                                onOpenSleepTimer = onOpenSleepTimer
                            )

                            FullPlayerVolumeBar(
                                volume = volume,
                                onVolumeChange = onVolumeChange
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    FullPlayerTopBar(
                        playbackStatus = playbackStatus,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        currentStation = currentStation,
                        onDismiss = onDismiss,
                        onToggleFavorite = onToggleFavorite,
                        onShare = onShare
                    )

                    val artworkSize = (screenWidth * 0.68f).coerceIn(180.dp, 280.dp)
                    FullPlayerArtwork(
                        currentTrackArtworkUrl = currentTrackArtworkUrl,
                        currentStation = currentStation,
                        animatedAngle = animatedAngle,
                        modifier = Modifier.size(artworkSize)
                    )

                    FullPlayerMetadata(
                        currentStation = currentStation,
                        currentTrackTitle = currentTrackTitle,
                        currentTrackArtist = currentTrackArtist
                    )

                    AudioVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    )

                    FullPlayerControls(
                        isPlaying = isPlaying,
                        playbackStatus = playbackStatus,
                        sleepSecondsLeft = sleepSecondsLeft,
                        onRandomStation = onRandomStation,
                        onPreviousStation = onPreviousStation,
                        onTogglePlay = onTogglePlay,
                        onNextStation = onNextStation,
                        onOpenSleepTimer = onOpenSleepTimer
                    )

                    FullPlayerVolumeBar(
                        volume = volume,
                        onVolumeChange = onVolumeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun FullPlayerTopBar(
    playbackStatus: PlaybackStatus,
    isPlaying: Boolean,
    isFavorite: Boolean,
    currentStation: RadioStation,
    onDismiss: () -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onShare: (RadioStation) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimizar",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "REPRODUCIENDO AHORA",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            val badgeText = when (playbackStatus) {
                PlaybackStatus.ERROR -> "ERROR ⚠️"
                PlaybackStatus.BUFFERING -> "CARGANDO..."
                PlaybackStatus.PLAYING -> "AL AIRE 🔴"
                else -> "EN PAUSA"
            }
            Text(
                badgeText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Row {
            IconButton(onClick = { onToggleFavorite(currentStation) }) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onShare(currentStation) }) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FullPlayerArtwork(
    currentTrackArtworkUrl: String?,
    currentStation: RadioStation,
    animatedAngle: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .shadow(16.dp, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!currentTrackArtworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentTrackArtworkUrl,
                contentDescription = "Carátula",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            val logoUrl = remember(currentStation.name, currentStation.url, currentStation.faviconUrl) {
                StationLogoResolver.getLogoForStation(currentStation.name, currentStation.url, currentStation.faviconUrl)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = currentStation.name,
                        modifier = Modifier
                            .fillMaxSize(0.42f)
                            .clip(CircleShape)
                            .graphicsLayer { rotationZ = -animatedAngle },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(0.35f)
                            .graphicsLayer { rotationZ = -animatedAngle },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FullPlayerMetadata(
    currentStation: RadioStation,
    currentTrackTitle: String?,
    currentTrackArtist: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val hasTrackDetails = !currentTrackTitle.isNullOrBlank() || !currentTrackArtist.isNullOrBlank()
        if (hasTrackDetails) {
            Text(
                text = currentTrackTitle ?: currentStation.name,
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = (currentTrackArtist ?: currentStation.genre).uppercase(),
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📻 ${currentStation.name}",
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = currentStation.name,
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentStation.genre.uppercase(),
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        val geoParts = listOfNotNull(
            currentStation.country.takeIf { it.isNotEmpty() },
            currentStation.region.takeIf { it.isNotEmpty() },
            currentStation.province.takeIf { it.isNotEmpty() }
        ).filter { it.isNotBlank() }

        if (geoParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = geoParts.joinToString(" • "),
                modifier = Modifier.basicMarquee(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FullPlayerControls(
    isPlaying: Boolean,
    playbackStatus: PlaybackStatus,
    sleepSecondsLeft: Int?,
    onRandomStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onTogglePlay: () -> Unit,
    onNextStation: () -> Unit,
    onOpenSleepTimer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onRandomStation,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = onPreviousStation,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onTogglePlay() },
            contentAlignment = Alignment.Center
        ) {
            if (playbackStatus == PlaybackStatus.BUFFERING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        IconButton(
            onClick = onNextStation,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(
            onClick = onOpenSleepTimer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                if (sleepSecondsLeft != null) Icons.Filled.Timer else Icons.Outlined.Timer,
                contentDescription = "Temporizador",
                tint = if (sleepSecondsLeft != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FullPlayerVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (volume > 0.05f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
            contentDescription = "Volumen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(volume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
fun AddStationOptionsDialog(
    onDismiss: () -> Unit,
    onAddLocal: () -> Unit,
    onOpenRadioBrowserAdd: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_station_options_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Agregar Nueva Radio",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    "Selecciona cómo deseas añadir la emisora:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // Option 1: Add Locally
                Card(
                    onClick = {
                        onDismiss()
                        onAddLocal()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("opt_add_local")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Agregar a mi App (Local)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Guarda la emisora en tu biblioteca privada en este dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 2: Register in Radio Browser
                Card(
                    onClick = {
                        onDismiss()
                        onOpenRadioBrowserAdd()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("opt_add_radio_browser")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Registrar en Radio Browser",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                "Añádela al directorio mundial radio-browser.info para que todos la escuchen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
fun AddStationDialog(
    onDismiss: () -> Unit,
    onAddStation: (String, String, String, String, String, String, String, String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var faviconUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_station_dialog_surface")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Agregar Estación Local",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    onClick = { uriHandler.openUri("https://www.radio-browser.info/add") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "¿Quieres registrarla en el directorio mundial? Toca aquí (Radio Browser)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Nombre de la radio") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_station_name_input")
                )
                if (nameError) {
                    Text(
                        "El nombre no puede estar vacío",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                    },
                    label = { Text("URL de la señal en vivo (Stream)") },
                    placeholder = { Text("http://... o https://...") },
                    isError = urlError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_station_url_input")
                )
                if (urlError) {
                    Text(
                        "Ingresa una URL válida (ej: http://...)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = faviconUrl,
                    onValueChange = { faviconUrl = it },
                    label = { Text("URL del Logo / Imagen (Opcional)") },
                    placeholder = { Text("https://ejemplo.com/logo.png") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_station_favicon_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Género / Categoría (ej: Pop, Rock, Jazz)") },
                    placeholder = { Text("Varios") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_station_genre_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("País", fontSize = 12.sp) },
                        placeholder = { Text("ej: Perú") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_station_country_input")
                    )
                    OutlinedTextField(
                        value = region,
                        onValueChange = { region = it },
                        label = { Text("Región", fontSize = 12.sp) },
                        placeholder = { Text("ej: Lima") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_station_region_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("Provincia (Opcional)", fontSize = 11.sp) },
                        placeholder = { Text("ej: Lima") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_station_province_input")
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("Distrito (Opcional)", fontSize = 11.sp) },
                        placeholder = { Text("ej: Miraflores") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_station_district_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_add_button")
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var hasErr = false
                            if (name.trim().isEmpty()) {
                                nameError = true
                                hasErr = true
                            }
                            val trimmedUrl = url.trim()
                            if (trimmedUrl.isEmpty() || (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))) {
                                urlError = true
                                hasErr = true
                            }
                            if (!hasErr) {
                                onAddStation(name, url, faviconUrl, genre, country, region, province, district)
                            }
                        },
                        modifier = Modifier.testTag("confirm_add_button")
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentSecondsLeft: Int?,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    var customHours by remember { mutableStateOf(0) }
    var customMinutes by remember { mutableStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("sleep_timer_dialog_surface")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Temporizador de Apagado",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!showCustomPicker) {
                    Text(
                        text = if (currentSecondsLeft != null) {
                            "Temporizador activo. Apagando en: ${formatTimeLeft(currentSecondsLeft)}"
                        } else {
                            "La radio se apagará automáticamente después del tiempo seleccionado."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    val options = listOf(
                        15 to "15 Minutos",
                        30 to "30 Minutos",
                        45 to "45 Minutos",
                        60 to "1 Hora"
                    )

                    options.forEach { (mins, label) ->
                        OutlinedButton(
                            onClick = { onSelectMinutes(mins) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("sleep_timer_btn_$mins")
                        ) {
                            Text(label)
                        }
                    }

                    OutlinedButton(
                        onClick = { showCustomPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("sleep_timer_btn_custom"),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Personalizado", color = MaterialTheme.colorScheme.primary)
                    }

                    if (currentSecondsLeft != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSelectMinutes(0) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sleep_timer_btn_cancel")
                        ) {
                            Text("Cancelar Temporizador")
                        }
                    }
                } else {
                    Text(
                        "Elige el tiempo (Máx 12h)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeValuePicker(
                            value = customHours,
                            onValueChange = { customHours = it },
                            label = "Horas",
                            range = 0..12
                        )
                        
                        Text(
                            ":",
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        TimeValuePicker(
                            value = customMinutes,
                            onValueChange = { customMinutes = it },
                            label = "Minutos",
                            range = 0..59
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val totalMinutes = (customHours * 60) + customMinutes
                    Text(
                        if (totalMinutes > 0) "Total: $totalMinutes minutos" else "Selecciona un tiempo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showCustomPicker = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Volver")
                        }
                        Button(
                            onClick = {
                                if (totalMinutes > 0) {
                                    onSelectMinutes(totalMinutes)
                                }
                            },
                            enabled = totalMinutes > 0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Iniciar")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun TimeValuePicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    range: IntRange
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            enabled = value < range.last
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Aumentar $label")
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.width(72.dp)
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
        
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            enabled = value > range.first
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Disminuir $label")
        }
        
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
