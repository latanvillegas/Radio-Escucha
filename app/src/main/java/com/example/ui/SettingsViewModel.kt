package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppTheme
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.data.ThemePreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SettingsViewModel(
    application: Application,
    private val repository: RadioRepository
) : AndroidViewModel(application) {

    private val themePreferences = ThemePreferences(application)
    val appTheme: StateFlow<AppTheme> = themePreferences.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.COSMICO
        )

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, RadioStation::class.java)
    private val adapter = moshi.adapter<List<RadioStation>>(listType)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferences.setTheme(theme)
        }
    }

    fun exportFavorites(context: Context, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val favorites = repository.allStations.first().filter { it.isFavorite }
                val json = adapter.toJson(favorites)
                val file = File(context.cacheDir, "favorites_export.json")
                FileOutputStream(file).use { it.write(json.toByteArray()) }
                onComplete(file)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }

    fun importFavorites(context: Context, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader().use { it?.readText() }
                if (json != null) {
                    val stations = adapter.fromJson(json)
                    if (stations != null) {
                        stations.forEach { station ->
                            // Ensure imported stations are treated correctly, maybe reset IDs to 0 to avoid conflicts
                            val stationToImport = station.copy(id = 0)
                            repository.insert(stationToImport)
                        }
                        onComplete(true)
                        return@launch
                    }
                }
                onComplete(false)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
