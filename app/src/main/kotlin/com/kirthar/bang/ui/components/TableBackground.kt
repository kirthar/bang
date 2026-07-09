package com.kirthar.bang.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kirthar.bang.ui.theme.BangColors

/**
 * Fondo de pantalla completo de la partida: una mesa ovalada de madera oscura con
 * veta dibujada a mano (gradientes y arcos en Canvas) sobre un ambiente de saloon
 * en penumbra. [content] se dibuja encima, ya alineado con el resto de la UI.
 */
@Composable
fun TableBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSaloonAmbient()
            drawWoodenOvalTable()
        }
        content()
    }
}

private fun DrawScope.drawSaloonAmbient() {
    val ambient = Brush.radialGradient(
        colors = listOf(BangColors.WoodDark, BangColors.WoodDarkest),
        center = Offset(size.width / 2f, size.height * 0.42f),
        radius = size.maxDimension * 0.8f,
    )
    drawRect(brush = ambient)
    val vignette = Brush.radialGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = size.maxDimension * 0.75f,
    )
    drawRect(brush = vignette)
}

private fun DrawScope.drawWoodenOvalTable() {
    val tableWidth = size.width * 0.88f
    val tableHeight = size.height * 0.80f
    val left = (size.width - tableWidth) / 2f
    val top = (size.height - tableHeight) / 2f
    val rim = size.minDimension * 0.035f

    // Aro exterior de madera oscura.
    drawOval(
        brush = Brush.radialGradient(listOf(BangColors.WoodMedium, BangColors.WoodDark)),
        topLeft = Offset(left - rim, top - rim),
        size = Size(tableWidth + rim * 2f, tableHeight + rim * 2f),
    )

    // Superficie principal con un foco de luz descentrado, como bajo una lámpara de saloon.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(BangColors.WoodLight, BangColors.WoodMedium, BangColors.WoodDark),
            center = Offset(left + tableWidth * 0.38f, top + tableHeight * 0.32f),
            radius = tableWidth * 0.75f,
        ),
        topLeft = Offset(left, top),
        size = Size(tableWidth, tableHeight),
    )

    // Vetas de madera concéntricas.
    val grainColor = BangColors.WoodDarkest.copy(alpha = 0.22f)
    val aspect = tableHeight / tableWidth
    for (i in 1..7) {
        val insetX = tableWidth * 0.055f * i
        val insetY = insetX * aspect
        if (insetX * 2f < tableWidth && insetY * 2f < tableHeight) {
            drawOval(
                color = grainColor,
                topLeft = Offset(left + insetX, top + insetY),
                size = Size(tableWidth - insetX * 2f, tableHeight - insetY * 2f),
                style = Stroke(width = size.minDimension * 0.004f),
            )
        }
    }

    // Cerco dorado, como el filo metálico de una mesa de saloon.
    drawOval(
        color = BangColors.Gold.copy(alpha = 0.55f),
        topLeft = Offset(left, top),
        size = Size(tableWidth, tableHeight),
        style = Stroke(width = size.minDimension * 0.006f),
    )
}
