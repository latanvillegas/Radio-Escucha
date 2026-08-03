package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// ==========================================
// 1. iHeartRadio API Models & Service
// ==========================================
data class IHeartResponse(
    @Json(name = "hits") val hits: List<IHeartStationDto>? = null,
    @Json(name = "items") val items: List<IHeartStationDto>? = null
)

data class IHeartStationDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "logo") val logo: String = "",
    @Json(name = "shoutcastUrl") val shoutcastUrl: String = "",
    @Json(name = "plsUrl") val plsUrl: String = "",
    @Json(name = "hlsUrl") val hlsUrl: String = "",
    @Json(name = "marketName") val marketName: String = "",
    @Json(name = "genres") val genres: List<IHeartGenreDto>? = null
) {
    fun toRadioStation(): RadioStation {
        val streamUrl = when {
            hlsUrl.isNotBlank() -> hlsUrl
            shoutcastUrl.isNotBlank() -> shoutcastUrl
            plsUrl.isNotBlank() -> plsUrl
            else -> "https://stream.revma.ihrhls.com/zc$id"
        }
        val genreStr = genres?.firstOrNull()?.name ?: description.takeIf { it.isNotBlank() } ?: "iHeartRadio Live"
        return RadioStation(
            id = if (id > 0) id.toInt() else (name.hashCode() and 0x7FFFFFFF),
            name = name.ifBlank { "iHeart Station" },
            url = streamUrl,
            genre = genreStr,
            isFavorite = false,
            isCustom = true,
            country = marketName.ifBlank { "International" },
            region = "iHeartMedia",
            faviconUrl = logo.trim()
        )
    }
}

data class IHeartGenreDto(
    @Json(name = "name") val name: String = ""
)

interface IHeartApiService {
    @GET("api/v2/content/liveStations")
    suspend fun getLiveStations(
        @Query("keywords") keywords: String? = null,
        @Query("limit") limit: Int = 30
    ): IHeartResponse
}

// ==========================================
// 2. TuneIn OPML JSON API Models & Service
// ==========================================
data class TuneInOpmlResponse(
    @Json(name = "head") val head: TuneInHead? = null,
    @Json(name = "body") val body: List<TuneInBodyItem>? = null
)

data class TuneInHead(
    @Json(name = "title") val title: String = "",
    @Json(name = "status") val status: String = ""
)

data class TuneInBodyItem(
    @Json(name = "text") val text: String = "",
    @Json(name = "URL") val url: String = "",
    @Json(name = "subtext") val subtext: String = "",
    @Json(name = "image") val image: String = "",
    @Json(name = "preset_id") val presetId: String = "",
    @Json(name = "type") val type: String = "",
    @Json(name = "item") val item: String = "",
    @Json(name = "element") val element: String = "",
    @Json(name = "children") val children: List<TuneInBodyItem>? = null
) {
    fun toRadioStation(): RadioStation {
        val cleanUrl = if (url.startsWith("http://opml.radiotime.com/Tune.ashx")) {
            url
        } else if (presetId.isNotBlank()) {
            "http://opml.radiotime.com/Tune.ashx?id=$presetId"
        } else {
            url
        }
        return RadioStation(
            id = (text.hashCode() and 0x7FFFFFFF),
            name = text.ifBlank { "TuneIn Station" },
            url = cleanUrl,
            genre = subtext.ifBlank { "TuneIn Global" },
            isFavorite = false,
            isCustom = true,
            country = "Internacional",
            region = "TuneIn",
            faviconUrl = image.trim()
        )
    }
}

interface TuneInApiService {
    @GET("Search.ashx?render=json&partnerId=RadioTime")
    suspend fun searchStations(
        @Query("query") query: String
    ): TuneInOpmlResponse

    @GET("Browse.ashx?c=presets&render=json&partnerId=RadioTime")
    suspend fun getPresets(): TuneInOpmlResponse

    @GET("Tune.ashx?render=json&partnerId=RadioTime")
    suspend fun tuneStation(
        @Query("id") id: String
    ): TuneInOpmlResponse
}

// ==========================================
// 3. GitHub Raw JSON Curated List Model & Service
// ==========================================
data class GitHubRadioItem(
    @Json(name = "name") val name: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "genre") val genre: String = "",
    @Json(name = "country") val country: String = "",
    @Json(name = "favicon") val favicon: String = ""
) {
    fun toRadioStation(): RadioStation {
        return RadioStation(
            id = (name.hashCode() and 0x7FFFFFFF),
            name = name.trim().ifBlank { "Radio GitHub" },
            url = url.trim(),
            genre = genre.trim().ifBlank { "Global Raw JSON" },
            isFavorite = false,
            isCustom = true,
            country = country.trim().ifBlank { "Mundial" },
            region = "Curada (Sin caídas)",
            faviconUrl = favicon.trim()
        )
    }
}

