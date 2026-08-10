package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class RadioBrowserStationDto(
    @Json(name = "stationuuid") val stationuuid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "url_resolved") val urlResolved: String = "",
    @Json(name = "homepage") val homepage: String = "",
    @Json(name = "favicon") val favicon: String = "",
    @Json(name = "tags") val tags: String = "",
    @Json(name = "country") val country: String = "",
    @Json(name = "countrycode") val countrycode: String = "",
    @Json(name = "state") val state: String = "",
    @Json(name = "language") val language: String = "",
    @Json(name = "votes") val votes: Int = 0,
    @Json(name = "codec") val codec: String = "",
    @Json(name = "bitrate") val bitrate: Int = 0
) {
    fun toRadioStation(): RadioStation {
        val streamUrl = urlResolved.ifBlank { url }
        val genreStr = tags.split(",")
            .map { it.trim().replaceFirstChar { char -> char.uppercase() } }
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(" / ")
            .ifBlank { "Radio Online" }

        return RadioStation(
            id = (stationuuid.hashCode() and 0x7FFFFFFF), // Ensure positive integer
            name = name.trim().ifBlank { "Radio Sin Nombre" },
            url = streamUrl,
            genre = genreStr,
            isFavorite = false,
            isCustom = true,
            country = country.trim(),
            region = state.trim(),
            faviconUrl = favicon.trim()
        )
    }
}

interface RadioBrowserApiService {
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("tag") tag: String? = null,
        @Query("country") country: String? = null,
        @Query("countrycode") countrycode: String? = null,
        @Query("state") state: String? = null,
        @Query("language") language: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserStationDto>

    @GET("json/stations/topclick/{limit}")
    suspend fun getTopClickStations(
        @Path("limit") limit: Int = 40
    ): List<RadioBrowserStationDto>

    @GET("json/stations/topvote/{limit}")
    suspend fun getTopVoteStations(
        @Path("limit") limit: Int = 40
    ): List<RadioBrowserStationDto>
}

object RadioBrowserApiClient {
    private const val BASE_URL = "https://de1.api.radio-browser.info/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "OpenRadioApp/1.0")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    val service: RadioBrowserApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadioBrowserApiService::class.java)
    }
}
