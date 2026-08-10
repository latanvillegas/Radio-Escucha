package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.text.Normalizer
import com.example.service.PlaybackService
import com.example.data.*
import com.example.util.ConnectivityObserver
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PlaybackStatus {
    IDLE, BUFFERING, PLAYING, PAUSED, ERROR
}

class RadioViewModel(
    application: Application,
    private val repository: RadioRepository
) : AndroidViewModel(application) {

    private val connectivityObserver = NetworkConnectivityObserver(application)
    val networkStatus = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectivityObserver.Status.Unavailable
        )

    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null


    // Exposed States
    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow<String?>(null)
    val currentTrackArtist: StateFlow<String?> = _currentTrackArtist.asStateFlow()

    private val _currentTrackArtworkUrl = MutableStateFlow<String?>(null)
    val currentTrackArtworkUrl: StateFlow<String?> = _currentTrackArtworkUrl.asStateFlow()

    private val _playbackStatus = MutableStateFlow(PlaybackStatus.IDLE)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Database Stations
    val stations: StateFlow<List<RadioStation>> = repository.allStations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentHistory: StateFlow<List<PlaybackHistory>> = repository.recentHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter and Search States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("Todas")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    // Filtered Stations list
    val filteredStations: StateFlow<List<RadioStation>> = combine(
        stations, _searchQuery, _selectedGenre
    ) { stationList, query, genre ->
        if (query.isBlank() && genre == "Todas") {
            return@combine stationList
        }
        val normalizedQuery = query.normalize()
        stationList.filter { s ->
            val matchesGenre = genre == "Todas" || s.genre == genre
            if (!matchesGenre) return@filter false
            
            if (normalizedQuery.isBlank()) return@filter true
            
            s.name.normalize().contains(normalizedQuery) ||
            s.genre.normalize().contains(normalizedQuery) ||
            s.country.normalize().contains(normalizedQuery) ||
            s.region.normalize().contains(normalizedQuery) ||
            s.province.normalize().contains(normalizedQuery) ||
            s.district.normalize().contains(normalizedQuery)
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun String.normalize(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
    }

    // Distinct Genres listing for filter chips
    val genresList: StateFlow<List<String>> = stations.map { stationList ->
        listOf("Todas") + stationList.map { it.genre }.distinct().sorted()
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Todas")
    )

    // Sleep Timer (Minutes and Countdown)
    private var sleepTimerJob: Job? = null
    private val _sleepSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepSecondsLeft: StateFlow<Int?> = _sleepSecondsLeft.asStateFlow()

    // Radio Provider selection ("Todas", "Radio-Browser", "iHeartRadio", "SomaFM", "GitHub Raw", "FMStream")
    private val _selectedRadioProvider = MutableStateFlow("Todas")
    val selectedRadioProvider: StateFlow<String> = _selectedRadioProvider.asStateFlow()

    // Radio Browser / Online Radio states
    sealed class RadioSearchMode {
        data class Query(val query: String) : RadioSearchMode()
        data class Tag(val tag: String) : RadioSearchMode()
        data class Country(val country: String) : RadioSearchMode()
        data class Location(val countryCode: String, val countryName: String) : RadioSearchMode()
        data class StateMode(val state: String) : RadioSearchMode()
        data class Language(val language: String) : RadioSearchMode()
        object TopVoted : RadioSearchMode()
        object TopClicked : RadioSearchMode()
        object None : RadioSearchMode()
    }

    private val _radioBrowserStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val radioBrowserStations: StateFlow<List<RadioStation>> = _radioBrowserStations.asStateFlow()

    private val _discoverVisibleCount = MutableStateFlow(25)
    val discoverVisibleCount: StateFlow<Int> = _discoverVisibleCount.asStateFlow()

    val pagedDiscoverStations: StateFlow<List<RadioStation>> = combine(
        _radioBrowserStations, _discoverVisibleCount
    ) { list, count ->
        list.take(count)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadMoreDiscoverChunk() {
        _discoverVisibleCount.value += 25
    }

    fun resetDiscoverChunk() {
        _discoverVisibleCount.value = 25
    }

    fun updateRadioBrowserStations(stations: List<RadioStation>, provider: String = _selectedRadioProvider.value, query: String = _radioBrowserSearchQuery.value) {
        _radioBrowserStations.value = stations
        DiscoverCacheManager.saveCachedStations(getApplication(), provider, query, stations)
    }

    private val _radioBrowserLoading = MutableStateFlow(false)
    val radioBrowserLoading: StateFlow<Boolean> = _radioBrowserLoading.asStateFlow()

    private val _radioBrowserError = MutableStateFlow<String?>(null)
    val radioBrowserError: StateFlow<String?> = _radioBrowserError.asStateFlow()

    private val _radioBrowserSearchQuery = MutableStateFlow("")
    val radioBrowserSearchQuery: StateFlow<String> = _radioBrowserSearchQuery.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private var currentOffset = 0
    private var currentSearchMode: RadioSearchMode = RadioSearchMode.None

    init {
        // Build and initialize ExoPlayer
        setupPlayer()

        // Sync initial DB items
        viewModelScope.launch {
            try { repository.checkAndPrepopulate(); android.util.Log.d("RadioViewModel", "DB prepopulated!") } catch (e: Exception) { android.util.Log.e("RadioViewModel", "Error DB", e) }
        }

        // Load default stations (All sources unified)
        loadOnlineStationsForCurrentProvider()
    }

    private fun setupPlayer() {
        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    mediaController = controllerFuture?.get()
                    mediaController?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            updateStatus()
                        }
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            updateStatus()
                        }
                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            updateStatus()
                        }
                        override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                            updateTrackMetadata(mediaMetadata)
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            _playbackStatus.value = PlaybackStatus.ERROR
                            val stationName = _currentStation.value?.name ?: "esta emisora"
                            _errorMessage.value = "La emisora '$stationName' no responde o el servidor está caído."
                        }
                    })
                    mediaController?.volume = _volume.value
                } catch (e: Exception) {
                    _errorMessage.value = "No se pudo iniciar el reproductor multimedia."
                }
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }

    private fun updateStatus() {
        val player = mediaController ?: return
        val state = player.playbackState
        val playing = player.isPlaying
        _playbackStatus.value = when {
            state == Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            playing -> PlaybackStatus.PLAYING
            state == Player.STATE_READY -> PlaybackStatus.PAUSED
            state == Player.STATE_ENDED || state == Player.STATE_IDLE -> PlaybackStatus.IDLE
            else -> PlaybackStatus.IDLE
        }
    }

    fun playStation(station: RadioStation) {
        _errorMessage.value = null
        _currentStation.value = station
        _playbackStatus.value = PlaybackStatus.BUFFERING
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
        _currentTrackArtworkUrl.value = null
        
        viewModelScope.launch {
            repository.insertHistory(station)

            val playableUrl = station.url

            mediaController?.let { player ->
                try {
                    player.stop()
                    player.clearMediaItems()
                    val mediaItem = MediaItem.Builder()
                        .setUri(playableUrl)
                        .setMediaId(station.id.toString())
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setArtist(station.genre)
                                .build()
                        )
                        .build()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                } catch (e: Exception) {
                    _playbackStatus.value = PlaybackStatus.ERROR
                    _errorMessage.value = "Error al abrir la señal web."
                }
            }
        }
    }

    private var artworkSearchJob: Job? = null

    private fun updateTrackMetadata(mediaMetadata: androidx.media3.common.MediaMetadata) {
        val rawTitle = mediaMetadata.title?.toString()
            ?: mediaMetadata.displayTitle?.toString()
            ?: mediaMetadata.subtitle?.toString()
            ?: mediaMetadata.description?.toString()
        val rawArtist = mediaMetadata.artist?.toString()
            ?: mediaMetadata.albumArtist?.toString()

        val currentName = _currentStation.value?.name ?: ""
        val currentGenre = _currentStation.value?.genre ?: ""

        var extractedTitle: String? = null
        var extractedArtist: String? = null

        // Direct artwork from metadata if stream provides it
        mediaMetadata.artworkUri?.toString()?.let { uriStr ->
            if (uriStr.isNotBlank()) {
                _currentTrackArtworkUrl.value = uriStr
            }
        }

        if (!rawArtist.isNullOrBlank() && rawArtist != currentGenre && rawArtist != currentName) {
            extractedArtist = rawArtist.trim()
            extractedTitle = rawTitle?.trim()
        } else if (!rawTitle.isNullOrBlank() && rawTitle != currentName) {
            val titleClean = rawTitle.trim()
            when {
                titleClean.contains(" - ") -> {
                    val parts = titleClean.split(" - ", limit = 2)
                    extractedArtist = parts[0].trim()
                    extractedTitle = parts[1].trim()
                }
                titleClean.contains(" – ") -> {
                    val parts = titleClean.split(" – ", limit = 2)
                    extractedArtist = parts[0].trim()
                    extractedTitle = parts[1].trim()
                }
                titleClean.contains(" : ") -> {
                    val parts = titleClean.split(" : ", limit = 2)
                    extractedArtist = parts[0].trim()
                    extractedTitle = parts[1].trim()
                }
                titleClean.contains(" | ") -> {
                    val parts = titleClean.split(" | ", limit = 2)
                    extractedArtist = parts[0].trim()
                    extractedTitle = parts[1].trim()
                }
                else -> {
                    extractedTitle = titleClean
                }
            }
        }

        if (extractedTitle.isNullOrBlank() && extractedArtist.isNullOrBlank()) {
            return
        }

        // Clean common prefixes like "Playing: " or "Now Playing: "
        extractedArtist = extractedArtist?.replace("(?i)^now playing:\\s*".toRegex(), "")
            ?.replace("(?i)^playing:\\s*".toRegex(), "")?.trim()
        extractedTitle = extractedTitle?.replace("(?i)^now playing:\\s*".toRegex(), "")
            ?.replace("(?i)^playing:\\s*".toRegex(), "")?.trim()

        _currentTrackTitle.value = extractedTitle
        _currentTrackArtist.value = extractedArtist

        if (_currentTrackArtworkUrl.value.isNullOrBlank()) {
            fetchArtwork(extractedArtist, extractedTitle)
        }
    }

    private fun fetchArtwork(artist: String?, song: String?) {
        artworkSearchJob?.cancel()
        val terms = listOfNotNull(artist, song).filter { it.isNotBlank() }
        if (terms.isEmpty()) {
            _currentTrackArtworkUrl.value = null
            return
        }

        val query = terms.joinToString(" ").trim()
        artworkSearchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = MultiSourceRadioClients.iTunesSearchService.searchSong(term = query)
                val firstResult = response.results?.firstOrNull()
                val artworkUrl = firstResult?.artworkUrl100?.replace("100x100bb", "600x600bb")
                _currentTrackArtworkUrl.value = artworkUrl
            } catch (e: Exception) {
                _currentTrackArtworkUrl.value = null
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        val current = _currentStation.value ?: return

        if (_playbackStatus.value == PlaybackStatus.ERROR) {
            playStation(current)
            return
        }

        if (player.isPlaying) {
            player.pause()
            _playbackStatus.value = PlaybackStatus.PAUSED
        } else {
            _errorMessage.value = null
            _playbackStatus.value = PlaybackStatus.BUFFERING
            
            // If stopped or idle, re-configure
            if (player.playbackState == Player.STATE_IDLE) {
                val mediaItem = MediaItem.Builder()
                    .setUri(current.url)
                    .setMediaId(current.id.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(current.name)
                            .setArtist(current.genre)
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
            }
            player.play()
        }
    }

    fun stopPlayback() {
        mediaController?.stop()
        _playbackStatus.value = PlaybackStatus.IDLE
        _currentStation.value = null
        _errorMessage.value = null
    }

    fun playNextStation() {
        val currentList = filteredStations.value.ifEmpty { stations.value }
        if (currentList.isEmpty()) return
        val current = _currentStation.value
        val currentIndex = if (current != null) currentList.indexOfFirst { it.id == current.id } else -1
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % currentList.size else 0
        playStation(currentList[nextIndex])
    }

    fun playPreviousStation() {
        val currentList = filteredStations.value.ifEmpty { stations.value }
        if (currentList.isEmpty()) return
        val current = _currentStation.value
        val currentIndex = if (current != null) currentList.indexOfFirst { it.id == current.id } else -1
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else if (currentIndex == 0) currentList.size - 1 else 0
        playStation(currentList[prevIndex])
    }

    fun playRandomStation() {
        val currentList = filteredStations.value.ifEmpty { stations.value }
        if (currentList.isEmpty()) return
        val randomIndex = kotlin.random.Random.nextInt(currentList.size)
        playStation(currentList[randomIndex])
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        mediaController?.volume = clamped
    }

    // Sleep Timer Controls (Temporizador de Apagado)
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepSecondsLeft.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentSecs = _sleepSecondsLeft.value
                if (currentSecs != null) {
                    if (currentSecs <= 1) {
                        _sleepSecondsLeft.value = null
                        stopPlayback()
                        break
                    } else {
                        _sleepSecondsLeft.value = currentSecs - 1
                    }
                } else {
                    break
                }
            }
        }
    }

    fun cancelSleepTimer() {
        _sleepSecondsLeft.value = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    // Search and Filter updates
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateGenreFilter(genre: String) {
        _selectedGenre.value = genre
    }

    // Database mutation mappings
    fun addCustomStation(name: String, url: String, genre: String, country: String, region: String, province: String, district: String) {
        viewModelScope.launch {
            val formattedGenre = genre.trim().ifEmpty { "Varios" }
            val station = RadioStation(
                name = name.trim(),
                url = url.trim(),
                genre = formattedGenre,
                isFavorite = false,
                isCustom = true,
                country = country.trim(),
                region = region.trim(),
                province = province.trim(),
                district = district.trim()
            )
            repository.insert(station)
        }
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            val updated = station.copy(isFavorite = !station.isFavorite)
            repository.update(updated)
            // Synchronize active player header if updated station is playing
            if (_currentStation.value?.id == station.id) {
                _currentStation.value = updated
            }
        }
    }

    fun deleteStation(station: RadioStation) {
        viewModelScope.launch {
            if (station.isCustom) {
                // If the deleted station is playing, stop player first
                if (_currentStation.value?.id == station.id) {
                    stopPlayback()
                }
                repository.delete(station)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Radio Provider selection & Multi-API Operations
    fun setRadioProvider(provider: String) {
        _selectedRadioProvider.value = provider
        _radioBrowserSearchQuery.value = ""
        resetDiscoverChunk()
        loadOnlineStationsForCurrentProvider()
    }

    fun loadOnlineStationsForCurrentProvider() {
        val provider = _selectedRadioProvider.value
        val query = _radioBrowserSearchQuery.value

        // Stale-While-Revalidate: Warm load from disk cache instantly (0 ms)
        val cached = DiscoverCacheManager.getCachedStations(getApplication(), provider, query)
        if (cached.isNotEmpty()) {
            _radioBrowserStations.value = cached
        }

        when (provider) {
            "Todas" -> performGlobalMultiApiSearch(query)
            "iHeartRadio" -> fetchIHeartRadioStations(query)
            "GitHub Raw" -> fetchGitHubCuratedStations(query)
            "SomaFM" -> fetchSomaFmStations(query)
            "FMStream" -> fetchFmStreamStations(query)
            "Radio-Browser" -> loadRadioBrowserTopVoted()
            else -> performGlobalMultiApiSearch(query)
        }
    }

    // Search Suggestions Models and Logic
    enum class SuggestionType {
        STATION, COUNTRY, REGION, GENRE, LANGUAGE, KEYWORD
    }

    data class SearchSuggestion(
        val title: String,
        val subtitle: String,
        val query: String,
        val type: SuggestionType
    )

    private val commonCountries = listOf(
        "Perú", "México", "Colombia", "Argentina", "España", "Chile", "Ecuador", "Venezuela",
        "Bolivia", "Uruguay", "Paraguay", "Brasil", "EE.UU.", "Guatemala", "República Dominicana",
        "Costa Rica", "Panamá", "Honduras", "El Salvador", "Nicaragua", "Puerto Rico", "Cuba",
        "Alemania", "Francia", "Italia", "Reino Unido", "Canadá", "Japón"
    )

    private val commonRegions = listOf(
        "Lima", "Arequipa", "Trujillo", "Cuzco", "California", "Texas", "Florida", "Bavaria", "Baviera",
        "Jalisco", "Nuevo León", "Puebla", "Antioquia", "Cundinamarca", "São Paulo", "Bogotá", "Medellín",
        "Cali", "Buenos Aires", "Córdoba", "Rosario", "Madrid", "Barcelona", "Cataluña", "Andalucía",
        "Valencia", "Sevilla", "Ciudad de México", "Guadalajara", "Monterrey", "Santiago", "Valparaíso",
        "Quito", "Guayaquil", "Caracas", "Montevideo", "Asunción", "La Paz", "Santa Cruz", "San José",
        "San Juan", "Miami", "Los Angeles", "Nueva York"
    )

    private val commonGenres = listOf(
        "Pop", "Rock", "Salsa", "Cumbia", "Reggaeton", "Bachata", "Noticias", "Deportes", "Jazz",
        "Clásica", "80s", "90s", "Hits", "Top 40", "Variada", "Electrónica", "Urban", "Baladas",
        "Folclore", "Merengue", "Vallenato", "Tango", "Trova", "Cristiana", "Instrumental"
    )

    private val commonLanguages = listOf(
        "Español", "Inglés", "Portugués", "Francés", "Alemán", "Italiano", "Ruso", "Japonés", "Chino", "Árabe", "Coreano", "Quechua", "Hindi", "Holandés", "Polaco"
    )

    private fun mapCountryAlias(query: String): List<String> {
        val q = query.normalize().lowercase().trim()
        return when {
            q.contains("peru") -> listOf("Peru", "Perú")
            q.contains("mexico") -> listOf("Mexico", "México")
            q.contains("espana") || q.contains("spain") -> listOf("Spain", "España")
            q.contains("argentina") -> listOf("Argentina")
            q.contains("colombia") -> listOf("Colombia")
            q.contains("chile") -> listOf("Chile")
            q.contains("eeuu") || q.contains("usa") || q.contains("estados unidos") -> listOf("United States", "US", "USA", "EE.UU.")
            q.contains("ecuador") -> listOf("Ecuador")
            q.contains("venezuela") -> listOf("Venezuela")
            q.contains("bolivia") -> listOf("Bolivia")
            q.contains("uruguay") -> listOf("Uruguay")
            q.contains("paraguay") -> listOf("Paraguay")
            q.contains("brasil") || q.contains("brazil") -> listOf("Brazil", "Brasil")
            else -> listOf(query)
        }
    }

    private fun mapLanguageAlias(query: String): List<String> {
        val q = query.normalize().lowercase().trim()
        return when {
            q.contains("espanol") || q.contains("spanish") || q.contains("castellano") -> listOf("spanish", "espanol", "español")
            q.contains("ingles") || q.contains("english") -> listOf("english", "ingles", "inglés")
            q.contains("portugues") || q.contains("portuguese") -> listOf("portuguese", "portugues", "português")
            q.contains("frances") || q.contains("french") -> listOf("french", "frances", "français")
            q.contains("aleman") || q.contains("german") || q.contains("deutsch") -> listOf("german", "aleman", "deutsch")
            q.contains("italiano") || q.contains("italian") -> listOf("italian", "italiano")
            q.contains("ruso") || q.contains("russian") -> listOf("russian", "ruso")
            q.contains("japones") || q.contains("japanese") -> listOf("japanese", "japones")
            q.contains("chino") || q.contains("chinese") -> listOf("chinese", "chino")
            q.contains("arabe") || q.contains("arabic") -> listOf("arabic", "arabe")
            q.contains("coreano") || q.contains("korean") -> listOf("korean", "coreano")
            q.contains("quechua") || q.contains("kichwa") -> listOf("quechua", "kichwa")
            else -> listOf(query.lowercase())
        }
    }

    private fun generateSuggestions(query: String, availableStations: List<RadioStation>): List<SearchSuggestion> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val normQ = q.normalize().lowercase()

        val suggestions = mutableListOf<SearchSuggestion>()

        // 1. Countries
        for (country in commonCountries) {
            if (country.normalize().lowercase().contains(normQ)) {
                suggestions.add(
                    SearchSuggestion(
                        title = country,
                        subtitle = "País • Buscar emisoras de $country",
                        query = country,
                        type = SuggestionType.COUNTRY
                    )
                )
            }
        }

        // 2. Regions / Cities
        for (region in commonRegions) {
            if (region.normalize().lowercase().contains(normQ)) {
                suggestions.add(
                    SearchSuggestion(
                        title = region,
                        subtitle = "Lugar / Región • Emisoras en $region",
                        query = region,
                        type = SuggestionType.REGION
                    )
                )
            }
        }

        // 3. Genres / Tags
        for (genre in commonGenres) {
            if (genre.normalize().lowercase().contains(normQ)) {
                suggestions.add(
                    SearchSuggestion(
                        title = genre,
                        subtitle = "Género Musical • Radios de $genre",
                        query = genre,
                        type = SuggestionType.GENRE
                    )
                )
            }
        }

        // 4. Languages
        for (language in commonLanguages) {
            if (language.normalize().lowercase().contains(normQ)) {
                suggestions.add(
                    SearchSuggestion(
                        title = language,
                        subtitle = "Idioma • Emisoras en idioma $language",
                        query = language,
                        type = SuggestionType.LANGUAGE
                    )
                )
            }
        }

        // 5. Stations
        val matchingStations = availableStations.filter { s ->
            s.name.normalize().lowercase().contains(normQ) ||
            s.genre.normalize().lowercase().contains(normQ) ||
            s.country.normalize().lowercase().contains(normQ) ||
            s.region.normalize().lowercase().contains(normQ)
        }.take(5)

        for (station in matchingStations) {
            suggestions.add(
                SearchSuggestion(
                    title = station.name,
                    subtitle = "${station.genre} • ${station.country.ifBlank { "Radio Online" }}",
                    query = station.name,
                    type = SuggestionType.STATION
                )
            )
        }

        return suggestions.distinctBy { it.title.lowercase() }.take(8)
    }

    val homeSuggestions: StateFlow<List<SearchSuggestion>> = combine(
        _searchQuery, stations
    ) { query, stationList ->
        generateSuggestions(query, stationList)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val onlineSuggestions: StateFlow<List<SearchSuggestion>> = combine(
        _radioBrowserSearchQuery, _radioBrowserStations
    ) { query, onlineList ->
        generateSuggestions(query, onlineList)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun String.containsNormalized(query: String): Boolean {
        if (query.isBlank()) return true
        val normSelf = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "").lowercase().trim()
        val normQuery = java.text.Normalizer.normalize(query, java.text.Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "").lowercase().trim()
        return normSelf.contains(normQuery)
    }

    private suspend fun fetchRadioBrowserStationsExpanded(query: String, limitPerQuery: Int = 40, offset: Int = 0): List<RadioStation> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val qNormalized = q.normalize()
        val countryAliases = mapCountryAlias(q)

        return coroutineScope {
            // 1. By Name
            val byNameDeferred = async {
                try { RadioBrowserApiClient.service.searchStations(name = q, limit = limitPerQuery, offset = offset).map { it.toRadioStation() } }
                catch (e: Exception) { emptyList() }
            }

            // 2. By Country (mapped)
            val byCountryDeferred = async {
                val list = mutableListOf<RadioStation>()
                for (alias in countryAliases) {
                    try {
                        list.addAll(RadioBrowserApiClient.service.searchStations(country = alias, limit = limitPerQuery, offset = offset).map { it.toRadioStation() })
                    } catch (e: Exception) {}
                }
                list
            }

            // 3. By State / Region / City
            val byStateDeferred = async {
                try { RadioBrowserApiClient.service.searchStations(state = q, limit = limitPerQuery, offset = offset).map { it.toRadioStation() } }
                catch (e: Exception) { emptyList() }
            }

            // 4. By Tag / Genre
            val byTagDeferred = async {
                try { RadioBrowserApiClient.service.searchStations(tag = q.lowercase(), limit = limitPerQuery, offset = offset).map { it.toRadioStation() } }
                catch (e: Exception) { emptyList() }
            }

            // 5. By Language (mapped & raw)
            val byLanguageDeferred = async {
                val list = mutableListOf<RadioStation>()
                val langAliases = mapLanguageAlias(q)
                for (alias in langAliases) {
                    try {
                        list.addAll(RadioBrowserApiClient.service.searchStations(language = alias, limit = limitPerQuery, offset = offset).map { it.toRadioStation() })
                    } catch (e: Exception) {}
                }
                list
            }

            // 6. Tokenized search if multiple words
            val tokens = q.split(" ").filter { it.length >= 2 }
            val tokenizedDeferred = async {
                if (tokens.size >= 2) {
                    val tokenList = mutableListOf<RadioStation>()
                    for (token in tokens) {
                        try {
                            tokenList.addAll(RadioBrowserApiClient.service.searchStations(name = token, limit = 25, offset = offset).map { it.toRadioStation() })
                        } catch (e: Exception) {}
                    }
                    tokenList
                } else emptyList()
            }

            val nameList = byNameDeferred.await()
            val countryList = byCountryDeferred.await()
            val stateList = byStateDeferred.await()
            val tagList = byTagDeferred.await()
            val languageList = byLanguageDeferred.await()
            val tokenList = tokenizedDeferred.await()

            val combinedAll = (nameList + countryList + stateList + tagList + languageList + tokenList)

            // Normalize and rank results
            val tokensNorm = qNormalized.lowercase().split(" ").filter { it.isNotBlank() }

            combinedAll
                .filter { station ->
                    if (tokensNorm.isEmpty()) true
                    else {
                        val fullTextNorm = "${station.name} ${station.genre} ${station.country} ${station.region}".normalize().lowercase()
                        tokensNorm.any { token -> fullTextNorm.contains(token) }
                    }
                }
                .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
        }
    }

    fun searchRadioBrowser(query: String) {
        _radioBrowserSearchQuery.value = query
        val provider = _selectedRadioProvider.value
        resetDiscoverChunk()
        if (query.isBlank()) {
            loadOnlineStationsForCurrentProvider()
            return
        }

        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.Query(query)

        // Warm cache lookup for search queries
        val cached = DiscoverCacheManager.getCachedStations(getApplication(), provider, query)
        if (cached.isNotEmpty()) {
            _radioBrowserStations.value = cached
        }

        when (provider) {
            "Todas" -> performGlobalMultiApiSearch(query)
            "iHeartRadio" -> fetchIHeartRadioStations(query)
            "GitHub Raw" -> fetchGitHubCuratedStations(query)
            "SomaFM" -> fetchSomaFmStations(query)
            "FMStream" -> fetchFmStreamStations(query)
            "Radio-Browser" -> {
                viewModelScope.launch(Dispatchers.IO) {
                    _radioBrowserLoading.value = true
                    _radioBrowserError.value = null
                    try {
                        val stations = fetchRadioBrowserStationsExpanded(query.trim(), limitPerQuery = 40, offset = 0)
                        updateRadioBrowserStations(stations, provider, query)
                    } catch (e: Exception) {
                        _radioBrowserError.value = "Error al buscar en Radio Browser: ${e.localizedMessage ?: "Error de red"}"
                        if (_radioBrowserStations.value.isEmpty()) {
                            _radioBrowserStations.value = emptyList()
                        }
                    } finally {
                        _radioBrowserLoading.value = false
                    }
                }
            }
            else -> performGlobalMultiApiSearch(query)
        }
    }

    private fun performGlobalMultiApiSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = query.trim()

                // 1. Radio Browser (Expanded search: Name, Country, Tag)
                val rbDeferred = async {
                    try {
                        if (q.isBlank()) {
                            RadioBrowserApiClient.service.getTopVoteStations(limit = 20).map { it.toRadioStation() }
                        } else {
                            fetchRadioBrowserStationsExpanded(q, limitPerQuery = 35)
                        }
                    } catch (e: Exception) { emptyList() }
                }

                // 2. GitHub Raw Curated
                val ghDeferred = async {
                    try {
                        val list = MultiSourceRadioClients.fallbackGitHubCuratedList
                        if (q.isBlank()) list else list.filter {
                            it.name.containsNormalized(q) ||
                            it.genre.containsNormalized(q) ||
                            it.country.containsNormalized(q) ||
                            it.region.containsNormalized(q)
                        }
                    } catch (e: Exception) { emptyList() }
                }

                // 3. SomaFM
                val somaDeferred = async {
                    try {
                        val channels = MultiSourceRadioClients.somaFmService.getChannels().channels ?: emptyList()
                        val stations = channels.map { it.toRadioStation() }
                        if (q.isBlank()) stations.take(12) else stations.filter {
                            it.name.containsNormalized(q) ||
                            it.genre.containsNormalized(q)
                        }
                    } catch (e: Exception) { emptyList() }
                }

                // 4. iHeartRadio
                val iHeartDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(
                            keywords = q.takeIf { it.isNotBlank() },
                            limit = 30
                        )
                        val dtos = response.hits ?: response.items ?: emptyList()
                        val res = dtos.map { it.toRadioStation() }
                        val fallbackFiltered = listOf(
                            RadioStation(id = 8101, name = "iHeartRadio Top 40", url = "https://stream.revma.ihrhls.com/zc1469", genre = "Top 40 / Hits", country = "EE.UU.", region = "iHeartMedia"),
                            RadioStation(id = 8102, name = "iHeartRadio Country", url = "https://stream.revma.ihrhls.com/zc4414", genre = "Country", country = "EE.UU.", region = "iHeartMedia")
                        ).filter {
                            it.name.containsNormalized(q) ||
                            it.genre.containsNormalized(q) ||
                            it.country.containsNormalized(q)
                        }
                        (res + fallbackFiltered).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                    } catch (e: Exception) { emptyList() }
                }

                // 5. FMStream Directory
                val fmDeferred = async {
                    try {
                        val apiResults = if (q.isNotBlank()) {
                            try {
                                MultiSourceRadioClients.fmStreamService.searchFmStream(query = q).map { it.toRadioStation() }
                            } catch (e: Exception) { emptyList() }
                        } else {
                            MultiSourceRadioClients.fallbackFmStreamList
                        }

                        val fallbackFiltered = MultiSourceRadioClients.fallbackFmStreamList.filter {
                            it.name.containsNormalized(q) ||
                            it.genre.containsNormalized(q) ||
                            it.country.containsNormalized(q) ||
                            it.region.containsNormalized(q)
                        }

                        (apiResults + fallbackFiltered).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                    } catch (e: Exception) {
                        MultiSourceRadioClients.fallbackFmStreamList.filter {
                            it.name.containsNormalized(q) ||
                            it.genre.containsNormalized(q) ||
                            it.country.containsNormalized(q) ||
                            it.region.containsNormalized(q)
                        }
                    }
                }

                val rbList = rbDeferred.await()
                val ghList = ghDeferred.await()
                val somaList = somaDeferred.await()
                val ihList = iHeartDeferred.await()
                val fmList = fmDeferred.await()

                val combined = (rbList + ghList + somaList + ihList + fmList)
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                updateRadioBrowserStations(combined)
            } catch (e: Exception) {
                _radioBrowserError.value = "Error en la búsqueda multifuente: ${e.localizedMessage}"
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserStations.value = emptyList()
                }
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    private fun fetchFmStreamStations(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = query.trim()
                val list = if (q.isBlank()) {
                    MultiSourceRadioClients.fallbackFmStreamList
                } else {
                    val apiResults = try {
                        MultiSourceRadioClients.fmStreamService.searchFmStream(query = q).map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }

                    val fallbackFiltered = MultiSourceRadioClients.fallbackFmStreamList.filter {
                        it.name.containsNormalized(q) ||
                        it.genre.containsNormalized(q) ||
                        it.country.containsNormalized(q) ||
                        it.region.containsNormalized(q)
                    }

                    (apiResults + fallbackFiltered).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                }
                _radioBrowserStations.value = list
            } catch (e: Exception) {
                _radioBrowserStations.value = MultiSourceRadioClients.fallbackFmStreamList
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    private fun fetchIHeartRadioStations(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = query.trim()
                val response = MultiSourceRadioClients.iHeartService.getLiveStations(
                    keywords = q.takeIf { it.isNotBlank() },
                    limit = 35
                )
                val dtos = response.hits ?: response.items ?: emptyList()
                val result = dtos.map { it.toRadioStation() }
                val fallbackList = listOf(
                    RadioStation(id = 8101, name = "iHeartRadio Top 40", url = "https://stream.revma.ihrhls.com/zc1469", genre = "Top 40 / Hits", country = "EE.UU.", region = "iHeartMedia"),
                    RadioStation(id = 8102, name = "iHeartRadio Country", url = "https://stream.revma.ihrhls.com/zc4414", genre = "Country", country = "EE.UU.", region = "iHeartMedia"),
                    RadioStation(id = 8103, name = "iHeartRock Classic", url = "https://stream.revma.ihrhls.com/zc4418", genre = "Classic Rock", country = "EE.UU.", region = "iHeartMedia"),
                    RadioStation(id = 8104, name = "iHeart90s Hits", url = "https://stream.revma.ihrhls.com/zc4422", genre = "90s Hits", country = "EE.UU.", region = "iHeartMedia")
                )
                val filteredFallback = if (q.isNotBlank()) {
                    fallbackList.filter {
                        it.name.containsNormalized(q) ||
                        it.genre.containsNormalized(q) ||
                        it.country.containsNormalized(q)
                    }
                } else fallbackList

                _radioBrowserStations.value = (result + filteredFallback).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
            } catch (e: Exception) {
                val q = query.trim()
                val fallbackList = listOf(
                    RadioStation(id = 8101, name = "iHeartRadio Top 40", url = "https://stream.revma.ihrhls.com/zc1469", genre = "Top 40 / Hits", country = "EE.UU.", region = "iHeartMedia"),
                    RadioStation(id = 8102, name = "iHeartRadio Country", url = "https://stream.revma.ihrhls.com/zc4414", genre = "Country", country = "EE.UU.", region = "iHeartMedia"),
                    RadioStation(id = 8103, name = "iHeartRock Classic", url = "https://stream.revma.ihrhls.com/zc4418", genre = "Classic Rock", country = "EE.UU.", region = "iHeartMedia"),
                    RadioStation(id = 8104, name = "iHeart90s Hits", url = "https://stream.revma.ihrhls.com/zc4422", genre = "90s Hits", country = "EE.UU.", region = "iHeartMedia")
                )
                _radioBrowserStations.value = if (q.isBlank()) fallbackList else fallbackList.filter {
                    it.name.containsNormalized(q) ||
                    it.genre.containsNormalized(q) ||
                    it.country.containsNormalized(q)
                }
                _radioBrowserError.value = null
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    private fun fetchGitHubCuratedStations(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = query.trim()
                val list = MultiSourceRadioClients.fallbackGitHubCuratedList
                val filtered = if (q.isNotBlank()) {
                    list.filter {
                        it.name.containsNormalized(q) ||
                        it.genre.containsNormalized(q) ||
                        it.country.containsNormalized(q) ||
                        it.region.containsNormalized(q)
                    }
                } else {
                    list
                }
                _radioBrowserStations.value = filtered
            } catch (e: Exception) {
                _radioBrowserStations.value = MultiSourceRadioClients.fallbackGitHubCuratedList
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    private fun fetchSomaFmStations(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = query.trim()
                val response = MultiSourceRadioClients.somaFmService.getChannels()
                val channels = response.channels ?: emptyList()
                val stations = channels.map { it.toRadioStation() }
                val filtered = if (q.isNotBlank()) {
                    stations.filter {
                        it.name.containsNormalized(q) ||
                        it.genre.containsNormalized(q)
                    }
                } else {
                    stations
                }
                _radioBrowserStations.value = filtered
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al conectar con SomaFM: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun loadRadioBrowserTopVoted() {
        _radioBrowserSearchQuery.value = ""
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.TopVoted
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val rbDeferred = async {
                    try { RadioBrowserApiClient.service.getTopVoteStations(limit = 35).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                }
                val ghDeferred = async {
                    try { MultiSourceRadioClients.fallbackGitHubCuratedList.take(10) } catch (e: Exception) { emptyList() }
                }
                val somaDeferred = async {
                    try {
                        val channels = MultiSourceRadioClients.somaFmService.getChannels().channels ?: emptyList()
                        channels.take(8).map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }
                val ihDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(limit = 15)
                        val dtos = response.hits ?: response.items ?: emptyList()
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }
                val fmDeferred = async {
                    try { MultiSourceRadioClients.fallbackFmStreamList.take(10) } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + somaDeferred.await() + ihDeferred.await() + fmDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al conectar con las fuentes de radio: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun loadRadioBrowserTopClicked() {
        _radioBrowserSearchQuery.value = ""
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.TopClicked
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val rbDeferred = async {
                    try { RadioBrowserApiClient.service.getTopClickStations(limit = 35).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                }
                val ghDeferred = async {
                    try { MultiSourceRadioClients.fallbackGitHubCuratedList.take(10) } catch (e: Exception) { emptyList() }
                }
                val somaDeferred = async {
                    try {
                        val channels = MultiSourceRadioClients.somaFmService.getChannels().channels ?: emptyList()
                        channels.take(8).map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }
                val ihDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(limit = 15)
                        val dtos = response.hits ?: response.items ?: emptyList()
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }
                val fmDeferred = async {
                    try { MultiSourceRadioClients.fallbackFmStreamList.take(10) } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + somaDeferred.await() + ihDeferred.await() + fmDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al conectar con las fuentes de radio: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByTag(tag: String) {
        _radioBrowserSearchQuery.value = tag
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.Tag(tag)
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = tag.lowercase().trim()
                val rbDeferred = async {
                    try { RadioBrowserApiClient.service.searchStations(tag = q, limit = 35).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                }
                val ghDeferred = async {
                    try { MultiSourceRadioClients.fallbackGitHubCuratedList.filter { it.genre.containsNormalized(q) || it.name.containsNormalized(q) } } catch (e: Exception) { emptyList() }
                }
                val somaDeferred = async {
                    try {
                        val channels = MultiSourceRadioClients.somaFmService.getChannels().channels ?: emptyList()
                        channels.map { it.toRadioStation() }.filter { it.genre.containsNormalized(q) || it.name.containsNormalized(q) }
                    } catch (e: Exception) { emptyList() }
                }
                val ihDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(keywords = q, limit = 20)
                        val dtos = response.hits ?: response.items ?: emptyList()
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }
                val fmDeferred = async {
                    try { MultiSourceRadioClients.fallbackFmStreamList.filter { it.genre.containsNormalized(q) || it.name.containsNormalized(q) } } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + somaDeferred.await() + ihDeferred.await() + fmDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al buscar por género: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByCountry(country: String) {
        _radioBrowserSearchQuery.value = country
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.Country(country)
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = country.trim()
                val rbDeferred = async {
                    try { RadioBrowserApiClient.service.searchStations(country = q, limit = 35).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                }
                val ghDeferred = async {
                    try { MultiSourceRadioClients.fallbackGitHubCuratedList.filter { it.country.containsNormalized(q) || it.name.containsNormalized(q) } } catch (e: Exception) { emptyList() }
                }
                val fmDeferred = async {
                    try {
                        val apiResults = try { MultiSourceRadioClients.fmStreamService.searchFmStream(query = q).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                        val fallback = MultiSourceRadioClients.fallbackFmStreamList.filter { it.country.containsNormalized(q) || it.name.containsNormalized(q) }
                        (apiResults + fallback).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                    } catch (e: Exception) { emptyList() }
                }
                val ihDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(keywords = q, limit = 15)
                        val dtos = response.hits ?: response.items ?: emptyList()
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + fmDeferred.await() + ihDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al buscar por país: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByDeviceLocation() {
        val locale = java.util.Locale.getDefault()
        val countryCode = locale.country // e.g. "PE", "MX", "ES", "US", "AR"
        val englishCountry = locale.getDisplayCountry(java.util.Locale.ENGLISH)
        val spanishCountry = locale.getDisplayCountry(java.util.Locale("es"))
        val targetCountryName = if (spanishCountry.isNotBlank()) spanishCountry else englishCountry

        _radioBrowserSearchQuery.value = targetCountryName
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.Location(countryCode, targetCountryName)

        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val rbDeferred = async {
                    try {
                        var dtos = emptyList<com.example.data.RadioBrowserStationDto>()
                        if (countryCode.isNotBlank()) {
                            dtos = RadioBrowserApiClient.service.searchStations(countrycode = countryCode.lowercase(), limit = 35)
                        }
                        if (dtos.isEmpty() && englishCountry.isNotBlank()) {
                            dtos = RadioBrowserApiClient.service.searchStations(country = englishCountry, limit = 35)
                        }
                        if (dtos.isEmpty() && targetCountryName.isNotBlank()) {
                            dtos = RadioBrowserApiClient.service.searchStations(country = targetCountryName, limit = 35)
                        }
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }

                val ghDeferred = async {
                    try {
                        MultiSourceRadioClients.fallbackGitHubCuratedList.filter {
                            it.country.containsNormalized(targetCountryName) ||
                            it.country.containsNormalized(englishCountry) ||
                            (countryCode.isNotBlank() && it.country.containsNormalized(countryCode))
                        }
                    } catch (e: Exception) { emptyList() }
                }

                val fmDeferred = async {
                    try {
                        val apiResults = try {
                            MultiSourceRadioClients.fmStreamService.searchFmStream(query = targetCountryName).map { it.toRadioStation() }
                        } catch (e: Exception) { emptyList() }
                        val fallback = MultiSourceRadioClients.fallbackFmStreamList.filter {
                            it.country.containsNormalized(targetCountryName) ||
                            it.country.containsNormalized(englishCountry) ||
                            (countryCode.isNotBlank() && it.country.containsNormalized(countryCode))
                        }
                        (apiResults + fallback).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                    } catch (e: Exception) { emptyList() }
                }

                val ihDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(keywords = targetCountryName, limit = 15)
                        val dtos = response.hits ?: response.items ?: emptyList()
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + fmDeferred.await() + ihDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserError.value = "No se encontraron emisoras en línea para tu país ($targetCountryName)."
                }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al buscar radios por ubicación: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByState(state: String) {
        _radioBrowserSearchQuery.value = state
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.StateMode(state)
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val q = state.trim()
                val rbDeferred = async {
                    try { RadioBrowserApiClient.service.searchStations(state = q, limit = 35).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                }
                val ghDeferred = async {
                    try { MultiSourceRadioClients.fallbackGitHubCuratedList.filter { it.region.containsNormalized(q) || it.name.containsNormalized(q) } } catch (e: Exception) { emptyList() }
                }
                val fmDeferred = async {
                    try {
                        val apiResults = try { MultiSourceRadioClients.fmStreamService.searchFmStream(query = q).map { it.toRadioStation() } } catch (e: Exception) { emptyList() }
                        val fallback = MultiSourceRadioClients.fallbackFmStreamList.filter { it.region.containsNormalized(q) || it.name.containsNormalized(q) }
                        (apiResults + fallback).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                    } catch (e: Exception) { emptyList() }
                }
                val ihDeferred = async {
                    try {
                        val response = MultiSourceRadioClients.iHeartService.getLiveStations(keywords = q, limit = 15)
                        val dtos = response.hits ?: response.items ?: emptyList()
                        dtos.map { it.toRadioStation() }
                    } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + fmDeferred.await() + ihDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al buscar por estado/región: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByLanguage(language: String) {
        _radioBrowserSearchQuery.value = language
        currentOffset = 0
        _canLoadMore.value = true
        currentSearchMode = RadioSearchMode.Language(language)
        viewModelScope.launch(Dispatchers.IO) {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val langAliases = mapLanguageAlias(language)
                val q = language.trim()

                val rbDeferred = async {
                    try {
                        val list = mutableListOf<RadioStation>()
                        for (alias in langAliases) {
                            val dtos = RadioBrowserApiClient.service.searchStations(language = alias, limit = 25)
                            list.addAll(dtos.map { it.toRadioStation() })
                        }
                        list
                    } catch (e: Exception) { emptyList() }
                }

                val ghDeferred = async {
                    try { MultiSourceRadioClients.fallbackGitHubCuratedList.filter { it.genre.containsNormalized(q) || it.name.containsNormalized(q) } } catch (e: Exception) { emptyList() }
                }

                val fmDeferred = async {
                    try {
                        MultiSourceRadioClients.fallbackFmStreamList.filter { it.genre.containsNormalized(q) || it.name.containsNormalized(q) }
                    } catch (e: Exception) { emptyList() }
                }

                val combined = (rbDeferred.await() + ghDeferred.await() + fmDeferred.await())
                    .distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }

                _radioBrowserStations.value = combined
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al buscar por idioma: ${e.localizedMessage ?: "Error de red"}"
                _radioBrowserStations.value = emptyList()
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun loadMoreStations() {
        if (_isLoadingMore.value || !_canLoadMore.value || _radioBrowserLoading.value) return
        val mode = currentSearchMode
        if (mode is RadioSearchMode.None) return

        currentOffset += 40
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            try {
                val nextStations: List<RadioStation> = when (mode) {
                    is RadioSearchMode.Query -> fetchRadioBrowserStationsExpanded(mode.query, limitPerQuery = 40, offset = currentOffset)
                    is RadioSearchMode.Tag -> RadioBrowserApiClient.service.searchStations(tag = mode.tag.lowercase(), limit = 40, offset = currentOffset).map { it.toRadioStation() }
                    is RadioSearchMode.Country -> RadioBrowserApiClient.service.searchStations(country = mode.country, limit = 40, offset = currentOffset).map { it.toRadioStation() }
                    is RadioSearchMode.Location -> {
                        var dtos = emptyList<com.example.data.RadioBrowserStationDto>()
                        if (mode.countryCode.isNotBlank()) {
                            dtos = RadioBrowserApiClient.service.searchStations(countrycode = mode.countryCode.lowercase(), limit = 40, offset = currentOffset)
                        }
                        if (dtos.isEmpty() && mode.countryName.isNotBlank()) {
                            dtos = RadioBrowserApiClient.service.searchStations(country = mode.countryName, limit = 40, offset = currentOffset)
                        }
                        dtos.map { it.toRadioStation() }
                    }
                    is RadioSearchMode.StateMode -> RadioBrowserApiClient.service.searchStations(state = mode.state, limit = 40, offset = currentOffset).map { it.toRadioStation() }
                    is RadioSearchMode.Language -> {
                        val langAliases = mapLanguageAlias(mode.language)
                        val list = mutableListOf<RadioStation>()
                        for (alias in langAliases) {
                            val dtos = RadioBrowserApiClient.service.searchStations(language = alias, limit = 40, offset = currentOffset)
                            list.addAll(dtos.map { it.toRadioStation() })
                        }
                        list.distinctBy { it.url }
                    }
                    is RadioSearchMode.TopVoted -> {
                        RadioBrowserApiClient.service.searchStations(order = "votes", limit = 40, offset = currentOffset).map { it.toRadioStation() }
                    }
                    is RadioSearchMode.TopClicked -> {
                        RadioBrowserApiClient.service.searchStations(order = "clickcount", limit = 40, offset = currentOffset).map { it.toRadioStation() }
                    }
                    is RadioSearchMode.None -> emptyList()
                }

                if (nextStations.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    val currentList = _radioBrowserStations.value
                    val combined = (currentList + nextStations).distinctBy { (it.name.lowercase().trim()) to (it.url.lowercase().trim()) }
                    if (combined.size == currentList.size) {
                        _canLoadMore.value = false
                    } else {
                        _radioBrowserStations.value = combined
                    }
                }
            } catch (e: Exception) {
                _canLoadMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun saveRadioBrowserStation(station: RadioStation) {
        viewModelScope.launch {
            val newStation = station.copy(id = 0, isCustom = true)
            repository.insert(newStation)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelSleepTimer()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}

class RadioViewModelFactory(
    private val application: Application,
    private val repository: RadioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RadioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RadioViewModel(application, repository) as T
        }
        throw IllegalArgumentException("ViewModel class desconocido")
    }
}
