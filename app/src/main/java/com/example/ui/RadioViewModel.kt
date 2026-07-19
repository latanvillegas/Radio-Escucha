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
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.data.PlaybackHistory
import com.example.util.ConnectivityObserver
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    init {
        // Build and initialize ExoPlayer
        setupPlayer()

        // Sync initial DB items
        viewModelScope.launch {
            try { repository.checkAndPrepopulate(); android.util.Log.d("RadioViewModel", "DB prepopulated!") } catch (e: Exception) { android.util.Log.e("RadioViewModel", "Error DB", e) }
        }
    }

    private fun setupPlayer() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
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
                        override fun onPlayerError(error: PlaybackException) {
                            _playbackStatus.value = PlaybackStatus.ERROR
                            _errorMessage.value = "Error de red o stream no disponible"
                            _currentStation.value = null
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
        
        viewModelScope.launch {
            repository.insertHistory(station)
        }

        mediaController?.let { player ->
            try {
                player.stop()
                player.clearMediaItems()
                                val mediaItem = MediaItem.Builder()
                    .setUri(station.url)
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

    fun togglePlayPause() {
        val player = mediaController ?: return
        val current = _currentStation.value ?: return

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
