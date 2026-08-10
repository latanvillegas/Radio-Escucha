package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

object StationLogoResolver {
    private val BRAND_LOGOS = mapOf(
        "rpp" to "https://e.rpp-noticias.io/static/images/rpp-logo.png",
        "studio 92" to "https://e.rpp-noticias.io/static/images/s92-logo.png",
        "studio92" to "https://e.rpp-noticias.io/static/images/s92-logo.png",
        "exitosa" to "https://exitosanoticias.pe/favicon.ico",
        "moda" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_moda.png",
        "ritmo romantica" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_ritmoromantica.png",
        "ritmo romántica" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_ritmoromantica.png",
        "z rock" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_z.png",
        "la zona" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_lazona.png",
        "onda cero" to "https://www.ondacero.com.pe/favicon.ico",
        "la inolvidable" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_lainolvidable.png",
        "felicidad" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_felicidad.png",
        "oxigeno" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_oxigeno.png",
        "oxígeno" to "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_oxigeno.png",
        "panamericana" to "https://panamericana.pe/favicon.ico",
        "cadena ser" to "https://cadenaser.com/static/cadenaser/main/logo.png",
        "cadena 100" to "https://www.cadena100.es/favicon.ico",
        "los 40" to "https://los40.com/static/LOS40/main/logo.png",
        "disney" to "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Radio_Disney_logo.svg/1200px-Radio_Disney_logo.svg.png",
        "w radio" to "https://wradio.com.mx/static/wradio/main/logo.png",
        "rock & pop" to "https://www.rockandpop.cl/static/rockandpop/main/logo.png",
        "fm dos" to "https://www.fmdos.cl/static/fmdos/main/logo.png",
        "radio marca" to "https://e00-marca.uecdn.es/assets/v23/img/logo-marca.png",
        "bbc" to "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/BBC_World_Service_2022.svg/1200px-BBC_World_Service_2022.svg.png",
        "kiss fm" to "https://www.kissfm.es/wp-content/themes/kissfm/images/logo_kissfm.png",
        "npr" to "https://media.npr.org/chrome/news/npr-logo.png",
        "kexp" to "https://www.kexp.org/static/assets/img/kexp-logo.png",
        "classic fm" to "https://www.classicfm.com/assets_v4/classicfm/images/logo.png",
        "la mega" to "https://www.lamega.com.co/favicon.ico"
    )

    fun extractDomain(url: String): String? {
        if (url.isBlank()) return null
        return try {
            val clean = url.trim()
            val uri = java.net.URI(if (!clean.startsWith("http://") && !clean.startsWith("https://")) "https://$clean" else clean)
            val host = uri.host ?: return null
            host.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }

    fun getGoogleFaviconUrl(domain: String): String {
        return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
    }

    fun getDuckDuckGoFaviconUrl(domain: String): String {
        return "https://icons.duckduckgo.com/ip3/$domain.ico"
    }

    fun getInitials(name: String): String {
        val words = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "FM"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words[1].first()}".uppercase()
        }
    }

    fun getLogoForStation(name: String, url: String, faviconUrl: String = ""): String? {
        return getCandidateLogos(name, url, faviconUrl).firstOrNull()
    }

    fun getCandidateLogos(name: String, url: String, faviconUrl: String): List<String> {
        val list = mutableListOf<String>()

        // 1. Explicit Favicon URL if present
        val fav = faviconUrl.trim()
        if (fav.isNotBlank() && (fav.startsWith("http://") || fav.startsWith("https://"))) {
            list.add(fav)
            if (fav.startsWith("http://")) {
                list.add(fav.replaceFirst("http://", "https://"))
            }
        }

        // 2. Brand Name Match
        val nameLower = name.lowercase()
        for ((key, logo) in BRAND_LOGOS) {
            if (nameLower.contains(key)) {
                list.add(logo)
                break
            }
        }

        // 3. Domain Google & DuckDuckGo Favicon
        val domain = extractDomain(url)
        if (domain != null && domain.contains(".")) {
            list.add(getGoogleFaviconUrl(domain))
            list.add(getDuckDuckGoFaviconUrl(domain))
        }

        return list.distinct()
    }
}

@Composable
fun StationLogo(
    stationName: String,
    stationUrl: String,
    faviconUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val candidateUrls = remember(stationName, stationUrl, faviconUrl) {
        StationLogoResolver.getCandidateLogos(stationName, stationUrl, faviconUrl)
    }

    var currentAttemptIndex by remember(candidateUrls) { mutableIntStateOf(0) }
    var hasErrorAll by remember(candidateUrls) { mutableStateOf(false) }

    val currentUrl = candidateUrls.getOrNull(currentAttemptIndex)

    val initials = remember(stationName) { StationLogoResolver.getInitials(stationName) }
    val badgeColor = remember(stationName) {
        val charCode = stationName.fold(0) { acc, c -> acc + c.code }
        val hue = (charCode * 47) % 360
        Color.hsv(hue.toFloat(), 0.65f, 0.70f)
    }

    Box(
        modifier = modifier.background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        // Initials placeholder vector always rendered underneath instantly
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            fontSize = 14.sp
        )

        if (!hasErrorAll && currentUrl != null) {
            val imageRequest = remember(currentUrl) {
                ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription ?: "Logo de $stationName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (currentAttemptIndex + 1 < candidateUrls.size) {
                        currentAttemptIndex++
                    } else {
                        hasErrorAll = true
                    }
                }
            )
        }
    }
}
