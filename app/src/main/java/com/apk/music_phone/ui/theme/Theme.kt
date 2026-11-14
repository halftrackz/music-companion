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
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1C1C1C),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFF2A2A2A),
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
