package com.kirthar.bang.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// PLACEHOLDER: lo sustituye el agente de arte con la estética western definitiva del botón.
/** Botón de acción estándar de la aplicación. */
@Composable
fun WesternButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text)
    }
}
