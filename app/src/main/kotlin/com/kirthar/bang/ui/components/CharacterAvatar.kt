package com.kirthar.bang.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.ui.theme.BangColors

/**
 * Avatar vectorial genérico y original por personaje: silueta de busto con
 * sombrero y un color propio por [CharacterId] (no son los 16 retratos únicos del
 * juego oficial, pero sí distinguibles a simple vista por color y detalle).
 */
@Composable
fun CharacterAvatar(character: CharacterId, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val color = characterColor(character)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color, color.copy(alpha = 0.75f))))
            .border(1.dp, BangColors.GoldDark.copy(alpha = 0.7f), CircleShape),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.14f),
        ) {
            drawCharacterGlyph(character, accent = BangColors.ParchmentLight)
        }
    }
}

/** Color distintivo asignado a cada uno de los 16 personajes del juego base. */
fun characterColor(character: CharacterId): Color = when (character) {
    CharacterId.BART_CASSIDY -> Color(0xFFB33A3A)
    CharacterId.BLACK_JACK -> Color(0xFF3A3A3A)
    CharacterId.CALAMITY_JANET -> Color(0xFF7A3E7A)
    CharacterId.EL_GRINGO -> Color(0xFFC79A2E)
    CharacterId.JESSE_JONES -> Color(0xFF3E6B8A)
    CharacterId.JOURDONNAIS -> Color(0xFF4A7A4A)
    CharacterId.KIT_CARLSON -> Color(0xFF8A6B14)
    CharacterId.LUCKY_DUKE -> Color(0xFF2C5A78)
    CharacterId.PAUL_REGRET -> Color(0xFF5D3A8A)
    CharacterId.PEDRO_RAMIREZ -> Color(0xFFA65B2A)
    CharacterId.ROSE_DOOLAN -> Color(0xFFB3467C)
    CharacterId.SID_KETCHUM -> Color(0xFF6B4423)
    CharacterId.SLAB_THE_KILLER -> Color(0xFF262626)
    CharacterId.SUZY_LAFAYETTE -> Color(0xFFC77DA0)
    CharacterId.VULTURE_SAM -> Color(0xFF565A34)
    CharacterId.WILLY_THE_KID -> Color(0xFFA31E22)
}

private fun DrawScope.drawCharacterGlyph(character: CharacterId, accent: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val u = size.minDimension

    // Cabeza
    drawCircle(color = accent, radius = u * 0.22f, center = Offset(cx, cy + u * 0.10f))
    // Ala del sombrero
    drawOval(
        color = accent,
        topLeft = Offset(cx - u * 0.36f, cy - u * 0.18f),
        size = Size(u * 0.72f, u * 0.14f),
    )
    // Copa del sombrero
    drawRoundRect(
        color = accent,
        topLeft = Offset(cx - u * 0.16f, cy - u * 0.36f),
        size = Size(u * 0.32f, u * 0.20f),
        cornerRadius = CornerRadius(u * 0.06f),
    )

    // Pequeño detalle distintivo, variando por personaje (bandana / bigote / cinta).
    when (character.ordinal % 3) {
        0 -> drawRoundRect(
            color = accent,
            topLeft = Offset(cx - u * 0.20f, cy + u * 0.15f),
            size = Size(u * 0.40f, u * 0.09f),
            cornerRadius = CornerRadius(u * 0.03f),
        )
        1 -> drawArc(
            color = accent,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = u * 0.03f),
            topLeft = Offset(cx - u * 0.14f, cy + u * 0.10f),
            size = Size(u * 0.28f, u * 0.14f),
        )
        else -> drawLine(
            color = accent,
            start = Offset(cx - u * 0.20f, cy - u * 0.02f),
            end = Offset(cx + u * 0.20f, cy - u * 0.02f),
            strokeWidth = u * 0.03f,
        )
    }
}
