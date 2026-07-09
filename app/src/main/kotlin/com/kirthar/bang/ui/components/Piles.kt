package com.kirthar.bang.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.ui.theme.BangColors

/** Cuántas cartas "de relleno" se dibujan como máximo para simular el grosor de una pila. */
private const val MAX_STACK_LAYERS = 5

/**
 * Pila del mazo: un montón de cartas bocabajo con efecto de grosor y un contador
 * de cartas restantes.
 */
@Composable
fun DeckPile(count: Int, modifier: Modifier = Modifier) {
    val layers = count.coerceIn(0, MAX_STACK_LAYERS)
    Box(modifier = modifier.width(90.dp), contentAlignment = Alignment.Center) {
        if (count <= 0) {
            CardView(card = null, faceUp = false, enabled = false, modifier = Modifier.width(90.dp))
        } else {
            for (i in layers downTo 1) {
                CardView(
                    card = null,
                    faceUp = false,
                    modifier = Modifier
                        .width(90.dp)
                        .offset(x = 0.dp, y = -(i - 1).dp * 1.5f),
                )
            }
        }
        PileCounter(count = count)
    }
}

/**
 * Pila de descartes: muestra la carta superior bocarriba (o vacía si aún no hay
 * descartes) con el mismo efecto de grosor que [DeckPile].
 */
@Composable
fun DiscardPile(top: Card?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(90.dp), contentAlignment = Alignment.Center) {
        if (top == null) {
            EmptyPileSlot()
        } else {
            for (i in 2 downTo 1) {
                CardView(
                    card = top,
                    faceUp = i == 1,
                    modifier = Modifier
                        .width(90.dp)
                        .offset(x = 0.dp, y = -(i - 1).dp * 1.5f),
                )
            }
        }
    }
}

@Composable
private fun EmptyPileSlot() {
    Surface(
        modifier = Modifier
            .width(90.dp)
            .padding(2.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, BangColors.Gold.copy(alpha = 0.4f)),
        shape = MaterialTheme.shapes.small,
    ) {
        Box(
            modifier = Modifier
                .width(90.dp)
                .aspectRatio(0.68f, matchHeightConstraintsFirst = false),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "—", color = BangColors.Gold.copy(alpha = 0.6f), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PileCounter(count: Int) {
    Surface(
        color = BangColors.Ink.copy(alpha = 0.78f),
        contentColor = BangColors.ParchmentLight,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
