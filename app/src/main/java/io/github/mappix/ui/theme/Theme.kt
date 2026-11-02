package io.github.mappix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A0FA), secondary = Color(0xFFCCC2DD),
    background = Color(0xFF111017), surface = Color(0xFF1C1A23),
    surfaceVariant = Color(0xFF302C3B), onBackground = Color(0xFFF4F1F9),
    onSurface = Color(0xFFF4F1F9)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF6547AC), secondary = Color(0xFF645A76),
    background = Color(0xFFFBF9FF), surface = Color(0xFFF5F1FB)
)

@Composable
fun MappixTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
