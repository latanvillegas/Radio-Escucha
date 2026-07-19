package com.example.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val CUSTOM_COMMAND_FAVORITE = "ACTION_FAVORITE"
    private val ROOT_ID = "ROOT"

    override fun onCreate() {
        super.onCreate()
        val database = RadioDatabase.getDatabase(this)
        repository = RadioRepository(database.radioDao(), database.playbackHistoryDao())

        exoPlayer = ExoPlayer.Builder(this)
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

        val favoriteCommand = SessionCommand(CUSTOM_COMMAND_FAVORITE, Bundle.EMPTY)
        val favoriteButton = CommandButton.Builder()
            .setSessionCommand(favoriteCommand)
            .setDisplayName("Favorito")
            .setIconResId(android.R.drawable.btn_star) // Simple star icon
            .build()

        val callback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(favoriteCommand)
                    .build()
                
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(connectionResult.availablePlayerCommands)
                    .setCustomLayout(listOf(favoriteButton))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == CUSTOM_COMMAND_FAVORITE) {
                    toggleFavorite()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
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

                        if (lastStation != null) {
                            val mediaItem = mapStationToMediaItem(lastStation)
                            MediaItemsWithStartPosition(listOf(mediaItem), 0, 0L)
                        } else {
                            throw UnsupportedOperationException("No playback history available")
                        }
                    } catch (e: Exception) {
                        throw UnsupportedOperationException("Playback resumption failed", e)
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
