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
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : MediaLibraryService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var repository: RadioRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val CUSTOM_COMMAND_PREVIOUS = "CUSTOM_COMMAND_PREVIOUS"
    private val CUSTOM_COMMAND_NEXT = "CUSTOM_COMMAND_NEXT"
    private val CUSTOM_COMMAND_FAVORITE = "CUSTOM_COMMAND_FAVORITE"
    private val CUSTOM_COMMAND_RANDOM = "CUSTOM_COMMAND_RANDOM"
    private val CUSTOM_COMMAND_STOP = "CUSTOM_COMMAND_STOP"
    private val ROOT_ID = "root_stations"

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
            .setUserAgent("OpenRadio/1.0 (Android; Linux)")

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
                            .setTitle("Mis Estaciones")
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
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch {
                    val stations = repository.allStations.first()
                    val mediaItems = stations.map { mapStationToMediaItem(it) }
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                }
                return future
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                controller: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val future = com.google.common.util.concurrent.SettableFuture.create<LibraryResult<MediaItem>>()
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
        }

        val builder = MediaLibrarySession.Builder(this, player, sessionCallback)
        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent)
        }
        mediaLibrarySession = builder.build()
    }

    private fun mapStationToMediaItem(station: RadioStation): MediaItem {
        return MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.genre)
                    .setArtworkUri(if (station.faviconUrl.isNotBlank()) android.net.Uri.parse(station.faviconUrl) else null)
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
