package com.kirthar.bang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirthar.bang.ui.theme.BangColors

/**
 * Botón con aspecto de cartel de madera tallada / cartel de "se busca": tabla con
 * veta sutil, marco dorado grueso y texto en mayúsculas con tipografía western.
 */
@Composable
fun WesternButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 120.dp, minHeight = 44.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(if (enabled) 6.dp else 0.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(BangColors.WoodLight, BangColors.WoodPlank)))
            .border(2.dp, BangColors.Gold.copy(alpha = 0.85f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = BangColors.ParchmentLight,
            textAlign = TextAlign.Center,
        )
    }
}
