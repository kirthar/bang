package com.kirthar.bang.fake

import com.kirthar.bang.core.deck.DeckFactory
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.core.view.PublicPlayerInfo
import com.kirthar.bang.viewmodel.GameOverInfo
import com.kirthar.bang.viewmodel.LogEntry

/**
 * Datos de ejemplo para los @Preview de las pantallas: una partida de 5 jugadores a
 * mitad, una reacción a ¡BANG! pendiente y un fin de partida. Solo para desarrollo.
 */

private val deck = DeckFactory.createBaseDeck()

private fun cardOf(kind: CardKind): Card = deck.first { it.kind == kind }
private fun cardsOf(kind: CardKind, count: Int): List<Card> = deck.filter { it.kind == kind }.take(count)

private fun previewPlayers(): List<PublicPlayerInfo> = listOf(
    PublicPlayerInfo(
        seat = 0, name = "Tú", character = CharacterId.CALAMITY_JANET,
        health = 3, maxHealth = 4, handCount = 4,
        inPlay = listOf(cardOf(CardKind.SCHOFIELD)),
        role = Role.OUTLAW, isAlive = true, distance = 0,
    ),
    PublicPlayerInfo(
        seat = 1, name = "IA 1", character = CharacterId.SLAB_THE_KILLER,
        health = 4, maxHealth = 4, handCount = 5,
        inPlay = listOf(cardOf(CardKind.MUSTANG)),
        role = null, isAlive = true, distance = 1,
    ),
    PublicPlayerInfo(
        seat = 2, name = "IA 2", character = CharacterId.PAUL_REGRET,
        health = 5, maxHealth = 5, handCount = 2,
        inPlay = listOf(cardOf(CardKind.BARREL), cardOf(CardKind.WINCHESTER)),
        role = Role.SHERIFF, isAlive = true, distance = 2,
    ),
    PublicPlayerInfo(
        seat = 3, name = "IA 3", character = CharacterId.ROSE_DOOLAN,
        health = 0, maxHealth = 4, handCount = 0,
        inPlay = emptyList(),
        role = Role.OUTLAW, isAlive = false, distance = null,
    ),
    PublicPlayerInfo(
        seat = 4, name = "IA 4", character = CharacterId.JOURDONNAIS,
        health = 2, maxHealth = 4, handCount = 3,
        inPlay = listOf(cardOf(CardKind.DYNAMITE)),
        role = null, isAlive = true, distance = 1,
    ),
)

private fun previewHand(): List<Card> = listOf(
    cardsOf(CardKind.BANG, 2)[0],
    cardsOf(CardKind.BANG, 2)[1],
    cardOf(CardKind.MISSED),
    cardOf(CardKind.BEER),
)

/** Partida de 5 jugadores a mitad: es tu fase de juego. */
fun previewMidGameView(): PlayerGameView {
    val hand = previewHand()
    return PlayerGameView(
        mySeat = 0,
        myRole = Role.OUTLAW,
        myHand = hand,
        players = previewPlayers(),
        deckSize = 42,
        discardTop = cardOf(CardKind.CAT_BALOU),
        currentTurnSeat = 0,
        phase = GamePhase.PLAY,
        turnNumber = 7,
        myPendingRequest = DecisionRequest.PlayOrPass(
            options = listOf(
                GameCommand.PlayCard(hand[0].id, targetSeat = 1),
                GameCommand.PlayCard(hand[0].id, targetSeat = 4),
                GameCommand.PlayCard(hand[3].id),
                GameCommand.EndPlayPhase,
            ),
        ),
    )
}

/** Reacción pendiente: te han disparado un ¡BANG! y puedes responder con ¡Fallaste!. */
fun previewReactView(): PlayerGameView {
    val hand = previewHand()
    val missed = hand[2]
    return previewMidGameView().copy(
        currentTurnSeat = 1,
        myPendingRequest = DecisionRequest.React(
            kind = CardKind.BANG,
            sourceSeat = 1,
            missesNeeded = 1,
            options = listOf(
                GameCommand.Respond(listOf(missed.id)),
                GameCommand.TakeHit,
            ),
        ),
    )
}

/** Log de ejemplo para los @Preview. */
fun previewLogEntries(): List<LogEntry> = listOf(
    "¡Comienza la partida! El Sheriff es IA 2.",
    "Turno 6: le toca a IA 4.",
    "IA 4 roba 2 carta(s).",
    "IA 4 juega ¡BANG! contra Tú.",
    "Tú pierde 1 vida(s) (le quedan 3).",
    "Turno 7: le toca a Tú.",
    "Tú roba 2 carta(s).",
).mapIndexed { index, text -> LogEntry(id = index.toLong(), text = text) }

/** Fin de partida de ejemplo: ganan los Forajidos. */
fun previewGameOverInfo(): GameOverInfo = GameOverInfo(
    result = GameResult(winningRole = Role.OUTLAW, winnerSeats = listOf(0, 3)),
    players = previewPlayers().map { player ->
        player.copy(
            role = when (player.seat) {
                1 -> Role.RENEGADE
                4 -> Role.DEPUTY
                else -> player.role
            },
        )
    },
)
