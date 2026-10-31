package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.DiscoverCacheManager
import com.example.data.MultiSourceRadioClients
import com.example.data.PlaybackHistory
import com.example.data.RadioBrowserApiClient
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.service.PlaybackService
import com.example.util.ConnectivityObserver
import com.example.util.NetworkConnectivityObserver
import com.example.util.StreamFormatHelper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    @androidx.compose.runtime.Immutable
    data class SearchSuggestion(
        val title: String,
        val subtitle: String,
        val query: String,
        val type: SuggestionType
    )

    private val connectivityObserver = NetworkConnectivityObserver(application)
    val networkStatus: StateFlow<ConnectivityObserver.Status> = connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectivityObserver.Status.Available)

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow<String?>(null)
    val currentTrackArtist: StateFlow<String?> = _currentTrackArtist.asStateFlow()

    private val _currentTrackAlbum = MutableStateFlow<String?>(null)
    val currentTrackAlbum: StateFlow<String?> = _currentTrackAlbum.asStateFlow()

    private val _currentTrackArtworkUrl = MutableStateFlow<String?>(null)
    val currentTrackArtworkUrl: StateFlow<String?> = _currentTrackArtworkUrl.asStateFlow()

    private val _playbackStatus = MutableStateFlow(PlaybackStatus.IDLE)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Audio Equalizer & Bass Boost state
    val eqPresets = listOf("Normal", "Rock", "Pop", "Voz / Noticias", "Clásica", "Jazz / Acústico")
    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _selectedEqPreset = MutableStateFlow("Normal")
    val selectedEqPreset: StateFlow<String> = _selectedEqPreset.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0f)
    val bassBoostStrength: StateFlow<Float> = _bassBoostStrength.asStateFlow()

    private var equalizerEffect: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null

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
                        normalize(it.country).contains(q, ignoreCase = true) ||
                        normalize(it.region).contains(q, ignoreCase = true) ||
                        normalize(it.province).contains(q, ignoreCase = true) ||
                        normalize(it.district).contains(q, ignoreCase = true)
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
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todas"))

    private val _sleepSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepSecondsLeft: StateFlow<Int?> = _sleepSecondsLeft.asStateFlow()
    private var sleepTimerJob: Job? = null

    val availableProviders = listOf("RadioBrowser", "SomaFM", "iHeartRadio", "FMStream", "Curadas")
    private val _selectedRadioProvider = MutableStateFlow("RadioBrowser")
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

    private val _visibleDiscoverCount = MutableStateFlow(25)
    val visibleDiscoverCount: StateFlow<Int> = _visibleDiscoverCount.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private var localPlayer: ExoPlayer? = null
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var artworkFetchJob: Job? = null
    private var reconnectJob: Job? = null
    private var retryAttempt = 0

    private val normalizeCache = android.util.LruCache<String, String>(512)

    companion object {
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val BRACKET_ALBUM_REGEX = Regex("^(.*?)\\s*\\[([^]]+)]$")
        private val NON_ALPHANUM_REGEX = Regex("[^\\p{L}\\p{N}\\s-]")
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val stationListType = Types.newParameterizedType(List::class.java, RadioStation::class.java)
    private val stationListAdapter = moshi.adapter<List<RadioStation>>(stationListType)

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulate()
        }
        initPlayer(application)
        loadRadioBrowserTopVoted()
        detectAutoIpLocation(triggerFetch = false)
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
            .setUserAgent("OpenRadio/1.2 (Android; Linux)")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
        val dataSourceFactory = DefaultDataSource.Factory(application, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        localPlayer = ExoPlayer.Builder(application)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build().also {
                StreamFormatHelper.applyAudioOnlyRadioMode(it)
            }
        setupPlayerListener(localPlayer)
    }

    private fun setupPlayerListener(player: Player?) {
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> _playbackStatus.value = PlaybackStatus.BUFFERING
                    Player.STATE_READY -> {
                        retryAttempt = 0
                        reconnectJob?.cancel()
                        _playbackStatus.value = if (player.isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                        bindAudioEffectsIfAvailable()
                    }
                    Player.STATE_ENDED -> _playbackStatus.value = PlaybackStatus.IDLE
                    Player.STATE_IDLE -> {
                        if (_playbackStatus.value != PlaybackStatus.ERROR) {
                            _playbackStatus.value = PlaybackStatus.IDLE
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    retryAttempt = 0
                    _playbackStatus.value = PlaybackStatus.PLAYING
                    _errorMessage.value = null
                } else if (_playbackStatus.value != PlaybackStatus.BUFFERING && _playbackStatus.value != PlaybackStatus.ERROR) {
                    _playbackStatus.value = PlaybackStatus.PAUSED
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) return
                val id = mediaItem.mediaId.toIntOrNull()
                val matched = stations.value.find { it.id == id }
                    ?: _radioBrowserStations.value.find { it.id == id }
                if (matched != null && _currentStation.value?.id != matched.id) {
                    _currentStation.value = matched
                    _currentTrackTitle.value = null
                    _currentTrackArtist.value = null
                    _currentTrackAlbum.value = null
                    _currentTrackArtworkUrl.value = matched.faviconUrl
                } else if (_currentStation.value == null) {
                    val extras = mediaItem.mediaMetadata.extras
                    val url = extras?.getString("station_url") ?: mediaItem.localConfiguration?.uri?.toString() ?: ""
                    val name = extras?.getString("station_name")
                        ?: mediaItem.mediaMetadata.station?.toString()
                        ?: mediaItem.mediaMetadata.title?.toString()
                        ?: "Radio en Vivo"
                    val genre = extras?.getString("station_genre")
                        ?: mediaItem.mediaMetadata.subtitle?.toString()
                        ?: mediaItem.mediaMetadata.artist?.toString()
                        ?: "Directo"
                    val favicon = extras?.getString("station_favicon") ?: mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                    if (url.isNotBlank()) {
                        _currentStation.value = RadioStation(
                            id = id ?: 0,
                            name = name,
                            url = url,
                            genre = genre,
                            faviconUrl = favicon
                        )
                    }
                }
            }

            override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                val builder = MediaMetadata.Builder()
                for (i in 0 until metadata.length()) {
                    metadata.get(i).populateMediaMetadata(builder)
                }
                val populated = builder.build()
                handleIncomingStreamMetadata(
                    rawTitle = populated.title?.toString(),
                    rawArtist = populated.artist?.toString(),
                    rawAlbum = populated.albumTitle?.toString(),
                    rawArtworkUri = populated.artworkUri?.toString()
                )
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val extras = mediaMetadata.extras
                if (extras?.getBoolean(PlaybackService.EXTRA_PARSED_BY_SERVICE) == true) {
                    val srvTitle = extras.getString(PlaybackService.EXTRA_TRACK_TITLE)
                    val srvArtist = extras.getString(PlaybackService.EXTRA_TRACK_ARTIST)
                    val srvAlbum = extras.getString(PlaybackService.EXTRA_TRACK_ALBUM)
                    val srvArt = extras.getString(PlaybackService.EXTRA_TRACK_ARTWORK_URL)
                    _currentTrackTitle.value = srvTitle
                    _currentTrackArtist.value = srvArtist
                    _currentTrackAlbum.value = srvAlbum
                    if (!srvArt.isNullOrBlank()) {
                        _currentTrackArtworkUrl.value = srvArt
                    }
                    return
                }
                handleIncomingStreamMetadata(
                    rawTitle = mediaMetadata.title?.toString(),
                    rawArtist = mediaMetadata.artist?.toString(),
                    rawAlbum = mediaMetadata.albumTitle?.toString(),
                    rawArtworkUri = mediaMetadata.artworkUri?.toString()
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                val station = _currentStation.value
                val currentNet = networkStatus.value
                val hasNetwork = currentNet == ConnectivityObserver.Status.Available ||
                    currentNet == ConnectivityObserver.Status.MobileData
                if (station != null && hasNetwork && retryAttempt < 3) {
                    retryAttempt++
                    val delayMs = retryAttempt * 2500L
                    _playbackStatus.value = PlaybackStatus.BUFFERING
                    _errorMessage.value = "Microcorte detectado. Reconectando ($retryAttempt/3)..."
                    reconnectJob?.cancel()
                    reconnectJob = viewModelScope.launch {
                        delay(delayMs)
                        val activePlayer = getActivePlayer()
                        activePlayer?.prepare()
                        activePlayer?.play()
                    }
                } else {
                    _playbackStatus.value = PlaybackStatus.ERROR
                    _errorMessage.value = "Error al reproducir emisora: ${error.message ?: "Stream no disponible"}"
                }
            }
        })
    }

    private fun handleIncomingStreamMetadata(
        rawTitle: String?,
        rawArtist: String?,
        rawAlbum: String?,
        rawArtworkUri: String?
    ) {
        val stationName = _currentStation.value?.name?.trim().orEmpty()
        val stationGenre = _currentStation.value?.genre?.trim().orEmpty()
        val cleanedRaw = rawTitle?.trim()
            ?.removePrefix("Now Playing:")
            ?.removePrefix("NOW PLAYING:")
            ?.trim()

        if (!cleanedRaw.isNullOrBlank() &&
            !cleanedRaw.equals(stationName, ignoreCase = true) &&
            !cleanedRaw.equals("unknown", ignoreCase = true) &&
            !cleanedRaw.equals("stream", ignoreCase = true)
        ) {
            var parsedArtist: String? = rawArtist?.trim()?.takeIf {
                it.isNotBlank() && !it.equals(stationGenre, ignoreCase = true) && !it.equals(stationName, ignoreCase = true)
            }
            var parsedTitle: String = cleanedRaw
            var parsedAlbum: String? = rawAlbum?.trim()?.takeIf { it.isNotBlank() }

            // Parse ICY combined format: "Artista - Canción" or "Artista - Canción - Álbum"
            val separators = listOf(" - ", " – ", " — ", " / ")
            val matchedSep = separators.firstOrNull { cleanedRaw.contains(it) }
            if (matchedSep != null) {
                val parts = cleanedRaw.split(matchedSep).map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    if (parsedArtist.isNullOrBlank()) {
                        parsedArtist = parts[0]
                    }
                    parsedTitle = parts[1]
                    if (parts.size >= 3 && parsedAlbum.isNullOrBlank()) {
                        parsedAlbum = parts[2]
                    }
                }
            }

            // Extract album in brackets/parentheses if formatted like "Song Title [Album Name]"
            val bracketRegex = BRACKET_ALBUM_REGEX.find(parsedTitle)
            if (bracketRegex != null && parsedAlbum.isNullOrBlank()) {
                parsedTitle = bracketRegex.groupValues[1].trim()
                parsedAlbum = bracketRegex.groupValues[2].trim()
            }

            _currentTrackTitle.value = parsedTitle
            _currentTrackArtist.value = parsedArtist
            if (!parsedAlbum.isNullOrBlank()) {
                _currentTrackAlbum.value = parsedAlbum
            }
            fetchTrackMetadataFromItunes(parsedTitle, parsedArtist)
        }

        if (!rawArtworkUri.isNullOrBlank()) {
            _currentTrackArtworkUrl.value = rawArtworkUri
        }
    }

    private fun fetchTrackMetadataFromItunes(title: String, artist: String?) {
        artworkFetchJob?.cancel()
        artworkFetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val searchTerm = listOfNotNull(artist, title)
                    .joinToString(" ")
                    .replace(NON_ALPHANUM_REGEX, " ")
                    .trim()
                if (searchTerm.length < 3) return@launch
                val match = MultiSourceRadioClients.lookupSongCached(searchTerm) ?: return@launch

                if (_currentTrackArtist.value.isNullOrBlank() && !match.artistName.isNullOrBlank()) {
                    _currentTrackArtist.value = match.artistName
                }
                if (!match.collectionName.isNullOrBlank()) {
                    _currentTrackAlbum.value = match.collectionName
                }
                val highResArtwork = match.artworkUrl100?.replace("100x100bb", "600x600bb")
                if (!highResArtwork.isNullOrBlank()) {
                    _currentTrackArtworkUrl.value = highResArtwork
                }
            } catch (_: Exception) {
                // Keep parsed ICY metadata and station logo if iTunes lookup fails
            }
        }
    }

    private fun bindAudioEffectsIfAvailable() {
        val sessionId = localPlayer?.audioSessionId ?: return
        if (sessionId <= 0) return
        try {
            if (equalizerEffect == null) {
                equalizerEffect = Equalizer(0, sessionId).apply {
                    enabled = _equalizerEnabled.value
                }
            }
            if (bassBoostEffect == null) {
                bassBoostEffect = BassBoost(0, sessionId).apply {
                    enabled = _equalizerEnabled.value
                }
            }
            applyEqualizerSettings()
        } catch (_: Exception) {
            // Device/Emulator without hardware DSP effects
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        try {
            equalizerEffect?.enabled = enabled
            bassBoostEffect?.enabled = enabled
            if (enabled) applyEqualizerSettings()
        } catch (_: Exception) {}
    }

    fun selectEqualizerPreset(presetName: String) {
        _selectedEqPreset.value = presetName
        if (!_equalizerEnabled.value) {
            _equalizerEnabled.value = true
        }
        applyEqualizerSettings()
    }

    fun setBassBoost(strength: Float) {
        val clamped = strength.coerceIn(0f, 1f)
        _bassBoostStrength.value = clamped
        if (!_equalizerEnabled.value && clamped > 0f) {
            _equalizerEnabled.value = true
        }
        applyEqualizerSettings()
    }

    private fun applyEqualizerSettings() {
        try {
            val eq = equalizerEffect
            if (eq != null) {
                eq.enabled = _equalizerEnabled.value
                val bands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLvl = range[0].toInt()
                val maxLvl = range[1].toInt()
                val mid = (minLvl + maxLvl) / 2
                val boost = (maxLvl - mid) / 2

                val profile = when (_selectedEqPreset.value) {
                    "Rock" -> listOf(boost, boost / 2, -boost / 3, boost / 2, boost)
                    "Pop" -> listOf(-boost / 4, boost / 2, boost, boost / 2, -boost / 4)
                    "Voz / Noticias" -> listOf(-boost / 2, boost / 3, boost, boost / 2, -boost / 2)
                    "Clásica" -> listOf(boost / 2, boost / 3, 0, boost / 3, boost / 2)
                    "Jazz / Acústico" -> listOf(boost / 2, 0, boost / 3, boost / 2, boost / 3)
                    else -> listOf(0, 0, 0, 0, 0)
                }
                for (b in 0 until bands) {
                    val targetOffset = profile.getOrElse(b % profile.size) { 0 }
                    val level = (mid + targetOffset).coerceIn(minLvl, maxLvl).toShort()
                    eq.setBandLevel(b.toShort(), level)
                }
            }
            val bb = bassBoostEffect
            if (bb != null && bb.strengthSupported) {
                bb.enabled = _equalizerEnabled.value
                bb.setStrength((_bassBoostStrength.value * 1000).toInt().toShort())
            }
        } catch (_: Exception) {}
    }

    private fun getActivePlayer(): Player? = mediaController ?: localPlayer

    fun playStation(station: RadioStation) {
        reconnectJob?.cancel()
        artworkFetchJob?.cancel()
        retryAttempt = 0
        _currentStation.value = station
        _errorMessage.value = null
        _playbackStatus.value = PlaybackStatus.BUFFERING
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
        _currentTrackAlbum.value = null
        _currentTrackArtworkUrl.value = station.faviconUrl

        viewModelScope.launch {
            repository.insertHistory(station)
        }

        viewModelScope.launch {
            val playableUrl = StreamFormatHelper.resolvePlayableStreamUrl(station.url)
            val extras = Bundle().apply {
                putString("station_name", station.name)
                putString("station_genre", station.genre)
                putString("station_url", playableUrl)
                putString("station_country", station.country)
                putString("station_region", station.region)
                putString("station_favicon", station.faviconUrl)
            }

            val metadata = MediaMetadata.Builder()
                .setStation(station.name)
                .setSubtitle(station.genre)
                .setGenre(station.genre)
                .setArtworkUri(if (station.faviconUrl.isNotBlank()) Uri.parse(station.faviconUrl) else null)
                .setExtras(extras)
                .build()

            val mediaItem = StreamFormatHelper.buildMediaItem(
                mediaId = station.id.toString(),
                streamUrl = playableUrl,
                mediaMetadata = metadata
            )

            val player = getActivePlayer()
            if (player != null) {
                StreamFormatHelper.applyAudioOnlyRadioMode(player)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
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
        reconnectJob?.cancel()
        artworkFetchJob?.cancel()
        getActivePlayer()?.stop()
        _playbackStatus.value = PlaybackStatus.IDLE
        _currentTrackTitle.value = null
        _currentTrackArtist.value = null
        _currentTrackAlbum.value = null
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
        sendSleepTimerToService(minutes)
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
        sendSleepTimerToService(0)
        _sleepSecondsLeft.value = null
    }

    private fun sendSleepTimerToService(minutes: Int) {
        try {
            val args = Bundle().apply {
                putInt(PlaybackService.ARG_SLEEP_MINUTES, minutes)
            }
            mediaController?.sendCustomCommand(
                SessionCommand(PlaybackService.CUSTOM_COMMAND_SLEEP_TIMER, Bundle.EMPTY),
                args
            )
        } catch (_: Exception) {}
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
            val updated = station.copy(isFavorite = !station.isFavorite)
            repository.update(updated)
            if (_currentStation.value?.id == station.id) {
                _currentStation.value = updated
            }
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
        _visibleDiscoverCount.value = 25
        loadOnlineStationsForCurrentProvider()
    }

    fun loadOnlineStationsForCurrentProvider() {
        val provider = _selectedRadioProvider.value
        val query = _radioBrowserSearchQuery.value.trim()
        val cacheQueryKey = query.ifBlank { "top" }
        val appContext = getApplication<Application>()

        // 1. Instant 0ms RAM cache hit if available
        DiscoverCacheManager.getMemoryCachedStations(provider, cacheQueryKey)?.let { ramCached ->
            if (ramCached.isNotEmpty()) {
                _radioBrowserStations.value = ramCached
            }
        }

        // 2. Disk cache fallback + Network refresh by provider
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cached = DiscoverCacheManager.getCachedStationsAsync(appContext, provider, cacheQueryKey)
            if (cached.isNotEmpty() && _radioBrowserStations.value.isEmpty()) {
                _radioBrowserStations.value = cached
            }
            _radioBrowserLoading.value = cached.isEmpty()
            _radioBrowserError.value = null
            try {
                val freshStations: List<RadioStation> = withContext(Dispatchers.IO) {
                    when (provider) {
                        "SomaFM" -> {
                            val channels = MultiSourceRadioClients.somaFmService.getChannels().channels.orEmpty()
                            val mapped = channels.map { it.toRadioStation() }
                            if (query.isNotBlank()) {
                                mapped.filter {
                                    it.name.contains(query, ignoreCase = true) ||
                                            it.genre.contains(query, ignoreCase = true)
                                }
                            } else mapped
                        }
                        "iHeartRadio" -> {
                            val response = MultiSourceRadioClients.iHeartService.getLiveStations(
                                keywords = query.ifBlank { "music" },
                                limit = 40
                            )
                            val list = (response.hits ?: response.items).orEmpty().map { it.toRadioStation() }
                            list.ifEmpty { MultiSourceRadioClients.fallbackFmStreamList }
                        }
                        "FMStream" -> {
                            val list = try {
                                MultiSourceRadioClients.fmStreamService.searchFmStream(query = query.ifBlank { "pop" })
                                    .map { it.toRadioStation() }
                            } catch (_: Exception) {
                                emptyList()
                            }
                            val fallback = MultiSourceRadioClients.fallbackFmStreamList
                            val base = list.ifEmpty { fallback }
                            if (query.isNotBlank()) {
                                base.filter {
                                    it.name.contains(query, ignoreCase = true) ||
                                            it.genre.contains(query, ignoreCase = true) ||
                                            it.country.contains(query, ignoreCase = true)
                                }.ifEmpty { base }
                            } else base
                        }
                        "Curadas" -> {
                            val defaultCurated = RadioRepository.getDefaultStations()
                                .mapIndexed { idx, s -> s.copy(id = 9500 + idx) }
                            val curated = defaultCurated + MultiSourceRadioClients.fallbackGitHubCuratedList + MultiSourceRadioClients.fallbackFmStreamList
                            if (query.isNotBlank()) {
                                curated.filter {
                                    it.name.contains(query, ignoreCase = true) ||
                                            it.genre.contains(query, ignoreCase = true) ||
                                            it.country.contains(query, ignoreCase = true) ||
                                            it.region.contains(query, ignoreCase = true) ||
                                            it.province.contains(query, ignoreCase = true) ||
                                            it.district.contains(query, ignoreCase = true)
                                }
                            } else curated
                        }
                        else -> {
                            if (query.isBlank()) {
                                RadioBrowserApiClient.service.getTopVoteStations(limit = 60).map { it.toRadioStation() }
                            } else {
                                RadioBrowserApiClient.service.searchStations(name = query, limit = 60).map { it.toRadioStation() }
                            }
                        }
                    }
                }
                if (freshStations.isNotEmpty()) {
                    _radioBrowserStations.value = freshStations
                    DiscoverCacheManager.saveCachedStationsAsync(appContext, provider, cacheQueryKey, freshStations)
                } else if (cached.isEmpty()) {
                    _radioBrowserStations.value = emptyList()
                }
            } catch (e: Exception) {
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserError.value = "No se pudieron cargar emisoras online (${e.localizedMessage ?: "Error de red"})"
                }
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun searchRadioBrowser(query: String) {
        _radioBrowserSearchQuery.value = query
        _visibleDiscoverCount.value = 25
        searchJob?.cancel()
        if (query.isBlank()) {
            loadOnlineStationsForCurrentProvider()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // Debounce 350ms to prevent race conditions while typing
            loadOnlineStationsForCurrentProvider()
        }
    }

    fun loadRadioBrowserTopVoted() {
        _selectedRadioProvider.value = "RadioBrowser"
        _radioBrowserSearchQuery.value = ""
        _visibleDiscoverCount.value = 25
        val appContext = getApplication<Application>()
        DiscoverCacheManager.getMemoryCachedStations("RadioBrowser", "top_voted")?.let {
            if (it.isNotEmpty()) _radioBrowserStations.value = it
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cached = DiscoverCacheManager.getCachedStationsAsync(appContext, "RadioBrowser", "top_voted")
            if (cached.isNotEmpty()) {
                _radioBrowserStations.value = cached
            }
            _radioBrowserLoading.value = cached.isEmpty()
            _radioBrowserError.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    RadioBrowserApiClient.service.getTopVoteStations(limit = 60).map { it.toRadioStation() }
                }
                _radioBrowserStations.value = results
                DiscoverCacheManager.saveCachedStationsAsync(appContext, "RadioBrowser", "top_voted", results)
            } catch (e: Exception) {
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserError.value = "Error al cargar estaciones populares"
                }
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun loadRadioBrowserTopClicked() {
        _selectedRadioProvider.value = "RadioBrowser"
        _visibleDiscoverCount.value = 25
        val appContext = getApplication<Application>()
        DiscoverCacheManager.getMemoryCachedStations("RadioBrowser", "top_clicked")?.let {
            if (it.isNotEmpty()) _radioBrowserStations.value = it
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cached = DiscoverCacheManager.getCachedStationsAsync(appContext, "RadioBrowser", "top_clicked")
            if (cached.isNotEmpty()) {
                _radioBrowserStations.value = cached
            }
            _radioBrowserLoading.value = cached.isEmpty()
            _radioBrowserError.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    RadioBrowserApiClient.service.getTopClickStations(limit = 60).map { it.toRadioStation() }
                }
                _radioBrowserStations.value = results
                DiscoverCacheManager.saveCachedStationsAsync(appContext, "RadioBrowser", "top_clicked", results)
            } catch (e: Exception) {
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserError.value = "Error al cargar estaciones en tendencia"
                }
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByTag(tag: String) {
        _selectedRadioProvider.value = "RadioBrowser"
        _visibleDiscoverCount.value = 25
        val appContext = getApplication<Application>()
        val cacheKey = "tag_${tag.trim().lowercase()}"
        DiscoverCacheManager.getMemoryCachedStations("RadioBrowser", cacheKey)?.let {
            if (it.isNotEmpty()) _radioBrowserStations.value = it
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cached = DiscoverCacheManager.getCachedStationsAsync(appContext, "RadioBrowser", cacheKey)
            if (cached.isNotEmpty()) {
                _radioBrowserStations.value = cached
            }
            _radioBrowserLoading.value = cached.isEmpty()
            _radioBrowserError.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    RadioBrowserApiClient.service.searchStations(tag = tag.trim(), limit = 60).map { it.toRadioStation() }
                }
                _radioBrowserStations.value = results
                DiscoverCacheManager.saveCachedStationsAsync(appContext, "RadioBrowser", cacheKey, results)
            } catch (e: Exception) {
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserError.value = "Error al cargar emisoras por género"
                }
            } finally {
                _radioBrowserLoading.value = false
            }
        }
    }

    fun fetchRadioBrowserByCountry(country: String) {
        _selectedRadioProvider.value = "RadioBrowser"
        _visibleDiscoverCount.value = 25
        val appContext = getApplication<Application>()
        val cacheKey = "country_${country.trim().lowercase()}"
        DiscoverCacheManager.getMemoryCachedStations("RadioBrowser", cacheKey)?.let {
            if (it.isNotEmpty()) _radioBrowserStations.value = it
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val cached = DiscoverCacheManager.getCachedStationsAsync(appContext, "RadioBrowser", cacheKey)
            if (cached.isNotEmpty()) {
                _radioBrowserStations.value = cached
            }
            _radioBrowserLoading.value = cached.isEmpty()
            _radioBrowserError.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    RadioBrowserApiClient.service.searchStations(country = country.trim(), limit = 60).map { it.toRadioStation() }
                }
                _radioBrowserStations.value = results
                DiscoverCacheManager.saveCachedStationsAsync(appContext, "RadioBrowser", cacheKey, results)
            } catch (e: Exception) {
                if (_radioBrowserStations.value.isEmpty()) {
                    _radioBrowserError.value = "Error al cargar emisoras por país"
                }
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
        detectAutoIpLocation(triggerFetch = true)
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
        detectAutoIpLocation(triggerFetch = true)
    }

    private fun detectAutoIpLocation(triggerFetch: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geo = try {
                    MultiSourceRadioClients.ipApiCoService.getGeoLocation()
                } catch (_: Exception) {
                    MultiSourceRadioClients.ipGeoService.getGeoLocation()
                }
                val country = geo.resolvedCountry()
                val code = geo.resolvedCountryCode()
                val city = geo.resolvedCity()
                if (country.isNotBlank() && !_isManualLocation.value) {
                    _userLocationCountry.value = country
                    if (code.isNotBlank()) _userLocationCountryCode.value = code
                    if (city.isNotBlank()) _userLocationCity.value = city
                    if (triggerFetch) {
                        fetchRadioBrowserByCountry(country)
                    }
                    return@launch
                }
            } catch (_: Exception) {
                // Fallback if offline or blocked
            }
            if (triggerFetch) {
                fetchRadioBrowserByCountry(_userLocationCountry.value)
            }
        }
    }

    fun loadMoreStations() {
        _visibleDiscoverCount.value += 25
    }

    fun loadMoreDiscoverChunk() {
        _visibleDiscoverCount.value += 25
    }

    fun resetDiscoverChunk() {
        _visibleDiscoverCount.value = 25
    }

    fun toggleSaveRadioBrowserStation(station: RadioStation) {
        viewModelScope.launch {
            val existing = stations.value.find {
                it.url.equals(station.url, ignoreCase = true) ||
                        (it.name.equals(station.name, ignoreCase = true) && it.country.equals(station.country, ignoreCase = true))
            }
            if (existing != null) {
                if (existing.isCustom) {
                    repository.delete(existing)
                } else {
                    repository.update(existing.copy(isFavorite = !existing.isFavorite))
                }
            } else {
                repository.insert(station.copy(id = 0, isCustom = true, isFavorite = true))
            }
        }
    }

    fun saveRadioBrowserStation(station: RadioStation) {
        toggleSaveRadioBrowserStation(station)
    }

    // Export / Import for SyncTab
    fun exportFavoritesToJsonUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favs = stations.value.filter { it.isFavorite }
                val json = stationListAdapter.toJson(favs)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                }
                _syncStatusMessage.value = "Se exportaron ${favs.size} emisoras favoritas en JSON correctamente."
            } catch (e: Exception) {
                _syncStatusMessage.value = "Error al exportar JSON: ${e.localizedMessage}"
            }
        }
    }

    fun importFavoritesFromJsonUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    val imported = stationListAdapter.fromJson(json).orEmpty()
                    var addedCount = 0
                    val currentUrls = stations.value.map { it.url.lowercase() }.toSet()
                    imported.forEach { station ->
                        if (station.url.isNotBlank() && !currentUrls.contains(station.url.lowercase())) {
                            repository.insert(station.copy(id = 0, isFavorite = true, isCustom = true))
                            addedCount++
                        }
                    }
                    _syncStatusMessage.value = "Se importaron $addedCount nuevas emisoras a tu biblioteca."
                } else {
                    _syncStatusMessage.value = "El archivo seleccionado está vacío."
                }
            } catch (e: Exception) {
                _syncStatusMessage.value = "Error al importar archivo: formato no válido."
            }
        }
    }

    fun exportPlaylistToM3uUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favs = stations.value.filter { it.isFavorite }.ifEmpty { stations.value }
                val sb = StringBuilder("#EXTM3U\n")
                favs.forEach { s ->
                    sb.append("#EXTINF:-1 tvg-logo=\"${s.faviconUrl}\" group-title=\"${s.genre}\",${s.name} (${s.country})\n")
                    sb.append("${s.url}\n")
                }
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
                _syncStatusMessage.value = "Lista M3U exportada (${favs.size} emisoras) compatible con VLC y reproductores externos."
            } catch (e: Exception) {
                _syncStatusMessage.value = "Error al exportar lista M3U: ${e.localizedMessage}"
            }
        }
    }

    fun clearSyncStatusMessage() {
        _syncStatusMessage.value = null
    }

    private fun generateHomeSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.isBlank()) {
            _homeSuggestions.value = emptyList()
            return
        }
        suggestionJob = viewModelScope.launch(Dispatchers.Default) {
            val q = normalize(query.trim())
            val list = stations.value
            val suggestions = mutableListOf<SearchSuggestion>()

            list.filter { normalize(it.name).contains(q, ignoreCase = true) }
                .take(3)
                .forEach {
                    val locInfo = listOf(it.province, it.region, it.country).filter { p -> p.isNotBlank() }.distinct().joinToString(", ")
                    val sub = if (locInfo.isNotBlank()) "${it.genre} • $locInfo" else it.genre
                    suggestions.add(SearchSuggestion(it.name, sub, it.name, SuggestionType.STATION))
                }

            list.mapNotNull { it.province.takeIf { p -> p.isNotBlank() } }
                .distinct()
                .filter { normalize(it).contains(q, ignoreCase = true) }
                .take(2)
                .forEach {
                    suggestions.add(SearchSuggestion(it, "Provincia / Localidad", it, SuggestionType.LOCATION))
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
    }

    fun normalize(str: String): String {
        if (str.isEmpty()) return ""
        normalizeCache.get(str)?.let { return it }
        val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .lowercase(Locale.getDefault())
        normalizeCache.put(str, normalized)
        return normalized
    }

    override fun onCleared() {
        try {
            equalizerEffect?.release()
            bassBoostEffect?.release()
        } catch (_: Exception) {}
        equalizerEffect = null
        bassBoostEffect = null
        localPlayer?.release()
        localPlayer = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        sleepTimerJob?.cancel()
        reconnectJob?.cancel()
        searchJob?.cancel()
        suggestionJob?.cancel()
        artworkFetchJob?.cancel()
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
