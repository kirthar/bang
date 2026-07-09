package com.kirthar.bang.ai

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.engine.CommandResult
import com.kirthar.bang.core.engine.GameEngine
import com.kirthar.bang.core.engine.GameEngineFactory
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.model.PlayerState
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.model.rolesFor
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.core.view.PublicPlayerInfo

/**
 * Fixtures y motores de prueba (stubs) para los tests de IA. No dependen de ninguna
 * implementación real del motor: solo cumplen los contratos congelados.
 */
object Fixtures {

    fun card(id: Int, kind: CardKind, suit: Suit = Suit.SPADES, rank: Rank = Rank.ACE): Card =
        Card(id, kind, suit, rank)

    fun publicInfo(
        seat: Int,
        name: String = "P$seat",
        character: CharacterId = CharacterId.WILLY_THE_KID,
        health: Int = 4,
        maxHealth: Int = 4,
        handCount: Int = 0,
        inPlay: List<Card> = emptyList(),
        role: Role? = null,
        isAlive: Boolean = true,
        distance: Int? = 1,
    ): PublicPlayerInfo = PublicPlayerInfo(
        seat, name, character, health, maxHealth, handCount, inPlay, role, isAlive, distance,
    )

    fun view(
        mySeat: Int,
        myRole: Role,
        myHand: List<Card>,
        players: List<PublicPlayerInfo>,
        deckSize: Int = 40,
        discardTop: Card? = null,
        currentTurnSeat: Int = mySeat,
        phase: GamePhase = GamePhase.PLAY,
        turnNumber: Int = 1,
        request: DecisionRequest? = null,
    ): PlayerGameView = PlayerGameView(
        mySeat = mySeat,
        myRole = myRole,
        myHand = myHand,
        players = players,
        deckSize = deckSize,
        discardTop = discardTop,
        currentTurnSeat = currentTurnSeat,
        phase = phase,
        turnNumber = turnNumber,
        myPendingRequest = request,
        result = null,
    )
}

/**
 * Motor de prueba para el test de UCT de MCTS: al recibir el comando raíz, termina la
 * partida de inmediato; gana el jugador activo si jugó la carta [goodCardId], y pierde
 * en caso contrario. Así una opción tiene valor 1 y el resto valor 0.
 */
class WinOnCardEngine(initial: GameState, private val goodCardId: Int) : GameEngine {
    private var st = initial
    override val state: GameState get() = st

    override fun viewFor(seat: Int): PlayerGameView =
        Fixtures.view(seat, st.player(seat).role, emptyList(), emptyList())

    override fun pendingDecision(): Pair<Int, DecisionRequest>? =
        if (st.result != null) null
        else st.currentSeat to DecisionRequest.PlayOrPass(listOf(GameCommand.EndPlayPhase))

    override fun submit(seat: Int, command: GameCommand): CommandResult {
        val win = command is GameCommand.PlayCard && command.cardId == goodCardId
        val winnerSeat = if (win) st.currentSeat else (st.currentSeat + 1) % st.players.size
        st = st.copy(
            phase = GamePhase.FINISHED,
            result = GameResult(st.player(winnerSeat).role, listOf(winnerSeat)),
        )
        return CommandResult.Accepted(emptyList())
    }
}

class WinOnCardFactory(private val goodCardId: Int) : GameEngineFactory {
    override fun create(config: GameConfig): GameEngine = error("no usado")
    override fun restore(state: GameState, seed: Long): GameEngine = WinOnCardEngine(state, goodCardId)
}

/**
 * Motor de prueba para el simulador: juego trivial en el que cada asiento vivo recibe
 * una única opción ([GameCommand.EndPlayPhase]); tras una ronda completa la partida
 * termina y gana el Sheriff (y sus Alguaciles). Ejercita todo el bucle [GameLoop].
 */
class FixedLoopEngine(initial: GameState) : GameEngine {
    private var st = initial
    private var decisions = 0

    override val state: GameState get() = st

    override fun viewFor(seat: Int): PlayerGameView {
        val me = st.player(seat)
        val publics = st.players.map { p ->
            Fixtures.publicInfo(
                seat = p.seat,
                name = p.name,
                character = p.character,
                health = p.health,
                maxHealth = p.maxHealth,
                handCount = p.hand.size,
                inPlay = p.inPlay,
                role = if (p.isSheriff || p.seat == seat || !p.isAlive) p.role else null,
                isAlive = p.isAlive,
                distance = if (p.seat == seat) 0 else 1,
            )
        }
        return Fixtures.view(
            mySeat = seat,
            myRole = me.role,
            myHand = me.hand,
            players = publics,
            deckSize = st.deck.size,
            currentTurnSeat = st.currentSeat,
            phase = st.phase,
            turnNumber = st.turnNumber,
        )
    }

    override fun pendingDecision(): Pair<Int, DecisionRequest>? {
        if (st.result != null) return null
        return st.currentSeat to DecisionRequest.PlayOrPass(listOf(GameCommand.EndPlayPhase))
    }

    override fun submit(seat: Int, command: GameCommand): CommandResult {
        if (command !is GameCommand.EndPlayPhase) return CommandResult.Rejected("solo EndPlayPhase")
        decisions++
        val nextSeat = (st.currentSeat + 1) % st.players.size
        if (decisions >= st.players.size) {
            val winners = st.players.filter { it.role == Role.SHERIFF || it.role == Role.DEPUTY }.map { it.seat }
            st = st.copy(phase = GamePhase.FINISHED, result = GameResult(Role.SHERIFF, winners))
        } else {
            st = st.copy(currentSeat = nextSeat, turnNumber = st.turnNumber + 1)
        }
        return CommandResult.Accepted(listOf(GameEvent.TurnStarted(nextSeat, st.turnNumber)))
    }
}

class FixedLoopFactory : GameEngineFactory {
    override fun create(config: GameConfig): GameEngine {
        val roles = rolesFor(config.players.size)
        val players = config.players.mapIndexed { seat, setup ->
            val character = setup.character ?: CharacterId.entries[seat % CharacterId.entries.size]
            val sheriff = roles[seat] == Role.SHERIFF
            PlayerState(
                seat = seat,
                name = setup.name,
                character = character,
                role = roles[seat],
                health = character.baseHealth + if (sheriff) 1 else 0,
                maxHealth = character.baseHealth + if (sheriff) 1 else 0,
                hand = emptyList(),
                inPlay = emptyList(),
            )
        }
        val state = GameState(
            players = players,
            deck = emptyList(),
            discardPile = emptyList(),
            currentSeat = players.indexOfFirst { it.isSheriff }.coerceAtLeast(0),
            phase = GamePhase.PLAY,
            pending = emptyList(),
        )
        return FixedLoopEngine(state)
    }

    override fun restore(state: GameState, seed: Long): GameEngine = FixedLoopEngine(state)
}
