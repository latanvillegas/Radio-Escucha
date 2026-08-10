package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages Stale-While-Revalidate local disk cache for online Discover stations.
 * Allows instant 0ms UI rendering from warm cache while background network queries refresh.
 */
object DiscoverCacheManager {
    private const val PREF_NAME = "discover_cache_prefs"

    fun saveCachedStations(context: Context, providerKey: String, query: String, stations: List<RadioStation>) {
        try {
            if (stations.isEmpty()) return
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            stations.take(60).forEach { s ->
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
            val key = "cache_${providerKey}_${query.lowercase().trim()}"
            prefs.edit().putString(key, jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Ignore cache save error
        }
    }

    fun getCachedStations(context: Context, providerKey: String, query: String): List<RadioStation> {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = "cache_${providerKey}_${query.lowercase().trim()}"
            val jsonStr = prefs.getString(key, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val result = mutableListOf<RadioStation>()
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
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
