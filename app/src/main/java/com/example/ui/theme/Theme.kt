package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AppAccentColor
import com.example.model.AppThemeMode

data class CustomThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val isDark: Boolean
)

val LocalCustomTheme = staticCompositionLocalOf {
    CustomThemePalette(
        background = YouTubeBlack,
        surface = YouTubeSurface,
        surfaceVariant = YouTubeSurfaceVariant,
        textPrimary = YouTubeTextPrimary,
        textSecondary = YouTubeTextSecondary,
        border = YouTubeBorder,
        isDark = true
    )
}

@Composable
fun RahmaRiadTheme(
    themeMode: AppThemeMode = AppThemeMode.YOUTUBE_DARK,
    accentColor: AppAccentColor = AppAccentColor.RED,
    content: @Composable () -> Unit
) {
    val primaryColor = Color(accentColor.hex)

    val (colorScheme, customPalette) = when (themeMode) {
        AppThemeMode.YOUTUBE_DARK -> {
            val palette = CustomThemePalette(
                background = YouTubeBlack,
                surface = YouTubeSurface,
                surfaceVariant = YouTubeSurfaceVariant,
                textPrimary = Color.White,
                textSecondary = Color(0xFFAAAAAA),
                border = Color(0xFF383838),
                isDark = true
            )
            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.White,
                secondary = YouTubeGold,
                background = palette.background,
                onBackground = palette.textPrimary,
                surface = palette.surface,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.surfaceVariant,
                onSurfaceVariant = palette.textSecondary,
                outline = palette.border
            ) to palette
        }
        AppThemeMode.AMOLED_BLACK -> {
            val palette = CustomThemePalette(
                background = Color(0xFF000000),
                surface = Color(0xFF101010),
                surfaceVariant = Color(0xFF181818),
                textPrimary = Color.White,
                textSecondary = Color(0xFFAAAAAA),
                border = Color(0xFF282828),
                isDark = true
            )
            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.White,
                secondary = YouTubeGold,
                background = palette.background,
                onBackground = palette.textPrimary,
                surface = palette.surface,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.surfaceVariant,
                onSurfaceVariant = palette.textSecondary,
                outline = palette.border
            ) to palette
        }
        AppThemeMode.LIGHT -> {
            val palette = CustomThemePalette(
                background = Color(0xFFF7F7F9),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEEEEF2),
                textPrimary = Color(0xFF121212),
                textSecondary = Color(0xFF555555),
                border = Color(0xFFE0E0E0),
                isDark = false
            )
            lightColorScheme(
                primary = primaryColor,
                onPrimary = Color.White,
                secondary = YouTubeGold,
                background = palette.background,
                onBackground = palette.textPrimary,
                surface = palette.surface,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.surfaceVariant,
                onSurfaceVariant = palette.textSecondary,
                outline = palette.border
            ) to palette
        }
        AppThemeMode.ROYAL_GOLD -> {
            val palette = CustomThemePalette(
                background = Color(0xFF120E05),
                surface = Color(0xFF201908),
                surfaceVariant = Color(0xFF2D230C),
                textPrimary = Color(0xFFFFF8E7),
                textSecondary = Color(0xFFD4AF37),
                border = Color(0xFF4A3B14),
                isDark = true
            )
            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.Black,
                secondary = YouTubeGold,
                background = palette.background,
                onBackground = palette.textPrimary,
                surface = palette.surface,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.surfaceVariant,
                onSurfaceVariant = palette.textSecondary,
                outline = palette.border
            ) to palette
        }
    }

    CompositionLocalProvider(LocalCustomTheme provides customPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
