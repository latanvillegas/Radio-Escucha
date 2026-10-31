package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import com.example.data.AppFontSize
import com.example.data.AppIconSize
import com.example.data.AppTheme

val LocalIconScale = staticCompositionLocalOf { 1.0f }

private val CosmicoColorScheme = darkColorScheme(
    primary = CosmicoPrimary,
    onPrimary = Color.White,
    primaryContainer = CosmicoPrimaryContainer,
    onPrimaryContainer = Color(0xFFEDE2FF),
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = CosmicoAccent,
    onTertiary = Color.White,
    background = CosmicoBg,
    onBackground = SophisticatedText,
    surface = CosmicoSurface,
    onSurface = SophisticatedText,
    surfaceVariant = CosmicoSurfaceVariant,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

private val AmoledColorScheme = darkColorScheme(
    primary = CosmicoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF311666),
    onPrimaryContainer = Color(0xFFEDE2FF),
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = CosmicoAccent,
    onTertiary = Color.White,
    background = AmoledBg,
    onBackground = Color.White,
    surface = AmoledSurface,
    onSurface = Color.White,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary = MidnightBluePrimary,
    onPrimary = Color.White,
    primaryContainer = MidnightBluePrimaryContainer,
    onPrimaryContainer = Color(0xFFD8EBFF),
    secondary = MidnightBlueSurfaceVariant,
    onSecondary = Color(0xFFD4E3FC),
    tertiary = MidnightBlueAccent,
    onTertiary = Color(0xFF002029),
    background = MidnightBlueBg,
    onBackground = Color(0xFFEAF2FF),
    surface = MidnightBlueSurface,
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = MidnightBlueSurfaceVariant,
    onSurfaceVariant = Color(0xFFC5D8F6),
    outline = Color(0xFF2E4985)
)

private val WarmAmberColorScheme = darkColorScheme(
    primary = WarmAmberPrimary,
    onPrimary = Color(0xFF241200),
    primaryContainer = WarmAmberPrimaryContainer,
    onPrimaryContainer = Color(0xFFFFE5C2),
    secondary = WarmAmberSurfaceVariant,
    onSecondary = Color(0xFFFDE6D2),
    tertiary = WarmAmberAccent,
    onTertiary = Color.White,
    background = WarmAmberBg,
    onBackground = Color(0xFFFFF5EC),
    surface = WarmAmberSurface,
    onSurface = Color(0xFFFFF5EC),
    surfaceVariant = WarmAmberSurfaceVariant,
    onSurfaceVariant = Color(0xFFF2D4BE),
    outline = Color(0xFF6B432C)
)

private val Emerald3DColorScheme = darkColorScheme(
    primary = Emerald3DPrimary,
    onPrimary = Color(0xFF002411),
    primaryContainer = Emerald3DPrimaryContainer,
    onPrimaryContainer = Color(0xFFC8FFE0),
    secondary = Emerald3DSurfaceVariant,
    onSecondary = Color(0xFFD4F7E6),
    tertiary = Emerald3DAccent,
    onTertiary = Color(0xFF002620),
    background = Emerald3DBg,
    onBackground = Color(0xFFEAFFF4),
    surface = Emerald3DSurface,
    onSurface = Color(0xFFEAFFF4),
    surfaceVariant = Emerald3DSurfaceVariant,
    onSurfaceVariant = Color(0xFFBBEAD2),
    outline = Color(0xFF25684F)
)

private val CyberNeonColorScheme = darkColorScheme(
    primary = CyberNeonPrimary,
    onPrimary = Color.White,
    primaryContainer = CyberNeonPrimaryContainer,
    onPrimaryContainer = Color(0xFFFFD9E8),
    secondary = CyberNeonSurfaceVariant,
    onSecondary = Color(0xFFF5E6FF),
    tertiary = CyberNeonAccent,
    onTertiary = Color(0xFF002326),
    background = CyberNeonBg,
    onBackground = Color(0xFFFFF0F8),
    surface = CyberNeonSurface,
    onSurface = Color(0xFFFFF0F8),
    surfaceVariant = CyberNeonSurfaceVariant,
    onSurfaceVariant = Color(0xFFE4C6FA),
    outline = Color(0xFF6D2F9E)
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
        AppTheme.ESMERALDA_3D -> Emerald3DColorScheme
        AppTheme.CYBER_NEON_3D -> CyberNeonColorScheme
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
