package zeki.productions.shorts.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OxbloodPalette = darkColorScheme(
    primary = Color(0xFF8B0000),
    secondary = Color(0xFF4A0000),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0000),
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color.White,
    onSurface = Color.White
)

private val MidnightPalette = darkColorScheme(
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF004499),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B),
    onBackground = Color.White,
    onSurface = Color.White
)

private val ForestPalette = darkColorScheme(
    primary = Color(0xFF10B981),
    secondary = Color(0xFF047857),
    background = Color(0xFF021008),
    surface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFF065F46),
    onBackground = Color.White,
    onSurface = Color.White
)

private val AmethystPalette = darkColorScheme(
    primary = Color(0xFFA855F7),
    secondary = Color(0xFF7E22CE),
    background = Color(0xFF180226),
    surface = Color(0xFF3B0764),
    surfaceVariant = Color(0xFF581C87),
    onBackground = Color.White,
    onSurface = Color.White
)

// Pristine Light Mode
private val LightPalette = lightColorScheme(
    primary = Color(0xFFE11D48), // Deep Rose Red for accents
    secondary = Color(0xFF9F1239),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun ShortsTheme(
    theme: AppTheme = AppTheme.OXBLOOD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.OXBLOOD -> OxbloodPalette
        AppTheme.MIDNIGHT -> MidnightPalette
        AppTheme.FOREST -> ForestPalette
        AppTheme.AMETHYST -> AmethystPalette
        AppTheme.LIGHT -> LightPalette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}