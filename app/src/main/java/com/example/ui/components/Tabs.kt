package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaybackHistory
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.ui.RadioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NavigationTabSwitcher(
    activeTab: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    genresList: List<String>,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    focusManager: FocusManager,
    filteredStations: List<RadioStation>,
    recentHistory: List<PlaybackHistory>,
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    viewModel: RadioViewModel,
    onActiveTabChange: (Int) -> Unit,
    onShare: (RadioStation) -> Unit,
    topBarOffsetHeightPx: Float,
    bottomBarOffsetHeightPx: Float,
    nestedScrollConnection: NestedScrollConnection
) {
    when (activeTab) {
        0 -> HomeTab(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            genresList = genresList,
            selectedGenre = selectedGenre,
            onGenreSelect = onGenreSelect,
            filteredStations = filteredStations,
            currentStation = currentStation,
            playbackStatus = playbackStatus,
            viewModel = viewModel,
            onShare = onShare,
            nestedScrollConnection = nestedScrollConnection
        )
        1 -> DiscoverTab(
            viewModel = viewModel,
            currentStation = currentStation,
            playbackStatus = playbackStatus,
            onShare = onShare,
            bottomBarOffsetHeightPx = bottomBarOffsetHeightPx
        )
        2 -> LibraryTab(
            allStations = filteredStations,
            recentHistory = recentHistory,
            currentStation = currentStation,
            playbackStatus = playbackStatus,
            viewModel = viewModel,
            onShare = onShare
        )
        3 -> SyncTab(
            bottomBarOffsetHeightPx = bottomBarOffsetHeightPx
        )
    }
}

@Composable
fun HomeTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    genresList: List<String>,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    filteredStations: List<RadioStation>,
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    viewModel: RadioViewModel,
    onShare: (RadioStation) -> Unit,
    nestedScrollConnection: NestedScrollConnection
) {
    val homeSuggestions by viewModel.homeSuggestions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Buscar por nombre, país o género...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("home_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        )

        // Suggestions
        SearchSuggestionsDropdown(
            suggestions = homeSuggestions,
            onSuggestionSelected = { suggestion ->
                onSearchQueryChange(suggestion.query)
            }
        )

        // Genres horizontal filter
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genresList) { genre ->
                val isSelected = genre == selectedGenre
                FilterChip(
                    selected = isSelected,
                    onClick = { onGenreSelect(genre) },
                    label = { Text(genre) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredStations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No se encontraron emisoras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prueba con otra búsqueda o selecciona 'Todas' en géneros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = filteredStations,
                    key = { it.id }
                ) { station ->
                    val isPlaying = currentStation?.id == station.id && playbackStatus == PlaybackStatus.PLAYING
                    val isBuffering = currentStation?.id == station.id && playbackStatus == PlaybackStatus.BUFFERING

                    StationItem(
                        station = station,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onSelect = { viewModel.playStation(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDelete = { viewModel.deleteStation(it) },
                        onShare = onShare
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverTab(
    viewModel: RadioViewModel,
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    onShare: (RadioStation) -> Unit,
    bottomBarOffsetHeightPx: Float
) {
    val onlineStations by viewModel.radioBrowserStations.collectAsState()
    val isLoading by viewModel.radioBrowserLoading.collectAsState()
    val errorMsg by viewModel.radioBrowserError.collectAsState()
    val searchQuery by viewModel.radioBrowserSearchQuery.collectAsState()
    val userCountry by viewModel.userLocationCountry.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchRadioBrowser(it) },
            placeholder = { Text("Buscar en miles de radios online...") },
            leadingIcon = {
                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchRadioBrowser("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        )

        // Quick Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                AssistChip(
                    onClick = { viewModel.loadRadioBrowserTopVoted() },
                    label = { Text("Más Votadas") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.loadRadioBrowserTopClicked() },
                    label = { Text("Populares") },
                    leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { showLocationDialog = true },
                    label = { Text("País: $userCountry") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.fetchRadioBrowserByTag("rock") },
                    label = { Text("Rock") }
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.fetchRadioBrowserByTag("news") },
                    label = { Text("Noticias") }
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.fetchRadioBrowserByTag("pop") },
                    label = { Text("Pop") }
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMsg != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = errorMsg ?: "Error de conexión", color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadRadioBrowserTopVoted() }) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = onlineStations,
                    key = { it.url }
                ) { station ->
                    val isPlaying = currentStation?.url == station.url && playbackStatus == PlaybackStatus.PLAYING
                    val isBuffering = currentStation?.url == station.url && playbackStatus == PlaybackStatus.BUFFERING

                    RadioBrowserStationItem(
                        station = station,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        isSaved = false,
                        onSelect = { viewModel.playStation(it) },
                        onSave = { viewModel.saveRadioBrowserStation(station) },
                        onShare = onShare
                    )
                }
            }
        }
    }

    if (showLocationDialog) {
        LocationSelectorDialog(
            currentLocation = userCountry,
            onDismiss = { showLocationDialog = false },
            onSelectLocation = { country, code, city ->
                showLocationDialog = false
                viewModel.setUserManualLocation(country, code, "", city)
            },
            onResetAutoIp = {
                showLocationDialog = false
                viewModel.resetToAutoIpLocation()
            }
        )
    }
}

