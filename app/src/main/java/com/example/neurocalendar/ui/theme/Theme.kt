package com.example.neurocalendar.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = WarmBlush,
    onPrimary = NightUmber,
    primaryContainer = DustyRose,
    onPrimaryContainer = Color(0xFFFFE9DC),
    secondary = NightSage,
    onSecondary = NightUmber,
    secondaryContainer = NightLeaf,
    onSecondaryContainer = Color(0xFFE0EAD6),
    tertiary = NightGold,
    onTertiary = NightUmber,
    background = NightUmber,
    onBackground = Color(0xFFF1E7DC),
    surface = NightSurface,
    onSurface = Color(0xFFF1E7DC),
    surfaceVariant = Color(0xFF49403A),
    onSurfaceVariant = Color(0xFFD2C4B8),
    outline = Color(0xFF9B897C),
    outlineVariant = Color(0xFF524840)
)

private val LightColorScheme = lightColorScheme(
    primary = Cinnamon,
    onPrimary = Linen,
    primaryContainer = SoftRose,
    onPrimaryContainer = WarmUmber,
    secondary = Sage,
    onSecondary = Linen,
    secondaryContainer = LeafCream,
    onSecondaryContainer = Color(0xFF34402F),
    tertiary = GoldenHoney,
    onTertiary = Linen,
    background = Oat,
    onBackground = WarmUmber,
    surface = Linen,
    onSurface = WarmUmber,
    surfaceVariant = Pebble,
    onSurfaceVariant = MutedUmber,
    outline = Color(0xFF85766B),
    outlineVariant = Color(0xFFD8CCBF)
)

@Composable
fun NeuroCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}