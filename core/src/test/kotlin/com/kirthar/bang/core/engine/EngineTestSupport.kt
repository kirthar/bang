package com.kirthar.bang.core.engine

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.model.PlayerState
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.view.DecisionRequest

/** Utilidades para construir estados de partida deterministas en los tests del motor. */
internal object Ids {
    private var n = 1000
    fun next(): Int = n++
}

internal fun card(kind: CardKind, suit: Suit = Suit.SPADES, rank: Rank = Rank.TWO): Card =
    Card(Ids.next(), kind, suit, rank)

internal fun player(
    seat: Int,
    role: Role,
    character: CharacterId = CharacterId.BLACK_JACK,
    health: Int = 4,
    maxHealth: Int = health,
    hand: List<Card> = emptyList(),
    inPlay: List<Card> = emptyList(),
    alive: Boolean = true,
): PlayerState = PlayerState(seat, "P$seat", character, role, health, maxHealth, hand, inPlay, alive)

internal fun gameState(
    players: List<PlayerState>,
    deck: List<Card> = List(30) { card(CardKind.BANG, Suit.DIAMONDS, Rank.KING) },
    discard: List<Card> = emptyList(),
    currentSeat: Int = 0,
    phase: GamePhase = GamePhase.PLAY,
    pending: List<com.kirthar.bang.core.model.PendingInteraction> = emptyList(),
    bangsPlayedThisTurn: Int = 0,
    turnNumber: Int = 1,
): GameState = GameState(players, deck, discard, currentSeat, phase, pending, bangsPlayedThisTurn, turnNumber, null)

internal fun engineFrom(state: GameState, seed: Long = 1L): GameEngine =
    DefaultGameEngineFactory().restore(state, seed)

internal fun GameEngine.actor(): Int = pendingDecision()!!.first
internal fun GameEngine.request(): DecisionRequest = pendingDecision()!!.second

/** Envía un comando y exige que sea aceptado. */
internal fun GameEngine.accept(seat: Int, command: GameCommand): List<com.kirthar.bang.core.event.GameEvent> {
    val result = submit(seat, command)
    check(result is CommandResult.Accepted) {
        "Comando rechazado inesperadamente: $command -> ${(result as CommandResult.Rejected).reason}"
    }
    return result.events
}
