package zeki.productions.shorts.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkRedPalette = darkColorScheme(
    primary = Color(0xFF8B0000),
    secondary = Color(0xFF4A0000),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0000),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ShortsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkRedPalette,
        typography = Typography(),
        content = content
    )
}