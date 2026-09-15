package com.earthnow.app.presentation.theme

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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF04202B),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFD0F0FF),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF180A3A),
    secondaryContainer = Color(0xFF3B2A6B),
    onSecondaryContainer = Color(0xFFE7DFFF),
    tertiary = Color(0xFF4ADE80),
    background = Color(0xFF05070D),
    onBackground = Color(0xFFE6ECF4),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFE6ECF4),
    surfaceVariant = Color(0xFF131C2E),
    onSurfaceVariant = Color(0xFF9FB0C7),
    outline = Color(0xFF3A4A63),
    error = Color(0xFFF87171),
    onError = Color(0xFF2B0A0A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0369A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0F0FF),
    onPrimaryContainer = Color(0xFF04202B),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7DFFF),
    onSecondaryContainer = Color(0xFF180A3A),
    tertiary = Color(0xFF16A34A),
    background = Color(0xFFF5F8FC),
    onBackground = Color(0xFF0B1220),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE3EAF3),
    onSurfaceVariant = Color(0xFF46566E),
    outline = Color(0xFF8494AB),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun EarthNowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}