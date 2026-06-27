package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldLight,
    tertiary = AquaLight,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    onPrimary = SlateBackgroundDark,
    onSecondary = SlateBackgroundDark,
    onBackground = SlateOnSurfaceDark,
    onSurface = SlateOnSurfaceDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldDark,
    secondary = EmeraldPrimary,
    tertiary = AquaWater,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    onPrimary = SlateBackgroundLight,
    onSecondary = SlateBackgroundLight,
    onBackground = SlateOnSurfaceLight,
    onSurface = SlateOnSurfaceLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Keep false to ensure our custom beautiful theme is fully shown by default!
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