interface GitHubRadioApiService {
    @GET
    suspend fun fetchGitHubJsonList(@Url url: String): List<GitHubRadioItem>
}

// ==========================================
// 4. SomaFM API Models & Service (Indie & Electronic Radio Channels)
// ==========================================
data class SomaFmResponse(
    @Json(name = "channels") val channels: List<SomaFmChannelDto>? = null
)

data class SomaFmChannelDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "dj") val dj: String = "",
    @Json(name = "genre") val genre: String = "",
    @Json(name = "image") val image: String = ""
) {
    fun toRadioStation(): RadioStation {
        val streamUrl = "https://ice1.somafm.com/${id}-128-mp3"
        return RadioStation(
            id = (id.hashCode() and 0x7FFFFFFF),
            name = "SomaFM: $title",
            url = streamUrl,
            genre = genre.replace("|", " / ").ifBlank { "Ambient / Indie" },
            isFavorite = false,
            isCustom = true,
            country = "Estados Unidos (San Francisco)",
            region = "SomaFM Radio",
            faviconUrl = image.trim()
        )
    }
}

interface SomaFmApiService {
    @GET("channels.json")
    suspend fun getChannels(): SomaFmResponse
}

// ==========================================
// 6. FMStream Directory API Models & Service
// ==========================================
data class FmStreamStationDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "stream") val stream: String = "",
    @Json(name = "logo") val logo: String = "",
    @Json(name = "icon") val icon: String = "",
    @Json(name = "genre") val genre: String = "",
    @Json(name = "style") val style: String = "",
    @Json(name = "country") val country: String = "",
    @Json(name = "city") val city: String = "",
    @Json(name = "bitrate") val bitrate: Int = 0
) {
    fun toRadioStation(): RadioStation {
        val streamUrl = stream.ifBlank { url }
        val logoUrl = logo.ifBlank { icon }
        val genreStr = genre.ifBlank { style }.ifBlank { "FMStream Live" }
        return RadioStation(
            id = if (id > 0) (id + 98000).toInt() else (name.hashCode() and 0x7FFFFFFF),
            name = name.ifBlank { "FMStream Station" },
            url = streamUrl,
            genre = genreStr,
            isFavorite = false,
            isCustom = true,
            country = country.ifBlank { "Internacional" },
            region = city.ifBlank { "FMStream Directory" },
            faviconUrl = logoUrl.trim()
        )
    }
}

interface FmStreamApiService {
    @GET("index.php")
    suspend fun searchFmStream(
        @Query("req") req: String = "search",
        @Query("q") query: String,
        @Query("format") format: String = "json"
    ): List<FmStreamStationDto>
}

// ==========================================
// Centralized API Clients Singleton
// ==========================================
// ==========================================
// 5. iTunes Search API Models & Service
// ==========================================
data class ITunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int = 0,
    @Json(name = "results") val results: List<ITunesSongResult>? = null
)

data class ITunesSongResult(
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "collectionName") val collectionName: String? = null,
    @Json(name = "artworkUrl100") val artworkUrl100: String? = null
)

interface ITunesSearchApiService {
    @GET("search")
    suspend fun searchSong(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 1
    ): ITunesSearchResponse
}

