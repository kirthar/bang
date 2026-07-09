package com.kirthar.bang.fake

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.deck.DeckFactory
import com.kirthar.bang.core.engine.CommandResult
import com.kirthar.bang.core.engine.GameEngine
import com.kirthar.bang.core.engine.GameEngineFactory
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.model.PlayerState
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.rolesFor
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.core.view.PublicPlayerInfo
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Motor de reglas de desarrollo: NO implementa las reglas de BANG!, solo un guion fijo
 * de unos pocos turnos (robar 2 cartas y pasar) para poder desarrollar y probar la UI
 * (navegación, `GameViewModel`, pantallas) mientras el motor real (`core/engine`) no
 * esté integrado. Se sustituye asignando `GameDependencies.engineFactory`.
 *
 * Cada asiento solo puede, en su turno, terminar la fase de juego
 * ([GameCommand.EndPlayPhase]); tras [TOTAL_TURNS] turnos la partida termina con un
 * resultado arbitrario (gana el bando del Sheriff) para poder ejercitar el flujo
 * completo Menú -> Configurar -> Partida -> Fin de partida.
 */
class FakeGameEngine private constructor(initialState: GameState) : GameEngine {

    private var currentState: GameState = initialState

    override val state: GameState get() = currentState

    override fun viewFor(seat: Int): PlayerGameView {
        val aliveSeats = currentState.players.filter { it.isAlive }.map { it.seat }
        val me = currentState.player(seat)
        val players = currentState.players.map { p ->
            val roleVisible = p.role == Role.SHERIFF || p.seat == seat || !p.isAlive || currentState.isFinished
            PublicPlayerInfo(
                seat = p.seat,
                name = p.name,
                character = p.character,
                health = p.health,
                maxHealth = p.maxHealth,
                handCount = p.hand.size,
                inPlay = p.inPlay,
                role = if (roleVisible) p.role else null,
                isAlive = p.isAlive,
                distance = ringDistance(seat, p.seat, aliveSeats),
            )
        }
        val pending = if (!currentState.isFinished && seat == currentState.currentSeat) {
            DecisionRequest.PlayOrPass(options = listOf(GameCommand.EndPlayPhase))
        } else {
            null
        }
        return PlayerGameView(
            mySeat = seat,
            myRole = me.role,
            myHand = me.hand,
            players = players,
            deckSize = currentState.deck.size,
            discardTop = currentState.discardPile.lastOrNull(),
            currentTurnSeat = currentState.currentSeat,
            phase = currentState.phase,
            turnNumber = currentState.turnNumber,
            myPendingRequest = pending,
            result = currentState.result,
        )
    }

    override fun pendingDecision(): Pair<Int, DecisionRequest>? {
        if (currentState.isFinished) return null
        return currentState.currentSeat to DecisionRequest.PlayOrPass(options = listOf(GameCommand.EndPlayPhase))
    }

    override fun submit(seat: Int, command: GameCommand): CommandResult {
        if (currentState.isFinished) return CommandResult.Rejected("La partida ya ha terminado")
        if (seat != currentState.currentSeat) return CommandResult.Rejected("No es el turno de este asiento")
        if (command != GameCommand.EndPlayPhase) {
            return CommandResult.Rejected("El motor de desarrollo solo admite terminar el turno")
        }

        val events = mutableListOf<GameEvent>()
        val playerCount = currentState.players.size

        if (currentState.turnNumber >= TOTAL_TURNS) {
            val winners = currentState.players.filter { it.role == Role.SHERIFF || it.role == Role.DEPUTY }
            val result = GameResult(winningRole = Role.SHERIFF, winnerSeats = winners.map { it.seat })
            events += currentState.players.filter { it.role != Role.SHERIFF }
                .map { GameEvent.RoleRevealed(it.seat, it.role) }
            events += GameEvent.GameEnded(result)
            currentState = currentState.copy(phase = GamePhase.FINISHED, result = result)
        } else {
            val nextSeat = (currentState.currentSeat + 1) % playerCount
            val nextTurn = currentState.turnNumber + 1
            events += GameEvent.TurnStarted(nextSeat, nextTurn)

            val drawCount = min(2, currentState.deck.size)
            val drawn = currentState.deck.take(drawCount)
            events += GameEvent.CardsDrawn(nextSeat, drawn)

            val updatedPlayers = currentState.players.map {
                if (it.seat == nextSeat) it.copy(hand = it.hand + drawn) else it
            }
            currentState = currentState.copy(
                players = updatedPlayers,
                deck = currentState.deck.drop(drawCount),
                currentSeat = nextSeat,
                turnNumber = nextTurn,
                phase = GamePhase.PLAY,
            )
        }
        return CommandResult.Accepted(events)
    }

    companion object Factory : GameEngineFactory {
        /** Nº de turnos totales del guion fijo de desarrollo. */
        private const val TOTAL_TURNS = 6

        override fun create(config: GameConfig): GameEngine = FakeGameEngine(buildInitialState(config))

        override fun restore(state: GameState, seed: Long): GameEngine = FakeGameEngine(state)

        private fun buildInitialState(config: GameConfig): GameState {
            val random = Random(config.seed)
            val roles = rolesFor(config.players.size).shuffled(random)
            val sheriffSeat = roles.indexOf(Role.SHERIFF)

            val availableCharacters = CharacterId.entries.toMutableList()
            config.players.forEach { it.character?.let(availableCharacters::remove) }
            availableCharacters.shuffle(random)

            var deck: List<Card> = DeckFactory.createBaseDeck().shuffled(random)
            val players = config.players.mapIndexed { seat, setup ->
                val role = roles[seat]
                val character = setup.character ?: availableCharacters.removeAt(0)
                val health = character.baseHealth + if (role == Role.SHERIFF) 1 else 0
                val hand = deck.take(health)
                deck = deck.drop(health)
                PlayerState(
                    seat = seat,
                    name = setup.name,
                    character = character,
                    role = role,
                    health = health,
                    maxHealth = health,
                    hand = hand,
                    inPlay = emptyList(),
                    isAlive = true,
                )
            }
            return GameState(
                players = players,
                deck = deck,
                discardPile = emptyList(),
                currentSeat = sheriffSeat,
                phase = GamePhase.PLAY,
                pending = emptyList(),
                turnNumber = 1,
            )
        }

        /** Distancia mínima en asientos entre [from] y [to] contando solo jugadores vivos. */
        private fun ringDistance(from: Int, to: Int, aliveSeatsSorted: List<Int>): Int? {
            if (from == to) return 0
            val idxFrom = aliveSeatsSorted.indexOf(from)
            val idxTo = aliveSeatsSorted.indexOf(to)
            if (idxFrom < 0 || idxTo < 0) return null
            val n = aliveSeatsSorted.size
            val diff = abs(idxFrom - idxTo)
            return min(diff, n - diff)
        }
    }
}
