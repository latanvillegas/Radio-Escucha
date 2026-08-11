package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PlaybackHistory
import com.example.data.RadioStation
import com.example.ui.PlaybackStatus
import com.example.ui.RadioViewModel
import com.example.ui.theme.LocalIconScale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun NavigationTabSwitcher(
    activeTab: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    genresList: List<String>,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
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
    val density = LocalDensity.current
    val searchSectionHeight = 120.dp
    val headerSectionHeight = 40.dp
    val bottomBarHeight = 80.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        when (activeTab) {
            0 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    StationsList(
                        filteredStations = filteredStations,
                        currentStation = currentStation,
                        playbackStatus = playbackStatus,
                        onStationSelect = { viewModel.playStation(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteStation = { viewModel.deleteStation(it) },
                        onShare = onShare,
                        modifier = Modifier.padding(top = searchSectionHeight + headerSectionHeight + with(density) { topBarOffsetHeightPx.coerceIn(-with(density) { searchSectionHeight.toPx() }, 0f).toDp() }),
                        contentPadding = PaddingValues(bottom = with(density) { (bottomBarHeight.toPx() - bottomBarOffsetHeightPx).toDp() })
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Collapsible Search Section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(searchSectionHeight + with(density) { topBarOffsetHeightPx.coerceIn(-with(density) { searchSectionHeight.toPx() }, 0f).toDp() })
                                .clipToBounds()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(0, topBarOffsetHeightPx.coerceIn(-with(density) { searchSectionHeight.toPx() }, 0f).roundToInt()) }
                            ) {
                                SearchAndFilterSection(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = onSearchQueryChange,
                                    genres = genresList,
                                    selectedGenre = selectedGenre,
                                    onGenreSelect = onGenreSelect,
                                    focusManager = focusManager,
                                    suggestions = viewModel.homeSuggestions.collectAsStateWithLifecycle().value,
                                    onSuggestionSelect = { suggestion -> viewModel.updateSearchQuery(suggestion.query) }
                                )
                            }
                        }

                        // Fixed "TUS RADIOS" Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerSectionHeight)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TUS RADIOS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.5.sp
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SINCRONIZADAS CON GITHUB",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                DiscoverTab(
                    viewModel = viewModel,
                    currentStation = currentStation,
                    playbackStatus = playbackStatus,
                    onShare = onShare,
                    bottomBarOffsetHeightPx = bottomBarOffsetHeightPx
                )
            }
            2 -> {
                val favorites = filteredStations.filter { it.isFavorite }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = with(density) { (80.dp.toPx() - bottomBarOffsetHeightPx).toDp() })
                ) {
                    // FAVORITOS Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MIS FAVORITOS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Sin favoritos",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val columns = if (maxWidth >= 480.dp) 2 else 1
                            if (columns > 1) {
                                val chunked = favorites.chunked(columns)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunked.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { station ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    StationItem(
                                                        station = station,
                                                        isPlaying = currentStation?.id == station.id && playbackStatus == PlaybackStatus.PLAYING,
                                                        isBuffering = currentStation?.id == station.id && playbackStatus == PlaybackStatus.BUFFERING,
                                                        onSelect = { viewModel.playStation(station) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(station) },
                                                        onDelete = { viewModel.deleteStation(station) },
                                                        onShare = onShare
                                                    )
                                                }
                                            }
                                            if (rowItems.size < columns) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    favorites.forEach { station ->
                                        StationItem(
                                            station = station,
                                            isPlaying = currentStation?.id == station.id && playbackStatus == PlaybackStatus.PLAYING,
                                            isBuffering = currentStation?.id == station.id && playbackStatus == PlaybackStatus.BUFFERING,
                                            onSelect = { viewModel.playStation(station) },
                                            onToggleFavorite = { viewModel.toggleFavorite(station) },
                                            onDelete = { viewModel.deleteStation(station) },
                                            onShare = onShare
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // HISTORIAL Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HISTORIAL",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        if (recentHistory.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text(
                                    "Limpiar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Historial vacío",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val columns = if (maxWidth >= 480.dp) 2 else 1
                            if (columns > 1) {
                                val chunked = recentHistory.chunked(columns)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunked.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { historyItem ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    HistoryItem(
                                                        historyItem = historyItem,
                                                        onSelect = {
                                                            val station = filteredStations.find { it.id.toLong() == historyItem.stationId }
                                                            if (station != null) {
                                                                viewModel.playStation(station)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            if (rowItems.size < columns) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recentHistory.forEach { historyItem ->
                                        HistoryItem(
                                            historyItem = historyItem,
                                            onSelect = {
                                                val station = filteredStations.find { it.id.toLong() == historyItem.stationId }
                                                if (station != null) {
                                                    viewModel.playStation(station)
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
            3 -> {
                SyncTab(bottomBarOffsetHeightPx = bottomBarOffsetHeightPx)
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
    val density = LocalDensity.current
    val context = LocalContext.current

    val selectedProvider by viewModel.selectedRadioProvider.collectAsStateWithLifecycle()
    val onlineStations by viewModel.radioBrowserStations.collectAsStateWithLifecycle()
    val pagedStations by viewModel.pagedDiscoverStations.collectAsStateWithLifecycle()
    val isLoading by viewModel.radioBrowserLoading.collectAsStateWithLifecycle()
    val errorMsg by viewModel.radioBrowserError.collectAsStateWithLifecycle()
    val searchQuery by viewModel.radioBrowserSearchQuery.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val onlineSuggestions by viewModel.onlineSuggestions.collectAsStateWithLifecycle()
    val localStations by viewModel.stations.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val canLoadMore by viewModel.canLoadMore.collectAsStateWithLifecycle()
    val userDetectedLoc by viewModel.userDetectedLocationInfo.collectAsStateWithLifecycle()
    val isDetectingIp by viewModel.isDetectingIpLocation.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val playbackStatus by viewModel.playbackStatus.collectAsStateWithLifecycle()

    var activeCategoryFilter by remember { mutableStateOf("Por Ubicación") }
    var showLocationDialog by remember { mutableStateOf(false) }

    if (showLocationDialog) {
        LocationSelectorDialog(
            currentLocation = userDetectedLoc,
            onDismiss = { showLocationDialog = false },
            onSelectLocation = { countryName, countryCode, stateName ->
                viewModel.setUserManualLocation(countryName, countryCode, stateName)
                activeCategoryFilter = "Por Ubicación"
            },
            onResetAutoIp = {
                viewModel.resetToAutoIpLocation()
                activeCategoryFilter = "Por Ubicación"
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = with(density) { (80.dp.toPx() - bottomBarOffsetHeightPx).toDp() })
    ) {
        // Compact Source Provider Chips & Location Chip
        val providerList = listOf(
            "Todas" to "Todas",
            "Radio-Browser" to "Radio-Browser",
            "FMStream" to "FMStream",
            "iHeartRadio" to "iHeartRadio",
            "SomaFM" to "SomaFM",
            "GitHub Raw" to "GitHub"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                val locText = userDetectedLoc
                    ?.replace("📍 ", "")
                    ?.replace("🌐 ", "")
                    ?.replace(" (IP)", "")
                    ?.replace(" (Locale)", "")
                    ?.replace(" (Seleccionado)", "")
                    ?.trim()
                    ?.ifBlank { "Ninguno" } ?: "Ninguno"
                val hasLocation = locText != "Ninguno" && locText != "Sin filtro"

                Surface(
                    onClick = { showLocationDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasLocation) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (hasLocation) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("location_selector_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            modifier = Modifier.size(15.dp),
                            tint = if (hasLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = locText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            items(providerList) { (key, title) ->
                val isSelected = selectedProvider == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setRadioProvider(key) },
                    label = { Text(title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("provider_chip_$key"),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var isOnlineSearchFocused by remember { mutableStateOf(false) }
        var hasSubmittedOnlineSearch by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                hasSubmittedOnlineSearch = false
                viewModel.searchRadioBrowser(query)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> isOnlineSearchFocused = focusState.isFocused }
                .testTag("radio_browser_search_input"),
            placeholder = { Text("Buscar por nombre, país, región, idioma o género...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        hasSubmittedOnlineSearch = true
                        viewModel.loadRadioBrowserTopVoted()
                        activeCategoryFilter = "Más Votadas"
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    hasSubmittedOnlineSearch = true
                    viewModel.searchRadioBrowser(searchQuery)
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        if (isOnlineSearchFocused && !hasSubmittedOnlineSearch && searchQuery.isNotBlank() && onlineSuggestions.isNotEmpty()) {
            SearchSuggestionsDropdown(
                suggestions = onlineSuggestions,
                onSuggestionSelected = { suggestion ->
                    hasSubmittedOnlineSearch = true
                    viewModel.searchRadioBrowser(suggestion.query)
                    focusManager.clearFocus()
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val categories = listOf(
            "Más Votadas" to { viewModel.loadRadioBrowserTopVoted() },
            "Más Escuchadas" to { viewModel.loadRadioBrowserTopClicked() },
            "Pop / Rock" to { viewModel.fetchRadioBrowserByTag("pop") },
            "Noticias" to { viewModel.fetchRadioBrowserByTag("news") },
            "Reggaeton / Urbana" to { viewModel.fetchRadioBrowserByTag("reggaeton") },
            "Salsa" to { viewModel.fetchRadioBrowserByTag("salsa") },
            "Cumbia" to { viewModel.fetchRadioBrowserByTag("cumbia") },
            "Bachata" to { viewModel.fetchRadioBrowserByTag("bachata") },
            "Merengue" to { viewModel.fetchRadioBrowserByTag("merengue") },
            "Vallenato" to { viewModel.fetchRadioBrowserByTag("vallenato") },
            "Ranchera / Mariachi" to { viewModel.fetchRadioBrowserByTag("ranchera") },
            "Folk" to { viewModel.fetchRadioBrowserByTag("folk") },
            "Baladas / Romántica" to { viewModel.fetchRadioBrowserByTag("romantic") },
            "Electrónica" to { viewModel.fetchRadioBrowserByTag("dance") },
            "Reggae" to { viewModel.fetchRadioBrowserByTag("reggae") },
            "Hip Hop / Rap" to { viewModel.fetchRadioBrowserByTag("hiphop") },
            "Clásica" to { viewModel.fetchRadioBrowserByTag("classical") },
            "Jazz" to { viewModel.fetchRadioBrowserByTag("jazz") },
            "Deportes" to { viewModel.fetchRadioBrowserByTag("sports") },
            "Cristiana / Religiosa" to { viewModel.fetchRadioBrowserByTag("christian") },
            "Español" to { viewModel.fetchRadioBrowserByLanguage("spanish") },
            "Inglés" to { viewModel.fetchRadioBrowserByLanguage("english") },
            "Portugués" to { viewModel.fetchRadioBrowserByLanguage("portuguese") },
            "Francés" to { viewModel.fetchRadioBrowserByLanguage("french") },
            "Alemán" to { viewModel.fetchRadioBrowserByLanguage("german") }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { (label, action) ->
                val isSelected = (activeCategoryFilter == label && searchQuery.isEmpty()) || (searchQuery.equals(label, ignoreCase = true))
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        activeCategoryFilter = label
                        action()
                    },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("radio_browser_chip_$label"),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) "RESULTADOS EN LÍNEA" else "ESTACIONES DESTACADAS ($activeCategoryFilter)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMsg != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.loadRadioBrowserTopVoted() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reintentar conexión")
                    }
                }
            }
        } else if (isLoading && onlineStations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Cargando radios en línea...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else if (onlineStations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No se encontraron resultados",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        "Intenta buscar otro nombre o género",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 480.dp) 2 else 1
                    if (columns > 1) {
                        val chunked = pagedStations.chunked(columns)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunked.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { station ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            val isAlreadySaved = localStations.any { it.url == station.url || it.name.equals(station.name, ignoreCase = true) }
                                            RadioBrowserStationItem(
                                                station = station,
                                                isPlaying = currentStation?.url == station.url && playbackStatus == PlaybackStatus.PLAYING,
                                                isBuffering = currentStation?.url == station.url && playbackStatus == PlaybackStatus.BUFFERING,
                                                isSaved = isAlreadySaved,
                                                onSelect = { viewModel.playStation(station) },
                                                onSave = {
                                                    viewModel.saveRadioBrowserStation(station)
                                                    android.widget.Toast.makeText(context, "${station.name} guardada en mis radios", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                onShare = onShare
                                            )
                                        }
                                    }
                                    if (rowItems.size < columns) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pagedStations.forEach { station ->
                                val isAlreadySaved = localStations.any { it.url == station.url || it.name.equals(station.name, ignoreCase = true) }
                                RadioBrowserStationItem(
                                    station = station,
                                    isPlaying = currentStation?.url == station.url && playbackStatus == PlaybackStatus.PLAYING,
                                    isBuffering = currentStation?.url == station.url && playbackStatus == PlaybackStatus.BUFFERING,
                                    isSaved = isAlreadySaved,
                                    onSelect = { viewModel.playStation(station) },
                                    onSave = {
                                        viewModel.saveRadioBrowserStation(station)
                                        android.widget.Toast.makeText(context, "${station.name} guardada en mis radios", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    onShare = onShare
                                )
                            }
                        }
                    }
                }

                val hasMoreChunks = onlineStations.size > pagedStations.size

                if (hasMoreChunks) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.loadMoreDiscoverChunk() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("load_more_radios_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mostrar más radios (mostrando ${pagedStations.size} de ${onlineStations.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncTab(bottomBarOffsetHeightPx: Float) {
    val iconScale = LocalIconScale.current
    val density = LocalDensity.current
    var isSyncing by remember { mutableStateOf(false) }
    var syncSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isSyncing) {
        if (isSyncing) {
            delay(1500)
            isSyncing = false
            syncSuccess = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = with(density) { (80.dp.toPx() - bottomBarOffsetHeightPx).toDp() }),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp * iconScale)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sincronización con GitHub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Conecta y sincroniza tus estaciones personalizadas, tus favoritos y el historial de reproducción de manera segura en tu cuenta de GitHub.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estado", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("Conectado (Mock)", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Último respaldo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(if (syncSuccess) "Hace unos segundos" else "Hace 2 horas", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repositorio", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("usuario/openradio-backup", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isSyncing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Button(
                        onClick = { isSyncing = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizar Ahora", fontWeight = FontWeight.Bold)
                    }
                }

                if (syncSuccess && !isSyncing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "¡Sincronización exitosa!",
                        color = Color.Green,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationSelectorDialog(
    currentLocation: String?,
    onDismiss: () -> Unit,
    onSelectLocation: (countryName: String, countryCode: String, stateName: String) -> Unit,
    onResetAutoIp: () -> Unit
) {
    var selectedCountry by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var selectedRegion by remember { mutableStateOf("") }
    var customSearch by remember { mutableStateOf("") }

    val countryList = listOf(
        // Ninguno / Global
        Triple("🌐 Ninguno (Sin filtro / Global)", "", "NONE"),

        // América Latina & Caribe
        Triple("🇵🇪 Perú", "Peru", "PE"),
        Triple("🇲🇽 México", "Mexico", "MX"),
        Triple("🇨🇴 Colombia", "Colombia", "CO"),
        Triple("🇦🇷 Argentina", "Argentina", "AR"),
        Triple("🇪🇸 España", "Spain", "ES"),
        Triple("🇨🇱 Chile", "Chile", "CL"),
        Triple("🇪🇨 Ecuador", "Ecuador", "EC"),
        Triple("🇻🇪 Venezuela", "Venezuela", "VE"),
        Triple("🇩🇴 Rep. Dominicana", "Dominican Republic", "DO"),
        Triple("🇧🇴 Bolivia", "Bolivia", "BO"),
        Triple("🇺🇾 Uruguay", "Uruguay", "UY"),
        Triple("🇵🇾 Paraguay", "Paraguay", "PY"),
        Triple("🇵🇷 Puerto Rico", "Puerto Rico", "PR"),
        Triple("🇨🇷 Costa Rica", "Costa Rica", "CR"),
        Triple("🇬🇹 Guatemala", "Guatemala", "GT"),
        Triple("🇭🇳 Honduras", "Honduras", "HN"),
        Triple("🇸🇻 El Salvador", "El Salvador", "SV"),
        Triple("🇳🇮 Nicaragua", "Nicaragua", "NI"),
        Triple("🇵🇦 Panamá", "Panama", "PA"),
        Triple("🇨🇺 Cuba", "Cuba", "CU"),
        Triple("🇭🇹 Haití", "Haiti", "HT"),
        Triple("🇯🇲 Jamaica", "Jamaica", "JM"),
        Triple("🇹🇹 Trinidad y Tobago", "Trinidad and Tobago", "TT"),

        // Norteamérica
        Triple("🇺🇸 EE.UU. (United States)", "United States", "US"),
        Triple("🇨🇦 Canadá", "Canada", "CA"),

        // Europa
        Triple("🇬🇧 Reino Unido (UK)", "United Kingdom", "GB"),
        Triple("🇫🇷 Francia", "France", "FR"),
        Triple("🇩🇪 Alemania", "Germany", "DE"),
        Triple("🇮🇹 Italia", "Italy", "IT"),
        Triple("🇵🇹 Portugal", "Portugal", "PT"),
        Triple("🇳🇱 Países Bajos", "Netherlands", "NL"),
        Triple("🇧🇪 Bélgica", "Belgium", "BE"),
        Triple("🇨🇭 Suiza", "Switzerland", "CH"),
        Triple("🇦🇹 Austria", "Austria", "AT"),
        Triple("🇸🇪 Suecia", "Sweden", "SE"),
        Triple("🇳🇴 Noruega", "Norway", "NO"),
        Triple("🇩🇰 Dinamarca", "Denmark", "DK"),
        Triple("🇫🇮 Finlandia", "Finland", "FI"),
        Triple("🇵🇱 Polonia", "Poland", "PL"),
        Triple("🇺🇦 Ucrania", "Ukraine", "UA"),
        Triple("🇷🇺 Rusia", "Russia", "RU"),
        Triple("🇷🇴 Rumanía", "Romania", "RO"),
        Triple("🇬🇷 Grecia", "Greece", "GR"),
        Triple("🇨🇿 Rep. Checa", "Czech Republic", "CZ"),
        Triple("🇭🇺 Hungría", "Hungary", "HU"),
        Triple("🇮🇪 Irlanda", "Ireland", "IE"),
        Triple("🇭🇷 Croacia", "Croatia", "HR"),
        Triple("🇸🇷 Serbia", "Serbia", "RS"),
        Triple("🇹🇷 Turquía", "Turkey", "TR"),
        Triple("🇧🇬 Bulgaria", "Bulgaria", "BG"),
        Triple("🇸🇰 Eslovaquia", "Slovakia", "SK"),
        Triple("🇸🇮 Eslovenia", "Slovenia", "SI"),

        // Asia & Oriente Medio
        Triple("🇯🇵 Japón", "Japan", "JP"),
        Triple("🇰🇷 Corea del Sur", "South Korea", "KR"),
        Triple("🇨🇳 China", "China", "CN"),
        Triple("🇹🇼 Taiwán", "Taiwan", "TW"),
        Triple("🇮🇳 India", "India", "IN"),
        Triple("🇮🇩 Indonesia", "Indonesia", "ID"),
        Triple("🇵🇭 Filipinas", "Philippines", "PH"),
        Triple("🇻🇳 Vietnam", "Vietnam", "VN"),
        Triple("🇹🇭 Tailandia", "Thailand", "TH"),
        Triple("🇲🇾 Malasia", "Malaysia", "MY"),
        Triple("🇸🇬 Singapur", "Singapore", "SG"),
        Triple("🇮🇱 Israel", "Israel", "IL"),
        Triple("🇦🇪 Emiratos Árabes", "United Arab Emirates", "AE"),
        Triple("🇸🇦 Arabia Saudita", "Saudi Arabia", "SA"),
        Triple("🇵🇰 Pakistán", "Pakistan", "PK"),
        Triple("🇧🇩 Bangladés", "Bangladesh", "BD"),

        // África
        Triple("🇪🇬 Egipto", "Egypt", "EG"),
        Triple("🇲🇦 Marruecos", "Morocco", "MA"),
        Triple("🇿🇦 Sudáfrica", "South Africa", "ZA"),
        Triple("🇳🇬 Nigeria", "Nigeria", "NG"),
        Triple("🇰🇪 Kenia", "Kenya", "KE"),
        Triple("🇩🇿 Argelia", "Algeria", "DZ"),
        Triple("🇹🇳 Túnez", "Tunisia", "TN"),
        Triple("🇸🇳 Senegal", "Senegal", "SN"),

        // Oceanía
        Triple("🇦🇺 Australia", "Australia", "AU"),
        Triple("🇳🇿 Nueva Zelanda", "New Zealand", "NZ")
    )

    val regionMap = mapOf(
        "PE" to listOf("Lima", "Arequipa", "Cusco", "La Libertad", "Piura", "Junín", "Puno", "Lambayeque", "Ica", "Ancash", "Tacna", "Loreto", "Cajamarca", "San Martín", "Ayacucho"),
        "MX" to listOf("CDMX", "Jalisco", "Nuevo León", "Puebla", "Guanajuato", "Veracruz", "Yucatán", "Chihuahua", "Baja California", "Edomex", "Querétaro"),
        "CO" to listOf("Bogotá", "Antioquia", "Valle del Cauca", "Atlántico", "Santander", "Cundinamarca", "Bolívar", "Risaralda", "Caldas", "Nariño"),
        "AR" to listOf("Buenos Aires", "Córdoba", "Santa Fe", "Mendoza", "Tucumán", "Salta", "Entre Ríos", "Misiones", "Chaco", "Neuquén"),
        "ES" to listOf("Madrid", "Cataluña", "Andalucía", "Comunidad Valenciana", "Galicia", "País Vasco", "Canarias", "Castilla y León", "Murcia"),
        "CL" to listOf("Santiago / Región Metropolitana", "Valparaíso", "Bío-Bío", "Antofagasta", "Araucanía", "Coquimbo", "Los Lagos"),
        "EC" to listOf("Pichincha (Quito)", "Guayas (Guayaquil)", "Azuay (Cuenca)", "Manabí", "Tungurahua", "Loja", "El Oro"),
        "VE" to listOf("Caracas / Distrito Capital", "Zulia", "Carabobo", "Lara", "Aragua", "Anzoátegui", "Bolívar"),
        "US" to listOf("California", "Florida", "Texas", "New York", "Illinois", "Georgia", "New Jersey", "North Carolina"),
        "BR" to listOf("São Paulo", "Rio de Janeiro", "Minas Gerais", "Bahia", "Paraná", "Rio Grande do Sul"),
        "DO" to listOf("Santo Domingo", "Santiago", "La Altagracia", "Puerto Plata"),
        "BO" to listOf("La Paz", "Santa Cruz", "Cochabamba", "Tarija", "Potosí", "Chuquisaca"),
        "UY" to listOf("Montevideo", "Maldonado", "Canelones", "Colonia"),
        "PR" to listOf("San Juan", "Ponce", "Mayagüez", "Bayamón"),
        "CR" to listOf("San José", "Alajuela", "Cartago", "Heredia", "Puntarenas", "Guanacaste"),
        "GT" to listOf("Guatemala", "Quetzaltenango", "Escuintla", "Alta Verapaz"),
        "JP" to listOf("Tokyo", "Osaka", "Kanagawa", "Aichi", "Hokkaido", "Kyoto", "Fukuoka")
    )

    val filteredCountries = if (customSearch.isBlank()) countryList else countryList.filter {
        it.first.contains(customSearch, ignoreCase = true) ||
        it.second.contains(customSearch, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Elegir País y Región", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedCountry != null) {
                    val country = selectedCountry!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("País seleccionado:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Text(country.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            TextButton(onClick = {
                                selectedCountry = null
                                selectedRegion = ""
                            }) {
                                Text("Cambiar País", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Región / Departamento (Opcional):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                    val regions = regionMap[country.third] ?: emptyList()
                    if (regions.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedRegion.isBlank(),
                                    onClick = { selectedRegion = "" },
                                    label = { Text("Todas") }
                                )
                            }
                            items(regions) { r ->
                                FilterChip(
                                    selected = selectedRegion.equals(r, ignoreCase = true),
                                    onClick = { selectedRegion = if (selectedRegion.equals(r, ignoreCase = true)) "" else r },
                                    label = { Text(r) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedRegion,
                        onValueChange = { selectedRegion = it },
                        placeholder = { Text("O escribe cualquier región/ciudad...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            onSelectLocation(country.second, country.third, selectedRegion.trim())
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (selectedRegion.isNotBlank()) "Ver radios en ${selectedRegion.trim()}, ${country.first}"
                            else "Ver radios en ${country.first}"
                        )
                    }
                } else {
                    Text(
                        text = "Selecciona un país para ver sus regiones y emisoras locales:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customSearch,
                        onValueChange = { customSearch = it },
                        placeholder = { Text("Buscar país o región (ej. Perú, Arequipa)...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredCountries) { triple ->
                            Surface(
                                onClick = {
                                    if (triple.third == "NONE") {
                                        onSelectLocation("", "", "")
                                        onDismiss()
                                    } else {
                                        selectedCountry = triple
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = triple.first,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (triple.third == "NONE") "ALL" else triple.third,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (customSearch.isNotBlank() && filteredCountries.isEmpty()) {
                            item {
                                Button(
                                    onClick = {
                                        onSelectLocation(customSearch.trim(), "", customSearch.trim())
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Buscar radios en '$customSearch'")
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSelectLocation("", "", "")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ninguno (Sin filtro)", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            onResetAutoIp()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto por IP", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
