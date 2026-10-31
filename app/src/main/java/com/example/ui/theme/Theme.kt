package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4EDEAA),
    onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF005139),
    onPrimaryContainer = Color(0xFF70FBC5),
    secondary = Color(0xFFB1CCC0),
    onSecondary = Color(0xFF1D352C),
    secondaryContainer = Color(0xFF334B42),
    onSecondaryContainer = Color(0xFFCDE8DC),
    background = KhataBackgroundDark,
    surface = KhataSurfaceDark,
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF45514B)
)

private val LightColorScheme = lightColorScheme(
    primary = KhataPrimary,
    onPrimary = Color.White,
    primaryContainer = KhataPrimaryContainer,
    onPrimaryContainer = KhataOnPrimaryContainer,
    secondary = KhataSecondary,
    onSecondary = Color.White,
    secondaryContainer = KhataSecondaryContainer,
    onSecondaryContainer = Color(0xFF0C1B15),
    background = KhataBackgroundLight,
    surface = KhataSurfaceLight,
    onBackground = Color(0xFF191C1B),
    onSurface = Color(0xFF191C1B),
    error = KhataRed,
    onError = Color.White,
    errorContainer = KhataRedContainer,
    onErrorContainer = Color(0xFF410002),
    outline = KhataOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
