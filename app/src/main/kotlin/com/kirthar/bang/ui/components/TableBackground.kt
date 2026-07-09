package com.kirthar.bang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// PLACEHOLDER: lo sustituye el agente de arte por la mesa ovalada de madera definitiva.
/** Fondo de la mesa de juego (estética western/saloon) que envuelve el contenido de la partida. */
@Composable
fun TableBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2E4D2E)),
        content = content,
    )
}
