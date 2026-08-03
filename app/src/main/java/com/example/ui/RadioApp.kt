package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.RadioStation
import com.example.ui.theme.LocalIconScale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioApp(
    viewModel: RadioViewModel,
    onNavigateToSettings: () -> Unit
) {
    val iconScale = LocalIconScale.current
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val playbackStatus by viewModel.playbackStatus.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val sleepSecondsLeft by viewModel.sleepSecondsLeft.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()

    val filteredStations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val genresList by viewModel.genresList.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    val shareStation: (RadioStation) -> Unit = { station ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Escuchando ${station.name}")
            putExtra(android.content.Intent.EXTRA_TEXT, "¡Hola! Estoy escuchando ${station.name} (${station.genre}) en la app Radio App. Sintonízala aquí: ${station.url}")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartir Radio"))
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }

    // Handle system back button to return to first tab or close modals
    BackHandler(enabled = activeTab != 0 || showAddDialog || showSleepTimerMenu) {
        when {
            showAddDialog -> showAddDialog = false
            showSleepTimerMenu -> showSleepTimerMenu = false
            activeTab != 0 -> activeTab = 0
        }
    }

    val density = LocalDensity.current
    val topBarHeight = 160.dp
    val topBarHeightPx = with(density) { topBarHeight.toPx() }
    var topBarOffsetHeightPx by remember { mutableStateOf(0f) }

    val bottomBarHeight = 80.dp
    val bottomBarHeightPx = with(density) { bottomBarHeight.toPx() }
    var bottomBarOffsetHeightPx by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                // Update top bar offset (hides when scrolling down, i.e., delta is negative)
                val newTopOffset = topBarOffsetHeightPx + delta
                topBarOffsetHeightPx = newTopOffset.coerceIn(-topBarHeightPx, 0f)

                // Update bottom bar offset (hides when scrolling down, i.e., delta is negative)
                // When scrolling down (delta < 0), we want the bottom bar to move DOWN (positive offset)
                val newBottomOffset = bottomBarOffsetHeightPx - delta
                bottomBarOffsetHeightPx = newBottomOffset.coerceIn(0f, bottomBarHeightPx)

                return Offset.Zero
            }
        }
    }

    val focusManager = LocalFocusManager.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_openradio_logo),
                                    contentDescription = "OpenRadio Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp * iconScale)
                                )
                            }
                            Text(
                                "OpenRadio",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onBackground
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
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(formatTimeLeft(sleepSecondsLeft), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    if (sleepSecondsLeft != null) Icons.Filled.Timer else Icons.Outlined.Timer,
                                    contentDescription = "Temporizador de apagado",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Profile avatar representer from HTML mockup
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp, start = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateToSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Ajustes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp * iconScale)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                if (!isTablet) {
                        FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("add_radio_fab")
                            .offset { IntOffset(0, bottomBarOffsetHeightPx.roundToInt()) }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar radio",
                            modifier = Modifier.size(28.dp * iconScale)
                        )
                    }
                }
            },
            bottomBar = {
                if (!isTablet) {
                    // Elegant M3-style bar conforming to Sophisticated Dark Palette
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(bottomBarHeight)
                            .offset { IntOffset(0, bottomBarOffsetHeightPx.roundToInt()) }
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val navItems = listOf(
                            Triple(Icons.Filled.Home, Icons.Outlined.Home, "Inicio"),
                            Triple(Icons.Filled.Explore, Icons.Outlined.Explore, "Descubrir"),
                            Triple(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "Biblioteca"),
                            Triple(Icons.Filled.Sync, Icons.Outlined.Sync, "Sincronizar")
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
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp * iconScale)
                                    )
                                },
                                label = {
                                    Text(
                                        item.third,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Left: Elegant M3 Navigation Rail on Tablet
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(80.dp),
                        header = {
                            Spacer(modifier = Modifier.height(8.dp))
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("add_radio_fab_rail")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Agregar radio",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    ) {
                        val navItems = listOf(
                            Triple(Icons.Filled.Home, Icons.Outlined.Home, "Inicio"),
                            Triple(Icons.Filled.Explore, Icons.Outlined.Explore, "Descubrir"),
                            Triple(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "Biblioteca"),
                            Triple(Icons.Filled.Sync, Icons.Outlined.Sync, "Sincronizar")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        navItems.forEachIndexed { index, item ->
                            val isSelected = activeTab == index
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { activeTab = index },
                                icon = {
                                    Icon(
                                        if (isSelected) item.first else item.second,
                                        contentDescription = item.third,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp * iconScale)
                                    )
                                },
                                label = {
                                    Text(
                                        item.third,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    // Right split rows
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: playback player controller
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            PlaybackDashboard(
                                currentStation = currentStation,
                                playbackStatus = playbackStatus,
                                volume = volume,
                                errorMessage = errorMessage,
                                sleepSecondsLeft = sleepSecondsLeft,
                                networkStatus = networkStatus,
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onStopPlayback = { viewModel.stopPlayback() },
                                onVolumeChange = { viewModel.setVolume(it) },
                                onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                                onShare = shareStation
                            )
                        }

                        // Right Column: Search filters and list scroll
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                        ) {
                            NavigationTabSwitcher(
                                activeTab = activeTab,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                genresList = genresList,
                                selectedGenre = selectedGenre,
                                onGenreSelect = { viewModel.updateGenreFilter(it) },
                                focusManager = focusManager,
                                filteredStations = filteredStations,
                                recentHistory = recentHistory,
                                currentStation = currentStation,
                                playbackStatus = playbackStatus,
                                viewModel = viewModel,
                                onActiveTabChange = { activeTab = it },
                                onShare = shareStation,
                                topBarOffsetHeightPx = topBarOffsetHeightPx,
                                bottomBarOffsetHeightPx = bottomBarOffsetHeightPx,
                                nestedScrollConnection = nestedScrollConnection
                            )
                        }
                    }
                }
            } else {
                // Mobile layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .background(MaterialTheme.colorScheme.background)
                ) {
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
                            networkStatus = networkStatus,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onStopPlayback = { viewModel.stopPlayback() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                            onShare = shareStation
                        )
 
                        Spacer(modifier = Modifier.height(16.dp))
 
                        // 2. SEARCH & LIST UI / TABS switch
                        NavigationTabSwitcher(
                            activeTab = activeTab,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            genresList = genresList,
                            selectedGenre = selectedGenre,
                            onGenreSelect = { viewModel.updateGenreFilter(it) },
                            focusManager = focusManager,
                            filteredStations = filteredStations,
                            recentHistory = recentHistory,
                            currentStation = currentStation,
                            playbackStatus = playbackStatus,
                            viewModel = viewModel,
                            onActiveTabChange = { activeTab = it },
                            onShare = shareStation,
                            topBarOffsetHeightPx = topBarOffsetHeightPx,
                            bottomBarOffsetHeightPx = bottomBarOffsetHeightPx,
                            nestedScrollConnection = nestedScrollConnection
                        )
                    }
                }
            }

            // Dialog wrappers
            if (showAddDialog) {
                AddStationDialog(
                    onDismiss = { showAddDialog = false },
                    onAddStation = { name, url, genre, country, region, province, district ->
                        viewModel.addCustomStation(name, url, genre, country, region, province, district)
                        showAddDialog = false
                    }
                )
            }

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
}

