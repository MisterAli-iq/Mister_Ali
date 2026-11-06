package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YouTubeDarkColorScheme = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    primaryContainer = YouTubeRedDark,
    onPrimaryContainer = Color.White,
    secondary = YouTubeGold,
    onSecondary = Color.Black,
    secondaryContainer = YouTubeSurfaceVariant,
    onSecondaryContainer = Color.White,
    background = YouTubeBlack,
    onBackground = YouTubeTextPrimary,
    surface = YouTubeSurface,
    onSurface = YouTubeTextPrimary,
    surfaceVariant = YouTubeSurfaceVariant,
    onSurfaceVariant = YouTubeTextSecondary,
    outline = YouTubeBorder,
    error = YouTubeRed,
    onError = Color.White
)

@Composable
fun RahmaRiadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YouTubeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
