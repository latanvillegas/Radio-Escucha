package com.example.service

import android.app.PendingIntent
import android.content.Intent
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionCommand
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionResult
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import android.os.Bundle
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.data.MultiSourceRadioClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.guava.future
import com.example.R
import com.google.common.collect.ImmutableList

class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: RadioRepository

    private val CUSTOM_COMMAND_PREVIOUS = "ACTION_PREVIOUS"
    private val CUSTOM_COMMAND_NEXT = "ACTION_NEXT"
    private val CUSTOM_COMMAND_FAVORITE = "ACTION_FAVORITE"
    private val CUSTOM_COMMAND_RANDOM = "ACTION_RANDOM"
    private val CUSTOM_COMMAND_STOP = "ACTION_STOP"
    private val ROOT_ID = "ROOT"

    override fun onCreate() {
        super.onCreate()
        val database = RadioDatabase.getDatabase(this)
        repository = RadioRepository(database.radioDao(), database.playbackHistoryDao())

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 4_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("OpenRadio/1.0 (Android; Linux)")

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handleAudioFocus
            )
            .build()

        val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
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

        val callback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(prevCommand)
                    .add(nextCommand)
                    .add(favoriteCommand)
                    .add(randomCommand)
                    .add(stopCommand)
                    .build()

                val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .setCustomLayout(listOf(prevButton, favoriteButton, randomButton, nextButton, stopButton))
                    .build()
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                if (playerCommand == Player.COMMAND_SEEK_TO_NEXT || playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                    playNextStationInService()
                    return SessionResult.RESULT_SUCCESS
                }
                if (playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS || playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                    playPreviousStationInService()
                    return SessionResult.RESULT_SUCCESS
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    CUSTOM_COMMAND_PREVIOUS -> {
                        playPreviousStationInService()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    CUSTOM_COMMAND_NEXT -> {
                        playNextStationInService()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    CUSTOM_COMMAND_FAVORITE -> {
                        toggleFavorite()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    CUSTOM_COMMAND_RANDOM -> {
                        playRandomStationInService()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    CUSTOM_COMMAND_STOP -> {
                        stopPlaybackInService()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: MediaLibraryService.LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootMetadata = MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setTitle("Radios")
                    .build()
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(rootMetadata)
                    .build()
                
                // For legacy browsers, some expect a non-null params even if empty
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params ?: MediaLibraryService.LibraryParams.Builder().build()))
            }

            override fun onPlaybackResumption(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaItemsWithStartPosition> {
                return serviceScope.future {
                    try {
                        val history = repository.recentHistory.first()
                        val lastStation = if (history.isNotEmpty()) {
                            repository.getStationById(history.first().stationId)
                        } else {
                            repository.allStations.first().firstOrNull()
                        }

                        val stationToPlay = lastStation ?: MultiSourceRadioClients.fallbackGitHubCuratedList.first()
                        val mediaItem = mapStationToMediaItem(stationToPlay)
                        MediaItemsWithStartPosition(listOf(mediaItem), 0, 0L)
                    } catch (e: Exception) {
                        val fallback = MultiSourceRadioClients.fallbackGitHubCuratedList.first()
                        val mediaItem = mapStationToMediaItem(fallback)
                        MediaItemsWithStartPosition(listOf(mediaItem), 0, 0L)
                    }
                }
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return serviceScope.future {
                    if (parentId == ROOT_ID) {
                        val stations = repository.allStations.first()
                        val mediaItems = stations.map { mapStationToMediaItem(it) }
                        LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params)
                    } else {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    }
                }
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return serviceScope.future {
                    val stationId = mediaId.toLongOrNull()
                    if (stationId != null) {
                        val station = repository.getStationById(stationId)
                        if (station != null) {
                            LibraryResult.ofItem(mapStationToMediaItem(station), null)
                        } else {
                            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                        }
                    } else {
                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                    }
                }
            }
        }

        val builder = MediaLibrarySession.Builder(this, exoPlayer!!, callback)
            
        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent)
        }
        mediaLibrarySession = builder.build()
    }

    private fun mapStationToMediaItem(station: RadioStation): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setSubtitle(station.genre)
            .setArtist(station.genre)
            .setAlbumTitle("Radios en Vivo")
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build()
        
        return MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.url)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun toggleFavorite() {
        val currentMediaItem = exoPlayer?.currentMediaItem ?: return
        val stationId = currentMediaItem.mediaId.toLongOrNull() ?: return
        
        serviceScope.launch {
            val station = repository.getStationById(stationId)
            if (station != null) {
                repository.update(station.copy(isFavorite = !station.isFavorite))
            }
        }
    }

    private fun playNextStationInService() {
        serviceScope.launch {
            val stations = repository.allStations.first()
            if (stations.isEmpty()) return@launch
            val currentId = exoPlayer?.currentMediaItem?.mediaId?.toIntOrNull()
            val currentIndex = if (currentId != null) stations.indexOfFirst { it.id == currentId } else -1
            val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % stations.size else 0
            playStationInService(stations[nextIndex])
        }
    }

    private fun playPreviousStationInService() {
        serviceScope.launch {
            val stations = repository.allStations.first()
            if (stations.isEmpty()) return@launch
            val currentId = exoPlayer?.currentMediaItem?.mediaId?.toIntOrNull()
            val currentIndex = if (currentId != null) stations.indexOfFirst { it.id == currentId } else -1
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else if (currentIndex == 0) stations.size - 1 else 0
            playStationInService(stations[prevIndex])
        }
    }

    private fun playRandomStationInService() {
        serviceScope.launch {
            val stations = repository.allStations.first()
            if (stations.isEmpty()) return@launch
            val randomIndex = kotlin.random.Random.nextInt(stations.size)
            playStationInService(stations[randomIndex])
        }
    }

    private fun stopPlaybackInService() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
    }

    private suspend fun playStationInService(station: RadioStation) {
        val playableUrl = station.url

        repository.insertHistory(station)

        val player = exoPlayer ?: return
        player.stop()
        player.clearMediaItems()
        val mediaItem = mapStationToMediaItem(station).buildUpon().setUri(playableUrl).build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        exoPlayer = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
