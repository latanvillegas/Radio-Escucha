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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.ui.theme.Surface3D
import com.example.ui.theme.card3D
import com.example.ui.theme.glassCircle3D
import com.example.ui.theme.orbButton3D
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

    val rotationAngle = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vinyl_rotation"
        )
        angle
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface3D.ambientScreenBrush())
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
            // Top Bar with 3D Glass Orbs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .glassCircle3D(elevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPRODUCIENDO EN 3D",
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .size(38.dp)
                            .glassCircle3D(elevation = if (equalizerEnabled) 6.dp else 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Ecualizador",
                            tint = if (equalizerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(currentStation) },
                        modifier = Modifier
                            .size(38.dp)
                            .glassCircle3D(elevation = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onShare(currentStation) },
                        modifier = Modifier
                            .size(38.dp)
                            .glassCircle3D(elevation = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Central 3D Glowing Circular Artwork
            Box(
                modifier = Modifier
                    .size(284.dp)
                    .orbButton3D(baseColor = MaterialTheme.colorScheme.surfaceVariant, elevation = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                StationLogo(
                    stationName = currentStation.name,
                    stationUrl = currentStation.url,
                    faviconUrl = currentTrackArtworkUrl ?: currentStation.faviconUrl,
                    modifier = Modifier
                        .size(268.dp)
                        .clip(CircleShape)
                        .graphicsLayer {
                            rotationZ = if (isPlaying) rotationAngle else 0f
                        }
                )
            }

            // Track metadata: Centered Station + 3D Card for Song, Artist, Album
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .card3D(shape = RoundedCornerShape(18.dp), isSelectedOrPlaying = isPlaying, elevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Nombre de la Canción (Centrado)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Canción",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentTrackTitle?.takeIf { it.isNotBlank() } ?: "Transmisión en directo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        // Artista (Centrado)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Artista",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val artistLabel = currentTrackArtist?.takeIf { it.isNotBlank() }
                                ?: currentStation.genre.ifBlank { "Emisora en vivo" }
                            Text(
                                text = "Artista: $artistLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        // Álbum (Centrado)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "Álbum",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val albumLabel = currentTrackAlbum?.takeIf { it.isNotBlank() }
                                ?: currentStation.country.ifBlank { "Radio Online" }
                            Text(
                                text = "Álbum: $albumLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }

                if (sleepSecondsLeft != null && sleepSecondsLeft > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
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

            // 3D Transport Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRandomStation,
                        modifier = Modifier
                            .size(44.dp)
                            .glassCircle3D(elevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onPreviousStation,
                        modifier = Modifier
                            .size(50.dp)
                            .glassCircle3D(elevation = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .orbButton3D(baseColor = MaterialTheme.colorScheme.primary, elevation = 14.dp)
                            .clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center
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
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNextStation,
                        modifier = Modifier
                            .size(50.dp)
                            .glassCircle3D(elevation = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier
                            .size(44.dp)
                            .glassCircle3D(elevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Temporizador de apagado",
                            tint = if (sleepSecondsLeft != null && sleepSecondsLeft > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
    val bandGains by com.example.util.RadioAudioDspEngine.bandGainsDb.collectAsStateWithLifecycle()
    val loudnessBoost by com.example.util.RadioAudioDspEngine.loudnessBoost.collectAsStateWithLifecycle()
    val bandLabels = com.example.util.RadioAudioDspEngine.bandLabels

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
                    Column {
                        Text("Ecualizador y Audio DSP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (enabled) "Activo • Perfil: $selectedPreset" else "Desactivado (Audio plano)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 5-Band Biquad EQ Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bandas de Frecuencia (5 Bandas DSP)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedPreset == "Personalizado") {
                        Text(
                            text = "Personalizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    bandLabels.forEachIndexed { index, freqLabel ->
                        val gain = bandGains.getOrElse(index) { 0f }
                        val formattedGain = if (gain > 0f) "+${String.format(java.util.Locale.US, "%.1f", gain)} dB"
                        else "${String.format(java.util.Locale.US, "%.1f", gain)} dB"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(46.dp)
                            )
                            Slider(
                                value = gain,
                                onValueChange = { com.example.util.RadioAudioDspEngine.setBandGainDb(index, it) },
                                valueRange = -10f..10f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            )
                            Text(
                                text = formattedGain,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (gain != 0f && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Bass Boost
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Refuerzo de Graves (Sub-Bass)",
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
                        valueRange = 0f..1f,
                        modifier = Modifier.height(32.dp)
                    )
                }

                // Loudness & Vocal Clarity Boost
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Claridad y Potencia HD (Preamp)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(loudnessBoost * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = loudnessBoost,
                        onValueChange = { com.example.util.RadioAudioDspEngine.setLoudnessBoost(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.height(32.dp)
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
                    label = { Text("URL del stream (Radio o TV en Modo Radio) *") },
                    placeholder = { Text("https://... (.m3u8, .mp3, .aac, .pls, .m3u, .mpd)") },
                    supportingText = {
                        Text(
                            "Soporta Icecast/Shoutcast (.mp3/.aac/.ogg), HLS (.m3u8), DASH (.mpd), listas (.pls/.m3u) y canales de TV en modo solo audio.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StationLogo(
                        stationName = name.ifBlank { "RD" },
                        stationUrl = url,
                        faviconUrl = faviconUrl,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    OutlinedTextField(
                        value = faviconUrl,
                        onValueChange = { faviconUrl = it },
                        label = { Text("URL del logotipo o web (.png, .webp, .svg, .jpg, .gif, .ico)") },
                        placeholder = { Text("https://... o www.miradio.com") },
                        supportingText = {
                            Text(
                                "Acepta PNG, JPG, WebP, SVG, GIF, ICO, Base64 o el dominio web de la emisora.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
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
                        val cleanedLogo = com.example.util.LogoImageLoader.sanitizeLogoUrl(faviconUrl)
                        onAddStation(name.trim(), url.trim(), cleanedLogo, genre.trim(), country.trim(), region.trim(), province.trim(), district.trim())
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
