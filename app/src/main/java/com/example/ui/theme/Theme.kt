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
import androidx.compose.ui.graphics.Color

import com.example.data.AppTheme

private val CosmicoColorScheme = darkColorScheme(
    primary = CosmicoPrimary,
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFF4A4458),
    onSecondary = Color(0xFFE6E1E5),
    tertiary = CosmicoAccent,
    background = CosmicoBg,
    onBackground = Color(0xFFE6E1E5),
    surface = CosmicoBg,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = CosmicoSurface,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F)
)

private val AmoledColorScheme = darkColorScheme(
    primary = CosmicoPrimary,
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFF4A4458),
    onSecondary = Color(0xFFE6E1E5),
    tertiary = CosmicoAccent,
    background = AmoledBg,
    onBackground = Color.White,
    surface = AmoledBg,
    onSurface = Color.White,
    surfaceVariant = AmoledSurface,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F)
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary = MidnightBluePrimary,
    onPrimary = Color.White,
    secondary = Color(0xFF161B22),
    onSecondary = Color(0xFF8B949E),
    tertiary = Color(0xFF58A6FF),
    background = MidnightBlueBg,
    onBackground = Color(0xFFC9D1D9),
    surface = MidnightBlueBg,
    onSurface = Color(0xFFC9D1D9),
    surfaceVariant = MidnightBlueSurface,
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D)
)

private val WarmAmberColorScheme = darkColorScheme(
    primary = WarmAmberPrimary,
    onPrimary = Color.Black,
    secondary = Color(0xFF292524),
    onSecondary = Color(0xFFD6D3D1),
    tertiary = Color(0xFFFDE68A),
    background = WarmAmberBg,
    onBackground = Color(0xFFF5F5F4),
    surface = WarmAmberBg,
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = WarmAmberSurface,
    onSurfaceVariant = Color(0xFFD6D3D1),
    outline = Color(0xFF44403C)
)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  appTheme: AppTheme = AppTheme.COSMICO,
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (appTheme) {
      AppTheme.COSMICO -> CosmicoColorScheme
      AppTheme.AMOLED -> AmoledColorScheme
      AppTheme.MEDIANOCHE_AZUL -> MidnightBlueColorScheme
      AppTheme.AMBAR_CALIDO -> WarmAmberColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
