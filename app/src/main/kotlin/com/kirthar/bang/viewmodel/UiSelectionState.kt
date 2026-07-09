package com.kirthar.bang.viewmodel

/**
 * Estado de selección en curso dentro de [GameScreen][com.kirthar.bang.ui.screens.GameScreen]:
 * qué carta se ha tocado y, si necesita un objetivo, qué asientos son objetivos legales.
 *
 * @param selectedCardId carta de la mano tocada en flujo «jugar carta con objetivo» (BANG!,
 *   Cat Balou, Panic!, Duelo...). `null` si no hay ninguna carta seleccionada en ese flujo.
 * @param targetableSeats asientos válidos como objetivo de [selectedCardId]. Tocar uno de
 *   ellos envía el comando; tocar de nuevo [selectedCardId] deshace la selección.
 * @param multiSelected cartas acumuladas en un flujo de selección múltiple (reaccionar con
 *   ¡Fallaste!/BANG!, descartar al límite de mano, usar la habilidad de Sid Ketchum). Se
 *   envía automáticamente en cuanto coincide exactamente con una de las opciones legales.
 * @param abilityMode si `true`, tocar cartas de la mano se interpreta como selección para
 *   [com.kirthar.bang.core.command.GameCommand.UseCharacterAbility] en vez de para jugarlas.
 */
data class UiSelectionState(
    val selectedCardId: Int? = null,
    val targetableSeats: Set<Int> = emptySet(),
    val multiSelected: Set<Int> = emptySet(),
    val abilityMode: Boolean = false,
)