@Composable
fun PlaybackDashboard(
    currentStation: RadioStation?,
    playbackStatus: PlaybackStatus,
    volume: Float,
    errorMessage: String?,
    sleepSecondsLeft: Int?,
    networkStatus: com.example.util.ConnectivityObserver.Status,
    onTogglePlay: () -> Unit,
    onStopPlayback: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onShare: (RadioStation) -> Unit
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
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "AL AIRE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Connection Status Indicator
                    if (networkStatus == com.example.util.ConnectivityObserver.Status.MobileData) {
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

                    // Actions group: Share & Stop
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Share button
                        IconButton(
                            onClick = { currentStation?.let { onShare(it) } },
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.onPrimary,
                                        MaterialTheme.colorScheme.primary
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
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center Metadata Info
                    Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive procedural live visualizer
                AudioVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.primary
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp * iconScale).clickable { /* Placeholder navigation */ }
                        )

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
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("play_pause_button")
                                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onTogglePlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp * iconScale)
                                    )
                                }
                            }
                        }

                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp * iconScale).clickable { /* Placeholder navigation */ }
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
            placeholder = { Text("Buscar radios, categorías, géneros...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar consulta", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

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
            // Radio circle thumbnail representing HTML mockup avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && !isBuffering) {
                    // Mini jumping equalizer
                    MiniVisualizer(color = MaterialTheme.colorScheme.primary)
                } else if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
        // Empty placeholder state
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
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .testTag("stations_lazy_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = contentPadding
        ) {
            items(filteredStations) { station ->
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
    val transition = rememberInfiniteTransition(label = "visualizer")
    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalSpacingUnit = barCount * 2 - 1
        val barWidth = width / totalSpacingUnit

        for (i in 0 until barCount) {
            val factor = if (isPlaying) {
                val angle = (animationProgress * 2 * Math.PI) + (i * 1.5)
                val baseValue = Math.sin(angle) * 0.45 + 0.55
                val noise = Math.cos(angle * 1.7 + i) * 0.15
                (baseValue + noise).toFloat().coerceIn(0.1f, 1.0f)
            } else {
                0.08f
            }
            val barHeight = height * factor
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
    onAddStation: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
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

                Spacer(modifier = Modifier.height(10.dp))

                // Country & Region Input Row
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

                // Province & District Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("Provincia", fontSize = 12.sp) },
                        placeholder = { Text("ej: Lima") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_station_province_input")
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("Distrito", fontSize = 12.sp) },
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
                                onAddStation(name, url, genre, country, region, province, district)
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

                    OutlinedButton(
                        onClick = { showCustomPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("sleep_timer_btn_custom"),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
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
                    // Custom Picker
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
                        // Hours
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

                        // Minutes
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    recentHistory: List<com.example.data.PlaybackHistory>,
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
                                    focusManager = focusManager
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
                    // Non-scrolling list inside scrollable column (simplified representation)
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                    recentHistory.forEach { historyItem ->
                        HistoryItem(
                            historyItem = historyItem,
                            onSelect = {
                                // Find station by ID or just play from historical metadata if possible
                                // For now, we try to find it in the current list
                                val station = filteredStations.find { it.id.toLong() == historyItem.stationId }
                                if (station != null) {
                                    viewModel.playStation(station)
                                } else {
                                    // If not found (deleted custom station), we could potentially re-add it or just show error
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
    val iconScale = LocalIconScale.current
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val selectedProvider by viewModel.selectedRadioProvider.collectAsStateWithLifecycle()
    val onlineStations by viewModel.radioBrowserStations.collectAsStateWithLifecycle()
    val isLoading by viewModel.radioBrowserLoading.collectAsStateWithLifecycle()
    val errorMsg by viewModel.radioBrowserError.collectAsStateWithLifecycle()
    val searchQuery by viewModel.radioBrowserSearchQuery.collectAsStateWithLifecycle()
    val localStations by viewModel.stations.collectAsStateWithLifecycle()

    var activeCategoryFilter by remember { mutableStateOf("Más Votadas") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = with(density) { (80.dp.toPx() - bottomBarOffsetHeightPx).toDp() })
    ) {
        // Source Provider Selector Row
        Text(
            text = "FUENTE DE EXPLORACIÓN",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        val providerList = listOf(
            "Todas" to "Todas las fuentes (Búsqueda Unificada)",
            "Radio-Browser" to "Radio-Browser (+40k)",
            "iHeartRadio" to "iHeartRadio",
            "TuneIn" to "TuneIn Directory",
            "SomaFM" to "SomaFM (Indie/Ambient)",
            "GitHub Raw" to "GitHub Raw (Curadas)"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(providerList) { (key, title) ->
                val isSelected = selectedProvider == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setRadioProvider(key) },
                    label = { Text(title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    modifier = Modifier.testTag("provider_chip_$key"),
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-API Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (selectedProvider) {
                        "Todas" -> Icons.Default.Public
                        "iHeartRadio" -> Icons.Default.Favorite
                        "TuneIn" -> Icons.Default.Radio
                        "SomaFM" -> Icons.Default.GraphicEq
                        "GitHub Raw" -> Icons.Default.CloudDownload
                        else -> Icons.Default.Public
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val title = when (selectedProvider) {
                        "Todas" -> "Búsqueda Unificada (Todas las APIs)"
                        "iHeartRadio" -> "API iHeartRadio"
                        "TuneIn" -> "Directorio TuneIn"
                        "SomaFM" -> "SomaFM Independent Radio"
                        "GitHub Raw" -> "Listas Curadas (GitHub)"
                        else -> "Navegador Radio-Browser"
                    }
                    val subtitle = when (selectedProvider) {
                        "Todas" -> "Busca en tiempo real simultáneamente en Radio-Browser, iHeart, TuneIn, SomaFM y GitHub"
                        "iHeartRadio" -> "Emisoras internacionales premium y de alta calidad"
                        "TuneIn" -> "Catálogo global y presets por identificador"
                        "SomaFM" -> "Radio independiente sin anuncios, ambient, lounge y electrónica"
                        "GitHub Raw" -> "Lista estática actualizada sin caídas de servidor"
                        else -> "Explora +40,000 emisoras abiertas de todo el mundo"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Online Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                viewModel.searchRadioBrowser(query)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("radio_browser_search_input"),
            placeholder = { Text("Buscar radio por nombre o género en línea...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.loadRadioBrowserTopVoted(); activeCategoryFilter = "Más Votadas" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Categories / Quick Filters Row
        val categories = listOf(
            "Más Votadas" to { viewModel.loadRadioBrowserTopVoted() },
            "Más Escuchadas" to { viewModel.loadRadioBrowserTopClicked() },
            "Pop / Rock" to { viewModel.fetchRadioBrowserByTag("pop") },
            "Noticias" to { viewModel.fetchRadioBrowserByTag("news") },
            "Salsa" to { viewModel.fetchRadioBrowserByTag("salsa") },
            "Perú" to { viewModel.fetchRadioBrowserByCountry("Peru") },
            "México" to { viewModel.fetchRadioBrowserByCountry("Mexico") },
            "España" to { viewModel.fetchRadioBrowserByCountry("Spain") },
            "Argentina" to { viewModel.fetchRadioBrowserByCountry("Argentina") },
            "Chile" to { viewModel.fetchRadioBrowserByCountry("Chile") },
            "Colombia" to { viewModel.fetchRadioBrowserByCountry("Colombia") }
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

        // Results Section Header
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
                onlineStations.forEach { station ->
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && !isBuffering) {
                    MiniVisualizer(color = MaterialTheme.colorScheme.primary)
                } else if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
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
                        text = station.genre,
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
                            text = station.country + if (station.region.isNotBlank()) " (${station.region})" else "",
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

            // Action buttons: Share & Save
            IconButton(
                onClick = { onShare(station) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp * iconScale)
                )
            }

            IconButton(
                onClick = { if (!isSaved) onSave() },
                enabled = !isSaved,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    if (isSaved) Icons.Filled.CheckCircle else Icons.Outlined.BookmarkAdd,
                    contentDescription = if (isSaved) "Guardada en mis radios" else "Guardar en mis radios",
                    tint = if (isSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp * iconScale)
                )
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
fun HistoryItem(
    historyItem: com.example.data.PlaybackHistory,
    onSelect: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()) }
    val formattedDate = remember(historyItem.timestamp) { dateFormat.format(java.util.Date(historyItem.timestamp)) }

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
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyItem.stationName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Reproducir",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
