package com.example.util

import android.net.Uri
import android.util.LruCache
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Multi-format Stream & TV-in-Radio-Mode ("Modo Radio") Helper.
 *
 * Supports:
 * - HLS (.m3u8) for both Radio and Live TV channels (in Audio-Only mode)
 * - MPEG-DASH (.mpd)
 * - Progressive Icecast / Shoutcast streams (.mp3, .aac, .ogg, .opus, .flac, ;stream)
 * - Playlist containers (.pls, .m3u, .asx, .xspf) resolved automatically to their underlying stream URL
 */
object StreamFormatHelper {

    private val resolvedUrlCache = LruCache<String, String>(64)
    private val PLAYLIST_URL_REGEX = Regex("(https?://[^\"'<>\\s]+)", RegexOption.IGNORE_CASE)

    /**
     * Configures ExoPlayer so that Live TV streams (.m3u8 / .mpd / .ts) operate in
     * "Modo Radio" (Audio-Only):
     * 1. Disables C.TRACK_TYPE_VIDEO so video segments/renders are never decoded.
     * 2. When an HLS master playlist has an audio-only variant or lowest-bandwidth variant,
     *    ExoPlayer selects only the audio track, saving up to 90% of mobile data and battery.
     */
    fun applyAudioOnlyRadioMode(player: Player) {
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        player.trackSelectionParameters = params
    }

    /**
     * Infers the explicit Media3 MimeType when the URL path or query parameters indicate
     * HLS (.m3u8) or DASH (.mpd), even if the URL has query tokens (e.g. ?token=xyz).
     */
    fun inferMimeType(url: String): String? {
        val cleanPath = url.substringBefore("?").lowercase().trim()
        return when {
            cleanPath.endsWith(".m3u8") || url.lowercase().contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            cleanPath.endsWith(".mpd") || url.lowercase().contains(".mpd") -> MimeTypes.APPLICATION_MPD
            else -> null
        }
    }

    /**
     * Builds a MediaItem with automatic MIME-type detection for HLS (.m3u8), DASH (.mpd),
     * and standard Icecast/Shoutcast streams.
     */
    fun buildMediaItem(
        mediaId: String,
        streamUrl: String,
        mediaMetadata: MediaMetadata
    ): MediaItem {
        val normalizedUrl = normalizeStreamUrl(streamUrl)
        val builder = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(Uri.parse(normalizedUrl))
            .setMediaMetadata(mediaMetadata)

        inferMimeType(normalizedUrl)?.let { mimeType ->
            builder.setMimeType(mimeType)
        }
        return builder.build()
    }

    /**
     * Cleans common Shoutcast/Icecast trailing semicolons (e.g., "https://host:7038/;")
     * and fixes missing protocols.
     */
    fun normalizeStreamUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.startsWith("ttps://", ignoreCase = true)) {
            url = "h$url"
        } else if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            // valid scheme
        } else if (url.isNotBlank()) {
            url = "https://$url"
        }
        // Convert Shoutcast v1 trailing "/;" to "/" so HttpDataSource parses cleanly
        if (url.endsWith("/;")) {
            url = url.removeSuffix(";")
        }
        return url
    }

    /**
     * If the station URL points to a static playlist wrapper (.pls, .m3u, .asx, .xspf)
     * rather than a live HLS (.m3u8) manifest, fetches and extracts the first playable stream URL.
     */
    suspend fun resolvePlayableStreamUrl(rawUrl: String): String {
        val normalized = normalizeStreamUrl(rawUrl)
        resolvedUrlCache.get(normalized)?.let { return it }

        val cleanPath = normalized.substringBefore("?").lowercase()
        val isPlaylistContainer = (cleanPath.endsWith(".pls") ||
            (cleanPath.endsWith(".m3u") && !cleanPath.endsWith(".m3u8")) ||
            cleanPath.endsWith(".asx") ||
            cleanPath.endsWith(".xspf"))

        if (!isPlaylistContainer) {
            return normalized
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(normalized)
                    .header("User-Agent", "OpenRadio/1.2 (Android)")
                    .build()
                LogoImageLoader.okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext normalized
                    val body = response.body?.string()?.take(16_384).orEmpty()
                    val extracted = parsePlaylistBody(body)
                    if (!extracted.isNullOrBlank()) {
                        val finalUrl = normalizeStreamUrl(extracted)
                        resolvedUrlCache.put(normalized, finalUrl)
                        finalUrl
                    } else {
                        normalized
                    }
                }
            } catch (_: Exception) {
                normalized
            }
        }
    }

    private fun parsePlaylistBody(content: String): String? {
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            // .pls format: File1=https://...
            if (line.startsWith("File", ignoreCase = true) && line.contains("=")) {
                val candidate = line.substringAfter("=").trim()
                if (candidate.startsWith("http", ignoreCase = true)) {
                    return candidate
                }
            }
            // .m3u format: direct http/https line (ignoring #EXTM3U / #EXTINF comments)
            if (!line.startsWith("#") && (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true))) {
                return line
            }
            // .asx / .xspf format: href="https://..." or <location>https://...</location>
            if (line.contains("http://", ignoreCase = true) || line.contains("https://", ignoreCase = true)) {
                val urlMatch = PLAYLIST_URL_REGEX.find(line)
                if (urlMatch != null) {
                    return urlMatch.groupValues[1]
                }
            }
        }
        return null
    }
}
