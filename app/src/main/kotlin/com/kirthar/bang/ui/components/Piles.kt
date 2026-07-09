package com.kirthar.bang.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirthar.bang.core.model.Card

// PLACEHOLDER: lo sustituye el agente de arte con la mesa/pilas definitivas.

/** Pila del mazo: dorso de carta con el número de cartas restantes. */
@Composable
fun DeckPile(count: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        CardView(card = null, faceUp = false)
        Text(
            text = "$count",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

/** Pila de descartes: la última carta descartada, boca arriba. */
@Composable
fun DiscardPile(top: Card?, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CardView(card = top, faceUp = top != null)
    }
}
