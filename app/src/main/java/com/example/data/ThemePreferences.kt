package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme(val displayName: String) {
    COSMICO("Cósmico"),
    AMOLED("AMOLED"),
    MEDIANOCHE_AZUL("Medianoche Azul"),
    AMBAR_CALIDO("Ámbar Cálido")
}

enum class AppFontSize(val displayName: String, val scale: Float) {
    PEQUENO("Pequeño", 0.85f),
    NORMAL("Normal", 1.0f),
    GRANDE("Grande", 1.2f),
    MUY_GRANDE("Muy Grande", 1.4f)
}

enum class AppIconSize(val displayName: String, val scale: Float) {
    PEQUENO("Pequeño", 0.8f),
    NORMAL("Normal", 1.0f),
    GRANDE("Grande(L)", 1.3f)
}

class ThemePreferences(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val ICON_SIZE_KEY = stringPreferencesKey("icon_size")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.COSMICO.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.COSMICO
        }
    }

    val fontSizeFlow: Flow<AppFontSize> = context.dataStore.data.map { preferences ->
        val fontSizeName = preferences[FONT_SIZE_KEY] ?: AppFontSize.NORMAL.name
        try {
            AppFontSize.valueOf(fontSizeName)
        } catch (e: Exception) {
            AppFontSize.NORMAL
        }
    }

    val iconSizeFlow: Flow<AppIconSize> = context.dataStore.data.map { preferences ->
        val iconSizeName = preferences[ICON_SIZE_KEY] ?: AppIconSize.NORMAL.name
        try {
            AppIconSize.valueOf(iconSizeName)
        } catch (e: Exception) {
            AppIconSize.NORMAL
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun setFontSize(fontSize: AppFontSize) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize.name
        }
    }

    suspend fun setIconSize(iconSize: AppIconSize) {
        context.dataStore.edit { preferences ->
            preferences[ICON_SIZE_KEY] = iconSize.name
        }
    }
}
