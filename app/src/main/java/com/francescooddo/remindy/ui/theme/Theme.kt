package com.francescooddo.remindy.ui.theme

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

val Indigo = Color(0xFF4F46E5)
val Teal = Color(0xFF14B8A6)

private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = Color(0xFF6366F1),
    tertiary = Teal
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    secondary = Color(0xFF818CF8),
    tertiary = Color(0xFF2DD4BF)
)

@Composable
fun RemindyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
