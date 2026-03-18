package com.turbofan3360.openeq.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = Background,
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    outline = EQOn,
    primaryContainer = MenuBackground
)

@Composable
fun OpenEQTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}