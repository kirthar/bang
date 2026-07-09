package com.kirthar.bang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BangColorScheme = darkColorScheme(
    primary = BangColors.SaloonRed,
    onPrimary = BangColors.ParchmentLight,
    primaryContainer = BangColors.SaloonRedDark,
    onPrimaryContainer = BangColors.ParchmentLight,
    secondary = BangColors.Gold,
    onSecondary = BangColors.Ink,
    secondaryContainer = BangColors.GoldDark,
    onSecondaryContainer = BangColors.ParchmentLight,
    tertiary = BangColors.WoodLight,
    onTertiary = BangColors.ParchmentLight,
    tertiaryContainer = BangColors.WoodMedium,
    onTertiaryContainer = BangColors.ParchmentLight,
    background = BangColors.WoodDarkest,
    onBackground = BangColors.ParchmentLight,
    surface = BangColors.WoodDark,
    onSurface = BangColors.ParchmentLight,
    surfaceVariant = BangColors.Parchment,
    onSurfaceVariant = BangColors.Ink,
    surfaceContainer = BangColors.WoodPlank,
    surfaceContainerHigh = BangColors.WoodMedium,
    surfaceContainerLow = BangColors.WoodDarkest,
    outline = BangColors.Gold,
    outlineVariant = BangColors.WoodLight,
    error = BangColors.SaloonRedLight,
    onError = BangColors.ParchmentLight,
    inversePrimary = BangColors.GoldLight,
    inverseSurface = BangColors.ParchmentLight,
    inverseOnSurface = BangColors.Ink,
)

/**
 * Tema visual western/saloon de BANG!: colores de madera y pergamino, tipografía
 * serif de trazo grueso y formas ligeramente redondeadas. Envuelve toda la UI del
 * juego (pantallas, componentes) para que compartan el mismo aspecto de cartel.
 */
@Composable
fun BangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BangColorScheme,
        typography = BangTypography,
        shapes = BangShapes,
        content = content,
    )
}
