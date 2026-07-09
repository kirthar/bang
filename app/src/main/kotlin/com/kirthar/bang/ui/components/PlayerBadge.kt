package com.kirthar.bang.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.kirthar.bang.core.view.PublicPlayerInfo

// PLACEHOLDER: lo sustituye el agente de arte con el avatar/estética definitivos.
/**
 * Insignia de un jugador (rival o propia) en la mesa: nombre, personaje, vida,
 * miniaturas de las cartas en juego y rol (si es público para el observador).
 */
@Composable
fun PlayerBadge(
    player: PublicPlayerInfo,
    isCurrentTurn: Boolean,
    isTargetable: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val border = when {
        isTargetable -> BorderStroke(3.dp, Color(0xFFD32F2F))
        isCurrentTurn -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, Color(0xFF5B4636))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (player.isAlive) Color(0xFF3B2A18) else Color(0xFF1C1C1C))
            .border(border, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(6.dp),
    ) {
        Text(
            text = player.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = GameTexts.characterName(player.character),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Row {
            repeat(player.maxHealth) { index ->
                Text(if (index < player.health) "❤" else "🖤")
            }
        }
        player.role?.let { role ->
            Text(text = GameTexts.roleName(role), color = MaterialTheme.colorScheme.primary)
        }
        if (!player.isAlive) {
            Text(text = "☠", color = Color.Gray)
        }
        Row {
            player.inPlay.take(4).forEach { card ->
                CardView(card = card, modifier = Modifier.size(width = 28.dp, height = 40.dp))
            }
        }
    }
}
