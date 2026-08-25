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

private val DarkColorScheme =
  darkColorScheme(
    primary = OrangePrimary,
    onPrimary = OnOrangePrimary,
    primaryContainer = OrangeHover,
    onPrimaryContainer = Color.White,
    secondary = OrangeContainer,
    onSecondary = InkDark,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFF3E3E3E),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OnOrangePrimary,
    primaryContainer = OrangeLight,
    onPrimaryContainer = OrangeHover,
    secondary = InkDark,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = InkDark,
    surface = SurfaceLight,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkMedium,
    outline = OutlineLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  // For BeBoss high-contrast brand, we prefer the vibrant orange identity by default
  dynamicColor: Boolean = false,
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

