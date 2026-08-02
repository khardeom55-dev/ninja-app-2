package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NinjaColorScheme = darkColorScheme(
    primary = NinjaRedPrimary,
    onPrimary = NinjaTextWhite,
    primaryContainer = NinjaBorderRed,
    onPrimaryContainer = NinjaRedGlow,
    secondary = NinjaRedGlow,
    onSecondary = NinjaTextWhite,
    background = NinjaDarkBackground,
    onBackground = NinjaTextWhite,
    surface = NinjaDarkSurface,
    onSurface = NinjaTextWhite,
    surfaceVariant = NinjaDarkSurfaceVariant,
    onSurfaceVariant = NinjaTextMuted,
    outline = NinjaCardOutline
)

@Composable
fun NinjaAutoEditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NinjaColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NinjaAutoEditorTheme(content = content)
}
