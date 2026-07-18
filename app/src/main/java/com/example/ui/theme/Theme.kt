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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

import com.example.data.AppTheme
import com.example.data.AppFontSize
import com.example.data.AppIconSize

val LocalIconScale = staticCompositionLocalOf { 1.0f }

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
  appFontSize: AppFontSize = AppFontSize.NORMAL,
  appIconSize: AppIconSize = AppIconSize.NORMAL,
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

  val scaledTypography = scaleTypography(Typography, appFontSize.scale)

  CompositionLocalProvider(LocalIconScale provides appIconSize.scale) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
  }
}

private fun scaleTypography(typography: Typography, scale: Float): Typography {
    return Typography(
        displayLarge = typography.displayLarge.scale(scale),
        displayMedium = typography.displayMedium.scale(scale),
        displaySmall = typography.displaySmall.scale(scale),
        headlineLarge = typography.headlineLarge.scale(scale),
        headlineMedium = typography.headlineMedium.scale(scale),
        headlineSmall = typography.headlineSmall.scale(scale),
        titleLarge = typography.titleLarge.scale(scale),
        titleMedium = typography.titleMedium.scale(scale),
        titleSmall = typography.titleSmall.scale(scale),
        bodyLarge = typography.bodyLarge.scale(scale),
        bodyMedium = typography.bodyMedium.scale(scale),
        bodySmall = typography.bodySmall.scale(scale),
        labelLarge = typography.labelLarge.scale(scale),
        labelMedium = typography.labelMedium.scale(scale),
        labelSmall = typography.labelSmall.scale(scale)
    )
}

private fun TextStyle.scale(scale: Float): TextStyle {
    return this.copy(
        fontSize = this.fontSize * scale,
        lineHeight = this.lineHeight * scale
    )
}
