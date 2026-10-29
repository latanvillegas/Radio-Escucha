package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.util.ConnectivityObserver
import kotlin.math.roundToInt

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(
    currentStation: RadioStation,
    currentTrackTitle: String?,
    currentTrackArtist: String?,
    currentTrackAlbum: String? = null,
    currentTrackArtworkUrl: String?,
    playbackStatus: PlaybackStatus,
    volume: Float,
    sleepSecondsLeft: Int?,
    networkStatus: ConnectivityObserver.Status,
    isFavorite: Boolean,
    equalizerEnabled: Boolean = false,
    onToggleFavorite: (RadioStation) -> Unit,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onPreviousStation: () -> Unit,
    onNextStation: () -> Unit,
    onRandomStation: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onShare: (RadioStation) -> Unit,
    onDismiss: () -> Unit
) {
    val isPlaying = playbackStatus == PlaybackStatus.PLAYING
    val isBuffering = playbackStatus == PlaybackStatus.BUFFERING

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPRODUCIENDO",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentStation.genre.ifBlank { currentStation.country ?: "Radio" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Ecualizador",
                            tint = if (equalizerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { onToggleFavorite(currentStation) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { onShare(currentStation) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Central Artwork / Vinyl
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                StationLogo(
                    stationName = currentStation.name,
                    stationUrl = currentStation.url,
                    faviconUrl = currentTrackArtworkUrl ?: currentStation.faviconUrl,
                    modifier = Modifier
                        .size(214.dp)
                        .clip(CircleShape)
                        .then(if (isPlaying) Modifier.rotate(rotationAngle) else Modifier)
                )

                // Vinyl center spindle hole
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            // Track metadata: Station + Song, Artist, Album Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentStation.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Nombre de la Canción
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Canción",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Canción:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentTrackTitle?.takeIf { it.isNotBlank() } ?: "Transmisión en directo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
                            )
                        }

                        // Artista
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Artista",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Artista:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentTrackArtist?.takeIf { it.isNotBlank() }
                                    ?: currentStation.genre.ifBlank { "Emisora en vivo" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
                            )
                        }

                        // Álbum
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "Álbum",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Álbum:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentTrackAlbum?.takeIf { it.isNotBlank() }
                                    ?: currentStation.country.ifBlank { "Radio Online" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee()
                            )
                        }
                    }
                }

                if (sleepSecondsLeft != null && sleepSecondsLeft > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Se apagará en: ${formatTimeLeft(sleepSecondsLeft)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main transport controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRandomStation) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onPreviousStation) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = onTogglePlay,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNextStation) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onOpenSleepTimer) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Temporizador de apagado",
                            tint = if (sleepSecondsLeft != null && sleepSecondsLeft > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Volume Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeDown,
                        contentDescription = "Mute",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onVolumeChange(0f) }
                    )

                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Máximo volumen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onVolumeChange(1f) }
                    )
                }
            }
        }
    }
}

@Composable
fun EqualizerDialog(
    enabled: Boolean,
    presets: List<String>,
    selectedPreset: String,
    bassBoostStrength: Float,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBassBoostChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Ecualizador y Audio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Perfil Acústico",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                presets.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = preset == selectedPreset && enabled
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPreset(preset) },
                                label = {
                                    Text(
                                        text = preset,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Refuerzo de Graves (Bass Boost)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(bassBoostStrength * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = bassBoostStrength,
                        onValueChange = onBassBoostChange,
                        valueRange = 0f..1f
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}

@Composable
fun AddStationOptionsDialog(
    onDismiss: () -> Unit,
    onAddLocal: () -> Unit,
    onOpenRadioBrowserAdd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nueva Emisora", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Elige cómo deseas añadir la emisora de radio:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        onDismiss()
                        onAddLocal()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir Emisora Personalizada")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenRadioBrowserAdd()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Public, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar en Radio Browser")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddStationDialog(
    onDismiss: () -> Unit,
    onAddStation: (
        name: String,
        url: String,
        faviconUrl: String,
        genre: String,
        country: String,
        region: String,
        province: String,
        district: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var faviconUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Emisora Personalizada", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la emisora *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL del stream *") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = faviconUrl,
                    onValueChange = { faviconUrl = it },
                    label = { Text("URL del logotipo / icono") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Género / Categoría") },
                    placeholder = { Text("Pop, Noticias, Rock, Salsa...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("País") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Región / Estado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text("Provincia / Ciudad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("Distrito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onAddStation(name.trim(), url.trim(), faviconUrl.trim(), genre.trim(), country.trim(), region.trim(), province.trim(), district.trim())
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    currentSecondsLeft: Int?,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit
) {
    val presets = listOf(5, 10, 15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Temporizador de Apagado", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentSecondsLeft != null && currentSecondsLeft > 0) {
                    Text(
                        text = "Tiempo restante: ${formatTimeLeft(currentSecondsLeft)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = { onSelectMinutes(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar Temporizador")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text("Selecciona cuándo detener la reproducción:", style = MaterialTheme.typography.bodySmall)

                presets.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { minutes ->
                            FilledTonalButton(
                                onClick = { onSelectMinutes(minutes) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("${minutes}m")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
