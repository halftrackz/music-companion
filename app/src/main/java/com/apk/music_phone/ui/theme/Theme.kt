package com.apk.music_phone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF002D5F),
    onPrimaryContainer = Color.White,
    secondary = SamsungPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D001C),
    onSecondaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF2B2B2B),
    surfaceVariant = Color(0xFF262626),
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = Color.Gray
)

@Composable
fun MusicPhoneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
