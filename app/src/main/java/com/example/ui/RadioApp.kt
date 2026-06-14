package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RadioStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioApp(viewModel: RadioViewModel) {
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val playbackStatus by viewModel.playbackStatus.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val sleepSecondsLeft by viewModel.sleepSecondsLeft.collectAsStateWithLifecycle()

    val filteredStations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val genresList by viewModel.genresList.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD0BCFF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "OpenRadio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.5).sp,
                                color = Color(0xFFE6E1E5)
                            )
                        )
                    }
                },
                actions = {
                    // Sleep Timer Action Button
                    IconButton(
                        onClick = { showSleepTimerMenu = true },
                        modifier = Modifier
                            .testTag("sleep_timer_top_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        BadgedBox(
                            badge = {
                                if (sleepSecondsLeft != null) {
                                    Badge(
                                        containerColor = Color(0xFFD0BCFF),
                                        contentColor = Color(0xFF381E72)
                                    ) {
                                        Text(formatTimeLeft(sleepSecondsLeft), fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (sleepSecondsLeft != null) Icons.Filled.Timer else Icons.Outlined.Timer,
                                contentDescription = "Temporizador de apagado",
                                tint = Color(0xFFCAC4D0)
                            )
                        }
                    }

                    // Profile avatar representer from HTML mockup
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp, start = 4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF49454F))
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1B1F),
                    titleContentColor = Color(0xFFE6E1E5)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("add_radio_fab")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar radio",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            // Elegant M3-style bar conforming to Sophisticated Dark Palette
            NavigationBar(
                containerColor = Color(0xFF211F26),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(80.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                var activeTab by remember { mutableStateOf(0) }
                val navItems = listOf(
                    Triple(Icons.Filled.Home, Icons.Outlined.Home, "Home"),
                    Triple(Icons.Filled.Explore, Icons.Outlined.Explore, "Discover"),
                    Triple(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "Library"),
                    Triple(Icons.Filled.Sync, Icons.Outlined.Sync, "Sync")
                )
                navItems.forEachIndexed { index, item ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        icon = {
                            Icon(
                                if (isSelected) item.first else item.second,
                                contentDescription = item.third,
                                tint = if (isSelected) Color(0xFF381E72) else Color(0xFFCAC4D0)
                            )
                        },
                        label = {
                            Text(
                                item.third,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFE6E1E5) else Color(0xFFCAC4D0)
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFE8DEF8)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF1C1B1F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1C1B1F))
        ) {
            // Main Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. PLAYBACK DASHBOARD (Header Player Panel)
                PlaybackDashboard(
                    currentStation = currentStation,
                    playbackStatus = playbackStatus,
                    volume = volume,
                    errorMessage = errorMessage,
                    sleepSecondsLeft = sleepSecondsLeft,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onStopPlayback = { viewModel.stopPlayback() },
                    onVolumeChange = { viewModel.setVolume(it) },
                    onCancelSleepTimer = { viewModel.cancelSleepTimer() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. SEARCH & GENRE FILTER row
                SearchAndFilterSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    genres = genresList,
                    selectedGenre = selectedGenre,
                    onGenreSelect = { viewModel.updateGenreFilter(it) },
                    focusManager = focusManager
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. LISTENERS / STATIONS LIST TITLE
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR RADIOS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCAC4D0),
                            letterSpacing = 1.5.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF313033), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SYNCED WITH GITHUB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF938F99),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. STATIONS LIST
                StationsList(
                    filteredStations = filteredStations,
                    currentStation = currentStation,
                    playbackStatus = playbackStatus,
                    onStationSelect = { viewModel.playStation(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDeleteStation = { viewModel.deleteStation(it) }
                )
            }
        }

        // Add Custom Station Dialog Sheet
        if (showAddDialog) {
            AddStationDialog(
                onDismiss = { showAddDialog = false },
                onAddStation = { name, url, genre ->
                    viewModel.addCustomStation(name, url, genre)
                    showAddDialog = false
                }
            )
        }

        // Sleep Timer Action Bottom Sheet / Simple Dialog
        if (showSleepTimerMenu) {
            SleepTimerDialog(
                currentSecondsLeft = sleepSecondsLeft,
                onDismiss = { showSleepTimerMenu = false },
                onSelectMinutes = { minutes ->
                    if (minutes == 0) {
                        viewModel.cancelSleepTimer()
                    } else {
                        viewModel.startSleepTimer(minutes)
                    }
                    showSleepTimerMenu = false
                }
            )
        }
    }
}

