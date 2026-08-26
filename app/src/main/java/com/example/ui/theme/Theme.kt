package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PlumPrimary,
    onPrimary = Color.White,
    primaryContainer = PlumPrimaryContainer,
    onPrimaryContainer = PlumOnPrimaryContainer,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldAccentContainer,
    onSecondaryContainer = GoldOnAccentContainer,
    tertiary = PlumPrimaryLight,
    onTertiary = Color.White,
    background = CreamBackground,
    onBackground = WarmOnSurface,
    surface = CreamSurface,
    onSurface = WarmOnSurface,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = PlumPrimaryDark,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkAccent,
    onSecondary = DarkBackground,
    secondaryContainer = GoldAccentDark,
    onSecondaryContainer = DarkAccent,
    tertiary = PlumPrimaryLight,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

val CadetTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp), // 20dp rounded corners as specified
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CadetTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CadetTrackShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CadetTrackTheme(darkTheme = darkTheme, content = content)
}
