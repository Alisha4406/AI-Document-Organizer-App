package com.example.aidocumentorganizer.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo40,
    onPrimaryContainer = Indigo90,

    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal90,

    tertiary = Amber80,
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber90,

    error = Rose80,
    onError = Color(0xFF450A0A),
    errorContainer = Rose40,
    onErrorContainer = Rose90,

    background = NeutralBackgroundDark,
    onBackground = NeutralOnSurfaceDark,

    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,

    outline = NeutralOutlineDark,
    outlineVariant = NeutralSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo50,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo20,

    secondary = Teal50,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal20,

    tertiary = Amber50,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber40,

    error = Rose50,
    onError = Color.White,
    errorContainer = Rose90,
    onErrorContainer = Rose40,

    background = NeutralBackgroundLight,
    onBackground = NeutralOnSurfaceLight,

    surface = NeutralSurfaceLight,
    onSurface = NeutralOnSurfaceLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralOnSurfaceVariantLight,

    outline = NeutralOutlineLight,
    outlineVariant = NeutralSurfaceVariantLight
)

@Composable
fun AIDocumentOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is OFF by default so the app keeps a consistent,
    // deliberate brand look across every device instead of inheriting
    // the user's wallpaper palette. Callers can still opt in.
    dynamicColor: Boolean = false,
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
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