@Composable
fun PlaybackDashboard(
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    volume: Float,
    errorMessage: String?,
    sleepSecondsLeft: Int?,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onCancelSleepTimer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("playback_dashboard_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4A4458), Color(0xFF211F26))
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
                            .background(Color(0xFFD0BCFF).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFFD0BCFF)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "SINTONIZAR RADIO",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Selecciona una estación de la lista para escuchar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFCAC4D0)
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Active player layout
                val isPlaying = playbackStatus == PlaybackStatus.PLAYING
                
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
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD0BCFF), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "ON AIR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF381E72),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Sleep indicator inside card if set
                    if (sleepSecondsLeft != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF313033).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable { onCancelSleepTimer() }
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFD0BCFF)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatTimeLeft(sleepSecondsLeft),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD0BCFF)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancelar temporizador",
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFCAC4D0)
                            )
                        }
                    }

                    // Stop button
                    IconButton(
                        onClick = onStopPlayback,
                        modifier = Modifier
                            .testTag("stop_playback_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Detener",
                            tint = Color(0xFFCAC4D0)
                        )
                    }
                }

                // Middle Station Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Rotating Disc
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .rotate(animatedAngle)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFD0BCFF),
                                        Color(0xFF381E72),
                                        Color(0xFFD0BCFF)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Center inner label
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF211F26)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFD0BCFF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center Metadata Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStation.genre.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFD0BCFF),
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (currentStation.isCustom) "Custom Stream Link" else "Original live link",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCAC4D0)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive procedural live visualizer
                AudioVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = 4.dp),
                    color = Color(0xFFD0BCFF)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error Message View
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Controls row matching HTML styling
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Buffer controls cluster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(28.dp).clickable { /* Placeholder navigation */ }
                        )

                        when (playbackStatus) {
                            PlaybackStatus.BUFFERING -> {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFD0BCFF), shape = RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = Color(0xFF381E72)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("play_pause_button")
                                        .background(Color(0xFFD0BCFF), shape = RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onTogglePlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                        tint = Color(0xFF381E72),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(28.dp).clickable { /* Placeholder navigation */ }
                        )
                    }

                    // Format Badge Indicator from HTML layout
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
                                color = Color(0xFFD0BCFF),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Volume slider matching the layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeDown,
                        contentDescription = "Bajar volumen",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("volume_slider"),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0xFF49454F),
                            thumbColor = Color(0xFFD0BCFF)
                        )
                    )
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Subir volumen",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    genres: List<String>,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Search Input styled with Sophisticated Dark specifications
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_stations_input"),
            placeholder = { Text("Search stations, moods, genres...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFCAC4D0)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar consulta", tint = Color(0xFFCAC4D0))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2B2930),
                unfocusedContainerColor = Color(0xFF2B2930),
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F).copy(alpha = 0.5f),
                focusedLabelColor = Color(0xFFD0BCFF),
                unfocusedLabelColor = Color(0xFFCAC4D0),
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFE6E1E5)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Genre Categories Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onGenreSelect(genre)
                        focusManager.clearFocus()
                    },
                    label = { Text(genre, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("genre_chip_$genre"),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF211F26),
                        labelColor = Color(0xFFCAC4D0),
                        selectedContainerColor = Color(0xFFD0BCFF),
                        selectedLabelColor = Color(0xFF381E72)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF49454F).copy(alpha = 0.4f),
                        selectedBorderColor = Color.Transparent
                    )
                )
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
    onDeleteStation: (RadioStation) -> Unit
) {
    if (filteredStations.isEmpty()) {
        // Empty placeholder state
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp)
                .testTag("empty_stations_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    tint = Color(0xFFCAC4D0).copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No se encontraron radios",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6E1E5).copy(alpha = 0.7f)
                    )
                )
                Text(
                    "Agrega una radio nueva o limpia los filtros",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFCAC4D0).copy(alpha = 0.5f)
                    )
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stations_lazy_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // Generous bottom offset to prevent overlap with navbar
        ) {
            items(filteredStations) { station ->
                val isPlaying = currentStation?.id == station.id
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStationSelect(station) }
                        .testTag("station_card_${station.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) Color(0xFF2B2930) else Color(0xFF2B2930).copy(alpha = 0.3f)
                    ),
                    border = if (isPlaying) {
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radio circle thumbnail representing HTML mockup avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPlaying) Color(0xFF4A4458) else Color(0xFF313033)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying && playbackStatus == PlaybackStatus.PLAYING) {
                                // Mini jumping equalizer
                                MiniVisualizer(color = Color(0xFFD0BCFF))
                            } else {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlaying) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text core infos
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1E5)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = station.genre,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCAC4D0)
                                )
                            )
                        }

                        // Right action buttons: Heart (Favorite) & Delete (If custom)
                        IconButton(
                            onClick = { onToggleFavorite(station) },
                            modifier = Modifier
                                .testTag("favorite_button_${station.id}")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (station.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (station.isFavorite) Color(0xFFF2B8B5) else Color(0xFF938F99)
                            )
                        }

                        if (station.isCustom) {
                            IconButton(
                                onClick = { onDeleteStation(station) },
                                modifier = Modifier
                                    .testTag("delete_button_${station.id}")
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar radio",
                                    tint = Color(0xFFF2B8B5).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniVisualizer(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_vis")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier.size(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(h1).background(color).clip(RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(h2).background(color).clip(RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(h3).background(color).clip(RoundedCornerShape(1.dp)))
    }
}

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    // We create multiple offset animators to drive each bar
    val animations = (0 until barCount).map { index ->
        val duration = remember(index) { (350..750).random() }
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalSpacingUnit = barCount * 2 - 1
        val barWidth = width / totalSpacingUnit

        for (i in 0 until barCount) {
            val progress = if (isPlaying) animations[i].value else 0.08f
            val barHeight = height * progress
            val x = i * barWidth * 2
            val y = height - barHeight
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@Composable
fun AddStationDialog(
    onDismiss: () -> Unit,
    onAddStation: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }

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
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Agregar Estación",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input
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

                // URL Input
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

                // Genre Input
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
                                onAddStation(name, url, genre)
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
                
                Text(
                    text = if (currentSecondsLeft != null) {
                        "Temporizador activo. Apagando en: ${formatTimeLeft(currentSecondsLeft)}"
                    } else {
                        "La radio se apagará automáticamente después del tiempo seleccionado."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
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

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
