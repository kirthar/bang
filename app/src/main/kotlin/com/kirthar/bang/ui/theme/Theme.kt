package com.kirthar.bang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// PLACEHOLDER: lo sustituye el agente de arte por la ambientación western/saloon definitiva.
private val BangColorScheme = darkColorScheme(
    primary = Color(0xFFC9A227),
    onPrimary = Color(0xFF241000),
    secondary = Color(0xFF8B4513),
    background = Color(0xFF241708),
    surface = Color(0xFF3B2A18),
    onBackground = Color(0xFFEFE0C0),
    onSurface = Color(0xFFEFE0C0),
    error = Color(0xFFB3261E),
)

/** Tema general de la aplicación. Envuelve toda la UI de BANG!. */
@Composable
fun BangTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BangColorScheme, content = content)
}
