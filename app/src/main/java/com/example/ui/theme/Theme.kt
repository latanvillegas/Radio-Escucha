package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = SophisticatedTertiary,
    onTertiary = SophisticatedOnTertiary,
    background = SophisticatedBg,
    onBackground = SophisticatedText,
    surface = SophisticatedBg,
    onSurface = SophisticatedText,
    surfaceVariant = SophisticatedSurface,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for Sophisticated Dark experience
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve customized brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) SophisticatedDarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
