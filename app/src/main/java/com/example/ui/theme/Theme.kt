package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DibinaBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DibinaBlueContainer,
    onPrimaryContainer = DibinaOnBlueContainer,
    secondary = DibinaTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = DibinaTealContainer,
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = DibinaOrangeAccent,
    background = DibinaBackground,
    surface = DibinaSurface,
    surfaceVariant = DibinaSurfaceVariant,
    onBackground = DibinaTextPrimary,
    onSurface = DibinaTextPrimary,
    onSurfaceVariant = DibinaTextSecondary,
    outline = DibinaDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = DibinaBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DibinaBlueDark,
    onPrimaryContainer = DibinaBlueContainer,
    secondary = DibinaTealSecondary,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent child-friendly brand palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun DibinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
