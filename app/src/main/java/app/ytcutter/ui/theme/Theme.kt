package app.ytcutter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color.White,
    surface = Color(0xFFFDF7F7),
    surfaceVariant = Color(0xFFF4DEDC),
    background = Color(0xFFFDF7F7)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    surface = Color(0xFF1A1111),
    surfaceVariant = Color(0xFF524343),
    background = Color(0xFF1A1111)
)

@Composable
fun YtCutterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        content = content
    )
}