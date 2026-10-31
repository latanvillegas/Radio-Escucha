package com.example.util

import android.content.Context
import android.os.Build
import android.util.Base64
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Multi-format station logo resolver and Coil ImageLoader singleton.
 *
 * Supports:
 * - Vector & Raster formats: PNG, JPG/JPEG, WebP, SVG, GIF, ICO, BMP, HEIF/AVIF (API 31+)
 * - Inline Base64 data URIs: data:image/png;base64,...
 * - Website domains: www.midominio.com -> high-res favicon resolution
 * - Hotlink-protected radio hosts (estacionesfm.com, emisorasperu.com, radoxo.com, etc.)
 *   via Browser User-Agent + Origin Referer headers
 * - Multi-stage fallback via wsrv.nl CDN proxy & transcoder for older Android TLS/SSL stores
 *   (e.g., Redmi A2) or unsupported image encodings (CMYK JPEG, multi-entry ICO, SVG in Notifications).
 */
object LogoImageLoader {

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private const val IMAGE_ACCEPT_HEADER =
        "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

    private val GENERIC_CDN_HOSTS = setOf(
        "www.estacionesfm.com",
        "estacionesfm.com",
        "static.mytuner.mobi",
        "static2.mytuner.mobi",
        "cdn.radoxo.com",
        "cdn.instant.audio",
        "images.zeno.fm",
        "proxy.zeno.fm",
        "wsrv.nl",
        "www.google.com"
    )

    private val WEBSITE_HTML_EXTENSIONS = setOf(
        ".html", ".htm", ".php", ".asp", ".aspx", ".jsp"
    )

    private val KNOWN_IMAGE_EXTENSIONS = setOf(
        ".png", ".jpg", ".jpeg", ".webp", ".svg", ".gif", ".ico", ".bmp", ".avif", ".jfif", ".heic"
    )

    private val MARKDOWN_LINK_REGEX = Regex("""\[.*?]\((https?://[^)\s]+)\)""")

    @Volatile
    private var imageLoaderInstance: ImageLoader? = null

