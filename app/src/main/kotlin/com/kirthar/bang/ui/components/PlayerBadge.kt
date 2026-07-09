package com.kirthar.bang.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.PublicPlayerInfo
import com.kirthar.bang.ui.theme.BangColors

/**
 * Placa de jugador: retrato vectorial del personaje, nombre, vidas como balas,
 * cartas en mano y miniaturas de cartas en juego. Marca visualmente el turno
 * activo, si es objetivo legal de la acción en curso, la estrella de Sheriff y
 * la lápida cuando el jugador ha sido eliminado.
 */
@Composable
fun PlayerBadge(
    player: PublicPlayerInfo,
    isCurrentTurn: Boolean,
    isTargetable: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    val borderColor = if (isCurrentTurn) BangColors.GoldLight else BangColors.WoodLight.copy(alpha = 0.45f)
    val borderWidth = if (isCurrentTurn) 3.dp else 1.dp

    var base: Modifier = modifier.width(132.dp)
    if (isTargetable) {
        base = base.shadow(
            elevation = 16.dp,
            shape = shape,
            ambientColor = BangColors.TargetGlow,
            spotColor = BangColors.TargetGlow,
        )
    }
    if (onClick != null) {
        base = base.clickable(onClick = onClick)
    }

    Surface(
        modifier = base,
        shape = shape,
        color = BangColors.WoodPlank,
        contentColor = BangColors.ParchmentLight,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .alpha(if (player.isAlive) 1f else 0.6f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                CharacterAvatar(character = player.character, size = 52.dp)
                if (player.role == Role.SHERIFF) {
                    SheriffStarBadge(modifier = Modifier.size(20.dp))
                }
                if (!player.isAlive) {
                    TombstoneOverlay(modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleSmall,
                color = BangColors.ParchmentLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = GameTexts.characterName(player.character),
                style = MaterialTheme.typography.labelSmall,
                color = BangColors.ParchmentDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            LivesRow(health = player.health, maxHealth = player.maxHealth)
            Spacer(Modifier.height(2.dp))
            HandCountBadge(count = player.handCount)
            if (player.inPlay.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                InPlayRow(cards = player.inPlay)
            }
        }
    }
}

@Composable
private fun SheriffStarBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(color = BangColors.Ink, radius = size.minDimension / 2f)
        drawCircle(color = BangColors.Gold, radius = size.minDimension / 2f, style = Stroke(width = size.minDimension * 0.08f))
        drawStarShape(
            center = Offset(size.width / 2f, size.height / 2f),
            outerRadius = size.minDimension * 0.40f,
            innerRadius = size.minDimension * 0.17f,
            color = BangColors.Gold,
        )
    }
}

@Composable
private fun TombstoneOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = size.minDimension / 2f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val u = size.minDimension
        drawRoundRect(
            color = BangColors.ParchmentLight,
            topLeft = Offset(cx - u * 0.15f, cy - u * 0.20f),
            size = Size(u * 0.30f, u * 0.38f),
            cornerRadius = CornerRadius(u * 0.15f, u * 0.15f),
        )
        drawLine(BangColors.Ink, Offset(cx, cy - u * 0.10f), Offset(cx, cy + u * 0.10f), strokeWidth = u * 0.035f)
        drawLine(BangColors.Ink, Offset(cx - u * 0.07f, cy - u * 0.01f), Offset(cx + u * 0.07f, cy - u * 0.01f), strokeWidth = u * 0.035f)
    }
}

@Composable
private fun LivesRow(health: Int, maxHealth: Int) {
    Row(horizontalArrangement = Arrangement.Center) {
        repeat(maxHealth.coerceAtLeast(0)) { i ->
            BulletIcon(filled = i < health, modifier = Modifier.padding(horizontal = 1.dp))
        }
    }
}

@Composable
private fun BulletIcon(filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 6.dp, height = 11.dp)) {
        val color = if (filled) BangColors.SaloonRedLight else BangColors.WoodLight.copy(alpha = 0.35f)
        drawRoundRect(color = color, cornerRadius = CornerRadius(size.width * 0.5f))
        if (filled) {
            drawRoundRect(
                color = BangColors.GoldLight.copy(alpha = 0.7f),
                topLeft = Offset(size.width * 0.15f, size.height * 0.06f),
                size = Size(size.width * 0.3f, size.height * 0.28f),
                cornerRadius = CornerRadius(size.width * 0.15f),
            )
        }
    }
}

@Composable
private fun HandCountBadge(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(14.dp)) {
            drawRoundRect(
                color = BangColors.ParchmentDark,
                topLeft = Offset(size.width * 0.18f, 0f),
                size = Size(size.width * 0.7f, size.height * 0.82f),
                cornerRadius = CornerRadius(2f),
            )
            drawRoundRect(
                color = BangColors.ParchmentLight,
                topLeft = Offset(0f, size.height * 0.18f),
                size = Size(size.width * 0.7f, size.height * 0.82f),
                cornerRadius = CornerRadius(2f),
                style = Stroke(width = 1f),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.labelSmall, color = BangColors.ParchmentLight)
    }
}

@Composable
private fun InPlayRow(cards: List<Card>) {
    val maxShown = 3
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentSize(),
    ) {
        cards.take(maxShown).forEach { c ->
            CardView(card = c, faceUp = true, modifier = Modifier.width(24.dp))
        }
        if (cards.size > maxShown) {
            Text(
                text = "+${cards.size - maxShown}",
                style = MaterialTheme.typography.labelSmall,
                color = BangColors.ParchmentLight,
            )
        }
    }
}
