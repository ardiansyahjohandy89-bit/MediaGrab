package com.mediagrab.app.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mediagrab.app.data.Prefs
import com.mediagrab.app.data.ThemeMode

val AccentColors = listOf(
    "Cyan" to Color(0xFF0891B2),
    "Hijau" to Color(0xFF16A34A),
    "Ungu" to Color(0xFF7C3AED),
    "Oranye" to Color(0xFFEA580C),
    "Merah" to Color(0xFFDC2626),
    "Pink" to Color(0xFFDB2777)
)

private fun accentScheme(c: Color, dark: Boolean): ColorScheme =
    if (dark) {
        val p = lerp(c, Color.White, 0.35f)
        val container = lerp(c, Color.Black, 0.6f)
        darkColorScheme(
            primary = p,
            onPrimary = Color.Black,
            primaryContainer = container,
            onPrimaryContainer = Color.White,
            secondary = p,
            secondaryContainer = container,
            onSecondaryContainer = Color.White,
            tertiary = p
        )
    } else {
        val container = lerp(c, Color.White, 0.82f)
        lightColorScheme(
            primary = c,
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = Color(0xFF111111),
            secondary = c,
            secondaryContainer = container,
            onSecondaryContainer = Color(0xFF111111),
            tertiary = c
        )
    }

@Composable
fun MediaGrabTheme(content: @Composable () -> Unit) {
    val mode by Prefs.themeMode.collectAsState()
    val dynamic by Prefs.dynamicColor.collectAsState()
    val accent by Prefs.accent.collectAsState()
    val ctx = LocalContext.current
    val view = LocalView.current

    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        accentScheme(AccentColors[accent.coerceIn(0, AccentColors.lastIndex)].second, dark)
    }

    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
