package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.core.view.WindowCompat
import com.example.data.AppFontSize
import com.example.data.AppIconSize
import com.example.data.AppTheme

val LocalIconScale = staticCompositionLocalOf { 1.0f }

private val CosmicoColorScheme = darkColorScheme(
    primary = CosmicoPrimary,
    onPrimary = SophisticatedOnPrimary,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = CosmicoAccent,
    background = CosmicoBg,
    onBackground = SophisticatedText,
    surface = CosmicoSurface,
    onSurface = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

private val AmoledColorScheme = darkColorScheme(
    primary = CosmicoPrimary,
    onPrimary = SophisticatedOnPrimary,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = CosmicoAccent,
    background = AmoledBg,
    onBackground = Color.White,
    surface = AmoledSurface,
    onSurface = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary = MidnightBluePrimary,
    onPrimary = Color.White,
    secondary = MidnightBlueSurface,
    onSecondary = Color(0xFFD4E3FC),
    tertiary = MidnightBluePrimary,
    background = MidnightBlueBg,
    onBackground = Color(0xFFE8EFFF),
    surface = MidnightBlueSurface,
    onSurface = Color(0xFFD4E3FC),
    outline = Color(0xFF1E2E55)
)

private val WarmAmberColorScheme = darkColorScheme(
    primary = WarmAmberPrimary,
    onPrimary = Color.Black,
    secondary = WarmAmberSurface,
    onSecondary = Color(0xFFFDE6D2),
    tertiary = Color(0xFFFFB347),
    background = WarmAmberBg,
    onBackground = Color(0xFFFFF3E8),
    surface = WarmAmberSurface,
    onSurface = Color(0xFFFDE6D2),
    outline = Color(0xFF3F2B20)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private fun scale(style: TextStyle, scale: Float): TextStyle {
    return style.copy(
        fontSize = if (style.fontSize.isSpecified) style.fontSize * scale else style.fontSize,
        lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * scale else style.lineHeight
    )
}

private fun scaleTypography(typography: Typography, scale: Float): Typography {
    return Typography(
        displayLarge = scale(typography.displayLarge, scale),
        displayMedium = scale(typography.displayMedium, scale),
        displaySmall = scale(typography.displaySmall, scale),
        headlineLarge = scale(typography.headlineLarge, scale),
        headlineMedium = scale(typography.headlineMedium, scale),
        headlineSmall = scale(typography.headlineSmall, scale),
        titleLarge = scale(typography.titleLarge, scale),
        titleMedium = scale(typography.titleMedium, scale),
        titleSmall = scale(typography.titleSmall, scale),
        bodyLarge = scale(typography.bodyLarge, scale),
        bodyMedium = scale(typography.bodyMedium, scale),
        bodySmall = scale(typography.bodySmall, scale),
        labelLarge = scale(typography.labelLarge, scale),
        labelMedium = scale(typography.labelMedium, scale),
        labelSmall = scale(typography.labelSmall, scale)
    )
}

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.COSMICO,
    appFontSize: AppFontSize = AppFontSize.NORMAL,
    appIconSize: AppIconSize = AppIconSize.NORMAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.COSMICO -> CosmicoColorScheme
        AppTheme.AMOLED -> AmoledColorScheme
        AppTheme.MEDIANOCHE_AZUL -> MidnightBlueColorScheme
        AppTheme.AMBAR_CALIDO -> WarmAmberColorScheme
    }

    val scaledTypography = scaleTypography(Typography, appFontSize.scale)

    CompositionLocalProvider(
        LocalIconScale provides appIconSize.scale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}
