package com.kirthar.bang.core.event

import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.model.Role

/**
 * Evento emitido por el motor al procesar comandos. Los eventos son el registro
 * completo de la partida: sirven para animar la UI, para el log de partida y, en el
 * futuro online, para sincronizar a los clientes.
 *
 * Los eventos llevan la información completa; la redacción de información oculta
 * (p. ej. qué cartas robó otro jugador) se hace al proyectarlos por asiento con
 * [redactFor].
 */
sealed interface GameEvent {

    data class GameStarted(
        val playerNames: List<String>,
        val characters: List<CharacterId>,
        val sheriffSeat: Int,
    ) : GameEvent

    data class TurnStarted(val seat: Int, val turnNumber: Int) : GameEvent

    /** Robo de cartas. [cards] se oculta (lista vacía) a los demás jugadores salvo [revealed]. */
    data class CardsDrawn(
        val seat: Int,
        val cards: List<Card>,
        val count: Int = cards.size,
        val revealed: Boolean = false,
    ) : GameEvent

    data class CardPlayed(val seat: Int, val card: Card, val targetSeat: Int? = null) : GameEvent

    data class CardsDiscarded(val seat: Int, val cards: List<Card>) : GameEvent

    /** Una carta cambia de dueño (Panic!, El Gringo, Vulture Sam…). [card] se oculta si venía de una mano. */
    data class CardStolen(
        val fromSeat: Int,
        val toSeat: Int,
        val card: Card?,
        val fromHand: Boolean,
    ) : GameEvent

    /** Resultado de un «¡desenfunda!» (Barril, Dinamita, Cárcel; siempre público). */
    data class DrawChecked(
        val seat: Int,
        val reason: DrawCheckReason,
        val card: Card,
        val success: Boolean,
    ) : GameEvent

    data class HealthChanged(
        val seat: Int,
        val delta: Int,
        val newHealth: Int,
        val sourceSeat: Int? = null,
    ) : GameEvent

    data class PlayerEliminated(val seat: Int, val role: Role, val bySeat: Int? = null) : GameEvent

    data class RoleRevealed(val seat: Int, val role: Role) : GameEvent

    /** El mazo se agotó y se rebarajaron los descartes. */
    data object DeckReshuffled : GameEvent

    data class GameEnded(val result: GameResult) : GameEvent
}

enum class DrawCheckReason { BARREL, DYNAMITE, JAIL }

/**
 * Devuelve la versión de este evento visible para [seat] (`null` = espectador):
 * oculta las cartas robadas/robadas-de-mano que ese asiento no debe ver.
 */
fun GameEvent.redactFor(seat: Int?): GameEvent = when (this) {
    is GameEvent.CardsDrawn ->
        if (revealed || this.seat == seat) this else copy(cards = emptyList())
    is GameEvent.CardStolen ->
        if (!fromHand || fromSeat == seat || toSeat == seat) this else copy(card = null)
    else -> this
}
