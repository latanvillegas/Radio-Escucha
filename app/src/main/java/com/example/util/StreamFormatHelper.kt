package com.example.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.BaseRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.video.VideoRendererEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Multi-format Stream, Web TV Portal & TV-in-Radio-Mode ("Modo Radio") Engine.
 *
 * Supports:
 * - Direct HLS (.m3u8) and MPEG-DASH (.mpd) streams for Radio & Live TV channels
 * - Web TV & Web Radio player pages (e.g. https://www.tvperu.gob.pe/play, iblups, Mediastream, Dailymotion, etc.)
 *   by dynamically extracting the underlying HLS (.m3u8) / DASH (.mpd) / Icecast stream from HTML & iframes
 * - Muxed Audio+Video TV streams (.ts / .mp4) in pure Audio-Only mode via AudioOnlyVideoDiscardRenderer
 *   (discards compressed video packets without initializing a hardware video decoder or needing a Surface)
 * - Playlist containers (.pls, .m3u, .asx, .xspf)
 * - Progressive Icecast / Shoutcast streams (.mp3, .aac, .ogg, .opus, .flac, ;stream)
 */
@OptIn(UnstableApi::class)
object StreamFormatHelper {

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    data class ResolvedStreamInfo(
        val streamUrl: String,
        val mimeType: String? = null,
        val referer: String? = null,
        val timestampMs: Long = System.currentTimeMillis()
    )

    private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes TTL for dynamic TV tokens
    private val resolvedCache = LruCache<String, ResolvedStreamInfo>(128)
    private val streamRefererMap = ConcurrentHashMap<String, String>()
    private val forcedHlsUrls = ConcurrentHashMap.newKeySet<String>()

    private val PLAYLIST_URL_REGEX = Regex("(https?://[^\"'<>\\s]+)", RegexOption.IGNORE_CASE)
    private val M3U8_EXTRACT_REGEX = Regex(
        "(https?://[^\"'<>\\s\\\\]+\\.m3u8(?:\\?[^\"'<>\\s\\\\]*)?)",
        RegexOption.IGNORE_CASE
    )
    private val MPD_EXTRACT_REGEX = Regex(
        "(https?://[^\"'<>\\s\\\\]+\\.mpd(?:\\?[^\"'<>\\s\\\\]*)?)",
        RegexOption.IGNORE_CASE
    )
    private val AUDIO_EXTRACT_REGEX = Regex(
        "(https?://[^\"'<>\\s\\\\]+\\.(?:mp3|aac|ogg|opus)(?:\\?[^\"'<>\\s\\\\]*)?)",
        RegexOption.IGNORE_CASE
    )
    private val IFRAME_SRC_REGEX = Regex(
        "<iframe[^>]+src=[\"']([^\"']+)[\"']",
        RegexOption.IGNORE_CASE
    )
    private val MDSTRM_LIVE_REGEX = Regex(
        "mdstrm\\.com/live-stream/([a-zA-Z0-9_-]+)",
        RegexOption.IGNORE_CASE
    )
    private val MDSTRM_AUDIO_REGEX = Regex(
        "mdstrm\\.com/audio/([a-zA-Z0-9_-]+)",
        RegexOption.IGNORE_CASE
    )
    private val DAILYMOTION_EMBED_REGEX = Regex(
        "(?:dailymotion\\.com/embed/video/|dai\\.ly/)([a-zA-Z0-9]+)",
        RegexOption.IGNORE_CASE
    )
    private val IBLUPS_HASH_REGEX = Regex(
        "(?:cdnhd\\.iblups\\.com/hls/|iblups\\.com/e_)([a-zA-Z0-9]{20,40})",
        RegexOption.IGNORE_CASE
    )

    // Known verified HLS endpoints for TV Perú signals (Domestic HD, Variant, Noticias, Internacional)
    private val TV_PERU_HLS_CANDIDATES = listOf(
        "https://cdnhd.iblups.com/hls/777b4d4cc0984575a7d14f6ee57dbcaf.m3u8",
        "https://cdnhd.iblups.com/hls/777b4d4cc0984575a7d14f6ee57dbcaf7.m3u8",
        "https://cdnhd.iblups.com/hls/902c1a0395264f269f1160efa00660e4.m3u8",
        "https://cdnhd.iblups.com/hls/ee2450c81e554f4cae0e6292106993c27.m3u8"
    )

    /**
     * Custom lightweight Video Renderer that accepts any video track in muxed TV streams (.m3u8 / .ts / .mpd)
     * and discards the compressed video packets without creating a MediaCodec or requiring a Surface.
     *
     * Why this is essential for TV-in-Radio-Mode ("Modo Radio"):
     * - Most Live TV HLS streams mux AAC audio inside the H.264 MPEG-TS (.ts) video variants without a
     *   separate #EXT-X-MEDIA:TYPE=AUDIO rendition.
     * - If C.TRACK_TYPE_VIDEO is disabled, ExoPlayer refuses to select the muxed HLS variant.
     * - If MediaCodecVideoRenderer is used without a Surface, ExoPlayer hangs in STATE_BUFFERING waiting for a Surface.
     * - By draining and discarding video buffers here, ExoPlayer demuxes the .ts stream and plays the AAC audio
     *   track immediately at 0% video GPU/decoder cost!
     */
    private class AudioOnlyVideoDiscardRenderer : BaseRenderer(C.TRACK_TYPE_VIDEO) {
        private val discardBuffer = DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DISABLED)
        private var inputEOS = false

        override fun getName(): String = "AudioOnlyVideoDiscardRenderer"

        override fun supportsFormat(format: Format): Int {
            return if (MimeTypes.isVideo(format.sampleMimeType)) {
                RendererCapabilities.create(C.FORMAT_HANDLED)
            } else {
                RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE)
            }
        }

        override fun onEnabled(joining: Boolean, mayRenderStartOfStream: Boolean) {
            inputEOS = false
        }

        override fun onPositionReset(positionUs: Long, joining: Boolean) {
            inputEOS = false
        }

        override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
            if (inputEOS) return
            val formatHolder = formatHolder
            // Drain up to 16 video packets per render cycle to keep the demuxer flowing smoothly
            for (i in 0 until 16) {
                discardBuffer.clear()
                val result = readSource(formatHolder, discardBuffer, SampleStream.FLAG_OMIT_SAMPLE_DATA)
                when (result) {
                    C.RESULT_BUFFER_READ -> {
                        if (discardBuffer.isEndOfStream) {
                            inputEOS = true
                            return
                        }
                    }
                    C.RESULT_FORMAT_READ -> {
                        // Ignore video format changes
                    }
                    C.RESULT_NOTHING_READ -> return
                }
            }
        }

        override fun isReady(): Boolean = true

        override fun isEnded(): Boolean = inputEOS
    }

    /**
     * Builds a RenderersFactory that:
     * 1. Routes all decoded PCM audio through RadioAudioDspEngine.pcmProcessor (5-band EQ + Bass + Loudness).
     * 2. Uses AudioOnlyVideoDiscardRenderer for video tracks so muxed Live TV (.m3u8 / .ts / .mpd) channels
     *    play their audio track immediately in "Modo Radio" without needing a video Surface.
     */
    fun createAudioOnlyRenderersFactory(context: Context): DefaultRenderersFactory {
        return object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(RadioAudioDspEngine.pcmProcessor))
                    .build()
            }

            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                out.add(AudioOnlyVideoDiscardRenderer())
            }
        }
    }

    /**
     * Builds a MediaSource.Factory with smart per-request HTTP headers:
     * - Uses a browser User-Agent so strict TV CDNs (iblups, Cloudflare, Mediastream) never return HTTP 403.
     * - Injects Referer & Origin headers for streams resolved from web TV/radio portals.
     * - Sends Icy-MetaData: 1 only on direct audio streams, avoiding CORS/header rejection on HLS .m3u8/.ts CDNs.
     */
    fun createMediaSourceFactory(context: Context): MediaSource.Factory {
        val baseHttpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(18_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(BROWSER_USER_AGENT)

        val resolvingHttpFactory = ResolvingDataSource.Factory(baseHttpFactory) { dataSpec: DataSpec ->
            val uriStr = dataSpec.uri.toString()
            val lower = uriStr.lowercase()
            val isHlsOrDashSegment = lower.contains(".m3u8") ||
                lower.contains(".ts") ||
                lower.contains(".mpd") ||
                lower.contains(".m4s") ||
                lower.contains("iblups.com") ||
                lower.contains("mdstrm.com") ||
                forcedHlsUrls.contains(uriStr)

            val headers = HashMap<String, String>(dataSpec.httpRequestHeaders)
            val customReferer = streamRefererMap[uriStr]
                ?: streamRefererMap[uriStr.substringBeforeLast("/")]
                ?: if (lower.contains("iblups.com")) "https://www.tvperu.gob.pe/" else null

            if (customReferer != null) {
                headers["Referer"] = customReferer
                runCatching {
                    val refUri = URI(customReferer)
                    headers["Origin"] = "${refUri.scheme}://${refUri.host}"
                }
            }

            if (!isHlsOrDashSegment) {
                headers["Icy-MetaData"] = "1"
            }

            dataSpec.buildUpon()
                .setHttpRequestHeaders(headers)
                .build()
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, resolvingHttpFactory)
        return DefaultMediaSourceFactory(dataSourceFactory)
    }

    /**
     * Configures ExoPlayer so that Live TV streams (.m3u8 / .mpd / .ts) operate in
     * "Modo Radio" (Audio-Only):
     * - Keeps C.TRACK_TYPE_VIDEO enabled (handled by AudioOnlyVideoDiscardRenderer) so muxed HLS/TS
     *   TV streams are selected and demuxed properly.
     * - Caps preferred video bitrate to 350 kbps (with automatic fallback to higher bitrates if only HD exists)
     *   so multi-variant HLS streams pick the lightest stream to save mobile data and battery.
     */
    fun applyAudioOnlyRadioMode(player: Player) {
        val params = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .setMaxVideoBitrate(350_000)
            .build()
        player.trackSelectionParameters = params
    }

    /**
     * Infers the explicit Media3 MimeType when the URL path, query parameters, or probe results
     * indicate HLS (.m3u8) or DASH (.mpd).
     */
    fun inferMimeType(url: String): String? {
        val normalized = normalizeStreamUrl(url)
        if (forcedHlsUrls.contains(normalized)) {
            return MimeTypes.APPLICATION_M3U8
        }
        val cleanPath = normalized.substringBefore("?").lowercase().trim()
        return when {
            cleanPath.endsWith(".m3u8") || normalized.lowercase().contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            cleanPath.endsWith(".mpd") || normalized.lowercase().contains(".mpd") -> MimeTypes.APPLICATION_MPD
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
     * Cleans common Shoutcast/Icecast trailing semicolons (e.g., "https://host:7038/;"),
     * strips accidental markdown wrappers, and fixes missing protocols.
     */
    fun normalizeStreamUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.startsWith("[") && url.contains("](") && url.endsWith(")")) {
            url = url.substringAfter("](").removeSuffix(")").trim()
        }
        if (url.startsWith("ttps://", ignoreCase = true)) {
            url = "h$url"
        } else if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            // valid scheme
        } else if (url.isNotBlank()) {
            url = "https://$url"
        }
        if (url.endsWith("/;")) {
            url = url.removeSuffix(";")
        }
        return url
    }

    /**
     * Resolves a station or TV URL to a directly playable stream URL.
     * Supports:
     * - Direct audio/HLS/DASH URLs
     * - Static playlists (.pls, .m3u, .asx, .xspf)
     * - Web TV & Radio portals (e.g. https://www.tvperu.gob.pe/play, iblups, mdstrm, dailymotion, generic HTML players)
     */
    suspend fun resolvePlayableStreamUrl(rawUrl: String): String {
        val normalized = normalizeStreamUrl(rawUrl)
        if (normalized.isBlank()) return normalized

        val cached = resolvedCache.get(normalized)
        if (cached != null && (System.currentTimeMillis() - cached.timestampMs) < CACHE_TTL_MS) {
            cached.referer?.let { rememberStreamReferer(cached.streamUrl, it) }
            if (cached.mimeType == MimeTypes.APPLICATION_M3U8) {
                forcedHlsUrls.add(cached.streamUrl)
            }
            return cached.streamUrl
        }

        // Fast-path for direct Mediastream / Dailymotion / iBlups URLs before network probe
        resolveKnownPlatformPattern(normalized)?.let { resolved ->
            cacheResolved(normalized, resolved)
            return resolved.streamUrl
        }

        val cleanPath = normalized.substringBefore("?").lowercase()

        // If it already ends with a direct binary audio extension or has a typical Icecast port (e.g. :8000/stream),
        // play immediately without an extra HTTP round-trip
        val isDirectAudioPath = cleanPath.endsWith(".mp3") ||
            cleanPath.endsWith(".aac") ||
            cleanPath.endsWith(".ogg") ||
            cleanPath.endsWith(".opus") ||
            cleanPath.endsWith(".flac") ||
            cleanPath.endsWith(".mpd") ||
            cleanPath.endsWith(".m3u8") ||
            Regex(":\\d{4,5}(/.*)?$").containsMatchIn(cleanPath)

        if (isDirectAudioPath) {
            return normalized
        }

        return withContext(Dispatchers.IO) {
            // Special handling for TV Perú official portal (https://www.tvperu.gob.pe/play, etc.)
            if (normalized.contains("tvperu.gob.pe", ignoreCase = true)) {
                val tvPeruResolved = resolveTvPeruPortal(normalized)
                if (tvPeruResolved != null) {
                    cacheResolved(normalized, tvPeruResolved)
                    return@withContext tvPeruResolved.streamUrl
                }
            }

            // Generic probe for playlist wrappers (.pls, .m3u, .asx, .xspf) AND Web TV/Radio HTML pages
            val probed = probeAndExtractStream(normalized, referer = null, depth = 0)
            if (probed != null) {
                cacheResolved(normalized, probed)
                probed.streamUrl
            } else {
                normalized
            }
        }
    }

    private fun resolveKnownPlatformPattern(url: String): ResolvedStreamInfo? {
        MDSTRM_LIVE_REGEX.find(url)?.let { match ->
            val id = match.groupValues[1]
            val hlsUrl = "https://mdstrm.com/live-stream-playlist/$id.m3u8"
            return ResolvedStreamInfo(streamUrl = hlsUrl, mimeType = MimeTypes.APPLICATION_M3U8, referer = url)
        }
        MDSTRM_AUDIO_REGEX.find(url)?.let { match ->
            val id = match.groupValues[1]
            val audioUrl = "https://mdstrm.com/audio/$id/icecast.audio"
            return ResolvedStreamInfo(streamUrl = audioUrl, referer = url)
        }
        return null
    }

    private fun resolveTvPeruPortal(portalUrl: String): ResolvedStreamInfo? {
        // 1. First attempt to scrape the live tvperu.gob.pe/play page & its embedded player
        val scraped = probeAndExtractStream(portalUrl, referer = "https://www.tvperu.gob.pe/", depth = 0)
        if (scraped != null && scraped.streamUrl != portalUrl && isStreamReachable(scraped.streamUrl, "https://www.tvperu.gob.pe/")) {
            return scraped
        }

        // 2. Fallback: verify active TV Perú HLS streams on iblups CDN
        for (candidate in TV_PERU_HLS_CANDIDATES) {
            if (isStreamReachable(candidate, "https://www.tvperu.gob.pe/")) {
                return ResolvedStreamInfo(
                    streamUrl = candidate,
                    mimeType = MimeTypes.APPLICATION_M3U8,
                    referer = "https://www.tvperu.gob.pe/"
                )
            }
        }

        // Default to primary TV Perú HLS endpoint with Referer
        return ResolvedStreamInfo(
            streamUrl = TV_PERU_HLS_CANDIDATES.first(),
            mimeType = MimeTypes.APPLICATION_M3U8,
            referer = "https://www.tvperu.gob.pe/"
        )
    }

    private fun isStreamReachable(streamUrl: String, referer: String?): Boolean {
        return try {
            val reqBuilder = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "*/*")
            if (referer != null) {
                reqBuilder.header("Referer", referer)
            }
            LogoImageLoader.okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val preview = resp.body?.source()?.let { source ->
                    source.request(256)
                    source.buffer.clone().readUtf8(minOf(256L, source.buffer.size))
                }.orEmpty()
                preview.contains("#EXTM3U", ignoreCase = true) ||
                    resp.header("Content-Type").orEmpty().let { ct ->
                        ct.contains("mpegurl", ignoreCase = true) ||
                            ct.startsWith("audio/", ignoreCase = true) ||
                            ct.startsWith("video/", ignoreCase = true)
                    }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun probeAndExtractStream(
        targetUrl: String,
        referer: String?,
        depth: Int
    ): ResolvedStreamInfo? {
        if (depth > 2) return null

        resolveKnownPlatformPattern(targetUrl)?.let { return it }

        DAILYMOTION_EMBED_REGEX.find(targetUrl)?.let { match ->
            val videoId = match.groupValues[1]
            extractDailymotionHls(videoId)?.let { hls ->
                return ResolvedStreamInfo(streamUrl = hls, mimeType = MimeTypes.APPLICATION_M3U8, referer = targetUrl)
            }
        }

        return try {
            val reqBuilder = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            if (!referer.isNullOrBlank()) {
                reqBuilder.header("Referer", referer)
            }

            LogoImageLoader.okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (!response.isSuccessful) return null

                val contentType = response.header("Content-Type").orEmpty().lowercase()

                // If server already responded with raw binary audio/video stream, stop immediately and use finalUrl
                if (contentType.startsWith("audio/mpeg") ||
                    contentType.startsWith("audio/aac") ||
                    contentType.startsWith("audio/aacp") ||
                    contentType.startsWith("audio/ogg") ||
                    contentType.startsWith("application/ogg") ||
                    contentType.startsWith("video/mp2t") ||
                    contentType.startsWith("audio/flac")
                ) {
                    return ResolvedStreamInfo(streamUrl = finalUrl, referer = referer)
                }

                // Read up to 96 KB of text (HTML, M3U8, PLS, JSON)
                val body = response.body?.source()?.let { source ->
                    source.request(98_304)
                    source.buffer.clone().readUtf8(minOf(98_304L, source.buffer.size))
                }.orEmpty()

                if (body.isBlank()) return null

                // Check if body is directly an HLS manifest (#EXTM3U with HLS tags)
                if (body.trimStart().startsWith("#EXTM3U", ignoreCase = true)) {
                    val isHlsManifest = body.contains("#EXT-X-STREAM-INF", ignoreCase = true) ||
                        body.contains("#EXT-X-TARGETDURATION", ignoreCase = true) ||
                        body.contains("#EXT-X-MEDIA", ignoreCase = true)
                    if (isHlsManifest) {
                        return ResolvedStreamInfo(
                            streamUrl = finalUrl,
                            mimeType = MimeTypes.APPLICATION_M3U8,
                            referer = referer
                        )
                    } else {
                        // Static .m3u playlist pointing to an underlying stream
                        parsePlaylistBody(body)?.let { extracted ->
                            val normExtracted = normalizeStreamUrl(extracted)
                            return ResolvedStreamInfo(
                                streamUrl = normExtracted,
                                mimeType = inferMimeType(normExtracted),
                                referer = referer
                            )
                        }
                    }
                }

                // Check if it's a .pls / .asx / .xspf playlist container
                val cleanPath = finalUrl.substringBefore("?").lowercase()
                if (cleanPath.endsWith(".pls") || cleanPath.endsWith(".asx") || cleanPath.endsWith(".xspf")) {
                    parsePlaylistBody(body)?.let { extracted ->
                        val normExtracted = normalizeStreamUrl(extracted)
                        return ResolvedStreamInfo(
                            streamUrl = normExtracted,
                            mimeType = inferMimeType(normExtracted),
                            referer = referer
                        )
                    }
                }

                // Otherwise, parse HTML / JS for embedded streams (.m3u8, .mpd, iblups, mdstrm, dailymotion, audio, iframes)
                extractFromHtmlOrScript(body, baseUrl = finalUrl, depth = depth)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFromHtmlOrScript(
        rawHtml: String,
        baseUrl: String,
        depth: Int
    ): ResolvedStreamInfo? {
        val unescaped = rawHtml
            .replace("\\/", "/")
            .replace("&amp;", "&")

        // 1. Direct .m3u8 URL in HTML/JS
        M3U8_EXTRACT_REGEX.find(unescaped)?.let { match ->
            val hlsUrl = normalizeStreamUrl(match.groupValues[1])
            return ResolvedStreamInfo(
                streamUrl = hlsUrl,
                mimeType = MimeTypes.APPLICATION_M3U8,
                referer = baseUrl
            )
        }

        // 2. Direct .mpd URL in HTML/JS
        MPD_EXTRACT_REGEX.find(unescaped)?.let { match ->
            val mpdUrl = normalizeStreamUrl(match.groupValues[1])
            return ResolvedStreamInfo(
                streamUrl = mpdUrl,
                mimeType = MimeTypes.APPLICATION_MPD,
                referer = baseUrl
            )
        }

        // 3. iBlups hash / player in HTML
        IBLUPS_HASH_REGEX.find(unescaped)?.let { match ->
            val token = match.groupValues[1]
            val candidateHls = "https://cdnhd.iblups.com/hls/$token.m3u8"
            if (isStreamReachable(candidateHls, baseUrl)) {
                return ResolvedStreamInfo(
                    streamUrl = candidateHls,
                    mimeType = MimeTypes.APPLICATION_M3U8,
                    referer = baseUrl
                )
            }
        }

        // 4. Mediastream embed inside HTML
        resolveKnownPlatformPattern(unescaped)?.let { return it.copy(referer = baseUrl) }

        // 5. Dailymotion embed inside HTML
        DAILYMOTION_EMBED_REGEX.find(unescaped)?.let { match ->
            val videoId = match.groupValues[1]
            extractDailymotionHls(videoId)?.let { hls ->
                return ResolvedStreamInfo(
                    streamUrl = hls,
                    mimeType = MimeTypes.APPLICATION_M3U8,
                    referer = baseUrl
                )
            }
        }

        // 6. Direct .mp3 / .aac / .ogg stream inside HTML <audio> or player config
        AUDIO_EXTRACT_REGEX.find(unescaped)?.let { match ->
            val audioUrl = normalizeStreamUrl(match.groupValues[1])
            return ResolvedStreamInfo(
                streamUrl = audioUrl,
                referer = baseUrl
            )
        }

        // 7. Follow embedded player <iframe src="...">
        val iframes = IFRAME_SRC_REGEX.findAll(unescaped)
            .map { it.groupValues[1].trim() }
            .filter { src ->
                src.isNotBlank() &&
                    !src.contains("googletagmanager", ignoreCase = true) &&
                    !src.contains("doubleclick", ignoreCase = true) &&
                    !src.contains("facebook.com", ignoreCase = true) &&
                    !src.contains("google.com/recaptcha", ignoreCase = true)
            }
            .toList()

        for (iframeSrc in iframes.take(3)) {
            val resolvedIframeUrl = resolveRelativeUrl(baseUrl, iframeSrc)
            val fromIframe = probeAndExtractStream(
                targetUrl = resolvedIframeUrl,
                referer = baseUrl,
                depth = depth + 1
            )
            if (fromIframe != null) {
                return fromIframe
            }
        }

        return null
    }

    private fun extractDailymotionHls(videoId: String): String? {
        return try {
            val metaUrl = "https://www.dailymotion.com/player/metadata/video/$videoId"
            val request = Request.Builder()
                .url(metaUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()
            LogoImageLoader.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty().replace("\\/", "/")
                M3U8_EXTRACT_REGEX.find(body)?.groupValues?.getOrNull(1)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveRelativeUrl(baseUrl: String, candidate: String): String {
        return try {
            when {
                candidate.startsWith("http://", ignoreCase = true) ||
                    candidate.startsWith("https://", ignoreCase = true) -> candidate
                candidate.startsWith("//") -> "https:$candidate"
                else -> URI(baseUrl).resolve(candidate).toString()
            }
        } catch (_: Exception) {
            normalizeStreamUrl(candidate)
        }
    }

    private fun cacheResolved(rawKey: String, info: ResolvedStreamInfo) {
        resolvedCache.put(rawKey, info)
        info.referer?.let { rememberStreamReferer(info.streamUrl, it) }
        if (info.mimeType == MimeTypes.APPLICATION_M3U8 || info.streamUrl.contains(".m3u8", ignoreCase = true)) {
            forcedHlsUrls.add(info.streamUrl)
        }
    }

    private fun rememberStreamReferer(streamUrl: String, referer: String) {
        streamRefererMap[streamUrl] = referer
        val baseDir = streamUrl.substringBeforeLast("/", "")
        if (baseDir.startsWith("http")) {
            streamRefererMap[baseDir] = referer
        }
    }

    private fun parsePlaylistBody(content: String): String? {
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            if (line.startsWith("File", ignoreCase = true) && line.contains("=")) {
                val candidate = line.substringAfter("=").trim()
                if (candidate.startsWith("http", ignoreCase = true)) {
                    return candidate
                }
            }
            if (!line.startsWith("#") && (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true))) {
                return line
            }
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
