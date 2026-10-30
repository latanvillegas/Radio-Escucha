package com.example.data

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages two-tier (RAM LruCache + Disk SharedPreferences) Stale-While-Revalidate cache
 * for online Discover stations. Avoids main-thread JSON parsing and disk I/O jank.
 */
object DiscoverCacheManager {
    private const val PREF_NAME = "discover_cache_prefs"
    private val memoryCache = LruCache<String, List<RadioStation>>(24)

    private fun buildCacheKey(providerKey: String, query: String): String {
        return "cache_${providerKey}_${query.lowercase().trim()}"
    }

    fun getMemoryCachedStations(providerKey: String, query: String): List<RadioStation>? {
        return memoryCache.get(buildCacheKey(providerKey, query))
    }

    suspend fun saveCachedStationsAsync(
        context: Context,
        providerKey: String,
        query: String,
        stations: List<RadioStation>
    ) {
        if (stations.isEmpty()) return
        val capped = stations.take(60)
        val key = buildCacheKey(providerKey, query)
        memoryCache.put(key, capped)
        withContext(Dispatchers.IO) {
            saveCachedStations(context, providerKey, query, capped)
        }
    }

    suspend fun getCachedStationsAsync(
        context: Context,
        providerKey: String,
        query: String
    ): List<RadioStation> {
        val key = buildCacheKey(providerKey, query)
        memoryCache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            getCachedStations(context, providerKey, query)
        }
    }

    fun saveCachedStations(context: Context, providerKey: String, query: String, stations: List<RadioStation>) {
        try {
            if (stations.isEmpty()) return
            val capped = stations.take(60)
            val key = buildCacheKey(providerKey, query)
            memoryCache.put(key, capped)
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            capped.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("url", s.url)
                    put("genre", s.genre)
                    put("isFavorite", s.isFavorite)
                    put("isCustom", s.isCustom)
                    put("country", s.country)
                    put("region", s.region)
                    put("province", s.province)
                    put("district", s.district)
                    put("faviconUrl", s.faviconUrl)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(key, jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Ignore cache save error
        }
    }

    fun getCachedStations(context: Context, providerKey: String, query: String): List<RadioStation> {
        try {
            val key = buildCacheKey(providerKey, query)
            memoryCache.get(key)?.let { return it }
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(key, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val result = ArrayList<RadioStation>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    RadioStation(
                        id = obj.optInt("id", 0),
                        name = obj.optString("name", ""),
                        url = obj.optString("url", ""),
                        genre = obj.optString("genre", "Varios"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isCustom = obj.optBoolean("isCustom", false),
                        country = obj.optString("country", ""),
                        region = obj.optString("region", ""),
                        province = obj.optString("province", ""),
                        district = obj.optString("district", ""),
                        faviconUrl = obj.optString("faviconUrl", "")
                    )
                )
            }
            if (result.isNotEmpty()) {
                memoryCache.put(key, result)
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
