package com.nexus.browser.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

data class NexusExtendedPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val error: Color
)

val LocalNexusPalette = staticCompositionLocalOf {
    NexusExtendedPalette(
        isDark = true,
        background = NexusColors.DarkBackground,
        surface = NexusColors.DarkSurface,
        surfaceElevated = NexusColors.DarkSurfaceElevated,
        primary = NexusColors.DarkPrimary,
        secondary = NexusColors.DarkSecondary,
        textPrimary = NexusColors.DarkTextPrimary,
        textSecondary = NexusColors.DarkTextSecondary,
        border = NexusColors.DarkBorder,
        error = NexusColors.Error
    )
}

val NexusTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun NexusTheme(
    themeMode: String = "DARK",
    accentHex: String = "#5685FF",
    textScalePercent: Int = 100,
    isPrivateMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode.uppercase()) {
        "LIGHT" -> false
        "SYSTEM" -> systemDark
        else -> true
    }

    val customPrimary = if (isPrivateMode) {
        NexusColors.PrivateAccent
    } else {
        NexusColors.parseHexColor(
            accentHex,
            if (useDark) NexusColors.DarkPrimary else NexusColors.LightPrimary
        )
    }

    val palette = if (useDark) {
        NexusExtendedPalette(
            isDark = true,
            background = if (isPrivateMode) NexusColors.PrivateBackground else NexusColors.DarkBackground,
            surface = if (isPrivateMode) NexusColors.PrivateSurface else NexusColors.DarkSurface,
            surfaceElevated = NexusColors.DarkSurfaceElevated,
            primary = customPrimary,
            secondary = NexusColors.DarkSecondary,
            textPrimary = NexusColors.DarkTextPrimary,
            textSecondary = NexusColors.DarkTextSecondary,
            border = NexusColors.DarkBorder,
            error = NexusColors.Error
        )
    } else {
        NexusExtendedPalette(
            isDark = false,
            background = NexusColors.LightBackground,
            surface = NexusColors.LightSurface,
            surfaceElevated = NexusColors.LightSurfaceElevated,
            primary = customPrimary,
            secondary = NexusColors.LightSecondary,
            textPrimary = NexusColors.LightTextPrimary,
            textSecondary = NexusColors.LightTextSecondary,
            border = NexusColors.LightBorder,
            error = NexusColors.Error
        )
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceElevated,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.error
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceElevated,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.error
        )
    }

    val currentDensity = LocalDensity.current
    val scaledFontScale = currentDensity.fontScale * (textScalePercent.coerceIn(75, 160) / 100f)

    CompositionLocalProvider(
        LocalNexusPalette provides palette,
        LocalDensity provides Density(currentDensity.density, scaledFontScale)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexusTypography,
            content = content
        )
    }
}
