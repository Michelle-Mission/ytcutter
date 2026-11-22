package app.ytcutter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E8),
    onPrimaryContainer = Color(0xFF400013),
    secondary = Color(0xFF3F4756),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E9F0),
    onSecondaryContainer = Color(0xFF141A24),
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF14161B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161B),
    surfaceVariant = Color(0xFFF2F3F7),
    onSurfaceVariant = Color(0xFF5A6070),
    outline = Color(0xFFD8DAE2),
    outlineVariant = Color(0xFFE7E9EF),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF8095),
    onPrimary = Color(0xFF5C0018),
    primaryContainer = Color(0xFF8A0B2A),
    onPrimaryContainer = Color(0xFFFFDDE3),
    secondary = Color(0xFFBFC6D4),
    onSecondary = Color(0xFF141A24),
    secondaryContainer = Color(0xFF2A3140),
    onSecondaryContainer = Color(0xFFE6E9F0),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFE9EAEF),
    surface = Color(0xFF16181E),
    onSurface = Color(0xFFE9EAEF),
    surfaceVariant = Color(0xFF23262E),
    onSurfaceVariant = Color(0xFFA9AEBB),
    outline = Color(0xFF343842),
    outlineVariant = Color(0xFF23262E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YtCutterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}