@Composable
fun LibraryTab(
    allStations: List<RadioStation>,
    recentHistory: List<PlaybackHistory>,
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    viewModel: RadioViewModel,
    onShare: (RadioStation) -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) }
    val favorites = remember(allStations) { allStations.filter { it.isFavorite } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section selector
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("Favoritos (${favorites.size})") },
                icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("Historial (${recentHistory.size})") },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedSection == 0) {
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No tienes emisoras favoritas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toca el corazón en cualquier emisora para guardarla aquí.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(favorites, key = { it.id }) { station ->
                        val isPlaying = currentStation?.id == station.id && playbackStatus == PlaybackStatus.PLAYING
                        val isBuffering = currentStation?.id == station.id && playbackStatus == PlaybackStatus.BUFFERING

                        StationItem(
                            station = station,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            onSelect = { viewModel.playStation(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDelete = { viewModel.deleteStation(it) },
                            onShare = onShare
                        )
                    }
                }
            }
        } else {
            // Historial
            if (recentHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "El historial de reproducción está vacío.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Borrar Historial")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(recentHistory, key = { it.id }) { item ->
                            HistoryItem(
                                historyItem = item,
                                onSelect = {
                                    val matched = allStations.find { s -> s.id.toLong() == item.stationId }
                                    if (matched != null) {
                                        viewModel.playStation(matched)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncTab(
    bottomBarOffsetHeightPx: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Almacenamiento Local Seguro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Tus estaciones personalizadas, favoritos e historial se guardan localmente en tu dispositivo mediante SQLite Room.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Puedes hacer copias de seguridad de tus favoritos exportándolos a un archivo JSON desde Ajustes en cualquier momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LocationSelectorDialog(
    currentLocation: String,
    onDismiss: () -> Unit,
    onSelectLocation: (country: String, code: String, city: String) -> Unit,
    onResetAutoIp: () -> Unit
) {
    val countries = listOf(
        Triple("España", "ES", "Madrid"),
        Triple("México", "MX", "Ciudad de México"),
        Triple("Argentina", "AR", "Buenos Aires"),
        Triple("Colombia", "CO", "Bogotá"),
        Triple("Perú", "PE", "Lima"),
        Triple("Chile", "CL", "Santiago"),
        Triple("Venezuela", "VE", "Caracas"),
        Triple("Guatemala", "GT", "Ciudad de Guatemala"),
        Triple("Costa Rica", "CR", "San José"),
        Triple("Estados Unidos", "US", "Miami"),
        Triple("Francia", "FR", "París"),
        Triple("Alemania", "DE", "Berlín")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Ubicación", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = onResetAutoIp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Detectar automáticamente por red")
                    }
                }

                HorizontalDivider()

                countries.forEach { (country, code, city) ->
                    val isSelected = country == currentLocation
                    Surface(
                        onClick = { onSelectLocation(country, code, city) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(country, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
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
