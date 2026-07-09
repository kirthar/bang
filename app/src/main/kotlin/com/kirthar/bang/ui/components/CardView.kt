package com.kirthar.bang.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirthar.bang.core.model.Card

// PLACEHOLDER: lo sustituye el agente de arte con las ilustraciones definitivas de cada carta.
/**
 * Representación visual de una carta. Si [card] es `null` o [faceUp] es `false`, se
 * muestra el dorso.
 */
@Composable
fun CardView(
    card: Card?,
    modifier: Modifier = Modifier,
    faceUp: Boolean = true,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shown = card.takeIf { faceUp }
    val background = if (shown == null) Color(0xFF2E4057) else Color(0xFFF3E7C9)
    val border = when {
        selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, Color(0xFF5B4636))
    }
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 96.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(border, RoundedCornerShape(6.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (shown == null) {
            Text(text = "🎴", fontSize = 22.sp)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${shown.rank.display}${GameTexts.suitSymbol(shown.suit)}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    text = GameTexts.cardName(shown.kind),
                    color = Color.Black,
                    fontSize = 10.sp,
                    maxLines = 3,
                )
            }
        }
    }
}
