package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.data.MultiSourceRadioClients
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.util.LogoImageLoader
import com.example.util.RadioAudioDspEngine
import com.example.util.StreamFormatHelper
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class PlaybackService : MediaLibraryService() {

    private var exoPlayer: ExoPlayer? = null
    private var notificationPlayer: RadioNotificationPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var repository: RadioRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var serviceSleepTimerJob: Job? = null
    private var metadataEnrichJob: Job? = null
    private var stationArtworkJob: Job? = null

    private var currentStationName: String = "OpenRadio"
    private var currentStationGenre: String = "Radio en Vivo"
    private var currentStationCountry: String = ""
    private var currentStationFavicon: String = ""
    private var currentStationUrl: String = ""

    private var currentSongTitle: String? = null
    private var currentSongArtist: String? = null
    private var currentSongAlbum: String? = null
    private var currentArtworkUrl: String? = null
    private var currentArtworkBytes: ByteArray? = null

    private val artworkCache = android.util.LruCache<String, ByteArray>(24)

    companion object {
        const val CUSTOM_COMMAND_PREVIOUS = "CUSTOM_COMMAND_PREVIOUS"
        const val CUSTOM_COMMAND_NEXT = "CUSTOM_COMMAND_NEXT"
        const val CUSTOM_COMMAND_FAVORITE = "CUSTOM_COMMAND_FAVORITE"
        const val CUSTOM_COMMAND_RANDOM = "CUSTOM_COMMAND_RANDOM"
        const val CUSTOM_COMMAND_STOP = "CUSTOM_COMMAND_STOP"
        const val CUSTOM_COMMAND_SLEEP_TIMER = "CUSTOM_COMMAND_SLEEP_TIMER"
        const val ARG_SLEEP_MINUTES = "ARG_SLEEP_MINUTES"

        const val EXTRA_PARSED_BY_SERVICE = "parsed_by_service"
        const val EXTRA_TRACK_TITLE = "track_title"
        const val EXTRA_TRACK_ARTIST = "track_artist"
        const val EXTRA_TRACK_ALBUM = "track_album"
        const val EXTRA_TRACK_ARTWORK_URL = "track_artwork_url"

        private const val ROOT_ID = "root_stations"
        private const val FOLDER_FAVORITES = "folder_favorites"
        private const val FOLDER_RECENT = "folder_recent"
        private const val FOLDER_ALL = "folder_all"

        private val BRACKET_ALBUM_REGEX = Regex("^(.*?)\\s*\\[([^]]+)]$")
        private val NON_ALPHANUM_REGEX = Regex("[^\\p{L}\\p{N}\\s-]")

        private val FALLBACK_PALETTES = listOf(
            intArrayOf(Color.parseColor("#6366F1"), Color.parseColor("#A855F7")),
            intArrayOf(Color.parseColor("#EC4899"), Color.parseColor("#F43F5E")),
            intArrayOf(Color.parseColor("#0EA5E9"), Color.parseColor("#2563EB")),
            intArrayOf(Color.parseColor("#10B981"), Color.parseColor("#059669")),
            intArrayOf(Color.parseColor("#F59E0B"), Color.parseColor("#EA580C"))
        )
    }

    /**
     * ForwardingPlayer that:
     * 1. Exposes rich MediaMetadata (Song Title, Artist, Album, Station Name, and embedded Artwork bytes)
     *    to Android's System Media Notification, Lockscreen, Bluetooth AVRCP, and Android Auto without
     *    restarting or interrupting the live ExoPlayer stream.
     * 2. Enables native Previous / Next transport actions in Android 13/14/15 media notifications
     *    and Bluetooth / steering-wheel controls.
     */
    private inner class RadioNotificationPlayer(delegate: Player) : ForwardingPlayer(delegate) {
        private val registeredListeners = CopyOnWriteArraySet<Player.Listener>()
        private var customMetadata: MediaMetadata = MediaMetadata.EMPTY

        override fun addListener(listener: Player.Listener) {
            registeredListeners.add(listener)
            super.addListener(listener)
        }

        override fun removeListener(listener: Player.Listener) {
            registeredListeners.remove(listener)
            super.removeListener(listener)
        }

        fun updateNotificationMetadata(newMetadata: MediaMetadata) {
            customMetadata = newMetadata
            for (listener in registeredListeners) {
                listener.onMediaMetadataChanged(newMetadata)
            }
        }

        override fun getMediaMetadata(): MediaMetadata {
            return if (customMetadata != MediaMetadata.EMPTY) customMetadata else super.getMediaMetadata()
        }

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands()
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun seekToPrevious() {
            playPreviousStationInService()
        }

        override fun seekToPreviousMediaItem() {
            playPreviousStationInService()
        }

        override fun hasPreviousMediaItem(): Boolean = true

        override fun seekToNext() {
            playNextStationInService()
        }

        override fun seekToNextMediaItem() {
            playNextStationInService()
        }

        override fun hasNextMediaItem(): Boolean = true
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        RadioAudioDspEngine.init(this)
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
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(RadioAudioDspEngine.pcmProcessor))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
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
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        StreamFormatHelper.applyAudioOnlyRadioMode(player)
        RadioAudioDspEngine.bindAudioSession(player.audioSessionId)
        exoPlayer = player

        val wrappedPlayer = RadioNotificationPlayer(player)
        notificationPlayer = wrappedPlayer

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                RadioAudioDspEngine.bindAudioSession(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    RadioAudioDspEngine.bindAudioSession(player.audioSessionId)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) return
                val extras = mediaItem.mediaMetadata.extras
                currentStationName = extras?.getString("station_name")
                    ?: mediaItem.mediaMetadata.station?.toString()
                    ?: mediaItem.mediaMetadata.title?.toString()
                    ?: "OpenRadio"
                currentStationGenre = extras?.getString("station_genre")
                    ?: mediaItem.mediaMetadata.subtitle?.toString()
                    ?: "Radio en Vivo"
                currentStationCountry = extras?.getString("station_country").orEmpty()
                currentStationFavicon = extras?.getString("station_favicon")
                    ?: mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()
                currentStationUrl = extras?.getString("station_url")
                    ?: mediaItem.localConfiguration?.uri?.toString().orEmpty()

                currentSongTitle = null
                currentSongArtist = null
                currentSongAlbum = null
                currentArtworkUrl = currentStationFavicon

                // Generate immediate station badge artwork, then fetch real station logo
                currentArtworkBytes = generateFallbackStationLogoBytes(currentStationName)
                rebuildAndPublishNotificationMetadata()
                loadStationFaviconAsync(currentStationName, currentStationFavicon)
            }

            override fun onMetadata(metadata: Metadata) {
                val builder = MediaMetadata.Builder()
                for (i in 0 until metadata.length()) {
                    metadata.get(i).populateMediaMetadata(builder)
                }
                val populated = builder.build()
                processRawStreamMetadata(
                    rawTitle = populated.title?.toString(),
                    rawArtist = populated.artist?.toString(),
                    rawAlbum = populated.albumTitle?.toString()
                )
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (mediaMetadata.extras?.getBoolean(EXTRA_PARSED_BY_SERVICE) == true) return
                processRawStreamMetadata(
                    rawTitle = mediaMetadata.title?.toString(),
                    rawArtist = mediaMetadata.artist?.toString(),
                    rawAlbum = mediaMetadata.albumTitle?.toString()
                )
            }
        })

        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = sessionIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val prevCommand = SessionCommand(CUSTOM_COMMAND_PREVIOUS, Bundle.EMPTY)
        val nextCommand = SessionCommand(CUSTOM_COMMAND_NEXT, Bundle.EMPTY)

        val favoriteCommand = SessionCommand(CUSTOM_COMMAND_FAVORITE, Bundle.EMPTY)
        val favoriteButton = CommandButton.Builder()
            .setSessionCommand(favoriteCommand)
            .setDisplayName("Favorito")
            .setIconResId(android.R.drawable.btn_star_big_on)
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
                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(playerCommands)
                    .setCustomLayout(ImmutableList.of(favoriteButton, randomButton, stopButton))
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
                serviceScope.launch(Dispatchers.IO) {
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
                serviceScope.launch(Dispatchers.IO) {
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
                serviceScope.launch(Dispatchers.IO) {
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

        val builder = MediaLibrarySession.Builder(this, wrappedPlayer, sessionCallback)
        if (sessionActivityPendingIntent != null) {
            builder.setSessionActivity(sessionActivityPendingIntent)
        }
        mediaLibrarySession = builder.build()
    }

    private fun processRawStreamMetadata(
        rawTitle: String?,
        rawArtist: String?,
        rawAlbum: String?
    ) {
        val cleanedRaw = rawTitle?.trim()
            ?.removePrefix("Now Playing:")
            ?.removePrefix("NOW PLAYING:")
            ?.trim()

        if (cleanedRaw.isNullOrBlank() ||
            cleanedRaw.equals(currentStationName, ignoreCase = true) ||
            cleanedRaw.equals("unknown", ignoreCase = true) ||
            cleanedRaw.equals("stream", ignoreCase = true)
        ) {
            return
        }

        var parsedArtist: String? = rawArtist?.trim()?.takeIf {
            it.isNotBlank() &&
                    !it.equals(currentStationGenre, ignoreCase = true) &&
                    !it.equals(currentStationName, ignoreCase = true)
        }
        var parsedTitle: String = cleanedRaw
        var parsedAlbum: String? = rawAlbum?.trim()?.takeIf { it.isNotBlank() }

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

        val bracketRegex = BRACKET_ALBUM_REGEX.find(parsedTitle)
        if (bracketRegex != null && parsedAlbum.isNullOrBlank()) {
            parsedTitle = bracketRegex.groupValues[1].trim()
            parsedAlbum = bracketRegex.groupValues[2].trim()
        }

        if (parsedTitle == currentSongTitle && parsedArtist == currentSongArtist && parsedAlbum == currentSongAlbum) {
            return
        }

        currentSongTitle = parsedTitle
        currentSongArtist = parsedArtist
        currentSongAlbum = parsedAlbum
        rebuildAndPublishNotificationMetadata()

        // Enrich with iTunes Search API for Album Name, Official Artist, and HD Cover Art Bitmap
        enrichSongFromItunesInService(parsedTitle, parsedArtist)
    }

    private fun enrichSongFromItunesInService(title: String, artist: String?) {
        metadataEnrichJob?.cancel()
        metadataEnrichJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val searchTerm = listOfNotNull(artist, title)
                    .joinToString(" ")
                    .replace(NON_ALPHANUM_REGEX, " ")
                    .trim()
                if (searchTerm.length < 3) return@launch

                val match = MultiSourceRadioClients.lookupSongCached(searchTerm) ?: return@launch

                if (currentSongArtist.isNullOrBlank() && !match.artistName.isNullOrBlank()) {
                    currentSongArtist = match.artistName
                }
                if (!match.collectionName.isNullOrBlank()) {
                    currentSongAlbum = match.collectionName
                }
                val highResArtwork = match.artworkUrl100?.replace("100x100bb", "600x600bb")
                if (!highResArtwork.isNullOrBlank()) {
                    currentArtworkUrl = highResArtwork
                    val downloadedBytes = downloadImageBytes(highResArtwork)
                    if (downloadedBytes != null) {
                        currentArtworkBytes = downloadedBytes
                    }
                }

                withContext(Dispatchers.Main) {
                    rebuildAndPublishNotificationMetadata()
                }
            } catch (_: Exception) {
                // Keep parsed ICY metadata and station logo if iTunes lookup fails
            }
        }
    }

    private fun loadStationFaviconAsync(stationName: String, faviconUrl: String) {
        stationArtworkJob?.cancel()
        if (faviconUrl.isBlank()) return
        stationArtworkJob = serviceScope.launch(Dispatchers.IO) {
            val bytes = downloadImageBytes(faviconUrl)
            if (bytes != null && currentStationName == stationName && currentArtworkUrl == faviconUrl) {
                currentArtworkBytes = bytes
                withContext(Dispatchers.Main) {
                    rebuildAndPublishNotificationMetadata()
                }
            }
        }
    }

    private fun rebuildAndPublishNotificationMetadata() {
        val song = currentSongTitle?.takeIf { it.isNotBlank() }
        val artist = currentSongArtist?.takeIf { it.isNotBlank() }
        val album = currentSongAlbum?.takeIf { it.isNotBlank() }

        // Line 1 of Android Media Notification: Song Title (or Station Name if no song yet)
        val notificationTitle = song ?: currentStationName

        // Line 2 of Android Media Notification: "Artista • Álbum • Emisora" so all 3 are clearly visible
        val notificationArtistLine = when {
            artist != null && album != null -> "$artist • $album ($currentStationName)"
            artist != null -> "$artist • $currentStationName"
            album != null -> "$album • $currentStationName"
            currentStationCountry.isNotBlank() -> "$currentStationGenre • $currentStationCountry"
            else -> currentStationGenre
        }

        val notificationAlbum = album ?: currentStationName

        val extras = Bundle().apply {
            putBoolean(EXTRA_PARSED_BY_SERVICE, true)
            putString("station_name", currentStationName)
            putString("station_genre", currentStationGenre)
            putString("station_country", currentStationCountry)
            putString("station_url", currentStationUrl)
            putString("station_favicon", currentStationFavicon)
            putString(EXTRA_TRACK_TITLE, song)
            putString(EXTRA_TRACK_ARTIST, artist)
            putString(EXTRA_TRACK_ALBUM, album)
            putString(EXTRA_TRACK_ARTWORK_URL, currentArtworkUrl)
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(notificationTitle)
            .setDisplayTitle(notificationTitle)
            .setArtist(notificationArtistLine)
            .setAlbumTitle(notificationAlbum)
            .setAlbumArtist(artist ?: currentStationName)
            .setStation(currentStationName)
            .setSubtitle(notificationArtistLine)
            .setGenre(currentStationGenre)
            .setExtras(extras)
            .setIsBrowsable(false)
            .setIsPlayable(true)

        val artUrl = currentArtworkUrl
        if (!artUrl.isNullOrBlank()) {
            metadataBuilder.setArtworkUri(Uri.parse(artUrl))
        }
        val artBytes = currentArtworkBytes
        if (artBytes != null) {
            metadataBuilder.setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        notificationPlayer?.updateNotificationMetadata(metadataBuilder.build())
    }

    private fun downloadImageBytes(url: String): ByteArray? {
        artworkCache.get(url)?.let { return it }
        val candidates = LogoImageLoader.buildCandidateUrls(url, currentStationUrl)
        for (candidate in candidates) {
            val bytes = downloadAndDecodeBitmapBytes(candidate)
            if (bytes != null) {
                artworkCache.put(url, bytes)
                return bytes
            }
        }
        return null
    }

    private fun downloadAndDecodeBitmapBytes(candidateUrl: String): ByteArray? {
        return try {
            val rawBytes = LogoImageLoader.decodeDataUriIfPresent(candidateUrl) ?: run {
                val request = Request.Builder()
                    .url(candidateUrl)
                    .build()
                LogoImageLoader.okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    response.body?.bytes()
                }
            } ?: return null

            val maxDim = 384

            // First pass: decode bounds only without allocating pixel memory
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sampleSize = 1
            while (boundsOpts.outWidth / (sampleSize * 2) >= maxDim &&
                boundsOpts.outHeight / (sampleSize * 2) >= maxDim
            ) {
                sampleSize *= 2
            }

            // Second pass: decode with subsampling
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

            val bitmap = if (decoded.width > maxDim || decoded.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            bitmap.recycle()
            stream.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun generateFallbackStationLogoBytes(stationName: String): ByteArray? {
        val cacheKey = "fallback_${stationName.lowercase().trim()}"
        artworkCache.get(cacheKey)?.let { return it }
        return try {
            val size = 256
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val palette = FALLBACK_PALETTES[abs(stationName.hashCode()) % FALLBACK_PALETTES.size]

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    palette[0], palette[1],
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

            val initials = stationName
                .split(" ", "-", ".")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifBlank { "FM" }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 96f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val textBounds = Rect()
            textPaint.getTextBounds(initials, 0, initials.length, textBounds)
            val yPos = (size / 2f) - textBounds.exactCenterY()
            canvas.drawText(initials, size / 2f, yPos, textPaint)

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            bitmap.recycle()
            val bytes = out.toByteArray()
            artworkCache.put(cacheKey, bytes)
            bytes
        } catch (_: Exception) {
            null
        }
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

    private fun mapStationToMediaItem(station: RadioStation, resolvedUrl: String = station.url): MediaItem {
        val extras = Bundle().apply {
            putString("station_name", station.name)
            putString("station_genre", station.genre)
            putString("station_url", resolvedUrl)
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
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return StreamFormatHelper.buildMediaItem(
            mediaId = station.id.toString(),
            streamUrl = resolvedUrl,
            mediaMetadata = metadata
        )
    }

    private fun toggleFavorite() {
        serviceScope.launch {
            val currentMediaId = exoPlayer?.currentMediaItem?.mediaId?.toLongOrNull() ?: return@launch
            withContext(Dispatchers.IO) {
                val station = repository.getStationById(currentMediaId) ?: return@withContext
                repository.update(station.copy(isFavorite = !station.isFavorite))
            }
        }
    }

    private fun playNextStationInService() {
        serviceScope.launch {
            val currentId = exoPlayer?.currentMediaItem?.mediaId?.toIntOrNull()
            val nextStation = withContext(Dispatchers.IO) {
                val stations = repository.allStations.first()
                if (stations.isEmpty()) return@withContext null
                val currentIndex = stations.indexOfFirst { it.id == currentId }
                val nextIndex = if (currentIndex in 0 until stations.size - 1) currentIndex + 1 else 0
                stations[nextIndex]
            } ?: return@launch
            playStationInService(nextStation)
        }
    }

    private fun playPreviousStationInService() {
        serviceScope.launch {
            val currentId = exoPlayer?.currentMediaItem?.mediaId?.toIntOrNull()
            val prevStation = withContext(Dispatchers.IO) {
                val stations = repository.allStations.first()
                if (stations.isEmpty()) return@withContext null
                val currentIndex = stations.indexOfFirst { it.id == currentId }
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else stations.size - 1
                stations[prevIndex]
            } ?: return@launch
            playStationInService(prevStation)
        }
    }

    private fun playRandomStationInService() {
        serviceScope.launch {
            val randomStation = withContext(Dispatchers.IO) {
                val stations = repository.allStations.first()
                if (stations.isNotEmpty()) stations.random() else null
            } ?: return@launch
            playStationInService(randomStation)
        }
    }

    private fun stopPlaybackInService() {
        exoPlayer?.stop()
    }

    private suspend fun playStationInService(station: RadioStation) {
        val playableUrl = StreamFormatHelper.resolvePlayableStreamUrl(station.url)
        val mediaItem = mapStationToMediaItem(station, playableUrl)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        withContext(Dispatchers.IO) {
            repository.insertHistory(station)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        serviceSleepTimerJob?.cancel()
        metadataEnrichJob?.cancel()
        stationArtworkJob?.cancel()
        RadioAudioDspEngine.release()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        notificationPlayer = null
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
