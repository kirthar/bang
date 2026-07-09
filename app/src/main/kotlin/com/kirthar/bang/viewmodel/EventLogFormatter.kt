package com.kirthar.bang.viewmodel

import com.kirthar.bang.core.event.DrawCheckReason
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.ui.components.GameTexts

/**
 * Traduce un [GameEvent] a una línea de texto en español para el log de partida.
 * [nameOf] resuelve el nombre visible de un asiento (los eventos solo llevan el número).
 */
fun describeEvent(event: GameEvent, nameOf: (Int) -> String): String = when (event) {
    is GameEvent.GameStarted ->
        "¡Comienza la partida! El Sheriff es ${nameOf(event.sheriffSeat)}."

    is GameEvent.TurnStarted ->
        "Turno ${event.turnNumber}: le toca a ${nameOf(event.seat)}."

    is GameEvent.CardsDrawn ->
        if (event.revealed && event.cards.isNotEmpty()) {
            val cards = event.cards.joinToString { GameTexts.cardName(it.kind) }
            "${nameOf(event.seat)} roba ${event.count} carta(s): $cards."
        } else {
            "${nameOf(event.seat)} roba ${event.count} carta(s)."
        }

    is GameEvent.CardPlayed -> {
        val target = event.targetSeat
        if (target != null) {
            "${nameOf(event.seat)} juega ${GameTexts.cardName(event.card.kind)} contra ${nameOf(target)}."
        } else {
            "${nameOf(event.seat)} juega ${GameTexts.cardName(event.card.kind)}."
        }
    }

    is GameEvent.CardsDiscarded -> {
        val cards = event.cards.joinToString { GameTexts.cardName(it.kind) }
        "${nameOf(event.seat)} descarta: $cards."
    }

    is GameEvent.CardStolen -> {
        val what = if (event.fromHand) "una carta de la mano" else event.card?.let { GameTexts.cardName(it.kind) } ?: "una carta"
        "${nameOf(event.toSeat)} toma $what de ${nameOf(event.fromSeat)}."
    }

    is GameEvent.DrawChecked -> {
        val reason = when (event.reason) {
            DrawCheckReason.BARREL -> "Barril"
            DrawCheckReason.DYNAMITE -> "Dinamita"
            DrawCheckReason.JAIL -> "Cárcel"
        }
        val cardText = "${event.card.rank.display}${GameTexts.suitSymbol(event.card.suit)}"
        val outcome = if (event.success) "éxito" else "fallo"
        "${nameOf(event.seat)} hace «¡desenfunda!» ($reason): $cardText ($outcome)."
    }

    is GameEvent.HealthChanged ->
        if (event.delta < 0) {
            "${nameOf(event.seat)} pierde ${-event.delta} vida(s) (le quedan ${event.newHealth})."
        } else {
            "${nameOf(event.seat)} recupera ${event.delta} vida(s) (tiene ${event.newHealth})."
        }

    is GameEvent.PlayerEliminated ->
        "${nameOf(event.seat)} ha sido eliminado. Era ${GameTexts.roleName(event.role)}."

    is GameEvent.RoleRevealed ->
        "Se revela el rol de ${nameOf(event.seat)}: ${GameTexts.roleName(event.role)}."

    is GameEvent.DeckReshuffled ->
        "El mazo se agotó: se rebarajan los descartes."

    is GameEvent.GameEnded ->
        "Fin de la partida. Gana el bando: ${GameTexts.roleName(event.result.winningRole)}."
}
