package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.RadioStation
import com.example.ui.components.*
import com.example.ui.theme.LocalIconScale
import kotlin.math.roundToInt

private fun formatTimeLeft(seconds: Int?): String {
    if (seconds == null) return "00:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioApp(
    viewModel: RadioViewModel,
    onNavigateToSettings: () -> Unit
) {
    val iconScale = LocalIconScale.current
    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsStateWithLifecycle()
    val currentTrackArtist by viewModel.currentTrackArtist.collectAsStateWithLifecycle()
    val currentTrackArtworkUrl by viewModel.currentTrackArtworkUrl.collectAsStateWithLifecycle()
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

    val context = LocalContext.current

    val shareStation: (RadioStation) -> Unit = { station ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Escuchando ${station.name}")
            putExtra(android.content.Intent.EXTRA_TEXT, "¡Hola! Estoy escuchando ${station.name} (${station.genre}) en la app Radio App. Sintonízala aquí: ${station.url}")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartir Radio"))
    }

    val uriHandler = LocalUriHandler.current
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }

    // Handle system back button to return to first tab or close modals
    BackHandler(enabled = activeTab != 0 || showAddOptionsDialog || showAddDialog || showSleepTimerMenu || showFullPlayer) {
        when {
            showFullPlayer -> showFullPlayer = false
            showAddOptionsDialog -> showAddOptionsDialog = false
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

                val newTopOffset = topBarOffsetHeightPx + delta
                topBarOffsetHeightPx = newTopOffset.coerceIn(-topBarHeightPx, 0f)

                val newBottomOffset = bottomBarOffsetHeightPx - delta
                bottomBarOffsetHeightPx = newBottomOffset.coerceIn(0f, bottomBarHeightPx)

                return Offset.Zero
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage, playbackStatus) {
        val msg = errorMessage
        if (msg != null && playbackStatus == PlaybackStatus.ERROR) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Reintentar",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentStation?.let { viewModel.playStation(it) }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        onClick = { showAddOptionsDialog = true },
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
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(bottomBarHeight)
                            .offset { IntOffset(0, bottomBarOffsetHeightPx.roundToInt()) }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1400.dp)
                    ) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(80.dp),
                            header = {
                                Spacer(modifier = Modifier.height(8.dp))
                                FloatingActionButton(
                                    onClick = { showAddOptionsDialog = true },
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

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.40f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Top
                            ) {
                                PlaybackDashboard(
                                    currentStation = currentStation,
                                    currentTrackTitle = currentTrackTitle,
                                    currentTrackArtist = currentTrackArtist,
                                    currentTrackArtworkUrl = currentTrackArtworkUrl,
                                    playbackStatus = playbackStatus,
                                    volume = volume,
                                    errorMessage = errorMessage,
                                    sleepSecondsLeft = sleepSecondsLeft,
                                    networkStatus = networkStatus,
                                    onTogglePlay = { viewModel.togglePlayPause() },
                                    onStopPlayback = { viewModel.stopPlayback() },
                                    onPreviousStation = { viewModel.playPreviousStation() },
                                    onNextStation = { viewModel.playNextStation() },
                                    onRandomStation = { viewModel.playRandomStation() },
                                    onVolumeChange = { viewModel.setVolume(it) },
                                    onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                                    onShare = shareStation,
                                    onOpenFullScreen = { showFullPlayer = true }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(0.60f)
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
                }
            } else {
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

                        PlaybackDashboard(
                            currentStation = currentStation,
                            currentTrackTitle = currentTrackTitle,
                            currentTrackArtist = currentTrackArtist,
                            currentTrackArtworkUrl = currentTrackArtworkUrl,
                            playbackStatus = playbackStatus,
                            volume = volume,
                            errorMessage = errorMessage,
                            sleepSecondsLeft = sleepSecondsLeft,
                            networkStatus = networkStatus,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onStopPlayback = { viewModel.stopPlayback() },
                            onPreviousStation = { viewModel.playPreviousStation() },
                            onNextStation = { viewModel.playNextStation() },
                            onRandomStation = { viewModel.playRandomStation() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                            onShare = shareStation,
                            onOpenFullScreen = { showFullPlayer = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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

            if (showAddOptionsDialog) {
                AddStationOptionsDialog(
                    onDismiss = { showAddOptionsDialog = false },
                    onAddLocal = { showAddDialog = true },
                    onOpenRadioBrowserAdd = { uriHandler.openUri("https://www.radio-browser.info/add") }
                )
            }

            if (showAddDialog) {
                AddStationDialog(
                    onDismiss = { showAddDialog = false },
                    onAddStation = { name, url, faviconUrl, genre, country, region, province, district ->
                        viewModel.addCustomStation(name, url, faviconUrl, genre, country, region, province, district)
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

            val activeStation = currentStation
            AnimatedVisibility(
                visible = showFullPlayer && activeStation != null,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 350)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                if (activeStation != null) {
                    FullPlayerScreen(
                        currentStation = activeStation,
                        currentTrackTitle = currentTrackTitle,
                        currentTrackArtist = currentTrackArtist,
                        currentTrackArtworkUrl = currentTrackArtworkUrl,
                        playbackStatus = playbackStatus,
                        volume = volume,
                        sleepSecondsLeft = sleepSecondsLeft,
                        networkStatus = networkStatus,
                        isFavorite = activeStation.isFavorite,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onStopPlayback = { viewModel.stopPlayback() },
                        onPreviousStation = { viewModel.playPreviousStation() },
                        onNextStation = { viewModel.playNextStation() },
                        onRandomStation = { viewModel.playRandomStation() },
                        onVolumeChange = { viewModel.setVolume(it) },
                        onOpenSleepTimer = { showSleepTimerMenu = true },
                        onShare = shareStation,
                        onDismiss = { showFullPlayer = false }
                    )
                }
            }
        }
    }
}
