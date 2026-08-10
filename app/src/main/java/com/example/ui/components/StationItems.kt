package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaybackHistory
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.ui.theme.LocalIconScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StationItem(
    station: RadioStation,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onDelete: (RadioStation) -> Unit,
    onShare: (RadioStation) -> Unit
) {
    val iconScale = LocalIconScale.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(station) }
            .testTag("station_card_${station.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isPlaying) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio thumbnail with profile logo or icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                StationLogo(
                    stationName = station.name,
                    stationUrl = station.url,
                    faviconUrl = station.faviconUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Overlay for playing/buffering states
                if (isPlaying && !isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniVisualizer(color = Color.White)
                    }
                } else if (isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text core infos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = station.genre,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val geoParts = listOfNotNull(
                        station.country.takeIf { it.isNotEmpty() },
                        station.region.takeIf { it.isNotEmpty() },
                        station.province.takeIf { it.isNotEmpty() },
                        station.district.takeIf { it.isNotEmpty() }
                    ).filter { it.isNotBlank() }

                    if (geoParts.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                        Text(
                            text = geoParts.joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(2f, fill = false)
                        )
                    }
                }
            }

            // Right action buttons: Share, Heart (Favorite) & Delete (If custom)
            IconButton(
                onClick = { onShare(station) },
                modifier = Modifier
                    .testTag("share_button_${station.id}")
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir radio",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp * iconScale)
                )
            }

            IconButton(
                onClick = { onToggleFavorite(station) },
                modifier = Modifier
                    .testTag("favorite_button_${station.id}")
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (station.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (station.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp * iconScale)
                )
            }

            if (station.isCustom) {
                IconButton(
                    onClick = { onDelete(station) },
                    modifier = Modifier
                        .testTag("delete_button_${station.id}")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar radio",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun StationsList(
    filteredStations: List<RadioStation>,
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onDeleteStation: (RadioStation) -> Unit,
    onShare: (RadioStation) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp)
) {
    if (filteredStations.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp)
                .testTag("empty_stations_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No se encontraron radios",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                )
                Text(
                    "Agrega una radio nueva o limpia los filtros",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        }
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val useGrid = maxWidth >= 500.dp
            if (useGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 250.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stations_lazy_grid"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = contentPadding
                ) {
                    items(filteredStations, key = { it.id }) { station ->
                        StationItem(
                            station = station,
                            isPlaying = currentStation?.id == station.id && playbackStatus == PlaybackStatus.PLAYING,
                            isBuffering = currentStation?.id == station.id && playbackStatus == PlaybackStatus.BUFFERING,
                            onSelect = onStationSelect,
                            onToggleFavorite = onToggleFavorite,
                            onDelete = onDeleteStation,
                            onShare = onShare
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stations_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = contentPadding
                ) {
                    items(filteredStations, key = { it.id }) { station ->
                        StationItem(
                            station = station,
                            isPlaying = currentStation?.id == station.id && playbackStatus == PlaybackStatus.PLAYING,
                            isBuffering = currentStation?.id == station.id && playbackStatus == PlaybackStatus.BUFFERING,
                            onSelect = onStationSelect,
                            onToggleFavorite = onToggleFavorite,
                            onDelete = onDeleteStation,
                            onShare = onShare
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RadioBrowserStationItem(
    station: RadioStation,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isSaved: Boolean,
    onSelect: (RadioStation) -> Unit,
    onSave: () -> Unit,
    onShare: (RadioStation) -> Unit
) {
    val iconScale = LocalIconScale.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(station) }
            .testTag("radio_browser_item_${station.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isPlaying) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                StationLogo(
                    stationName = station.name,
                    stationUrl = station.url,
                    faviconUrl = station.faviconUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                if (isPlaying && !isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniVisualizer(color = Color.White)
                    }
                } else if (isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (station.genre.isNotBlank()) station.genre else "Global Radio",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (station.country.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                        Text(
                            text = station.country,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = { onShare(station) },
                modifier = Modifier
                    .testTag("share_browser_button_${station.id}")
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir radio",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp * iconScale)
                )
            }

            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .testTag("save_browser_button_${station.id}")
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isSaved) "Guardada" else "Guardar radio",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp * iconScale)
                )
            }
        }
    }
}

@Composable
fun HistoryItem(
    historyItem: PlaybackHistory,
    onSelect: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(historyItem.timestamp) { dateFormat.format(Date(historyItem.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("history_item_${historyItem.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                StationLogo(
                    stationName = historyItem.stationName,
                    stationUrl = "",
                    faviconUrl = "",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyItem.stationName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
