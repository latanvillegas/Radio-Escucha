package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
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

    var activeCategoryFilter by remember { mutableStateOf("Más Votadas") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = with(density) { (80.dp.toPx() - bottomBarOffsetHeightPx).toDp() })
    ) {
        // Compact Source Provider Chips
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
            "Por Ubicación" to { viewModel.fetchRadioBrowserByDeviceLocation() },
            "Pop / Rock" to { viewModel.fetchRadioBrowserByTag("pop") },
            "Noticias" to { viewModel.fetchRadioBrowserByTag("news") },
            "Salsa" to { viewModel.fetchRadioBrowserByTag("salsa") },
            "Cumbia" to { viewModel.fetchRadioBrowserByTag("cumbia") },
            "Deportes" to { viewModel.fetchRadioBrowserByTag("sports") },
            "Jazz" to { viewModel.fetchRadioBrowserByTag("jazz") },
            "Español" to { viewModel.fetchRadioBrowserByLanguage("spanish") },
            "Inglés" to { viewModel.fetchRadioBrowserByLanguage("english") },
            "Portugués" to { viewModel.fetchRadioBrowserByLanguage("portuguese") },
            "Francés" to { viewModel.fetchRadioBrowserByLanguage("french") },
            "Alemán" to { viewModel.fetchRadioBrowserByLanguage("german") },
            "Perú" to { viewModel.fetchRadioBrowserByCountry("Peru") },
            "México" to { viewModel.fetchRadioBrowserByCountry("Mexico") },
            "España" to { viewModel.fetchRadioBrowserByCountry("Spain") },
            "EE.UU." to { viewModel.fetchRadioBrowserByCountry("United States") },
            "Argentina" to { viewModel.fetchRadioBrowserByCountry("Argentina") },
            "Chile" to { viewModel.fetchRadioBrowserByCountry("Chile") },
            "Colombia" to { viewModel.fetchRadioBrowserByCountry("Colombia") },
            "California" to { viewModel.fetchRadioBrowserByState("California") },
            "Jalisco" to { viewModel.fetchRadioBrowserByState("Jalisco") },
            "Baviera" to { viewModel.fetchRadioBrowserByState("Bavaria") },
            "Antioquia" to { viewModel.fetchRadioBrowserByState("Antioquia") }
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