object MultiSourceRadioClients {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android; RadioApp)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val iHeartService: IHeartApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.iheart.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IHeartApiService::class.java)
    }

    val tuneInService: TuneInApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://opml.radiotime.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TuneInApiService::class.java)
    }

    val gitHubService: GitHubRadioApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubRadioApiService::class.java)
    }

    val somaFmService: SomaFmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.somafm.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SomaFmApiService::class.java)
    }

    val iTunesSearchService: ITunesSearchApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ITunesSearchApiService::class.java)
    }

    val fmStreamService: FmStreamApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://fmstream.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FmStreamApiService::class.java)
    }

    // Static fallback list of high quality TuneIn streams with verified direct audio links and logos
    val fallbackTuneInList = listOf(
        RadioStation(id = 7101, name = "Capital FM London 95.8", url = "https://stream-capital.musicradio.com/capitalmp3", genre = "Top 40 / Pop", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16535/images/logoq.png"),
        RadioStation(id = 7102, name = "Heart London 106.2", url = "https://stream-heart.musicradio.com/heartlondon", genre = "Pop / Adult Contemporary", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16534/images/logoq.png"),
        RadioStation(id = 7103, name = "LBC News London", url = "https://stream-lbc.musicradio.com/lbcuk", genre = "Noticias / Opinión", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16538/images/logoq.png"),
        RadioStation(id = 7104, name = "Smooth Radio UK 97.3", url = "https://stream-smooth.musicradio.com/smoothuk", genre = "Oldies / Relax", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16537/images/logoq.png"),
        RadioStation(id = 7105, name = "Absolute Radio UK", url = "https://stream-absolute.planetradio.co.uk/absoluteradio.mp3", genre = "Rock / Indie", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s1219/images/logoq.png"),
        RadioStation(id = 7106, name = "Classic FM UK", url = "https://stream-media.musicradio.com/ClassicFM", genre = "Música Clásica", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16536/images/logoq.png"),
        RadioStation(id = 7107, name = "Kiss FM UK", url = "https://stream-kiss.planetradio.co.uk/kissnational.mp3", genre = "Dance / Urban", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s1218/images/logoq.png"),
        RadioStation(id = 7108, name = "Radio X London", url = "https://stream-radiox.musicradio.com/radioxuk", genre = "Alternative Rock", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16539/images/logoq.png"),
        RadioStation(id = 7109, name = "Gold Radio 60s 70s 80s", url = "https://stream-gold.musicradio.com/golduk", genre = "Clásicos / Retro", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s16533/images/logoq.png"),
        RadioStation(id = 7110, name = "Jazz FM UK", url = "https://stream-jazz.planetradio.co.uk/jazznational.mp3", genre = "Jazz / Soul / Blues", country = "Reino Unido", region = "TuneIn Directory", faviconUrl = "https://cdn-profiles.tunein.com/s1220/images/logoq.png"),
        RadioStation(id = 7111, name = "KISS FM España Directo", url = "https://kissfm.kissfm.es/kissfm.mp3", genre = "Pop / Éxitos", country = "España", region = "TuneIn Directory", faviconUrl = "https://www.kissfm.es/wp-content/themes/kissfm/images/logo_kissfm.png"),
        RadioStation(id = 7112, name = "Radio Disney Internacional", url = "https://27433.live.streamtheworld.com/DISNEY_PER_LM_SC", genre = "Pop / Hits", country = "Internacional", region = "TuneIn Directory", faviconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Radio_Disney_logo.svg/1200px-Radio_Disney_logo.svg.png")
    )

    // Static fallback list of FMStream curated high quality streams with logos
    val fallbackFmStreamList = listOf(
        RadioStation(id = 9801, name = "Cadena SER España", url = "https://21633.live.streamtheworld.com/CADENASER.mp3", genre = "Noticias / Deportes", country = "España", region = "FMStream", faviconUrl = "https://cadenaser.com/static/cadenaser/main/logo.png"),
        RadioStation(id = 9802, name = "Radio Disney Latinoamérica", url = "https://27433.live.streamtheworld.com/DISNEY_PER_LM_SC", genre = "Pop / Hits", country = "Perú", region = "FMStream", faviconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Radio_Disney_logo.svg/1200px-Radio_Disney_logo.svg.png"),
        RadioStation(id = 9803, name = "Studio 92", url = "https://26503.live.streamtheworld.com/STUDIO92_AAC.aac", genre = "Pop / Rock / Urbano", country = "Perú", region = "FMStream", faviconUrl = "https://e.rpp-noticias.io/static/images/s92-logo.png"),
        RadioStation(id = 9804, name = "Z Rock & Pop", url = "https://26503.live.streamtheworld.com/Z_ROCKANDPOP_AAC.aac", genre = "Classic Rock / 80s 90s", country = "Perú", region = "FMStream", faviconUrl = "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_z.png"),
        RadioStation(id = 9805, name = "Alfa 91.3 México", url = "https://18323.live.streamtheworld.com/XHFAJ_FM.mp3", genre = "Pop / Top 40", country = "México", region = "FMStream", faviconUrl = "https://alfa913.com.mx/static/images/logo.png"),
        RadioStation(id = 9806, name = "W Radio México", url = "https://18323.live.streamtheworld.com/XEW_AM.mp3", genre = "Noticias / Hablemos", country = "México", region = "FMStream", faviconUrl = "https://wradio.com.mx/static/wradio/main/logo.png"),
        RadioStation(id = 9807, name = "Rock & Pop Chile", url = "https://21633.live.streamtheworld.com/ROCK_AND_POP.mp3", genre = "Rock & Pop", country = "Chile", region = "FMStream", faviconUrl = "https://www.rockandpop.cl/static/rockandpop/main/logo.png"),
        RadioStation(id = 9808, name = "FM Dos Chile", url = "https://24393.live.streamtheworld.com/FMDOS.mp3", genre = "Romántica / Pop", country = "Chile", region = "FMStream", faviconUrl = "https://www.fmdos.cl/static/fmdos/main/logo.png"),
        RadioStation(id = 9809, name = "Radio Marca España", url = "https://radiomarca.unidadeditorial.es/stream", genre = "Deportes", country = "España", region = "FMStream", faviconUrl = "https://e00-marca.uecdn.es/assets/v23/img/logo-marca.png"),
        RadioStation(id = 9810, name = "BBC World Service", url = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", genre = "Noticias Internacionales", country = "Reino Unido", region = "FMStream", faviconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/BBC_World_Service_2022.svg/1200px-BBC_World_Service_2022.svg.png")
    )

    // Static fallback list of high-quality GitHub Raw Curated radios
    val fallbackGitHubCuratedList = listOf(
        RadioStation(id = 9001, name = "BBC Radio 1", url = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one", genre = "Pop / Hits", country = "Reino Unido", region = "GitHub Curada", faviconUrl = "https://a1.sndcdn.com/images/default_avatar_large.png"),
        RadioStation(id = 9002, name = "Capital FM London", url = "https://stream-capital.musicradio.com/capitalmp3", genre = "Top 40", country = "Reino Unido", region = "GitHub Curada", faviconUrl = "https://static.wikia.nocities.org/radio/images/f/f6/Capital_FM_logo.png/revision/latest?cb=20180424203531"),
        RadioStation(id = 9003, name = "Kiss FM España", url = "https://kissfm.kissfm.es/kissfm.mp3", genre = "Pop Classics", country = "España", region = "GitHub Curada", faviconUrl = "https://www.kissfm.es/wp-content/themes/kissfm/images/logo_kissfm.png"),
        RadioStation(id = 9004, name = "Los 40 Principales España", url = "https://21633.live.streamtheworld.com/LOS40_ES.mp3", genre = "Pop / Latino", country = "España", region = "GitHub Curada", faviconUrl = "https://los40.com/static/LOS40/main/logo.png"),
        RadioStation(id = 9005, name = "Radio Cadena 3 Argentina", url = "https://cadena3.cdn.352media.net/cadena3.mp3", genre = "Noticias / Variado", country = "Argentina", region = "GitHub Curada", faviconUrl = "https://www.cadena3.com/images/cadena3_logo.png"),
        RadioStation(id = 9006, name = "NPR News Live", url = "https://npr-ice.streamguys1.com/live.mp3", genre = "Noticias / Talk", country = "Estados Unidos", region = "GitHub Curada", faviconUrl = "https://media.npr.org/chrome/news/npr-logo.png"),
        RadioStation(id = 9007, name = "KEXP Seattle", url = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3", genre = "Indie / Alternative", country = "Estados Unidos", region = "GitHub Curada", faviconUrl = "https://www.kexp.org/static/assets/img/kexp-logo.png"),
        RadioStation(id = 9008, name = "Classic FM UK", url = "https://stream-media.musicradio.com/ClassicFM", genre = "Clásica", country = "Reino Unido", region = "GitHub Curada", faviconUrl = "https://www.classicfm.com/assets_v4/classicfm/images/logo.png"),
        RadioStation(id = 9009, name = "Radio M80 / RockFM", url = "https://25623.live.streamtheworld.com/ROCKFM_ES.mp3", genre = "Rock", country = "España", region = "GitHub Curada", faviconUrl = "https://www.rockfm.fm/assets/images/logo-rockfm.png"),
        RadioStation(id = 9010, name = "Salsa Radio Miami", url = "https://stream.zeno.fm/5q098s46z68uv", genre = "Salsa / Tropical", country = "Estados Unidos", region = "GitHub Curada", faviconUrl = "https://zeno.fm/static/media/zeno-logo.png"),
        RadioStation(id = 9011, name = "Radio Programas del Perú (RPP)", url = "https://17803.live.streamtheworld.com/RPP_AAC.aac", genre = "Noticias / Perú", country = "Perú", region = "GitHub Curada", faviconUrl = "https://e.rpp-noticias.io/static/images/rpp-logo.png"),
        RadioStation(id = 9012, name = "Radio Moda Perú", url = "https://24423.live.streamtheworld.com/RADIO_MODA_AAC.aac", genre = "Reggaeton / Trap", country = "Perú", region = "GitHub Curada", faviconUrl = "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_moda.png"),
        RadioStation(id = 9013, name = "Radio Ritmo Romántica", url = "https://26503.live.streamtheworld.com/R_RITMO_ROMANTICA_AAC.aac", genre = "Baladas / Romántica", country = "Perú", region = "GitHub Curada", faviconUrl = "https://cr00.epimg.net/radio/imagenes/2020/03/30/logo_ritmoromantica.png"),
        RadioStation(id = 9014, name = "Ibiza Global Radio", url = "https://ibizaglobalradio.icfstream.com/ibizaglobalradio.mp3", genre = "Electrónica / House", country = "España", region = "GitHub Curada", faviconUrl = "https://ibizaglobalradio.com/wp-content/uploads/2021/04/IGR_LOGO.png")
    )
}