    /**
     * Shared OkHttpClient with Browser User-Agent, Referer injection for hotlink-protected
     * radio logo servers, and automatic retry on HTTP 403.
     */
    val okHttpClient: OkHttpClient by lazy {
        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val url = original.url
            val originReferer = "${url.scheme}://${url.host}/"

            val requestWithReferer = original.newBuilder()
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", IMAGE_ACCEPT_HEADER)
                .header("Referer", originReferer)
                .build()

            val response = chain.proceed(requestWithReferer)
            if (response.code == 403 || response.code == 406) {
                response.close()
                // Retry without Referer in case the CDN blocks self-referer
                val retryWithoutReferer = original.newBuilder()
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "*/*")
                    .removeHeader("Referer")
                    .build()
                chain.proceed(retryWithoutReferer)
            } else {
                response
            }
        }

        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(headerInterceptor)
            .build()
    }

    /**
     * Returns the application-wide singleton Coil ImageLoader configured with SVG, GIF,
     * animated WebP decoders, browser headers, and persistent disk/memory caches.
     */
    fun get(context: Context): ImageLoader {
        return imageLoaderInstance ?: synchronized(this) {
            imageLoaderInstance ?: buildImageLoader(context.applicationContext).also {
                imageLoaderInstance = it
            }
        }
    }

    private fun buildImageLoader(appContext: Context): ImageLoader {
        return ImageLoader.Builder(appContext)
            .okHttpClient { okHttpClient }
            .components {
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("station_logos_cache"))
                    .maxSizeBytes(40L * 1024 * 1024) // 40 MB persistent logo cache
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(180)
            .build()
    }

    /**
     * Cleans markdown escapes, accidental brackets, whitespace, or protocol-less URLs.
     */
    fun sanitizeLogoUrl(rawUrl: String): String {
        var cleaned = rawUrl.trim()
        if (cleaned.isEmpty()) return ""

        // Strip markdown link syntax if pasted like [url](url)
        val markdownMatch = MARKDOWN_LINK_REGEX.find(cleaned)
        if (markdownMatch != null) {
            cleaned = markdownMatch.groupValues[1]
        }

        // Remove markdown backslash escapes (e.g., https\://www\.estacionesfm.com/logos\_radio/...)
        cleaned = cleaned
            .replace("\\:", ":")
            .replace("\\.", ".")
            .replace("\\_", "_")
            .replace("\\-", "-")
            .replace("\\/", "/")
            .trim('"', '\'', ' ')

        if (cleaned.startsWith("data:image/", ignoreCase = true)) {
            return cleaned
        }

        if (cleaned.startsWith("//")) {
            cleaned = "https:$cleaned"
        } else if (!cleaned.startsWith("http://", ignoreCase = true) &&
            !cleaned.startsWith("https://", ignoreCase = true)
        ) {
            cleaned = "https://$cleaned"
        }

        // Encode unescaped spaces in path
        cleaned = cleaned.replace(" ", "%20")
        return cleaned
    }

    /**
     * Decodes a `data:image/...;base64,...` URI directly to ByteArray if applicable.
     */
    fun decodeDataUriIfPresent(url: String): ByteArray? {
        if (!url.startsWith("data:image/", ignoreCase = true)) return null
        val commaIndex = url.indexOf(',')
        if (commaIndex == -1 || commaIndex >= url.length - 1) return null
        val metadata = url.substring(0, commaIndex)
        val payload = url.substring(commaIndex + 1)
        return try {
            if (metadata.contains(";base64", ignoreCase = true)) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                java.net.URLDecoder.decode(payload, "UTF-8").toByteArray(Charsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds an ordered list of candidate image URLs to guarantee logo rendering across
     * all Android versions, network carriers, and image formats:
     * 1. Direct normalized URL (or Google S2 favicon if the user provided a website root URL)
     * 2. Global CDN Image Proxy & PNG Transcoder (wsrv.nl) to bypass SSL chain errors on
     *    older Android devices (Redmi A2), hotlink blocks, or exotic formats (.ico, .svg, CMYK .jpg)
     * 3. Website domain favicon fallback if a custom domain is available
     */
    fun buildCandidateUrls(rawFaviconUrl: String, stationUrl: String = ""): List<String> {
        val sanitized = sanitizeLogoUrl(rawFaviconUrl)
        if (sanitized.isEmpty()) return emptyList()

        if (sanitized.startsWith("data:image/", ignoreCase = true)) {
            return listOf(sanitized)
        }

        val candidates = LinkedHashSet<String>()
        val parsedUri = try {
            URI(sanitized)
        } catch (_: Exception) {
            null
        }

        val host = parsedUri?.host?.lowercase() ?: ""
        val path = parsedUri?.path?.lowercase() ?: ""

        // Check if the user pasted a website homepage instead of an image file
        val looksLikeWebsitePage = path.isEmpty() ||
            path == "/" ||
            WEBSITE_HTML_EXTENSIONS.any { path.endsWith(it) }

        val hasKnownImageHint = KNOWN_IMAGE_EXTENSIONS.any { path.contains(it) } ||
            sanitized.contains("image", ignoreCase = true) ||
            sanitized.contains("logo", ignoreCase = true) ||
            sanitized.contains("favicon", ignoreCase = true) ||
            sanitized.contains("media", ignoreCase = true) ||
            sanitized.contains("thumb", ignoreCase = true) ||
            sanitized.contains("cover", ignoreCase = true)

        if (looksLikeWebsitePage && !hasKnownImageHint && host.isNotEmpty()) {
            candidates.add("https://www.google.com/s2/favicons?domain=$host&sz=256")
            candidates.add("$sanitized/favicon.ico".replace("//favicon.ico", "/favicon.ico"))
            return candidates.toList()
        }

        // 1. Direct URL (prefer HTTPS first, or original)
        candidates.add(sanitized)

        // 2. Global CDN Image Proxy & Format Transcoder (wsrv.nl)
        // Automatically converts SVG, ICO, WebP, GIF, AVIF, CMYK JPG into a 256x256 PNG
        // over universally trusted Cloudflare TLS 1.2/1.3 and bypasses hotlink blocks.
        if (!host.contains("wsrv.nl") && !host.contains("google.com")) {
            val encodedUrl = try {
                URLEncoder.encode(sanitized, "UTF-8")
            } catch (_: Exception) {
                ""
            }
            if (encodedUrl.isNotEmpty()) {
                candidates.add("https://wsrv.nl/?url=$encodedUrl&w=256&h=256&fit=contain&output=png")
            }
        }

        // 3. If HTTP/HTTPS scheme mismatch might occur on legacy radio servers
        if (sanitized.startsWith("https://", ignoreCase = true) &&
            host.isNotEmpty() &&
            host !in GENERIC_CDN_HOSTS
        ) {
            candidates.add("http://" + sanitized.removePrefix("https://").removePrefix("HTTPS://"))
        } else if (sanitized.startsWith("http://", ignoreCase = true)) {
            candidates.add("https://" + sanitized.removePrefix("http://").removePrefix("HTTP://"))
        }

        // 4. Domain favicon fallback if the logo URL was hosted on the radio's own website
        if (host.isNotEmpty() && host !in GENERIC_CDN_HOSTS) {
            candidates.add("https://www.google.com/s2/favicons?domain=$host&sz=256")
        }

        return candidates.toList()
    }
}
