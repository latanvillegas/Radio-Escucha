package com.example.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.RadioBrowserApiClient
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.data.PlaybackHistory
import com.example.service.PlaybackService
import com.example.util.ConnectivityObserver
import com.example.util.NetworkConnectivityObserver
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

class RadioViewModel(
    application: Application,
    private val repository: RadioRepository
) : AndroidViewModel(application) {

    enum class SuggestionType {
        STATION,
        GENRE,
        LOCATION,
        COUNTRY,
        REGION,
        LANGUAGE,
        KEYWORD
    }

    data class SearchSuggestion(
        val title: String,
        val subtitle: String,
        val query: String,
        val type: SuggestionType
    )

    sealed class RadioSearchMode {
        data class Query(val query: String) : RadioSearchMode()
        data class Tag(val tag: String) : RadioSearchMode()
        data class Country(val country: String) : RadioSearchMode()
        data class Location(val location: String) : RadioSearchMode()
        data class IpLocation(val location: String) : RadioSearchMode()
        data class StateMode(val state: String) : RadioSearchMode()
        data class Language(val language: String) : RadioSearchMode()
        object TopVoted : RadioSearchMode()
        object TopClicked : RadioSearchMode()
        object None : RadioSearchMode()
    }

    private val connectivityObserver = NetworkConnectivityObserver(application)
    val networkStatus: StateFlow<ConnectivityObserver.Status> = connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectivityObserver.Status.Available)

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

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val stations: StateFlow<List<RadioStation>> = repository.allStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<PlaybackHistory>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("Todas")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    val filteredStations: StateFlow<List<RadioStation>> = combine(
        stations,
        _searchQuery,
        _selectedGenre
    ) { stationList, query, genre ->
        var list = stationList
        if (genre != "Todas" && genre.isNotBlank()) {
            list = list.filter { it.genre.contains(genre, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            val q = normalize(query.trim())
            list = list.filter {
                normalize(it.name).contains(q, ignoreCase = true) ||
                        normalize(it.genre).contains(q, ignoreCase = true) ||
                        normalize(it.country ?: "").contains(q, ignoreCase = true) ||
                        normalize(it.region ?: "").contains(q, ignoreCase = true)
            }
        }
        list
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genresList: StateFlow<List<String>> = stations.map { list ->
        val genres = list.map { it.genre }
            .flatMap { it.split("/", ",").map { g -> g.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("Todas") + genres
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todas"))

    private val _sleepSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepSecondsLeft: StateFlow<Int?> = _sleepSecondsLeft.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _selectedRadioProvider = MutableStateFlow("Todas")
    val selectedRadioProvider: StateFlow<String> = _selectedRadioProvider.asStateFlow()

    private val _homeSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val homeSuggestions: StateFlow<List<SearchSuggestion>> = _homeSuggestions.asStateFlow()

    private val _onlineSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val onlineSuggestions: StateFlow<List<SearchSuggestion>> = _onlineSuggestions.asStateFlow()

    private val _radioBrowserStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val radioBrowserStations: StateFlow<List<RadioStation>> = _radioBrowserStations.asStateFlow()

    private val _radioBrowserLoading = MutableStateFlow(false)
    val radioBrowserLoading: StateFlow<Boolean> = _radioBrowserLoading.asStateFlow()

    private val _radioBrowserError = MutableStateFlow<String?>(null)
    val radioBrowserError: StateFlow<String?> = _radioBrowserError.asStateFlow()

    private val _radioBrowserSearchQuery = MutableStateFlow("")
    val radioBrowserSearchQuery: StateFlow<String> = _radioBrowserSearchQuery.asStateFlow()

    private val _userLocationCity = MutableStateFlow("Madrid")
    val userLocationCity: StateFlow<String> = _userLocationCity.asStateFlow()

    private val _userLocationCountry = MutableStateFlow("España")
    val userLocationCountry: StateFlow<String> = _userLocationCountry.asStateFlow()

    private val _userLocationCountryCode = MutableStateFlow("ES")
    val userLocationCountryCode: StateFlow<String> = _userLocationCountryCode.asStateFlow()

    private val _isManualLocation = MutableStateFlow(false)
    val isManualLocation: StateFlow<Boolean> = _isManualLocation.asStateFlow()

    private val _visibleDiscoverCount = MutableStateFlow(20)
    val visibleDiscoverCount: StateFlow<Int> = _visibleDiscoverCount.asStateFlow()

    private var localPlayer: ExoPlayer? = null
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulate()
        }
        initPlayer(application)
        loadRadioBrowserTopVoted()
    }

    private fun initPlayer(application: Application) {
        try {
            val sessionToken = SessionToken(
                application,
                ComponentName(application, PlaybackService::class.java)
            )
            controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
            controllerFuture?.addListener({
                try {
                    mediaController = controllerFuture?.get()
                    setupPlayerListener(mediaController)
                } catch (e: Exception) {
                    setupFallbackExoPlayer(application)
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            setupFallbackExoPlayer(application)
        }
    }

    private fun setupFallbackExoPlayer(application: Application) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(4000, 30000, 2000, 4000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("OpenRadio/1.0 (Android; Linux)")
        val dataSourceFactory = DefaultDataSource.Factory(application, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        localPlayer = ExoPlayer.Builder(application)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
        setupPlayerListener(localPlayer)
    }

    private fun setupPlayerListener(player: Player?) {
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> _playbackStatus.value = PlaybackStatus.BUFFERING
                    Player.STATE_READY -> {
                        _playbackStatus.value = if (player.isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                    }
                    Player.STATE_ENDED -> _playbackStatus.value = PlaybackStatus.IDLE
                    Player.STATE_IDLE -> _playbackStatus.value = PlaybackStatus.IDLE
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playbackStatus.value = PlaybackStatus.PLAYING
                    _errorMessage.value = null
                } else if (_playbackStatus.value != PlaybackStatus.BUFFERING && _playbackStatus.value != PlaybackStatus.ERROR) {
                    _playbackStatus.value = PlaybackStatus.PAUSED
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _currentTrackTitle.value = mediaMetadata.title?.toString()
                _currentTrackArtist.value = mediaMetadata.artist?.toString()
                _currentTrackArtworkUrl.value = mediaMetadata.artworkUri?.toString()
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackStatus.value = PlaybackStatus.ERROR
                _errorMessage.value = "Error al reproducir emisora: ${error.message ?: "Conexión fallida"}"
            }
        })
    }

    private fun getActivePlayer(): Player? = mediaController ?: localPlayer

    fun playStation(station: RadioStation) {
        _currentStation.value = station
        _errorMessage.value = null
        _playbackStatus.value = PlaybackStatus.BUFFERING
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
        _currentTrackArtworkUrl.value = station.faviconUrl

        viewModelScope.launch {
            repository.insertHistory(station)
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.genre)
                    .setArtworkUri(if (station.faviconUrl.isNotBlank()) android.net.Uri.parse(station.faviconUrl) else null)
                    .build()
            )
            .build()

        val player = getActivePlayer()
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    fun togglePlayPause() {
        val player = getActivePlayer() ?: return
        if (player.isPlaying) {
            player.pause()
            _playbackStatus.value = PlaybackStatus.PAUSED
        } else {
            if (_currentStation.value != null) {
                player.play()
                _playbackStatus.value = PlaybackStatus.PLAYING
            } else {
                val list = filteredStations.value
                if (list.isNotEmpty()) {
                    playStation(list.first())
                }
            }
        }
    }

    fun stopPlayback() {
        getActivePlayer()?.stop()
        _playbackStatus.value = PlaybackStatus.IDLE
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
    }

    fun playNextStation() {
        val list = filteredStations.value
        if (list.isEmpty()) return
        val current = _currentStation.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        playStation(list[nextIndex])
    }

    fun playPreviousStation() {
        val list = filteredStations.value
        if (list.isEmpty()) return
        val current = _currentStation.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playStation(list[prevIndex])
    }

    fun playRandomStation() {
        val list = filteredStations.value
        if (list.isNotEmpty()) {
            playStation(list.random())
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        getActivePlayer()?.volume = clamped
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepSecondsLeft.value = null
            return
        }
        var seconds = minutes * 60
        _sleepSecondsLeft.value = seconds
        sleepTimerJob = viewModelScope.launch {
            while (seconds > 0) {
                delay(1000)
                seconds--
                _sleepSecondsLeft.value = seconds
            }
            stopPlayback()
            _sleepSecondsLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepSecondsLeft.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        generateHomeSuggestions(query)
    }

    fun updateGenreFilter(genre: String) {
        _selectedGenre.value = genre
    }

    fun addCustomStation(
        name: String,
        url: String,
        faviconUrl: String,
        genre: String,
        country: String,
        region: String,
        province: String,
        district: String
    ) {
        viewModelScope.launch {
            val station = RadioStation(
                id = 0,
                name = name,
                url = url,
                faviconUrl = faviconUrl,
                genre = genre.ifBlank { "Personalizada" },
                country = country,
                region = region,
                province = province,
                district = district,
                isCustom = true,
                isFavorite = true
            )
            repository.insert(station)
        }
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.update(station.copy(isFavorite = !station.isFavorite))
        }
    }

    fun deleteStation(station: RadioStation) {
        viewModelScope.launch {
            repository.delete(station)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setRadioProvider(provider: String) {
        _selectedRadioProvider.value = provider
    }

    fun loadOnlineStationsForCurrentProvider() {
        loadRadioBrowserTopVoted()
    }

    fun searchRadioBrowser(query: String) {
        _radioBrowserSearchQuery.value = query
        if (query.isBlank()) {
            loadRadioBrowserTopVoted()
            return
        }
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.searchStations(name = query.trim(), limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "No se pudieron cargar emisoras: ${e.message}"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun loadRadioBrowserTopVoted() {
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.getTopVoteStations(limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al cargar estaciones populares"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun loadRadioBrowserTopClicked() {
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.getTopClickStations(limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al cargar estaciones"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByTag(tag: String) {
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.searchStations(tag = tag.trim(), limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al cargar emisoras por género"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByCountry(country: String) {
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.searchStations(country = country.trim(), limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al cargar emisoras por país"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByState(state: String) {
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.searchStations(state = state.trim(), limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al cargar emisoras por región"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByLanguage(language: String) {
        viewModelScope.launch {
            _radioBrowserLoading.value = true
            _radioBrowserError.value = null
            try {
                val results = RadioBrowserApiClient.service.searchStations(language = language.trim(), limit = 60)
                _radioBrowserStations.value = results.map { it.toRadioStation() }
            } catch (e: Exception) {
                _radioBrowserError.value = "Error al cargar emisoras por idioma"
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByDeviceLocation() {
        fetchRadioBrowserByCountry(_userLocationCountry.value)
    }

    fun fetchRadioBrowserByIpLocation(forceRefresh: Boolean = false) {
        fetchRadioBrowserByCountry(_userLocationCountry.value)
    }

    fun setUserManualLocation(countryName: String, countryCode: String, stateName: String, cityName: String) {
        _userLocationCountry.value = countryName
        _userLocationCountryCode.value = countryCode
        _userLocationCity.value = cityName
        _isManualLocation.value = true
        fetchRadioBrowserByCountry(countryName)
    }

    fun resetToAutoIpLocation() {
        _isManualLocation.value = false
        _userLocationCountry.value = "España"
        _userLocationCountryCode.value = "ES"
        _userLocationCity.value = "Madrid"
        fetchRadioBrowserByCountry("España")
    }

    fun loadMoreStations() {
        _visibleDiscoverCount.value += 20
    }

    fun loadMoreDiscoverChunk() {
        _visibleDiscoverCount.value += 20
    }

    fun resetDiscoverChunk() {
        _visibleDiscoverCount.value = 20
    }

    fun saveRadioBrowserStation(station: RadioStation) {
        viewModelScope.launch {
            repository.insert(station.copy(id = 0, isCustom = true, isFavorite = true))
        }
    }

    private fun generateHomeSuggestions(query: String) {
        if (query.isBlank()) {
            _homeSuggestions.value = emptyList()
            return
        }
        val q = normalize(query.trim())
        val list = stations.value
        val suggestions = mutableListOf<SearchSuggestion>()

        list.filter { normalize(it.name).contains(q, ignoreCase = true) }
            .take(3)
            .forEach {
                suggestions.add(SearchSuggestion(it.name, it.genre, it.name, SuggestionType.STATION))
            }

        list.map { it.genre }
            .distinct()
            .filter { normalize(it).contains(q, ignoreCase = true) }
            .take(2)
            .forEach {
                suggestions.add(SearchSuggestion(it, "Género musical", it, SuggestionType.GENRE))
            }

        _homeSuggestions.value = suggestions
    }

    fun normalize(str: String): String {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.getDefault())
    }

    override fun onCleared() {
        localPlayer?.release()
        localPlayer = null
        mediaController?.release()
        mediaController = null
        sleepTimerJob?.cancel()
        super.onCleared()
    }
}

class RadioViewModelFactory(
    private val application: Application,
    private val repository: RadioRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RadioViewModel(application, repository) as T
    }
}
