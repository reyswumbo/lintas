package com.lintas.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Green40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Teal30,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Teal95,
    onTertiaryContainer = Teal10,
    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = SurfaceLight,
    onBackground = Color(0xFF191C1C),
    surface = SurfaceLight,
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTeal80,
    onPrimary = DarkTeal20,
    primaryContainer = DarkTeal30,
    onPrimaryContainer = DarkTeal90,
    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green30,
    onSecondaryContainer = Green90,
    tertiary = DarkTeal90,
    onTertiary = DarkTeal20,
    tertiaryContainer = DarkTeal40,
    onTertiaryContainer = DarkTeal90,
    error = ErrorRedLight,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = SurfaceDark,
    onBackground = Color(0xFFE1E3E1),
    surface = SurfaceDark,
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBFC9C6),
    outline = Color(0xFF899390)
)

@Composable
fun LintasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
