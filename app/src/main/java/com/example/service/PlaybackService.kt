package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : MediaLibraryService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var repository: RadioRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var serviceSleepTimerJob: Job? = null

    companion object {
        const val CUSTOM_COMMAND_PREVIOUS = "CUSTOM_COMMAND_PREVIOUS"
        const val CUSTOM_COMMAND_NEXT = "CUSTOM_COMMAND_NEXT"
        const val CUSTOM_COMMAND_FAVORITE = "CUSTOM_COMMAND_FAVORITE"
        const val CUSTOM_COMMAND_RANDOM = "CUSTOM_COMMAND_RANDOM"
        const val CUSTOM_COMMAND_STOP = "CUSTOM_COMMAND_STOP"
        const val CUSTOM_COMMAND_SLEEP_TIMER = "CUSTOM_COMMAND_SLEEP_TIMER"
        const val ARG_SLEEP_MINUTES = "ARG_SLEEP_MINUTES"

        private const val ROOT_ID = "root_stations"
        private const val FOLDER_FAVORITES = "folder_favorites"
        private const val FOLDER_RECENT = "folder_recent"
        private const val FOLDER_ALL = "folder_all"
    }

    override fun onCreate() {
        super.onCreate()
        val database = RadioDatabase.getDatabase(this)
        repository = RadioRepository(database.radioDao(), database.playbackHistoryDao())

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(4000, 30000, 2000, 4000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("OpenRadio/1.2 (Android; Linux)")

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer = player

        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = sessionIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val prevCommand = SessionCommand(CUSTOM_COMMAND_PREVIOUS, Bundle.EMPTY)
        val prevButton = CommandButton.Builder()
            .setSessionCommand(prevCommand)
            .setDisplayName("Anterior")
            .setIconResId(android.R.drawable.ic_media_previous)
            .build()

        val nextCommand = SessionCommand(CUSTOM_COMMAND_NEXT, Bundle.EMPTY)
        val nextButton = CommandButton.Builder()
            .setSessionCommand(nextCommand)
            .setDisplayName("Siguiente")
            .setIconResId(android.R.drawable.ic_media_next)
            .build()

        val favoriteCommand = SessionCommand(CUSTOM_COMMAND_FAVORITE, Bundle.EMPTY)
        val favoriteButton = CommandButton.Builder()
            .setSessionCommand(favoriteCommand)
            .setDisplayName("Favorito")
            .setIconResId(android.R.drawable.btn_star)
            .build()

        val randomCommand = SessionCommand(CUSTOM_COMMAND_RANDOM, Bundle.EMPTY)
        val randomButton = CommandButton.Builder()
            .setSessionCommand(randomCommand)
            .setDisplayName("Aleatorio")
            .setIconResId(android.R.drawable.ic_menu_rotate)
            .build()

        val stopCommand = SessionCommand(CUSTOM_COMMAND_STOP, Bundle.EMPTY)
        val stopButton = CommandButton.Builder()
            .setSessionCommand(stopCommand)
            .setDisplayName("Detener")
            .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
            .build()

        val sleepTimerCommand = SessionCommand(CUSTOM_COMMAND_SLEEP_TIMER, Bundle.EMPTY)

        val sessionCallback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(prevCommand)
                    .add(nextCommand)
                    .add(favoriteCommand)
                    .add(randomCommand)
                    .add(stopCommand)
                    .add(sleepTimerCommand)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(ImmutableList.of(prevButton, nextButton, favoriteButton, randomButton, stopButton))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    CUSTOM_COMMAND_PREVIOUS -> playPreviousStationInService()
                    CUSTOM_COMMAND_NEXT -> playNextStationInService()
                    CUSTOM_COMMAND_FAVORITE -> toggleFavorite()
                    CUSTOM_COMMAND_RANDOM -> playRandomStationInService()
                    CUSTOM_COMMAND_STOP -> stopPlaybackInService()
                    CUSTOM_COMMAND_SLEEP_TIMER -> {
                        val minutes = args.getInt(ARG_SLEEP_MINUTES, 0)
                        scheduleServiceSleepTimer(minutes)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("OpenRadio")
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch {
                    when (parentId) {
                        ROOT_ID -> {
                            val folders = listOf(
                                buildFolderItem(FOLDER_FAVORITES, "Favoritas", "Tus emisoras guardadas"),
                                buildFolderItem(FOLDER_RECENT, "Recientes", "Últimas emisoras escuchadas"),
                                buildFolderItem(FOLDER_ALL, "Todas las Emisoras", "Catálogo completo")
                            )
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(folders), params))
                        }
                        FOLDER_FAVORITES -> {
                            val all = repository.allStations.first()
                            val favs = all.filter { it.isFavorite }.ifEmpty { all }
                            val items = favs.map { mapStationToMediaItem(it) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        }
                        FOLDER_RECENT -> {
                            val all = repository.allStations.first()
                            val recent = repository.recentHistory.first()
                            val matched = recent.mapNotNull { h ->
                                all.find { it.id.toLong() == h.stationId || it.name == h.stationName }
                            }.distinctBy { it.id }.ifEmpty { all.take(10) }
                            val items = matched.map { mapStationToMediaItem(it) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        }
                        else -> {
                            val stations = repository.allStations.first()
                            val mediaItems = stations.map { mapStationToMediaItem(it) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                        }
                    }
                }
                return future
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val future = SettableFuture.create<LibraryResult<MediaItem>>()
                serviceScope.launch {
                    val id = mediaId.toLongOrNull()
                    val station = if (id != null) repository.getStationById(id) else null
                    if (station != null) {
                        future.set(LibraryResult.ofItem(mapStationToMediaItem(station), null))
                    } else {
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }
                return future
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val future = SettableFuture.create<MutableList<MediaItem>>()
                serviceScope.launch {
                    val allStations = repository.allStations.first()
                    val resolved = mediaItems.map { item ->
                        if (item.localConfiguration?.uri != null) {
                            item
                        } else {
                            val id = item.mediaId.toIntOrNull()
                            val station = allStations.find { it.id == id } ?: allStations.firstOrNull()
                            if (station != null) mapStationToMediaItem(station) else item
                        }
                    }.toMutableList()
                    future.set(resolved)
                }
                return future
            }
        }

        val builder = MediaLibrarySession.Builder(this, player, sessionCallback)
        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent)
        }
        mediaLibrarySession = builder.build()
    }

    private fun buildFolderItem(id: String, title: String, subtitle: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .build()
            )
            .build()
    }

    private fun scheduleServiceSleepTimer(minutes: Int) {
        serviceSleepTimerJob?.cancel()
        if (minutes <= 0) return
        serviceSleepTimerJob = serviceScope.launch {
            delay(minutes * 60_000L)
            stopPlaybackInService()
        }
    }

    private fun mapStationToMediaItem(station: RadioStation): MediaItem {
        val extras = Bundle().apply {
            putString("station_url", station.url)
            putString("station_country", station.country)
            putString("station_region", station.region)
            putString("station_favicon", station.faviconUrl)
        }
        return MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.genre)
                    .setArtworkUri(if (station.faviconUrl.isNotBlank()) android.net.Uri.parse(station.faviconUrl) else null)
                    .setExtras(extras)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private fun toggleFavorite() {
        serviceScope.launch {
            val currentMediaId = exoPlayer?.currentMediaItem?.mediaId?.toLongOrNull() ?: return@launch
            val station = repository.getStationById(currentMediaId) ?: return@launch
            repository.update(station.copy(isFavorite = !station.isFavorite))
        }
    }

    private fun playNextStationInService() {
        serviceScope.launch {
            val stations = repository.allStations.first()
            if (stations.isEmpty()) return@launch
            val currentId = exoPlayer?.currentMediaItem?.mediaId?.toIntOrNull()
            val currentIndex = stations.indexOfFirst { it.id == currentId }
            val nextIndex = if (currentIndex in 0 until stations.size - 1) currentIndex + 1 else 0
            playStationInService(stations[nextIndex])
        }
    }

    private fun playPreviousStationInService() {
        serviceScope.launch {
            val stations = repository.allStations.first()
            if (stations.isEmpty()) return@launch
            val currentId = exoPlayer?.currentMediaItem?.mediaId?.toIntOrNull()
            val currentIndex = stations.indexOfFirst { it.id == currentId }
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else stations.size - 1
            playStationInService(stations[prevIndex])
        }
    }

    private fun playRandomStationInService() {
        serviceScope.launch {
            val stations = repository.allStations.first()
            if (stations.isNotEmpty()) {
                playStationInService(stations.random())
            }
        }
    }

    private fun stopPlaybackInService() {
        exoPlayer?.stop()
    }

    private suspend fun playStationInService(station: RadioStation) {
        val mediaItem = mapStationToMediaItem(station)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        repository.insertHistory(station)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        serviceSleepTimerJob?.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        exoPlayer = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (exoPlayer?.playWhenReady == false || exoPlayer?.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
