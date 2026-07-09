package com.kirthar.bang.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardCategory
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.ui.theme.BangColors

/**
 * Carta de juego dibujada íntegramente en Compose (sin recursos de imagen): marco
 * de pergamino con borde de color según su categoría (marrón = efecto inmediato,
 * azul = permanece en juego), nombre en español, icono vectorial propio según el
 * [com.kirthar.bang.core.model.CardKind] y palo/valor en la esquina.
 *
 * Pensada para tamaños entre 60dp y 120dp de ancho; el llamador controla el ancho
 * mediante [modifier] (por ejemplo `Modifier.width(90.dp)`), y la carta mantiene su
 * proporción clásica.
 *
 * @param card la carta a mostrar, o `null` para una carta desconocida (siempre dorso).
 * @param faceUp si es `false` (o [card] es `null`) se dibuja el dorso decorado.
 * @param selected resalta la carta con borde dorado y más elevación (carta elegida).
 * @param enabled si es `false`, la carta se atenúa y no responde a toques.
 * @param onClick acción al tocar la carta; si es `null` la carta no es interactiva.
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
    val shape = RoundedCornerShape(10.dp)
    val showFace = faceUp && card != null
    val borderColor = when {
        selected -> BangColors.GoldLight
        !showFace -> BangColors.Gold.copy(alpha = 0.55f)
        card!!.kind.category == CardCategory.BROWN -> BangColors.BrownCardBorder
        else -> BangColors.BlueCardBorder
    }
    val borderWidth = if (selected) 3.dp else 1.5.dp
    val elevation = if (selected) 10.dp else 2.dp

    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .alpha(if (enabled) 1f else 0.4f)
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .let { m -> if (onClick != null) m.clickable(enabled = enabled, onClick = onClick) else m }
            .background(
                if (showFace) {
                    Brush.verticalGradient(listOf(BangColors.ParchmentLight, BangColors.Parchment))
                } else {
                    Brush.verticalGradient(listOf(BangColors.WoodPlank, BangColors.WoodDarkest))
                },
            )
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (showFace) {
            CardFaceContent(card!!)
        } else {
            CardBackContent()
        }
    }
}

@Composable
private fun CardFaceContent(card: Card) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = GameTexts.cardName(card.kind),
                style = MaterialTheme.typography.labelSmall,
                color = BangColors.Ink,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SuitRankBadge(card)
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            val accent = if (card.kind.category == CardCategory.BROWN) BangColors.SaloonRed else BangColors.BlueCardBorder
            drawCardKindIcon(card.kind, tint = BangColors.Ink, accent = accent)
        }

        if (card.kind.isWeapon) {
            WeaponRangeIndicator(range = card.kind.weaponRange ?: 1)
        }
    }
}

@Composable
private fun SuitRankBadge(card: Card) {
    val color = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) BangColors.SuitRed else BangColors.SuitBlack
    Column(horizontalAlignment = Alignment.End) {
        Text(text = card.rank.display, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(text = GameTexts.suitSymbol(card.suit), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun WeaponRangeIndicator(range: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(range.coerceIn(1, 6)) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(5.dp)
                    .background(BangColors.Gold, CircleShape),
            )
        }
    }
}

@Composable
private fun CardBackContent() {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height
        val inset = size.minDimension * 0.08f
        drawRoundRect(
            color = BangColors.GoldDark,
            topLeft = Offset(inset, inset),
            size = Size(w - 2 * inset, h - 2 * inset),
            cornerRadius = CornerRadius(size.minDimension * 0.08f),
            style = Stroke(width = size.minDimension * 0.035f),
        )
        drawStarShape(
            center = Offset(w / 2f, h / 2f),
            outerRadius = size.minDimension * 0.22f,
            innerRadius = size.minDimension * 0.09f,
            color = BangColors.Gold,
        )
        val cornerOffsets = listOf(
            Offset(inset * 1.6f, inset * 1.6f),
            Offset(w - inset * 1.6f, inset * 1.6f),
            Offset(inset * 1.6f, h - inset * 1.6f),
            Offset(w - inset * 1.6f, h - inset * 1.6f),
        )
        for (corner in cornerOffsets) {
            drawStarShape(
                center = corner,
                outerRadius = size.minDimension * 0.05f,
                innerRadius = size.minDimension * 0.02f,
                color = BangColors.GoldDark,
                points = 4,
                rotationDegrees = 0f,
            )
        }
    }
}
