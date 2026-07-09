package com.kirthar.bang.core.engine

import com.kirthar.bang.core.ability.AbilityContext
import com.kirthar.bang.core.ability.CharacterAbility
import com.kirthar.bang.core.ability.impl.AbilityRegistry
import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.deck.DeckFactory
import com.kirthar.bang.core.event.DrawCheckReason
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.DEFAULT_WEAPON_RANGE
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.model.PendingInteraction
import com.kirthar.bang.core.model.PlayerState
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.model.rolesFor
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PickPurpose
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.core.view.PublicPlayerInfo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Implementación autoritativa y determinista del motor de reglas de BANG! (juego base).
 *
 * La partida se representa con un modelo mutable interno ([MutablePlayer], mazo, pila
 * de descartes y pila de interacciones pendientes). Cada [submit] valida el comando
 * contra las opciones legales enumeradas, aplica su efecto y hace avanzar la partida
 * ([advance]) por todos los pasos automáticos (resolución de Barril, robos de fase,
 * eliminaciones, cambios de turno…) hasta el siguiente punto de decisión o el final.
 *
 * Determinismo: todo el azar (barajado, robos aleatorios de mano, «¡desenfunda!»,
 * rebarajados) pasa por un único [Random] sembrado con [GameConfig.seed].
 */
class BangGameEngine private constructor(
    private val rng: Random,
) : GameEngine {

    // ---- Modelo mutable interno ------------------------------------------------

    private class MutablePlayer(
        val seat: Int,
        val name: String,
        val character: CharacterId,
        val role: Role,
        var health: Int,
        val maxHealth: Int,
        val hand: MutableList<Card>,
        val inPlay: MutableList<Card>,
        var isAlive: Boolean = true,
    ) {
        fun hasInPlay(kind: CardKind): Boolean = inPlay.any { it.kind == kind }
        val weaponRange: Int
            get() = inPlay.firstOrNull { it.kind.isWeapon }?.kind?.weaponRange ?: DEFAULT_WEAPON_RANGE
        val isSheriff: Boolean get() = role == Role.SHERIFF

        fun toState(): PlayerState = PlayerState(
            seat, name, character, role, health, maxHealth,
            hand.toList(), inPlay.toList(), isAlive,
        )
    }

    /** Fase secuencial del arranque de turno (Dinamita → Cárcel → robo). */
    private enum class TurnStage { START, AFTER_DYNAMITE, AFTER_JAIL }

    private lateinit var players: List<MutablePlayer>
    private val deck = ArrayDeque<Card>()
    private val discard = ArrayDeque<Card>()
    private val pending = ArrayDeque<PendingInteraction>()
    private var currentSeat: Int = 0
    private var phase: GamePhase = GamePhase.DRAW
    private var turnStage: TurnStage = TurnStage.START
    private var bangsPlayedThisTurn: Int = 0
    private var turnNumber: Int = 1
    private var result: GameResult? = null

    private val outbox = ArrayList<GameEvent>()

    private val n: Int get() = players.size

    private fun emit(event: GameEvent) {
        outbox.add(event)
    }

    private fun abilityFor(p: MutablePlayer): CharacterAbility = AbilityRegistry.forCharacter(p.character)
    private fun abilityFor(seat: Int): CharacterAbility = abilityFor(players[seat])

    private fun aliveCount(): Int = players.count { it.isAlive }

    // ---- API GameEngine --------------------------------------------------------

    override val state: GameState
        get() = GameState(
            players = players.map { it.toState() },
            deck = deck.toList(),
            discardPile = discard.toList(),
            currentSeat = currentSeat,
            phase = phase,
            pending = pending.toList(),
            bangsPlayedThisTurn = bangsPlayedThisTurn,
            turnNumber = turnNumber,
            result = result,
        )

    override fun viewFor(seat: Int): PlayerGameView {
        val me = players[seat]
        val infos = players.map { p ->
            val role: Role? = when {
                p.seat == seat -> p.role
                p.isSheriff -> Role.SHERIFF
                !p.isAlive -> p.role
                else -> null
            }
            val distance: Int? = if (!me.isAlive || !p.isAlive) null else viewDistance(seat, p.seat)
            PublicPlayerInfo(
                seat = p.seat,
                name = p.name,
                character = p.character,
                health = p.health,
                maxHealth = p.maxHealth,
                handCount = p.hand.size,
                inPlay = p.inPlay.toList(),
                role = role,
                isAlive = p.isAlive,
                distance = distance,
            )
        }
        val decision = pendingDecision()
        val myRequest = decision?.takeIf { it.first == seat }?.second
        return PlayerGameView(
            mySeat = seat,
            myRole = me.role,
            myHand = me.hand.toList(),
            players = infos,
            deckSize = deck.size,
            discardTop = discard.lastOrNull(),
            currentTurnSeat = currentSeat,
            phase = phase,
            turnNumber = turnNumber,
            myPendingRequest = myRequest,
            result = result,
        )
    }

    override fun pendingDecision(): Pair<Int, DecisionRequest>? {
        if (result != null) return null
        return when (val top = pending.lastOrNull()) {
            is BangReactionInteraction -> top.awaitingSeat to buildBangReactionRequest(top)
            is IndiansReactionInteraction -> top.awaitingSeat to buildIndiansRequest(top)
            is DuelInteraction -> top.awaitingSeat to buildDuelRequest(top)
            is GeneralStoreInteraction -> top.awaitingSeat to buildGeneralStoreRequest(top)
            is DeathSaveInteraction -> top.awaitingSeat to buildDeathSaveRequest(top)
            is KitCarlsonInteraction -> top.awaitingSeat to buildKitRequest(top)
            is JesseJonesInteraction -> top.awaitingSeat to buildJesseRequest(top)
            is PedroRamirezInteraction -> top.awaitingSeat to buildPedroRequest(top)
            is LuckyDukeInteraction -> top.awaitingSeat to buildLuckyDukeRequest(top)
            null -> when (phase) {
                GamePhase.PLAY -> currentSeat to buildPlayRequest()
                GamePhase.DISCARD -> {
                    val p = players[currentSeat]
                    if (p.hand.size > p.health) currentSeat to buildDiscardRequest() else null
                }
                else -> null
            }
            else -> null
        }
    }

    override fun submit(seat: Int, command: GameCommand): CommandResult {
        val decision = pendingDecision()
            ?: return CommandResult.Rejected("No hay ninguna decisión pendiente")
        val (expectedSeat, request) = decision
        if (seat != expectedSeat) {
            return CommandResult.Rejected("No es el turno de decisión del asiento $seat (le toca a $expectedSeat)")
        }
        if (command !in request.options) {
            return CommandResult.Rejected("Comando ilegal para la decisión actual")
        }
        handleCommand(command)
        advance()
        val events = outbox.toList()
        outbox.clear()
        return CommandResult.Accepted(events)
    }

    // ---- Motor de avance automático -------------------------------------------

    private fun advance() {
        while (true) {
            if (result != null) return
            if (pending.isEmpty() && phase != GamePhase.FINISHED && !players[currentSeat].isAlive) {
                endTurnToNext()
                continue
            }
            val top = pending.lastOrNull()
            if (top != null) {
                if (autoResolve(top)) continue else return
            }
            when (phase) {
                GamePhase.DRAW -> driveTurnStart()
                GamePhase.PLAY -> return
                GamePhase.DISCARD -> {
                    val p = players[currentSeat]
                    if (p.hand.size > p.health) return
                    endTurnToNext()
                }
                GamePhase.FINISHED -> return
            }
        }
    }

    /** Resuelve automáticamente el tope de la pila si no requiere decisión del jugador. */
    private fun autoResolve(top: PendingInteraction): Boolean = when (top) {
        is BangReactionInteraction -> when {
            top.barrelTriesLeft > 0 -> { resolveOneBarrel(top); true }
            top.missesSoFar >= top.missesNeeded -> { pending.removeLast(); true } // BANG! cancelado
            else -> false
        }
        is DeathSaveInteraction -> {
            val p = players[top.awaitingSeat]
            if (aliveCount() <= 2 || !canAttemptDeathSave(p)) {
                pending.removeLast()
                eliminate(top.awaitingSeat, top.sourceSeat)
                true
            } else {
                false
            }
        }
        else -> false
    }

    private fun driveTurnStart() {
        when (turnStage) {
            TurnStage.START -> stepDynamite()
            TurnStage.AFTER_DYNAMITE -> stepJail()
            TurnStage.AFTER_JAIL -> stepDraw()
        }
    }

    // ---- Arranque de turno -----------------------------------------------------

    private fun stepDynamite() {
        val p = players[currentSeat]
        if (!p.hasInPlay(CardKind.DYNAMITE)) {
            turnStage = TurnStage.AFTER_DYNAMITE
            return
        }
        if (abilityFor(p).drawCheckCardCount() == 2) {
            val flipped = listOf(flipOne(), flipOne())
            pending.addLast(
                LuckyDukeInteraction(currentSeat, DrawCheckReason.DYNAMITE, flipped, LuckyDukeContext.Dynamite),
            )
        } else {
            applyDynamiteResult(flipOne(), emitCheck = true)
        }
    }

    private fun applyDynamiteResult(card: Card, emitCheck: Boolean) {
        val explodes = card.suit == Suit.SPADES && card.rank.ordinal <= Rank.NINE.ordinal
        if (emitCheck) {
            emit(GameEvent.DrawChecked(currentSeat, DrawCheckReason.DYNAMITE, card, explodes))
            discardCard(card)
        }
        turnStage = TurnStage.AFTER_DYNAMITE
        if (explodes) {
            removeInPlayToDiscard(currentSeat, CardKind.DYNAMITE)
            dealDamage(currentSeat, 3, sourceSeat = null)
        } else {
            passDynamiteLeft(currentSeat)
        }
    }

    private fun passDynamiteLeft(from: Int) {
        val dyn = players[from].inPlay.firstOrNull { it.kind == CardKind.DYNAMITE } ?: return
        val next = nextAliveSeat(from)
        if (next == from) return
        players[from].inPlay.remove(dyn)
        players[next].inPlay.add(dyn)
        emit(GameEvent.CardStolen(from, next, dyn, fromHand = false))
    }

    private fun stepJail() {
        val p = players[currentSeat]
        if (!p.hasInPlay(CardKind.JAIL)) {
            turnStage = TurnStage.AFTER_JAIL
            return
        }
        if (abilityFor(p).drawCheckCardCount() == 2) {
            val flipped = listOf(flipOne(), flipOne())
            pending.addLast(LuckyDukeInteraction(currentSeat, DrawCheckReason.JAIL, flipped, LuckyDukeContext.Jail))
        } else {
            applyJailResult(flipOne(), emitCheck = true)
        }
    }

    private fun applyJailResult(card: Card, emitCheck: Boolean) {
        val escaped = card.suit == Suit.HEARTS
        if (emitCheck) {
            emit(GameEvent.DrawChecked(currentSeat, DrawCheckReason.JAIL, card, escaped))
            discardCard(card)
        }
        removeInPlayToDiscard(currentSeat, CardKind.JAIL)
        turnStage = TurnStage.AFTER_JAIL
        if (!escaped) {
            endTurnToNext() // pierde el turno
        }
    }

    private fun stepDraw() {
        val p = players[currentSeat]
        val handled = abilityFor(p).onDrawPhase(EngineAbilityContext(currentSeat))
        if (!handled) {
            drawCardsToHand(currentSeat, 2)
        }
        if (pending.isEmpty()) finishDrawPhase()
    }

    private fun finishDrawPhase() {
        phase = GamePhase.PLAY
    }

    // ---- Fin de turno ----------------------------------------------------------

    private fun endTurnToNext() {
        if (result != null) return
        currentSeat = nextAliveSeat(currentSeat)
        turnNumber++
        bangsPlayedThisTurn = 0
        phase = GamePhase.DRAW
        turnStage = TurnStage.START
        emit(GameEvent.TurnStarted(currentSeat, turnNumber))
    }

    private fun nextAliveSeat(from: Int): Int {
        var s = (from + 1) % n
        var guard = 0
        while (!players[s].isAlive && guard < n) {
            s = (s + 1) % n
            guard++
        }
        return s
    }

    // ---- Despacho de comandos --------------------------------------------------

    private fun handleCommand(command: GameCommand) {
        when (val top = pending.lastOrNull()) {
            is BangReactionInteraction -> handleBangReaction(top, command)
            is IndiansReactionInteraction -> handleIndians(top, command)
            is DuelInteraction -> handleDuel(top, command)
            is GeneralStoreInteraction -> handleGeneralStore(top, command)
            is DeathSaveInteraction -> handleDeathSave(top, command)
            is KitCarlsonInteraction -> handleKit(top, command)
            is JesseJonesInteraction -> handleJesse(top, command)
            is PedroRamirezInteraction -> handlePedro(top, command)
            is LuckyDukeInteraction -> handleLuckyDuke(top, command)
            null -> when (phase) {
                GamePhase.PLAY -> handlePlay(command)
                GamePhase.DISCARD -> handleDiscard(command)
                else -> Unit
            }
            else -> Unit
        }
    }

    private fun handlePlay(command: GameCommand) {
        when (command) {
            is GameCommand.EndPlayPhase -> phase = GamePhase.DISCARD
            is GameCommand.PlayCard -> executePlay(command)
            is GameCommand.UseCharacterAbility -> sidDiscardHeal(currentSeat, command.cardIds)
            else -> Unit
        }
    }

    private fun handleDiscard(command: GameCommand) {
        if (command is GameCommand.Discard) {
            command.cardIds.forEach { id ->
                val card = players[currentSeat].hand.firstOrNull { it.id == id } ?: return@forEach
                players[currentSeat].hand.remove(card)
                discardCard(card)
                emit(GameEvent.CardsDiscarded(currentSeat, listOf(card)))
            }
            afterHandChange(currentSeat)
        }
    }

    // ---- Ejecución de cartas jugadas ------------------------------------------

    private fun executePlay(pc: GameCommand.PlayCard) {
        val p = players[currentSeat]
        val card = p.hand.first { it.id == pc.cardId }
        when (card.kind) {
            CardKind.BANG, CardKind.MISSED -> playBang(card, pc.targetSeat!!)
            CardKind.BEER -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                heal(currentSeat, 1)
                afterHandChange(currentSeat)
            }
            CardKind.PANIC -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card, pc.targetSeat))
                afterHandChange(currentSeat)
                panicSteal(pc.targetSeat!!, pc.targetCardId)
            }
            CardKind.CAT_BALOU -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card, pc.targetSeat))
                afterHandChange(currentSeat)
                catBalou(pc.targetSeat!!, pc.targetCardId)
            }
            CardKind.STAGECOACH -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
                drawCardsToHand(currentSeat, 2)
            }
            CardKind.WELLS_FARGO -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
                drawCardsToHand(currentSeat, 3)
            }
            CardKind.GATLING -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
                startGatling()
            }
            CardKind.INDIANS -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
                startIndians()
            }
            CardKind.DUEL -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card, pc.targetSeat))
                afterHandChange(currentSeat)
                pending.addLast(DuelInteraction(pc.targetSeat!!, currentSeat))
            }
            CardKind.GENERAL_STORE -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
                startGeneralStore()
            }
            CardKind.SALOON -> {
                removeFromHandToDiscard(currentSeat, card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
                aliveSeatsClockwiseFrom(currentSeat).forEach { heal(it, 1) }
            }
            CardKind.JAIL -> {
                p.hand.remove(card)
                players[pc.targetSeat!!].inPlay.add(card)
                emit(GameEvent.CardPlayed(currentSeat, card, pc.targetSeat))
                afterHandChange(currentSeat)
            }
            CardKind.DYNAMITE, CardKind.BARREL, CardKind.MUSTANG, CardKind.SCOPE -> {
                p.hand.remove(card)
                p.inPlay.add(card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
            }
            CardKind.VOLCANIC, CardKind.SCHOFIELD, CardKind.REMINGTON,
            CardKind.REV_CARABINE, CardKind.WINCHESTER -> {
                removeExistingWeapon(currentSeat)
                p.hand.remove(card)
                p.inPlay.add(card)
                emit(GameEvent.CardPlayed(currentSeat, card))
                afterHandChange(currentSeat)
            }
        }
    }

    private fun playBang(card: Card, targetSeat: Int) {
        removeFromHandToDiscard(currentSeat, card)
        bangsPlayedThisTurn++
        emit(GameEvent.CardPlayed(currentSeat, card, targetSeat))
        afterHandChange(currentSeat)
        initiateBangAgainst(targetSeat, currentSeat, fromGatling = false)
    }

    private fun initiateBangAgainst(target: Int, source: Int, fromGatling: Boolean) {
        val tp = players[target]
        if (!tp.isAlive) return
        val barrels = (if (tp.hasInPlay(CardKind.BARREL)) 1 else 0) +
            (if (abilityFor(tp).hasInnateBarrel()) 1 else 0)
        val missesNeeded = if (fromGatling) 1 else abilityFor(source).missesRequiredForHisBang()
        pending.addLast(
            BangReactionInteraction(
                awaitingSeat = target,
                sourceSeat = source,
                missesNeeded = missesNeeded,
                missesSoFar = 0,
                barrelTriesLeft = barrels,
                fromGatling = fromGatling,
            ),
        )
    }

    private fun startGatling() {
        othersInTurnOrder(currentSeat).reversed().forEach { t ->
            initiateBangAgainst(t, currentSeat, fromGatling = true)
        }
    }

    private fun startIndians() {
        othersInTurnOrder(currentSeat).reversed().forEach { t ->
            pending.addLast(IndiansReactionInteraction(t, currentSeat))
        }
    }

    private fun startGeneralStore() {
        val count = aliveCount()
        val cards = ArrayList<Card>()
        repeat(count) { drawOne()?.let { cards.add(it) } }
        if (cards.isEmpty()) return
        val order = aliveSeatsClockwiseFrom(currentSeat)
        pending.addLast(GeneralStoreInteraction(order.first(), order, cards))
    }

    // ---- Resolución de reacciones ---------------------------------------------

    private fun resolveOneBarrel(reaction: BangReactionInteraction) {
        val tp = players[reaction.awaitingSeat]
        if (abilityFor(tp).drawCheckCardCount() == 2) {
            val flipped = listOf(flipOne(), flipOne())
            replaceTopBangReaction { it.copy(barrelTriesLeft = it.barrelTriesLeft - 1) }
            pending.addLast(
                LuckyDukeInteraction(reaction.awaitingSeat, DrawCheckReason.BARREL, flipped, LuckyDukeContext.Barrel),
            )
        } else {
            val card = flipOne()
            val hit = card.suit == Suit.HEARTS
            emit(GameEvent.DrawChecked(reaction.awaitingSeat, DrawCheckReason.BARREL, card, hit))
            discardCard(card)
            replaceTopBangReaction {
                it.copy(
                    barrelTriesLeft = it.barrelTriesLeft - 1,
                    missesSoFar = it.missesSoFar + (if (hit) 1 else 0),
                )
            }
        }
    }

    private fun replaceTopBangReaction(transform: (BangReactionInteraction) -> BangReactionInteraction) {
        val idx = pending.indexOfLast { it is BangReactionInteraction }
        if (idx >= 0) {
            pending[idx] = transform(pending[idx] as BangReactionInteraction)
        }
    }

    private fun handleBangReaction(top: BangReactionInteraction, command: GameCommand) {
        when (command) {
            is GameCommand.TakeHit -> {
                pending.removeLast()
                dealDamage(top.awaitingSeat, 1, top.sourceSeat)
            }
            is GameCommand.Respond -> {
                command.cardIds.forEach { id ->
                    val c = players[top.awaitingSeat].hand.first { it.id == id }
                    removeFromHandToDiscard(top.awaitingSeat, c)
                    emit(GameEvent.CardPlayed(top.awaitingSeat, c))
                }
                pending.removeLast() // BANG! cancelado
                afterHandChange(top.awaitingSeat)
            }
            else -> Unit
        }
    }

    private fun handleIndians(top: IndiansReactionInteraction, command: GameCommand) {
        when (command) {
            is GameCommand.TakeHit -> {
                pending.removeLast()
                dealDamage(top.awaitingSeat, 1, top.sourceSeat)
            }
            is GameCommand.Respond -> {
                val c = players[top.awaitingSeat].hand.first { it.id == command.cardIds.first() }
                removeFromHandToDiscard(top.awaitingSeat, c)
                emit(GameEvent.CardPlayed(top.awaitingSeat, c))
                pending.removeLast()
                afterHandChange(top.awaitingSeat)
            }
            else -> Unit
        }
    }

    private fun handleDuel(top: DuelInteraction, command: GameCommand) {
        when (command) {
            is GameCommand.TakeHit -> {
                pending.removeLast()
                dealDamage(top.awaitingSeat, 1, top.opponentSeat)
            }
            is GameCommand.Respond -> {
                val c = players[top.awaitingSeat].hand.first { it.id == command.cardIds.first() }
                removeFromHandToDiscard(top.awaitingSeat, c)
                emit(GameEvent.CardPlayed(top.awaitingSeat, c))
                pending.removeLast()
                pending.addLast(DuelInteraction(top.opponentSeat, top.awaitingSeat))
                afterHandChange(top.awaitingSeat)
            }
            else -> Unit
        }
    }

    private fun handleGeneralStore(top: GeneralStoreInteraction, command: GameCommand) {
        if (command !is GameCommand.ChooseCard) return
        val card = top.cards.first { it.id == command.cardId }
        players[top.awaitingSeat].hand.add(card)
        emit(GameEvent.CardsDrawn(top.awaitingSeat, listOf(card), 1, revealed = true))
        pending.removeLast()
        val remainingSeats = top.remainingSeats.drop(1)
        val remainingCards = top.cards - card
        if (remainingSeats.isNotEmpty() && remainingCards.isNotEmpty()) {
            pending.addLast(GeneralStoreInteraction(remainingSeats.first(), remainingSeats, remainingCards))
        }
    }

    private fun handleDeathSave(top: DeathSaveInteraction, command: GameCommand) {
        when (command) {
            is GameCommand.TakeHit -> {
                pending.removeLast()
                eliminate(top.awaitingSeat, top.sourceSeat)
            }
            is GameCommand.Respond -> {
                val c = players[top.awaitingSeat].hand.first { it.id == command.cardIds.first() }
                removeFromHandToDiscard(top.awaitingSeat, c)
                emit(GameEvent.CardPlayed(top.awaitingSeat, c))
                heal(top.awaitingSeat, 1)
                afterHandChange(top.awaitingSeat)
                if (players[top.awaitingSeat].health >= 1) pending.removeLast()
            }
            is GameCommand.UseCharacterAbility -> {
                sidDiscardHeal(top.awaitingSeat, command.cardIds)
                if (players[top.awaitingSeat].health >= 1) pending.removeLast()
            }
            else -> Unit
        }
    }

    private fun handleKit(top: KitCarlsonInteraction, command: GameCommand) {
        if (command !is GameCommand.ChooseCard) return
        val card = top.pool.first { it.id == command.cardId }
        deck.remove(card)
        players[top.awaitingSeat].hand.add(card)
        emit(GameEvent.CardsDrawn(top.awaitingSeat, listOf(card), 1, revealed = false))
        pending.removeLast()
        val kept = top.keptSoFar + 1
        if (kept < 2) {
            pending.addLast(KitCarlsonInteraction(top.awaitingSeat, top.pool - card, kept))
        } else {
            finishDrawPhase() // la carta sobrante permanece en la cima del mazo
        }
    }

    private fun handleJesse(top: JesseJonesInteraction, command: GameCommand) {
        if (command !is GameCommand.ChooseDrawSource) return
        when (val src = command.source) {
            is DrawSource.Deck -> drawCardsToHand(top.awaitingSeat, 1)
            is DrawSource.PlayerHand -> stealRandomHand(src.seat, top.awaitingSeat)
            is DrawSource.DiscardPile -> Unit
        }
        drawCardsToHand(top.awaitingSeat, 1) // 2ª carta siempre del mazo
        pending.removeLast()
        finishDrawPhase()
    }

    private fun handlePedro(top: PedroRamirezInteraction, command: GameCommand) {
        if (command !is GameCommand.ChooseDrawSource) return
        when (command.source) {
            is DrawSource.DiscardPile -> {
                val c = discard.removeLast()
                players[top.awaitingSeat].hand.add(c)
                emit(GameEvent.CardsDrawn(top.awaitingSeat, listOf(c), 1, revealed = true))
            }
            else -> drawCardsToHand(top.awaitingSeat, 1)
        }
        drawCardsToHand(top.awaitingSeat, 1) // 2ª carta siempre del mazo
        pending.removeLast()
        finishDrawPhase()
    }

    private fun handleLuckyDuke(top: LuckyDukeInteraction, command: GameCommand) {
        if (command !is GameCommand.ChooseCard) return
        val chosen = top.flipped.first { it.id == command.cardId }
        top.flipped.forEach { discardCard(it) }
        pending.removeLast()
        val success = when (top.reason) {
            DrawCheckReason.DYNAMITE -> chosen.suit == Suit.SPADES && chosen.rank.ordinal <= Rank.NINE.ordinal
            DrawCheckReason.JAIL -> chosen.suit == Suit.HEARTS
            DrawCheckReason.BARREL -> chosen.suit == Suit.HEARTS
        }
        emit(GameEvent.DrawChecked(top.awaitingSeat, top.reason, chosen, success))
        when (top.context) {
            LuckyDukeContext.Dynamite -> applyDynamiteResult(chosen, emitCheck = false)
            LuckyDukeContext.Jail -> applyJailResult(chosen, emitCheck = false)
            LuckyDukeContext.Barrel ->
                if (chosen.suit == Suit.HEARTS) replaceTopBangReaction { it.copy(missesSoFar = it.missesSoFar + 1) }
        }
    }

    private fun sidDiscardHeal(seat: Int, cardIds: List<Int>) {
        cardIds.forEach { id ->
            val c = players[seat].hand.firstOrNull { it.id == id } ?: return@forEach
            players[seat].hand.remove(c)
            discardCard(c)
            emit(GameEvent.CardsDiscarded(seat, listOf(c)))
        }
        heal(seat, 1)
        afterHandChange(seat)
    }

    // ---- Efectos de robo/descarte dirigido ------------------------------------

    private fun panicSteal(target: Int, targetCardId: Int?) {
        if (targetCardId == null) {
            stealRandomHand(target, currentSeat)
        } else {
            val c = players[target].inPlay.first { it.id == targetCardId }
            players[target].inPlay.remove(c)
            players[currentSeat].hand.add(c)
            emit(GameEvent.CardStolen(target, currentSeat, c, fromHand = false))
        }
    }

    private fun catBalou(target: Int, targetCardId: Int?) {
        val c = if (targetCardId == null) {
            removeRandomHand(target)
        } else {
            players[target].inPlay.first { it.id == targetCardId }.also { players[target].inPlay.remove(it) }
        } ?: return
        discardCard(c)
        emit(GameEvent.CardsDiscarded(target, listOf(c)))
        afterHandChange(target)
    }

    // ---- Daño, curación, muerte -----------------------------------------------

    private fun dealDamage(seat: Int, amount: Int, sourceSeat: Int?) {
        val p = players[seat]
        if (!p.isAlive) return
        p.health -= amount
        emit(GameEvent.HealthChanged(seat, -amount, p.health, sourceSeat))
        abilityFor(p).onDamaged(EngineAbilityContext(seat), amount, sourceSeat)
        if (p.health <= 0) {
            pending.addLast(DeathSaveInteraction(seat, sourceSeat))
        }
    }

    private fun heal(seat: Int, amount: Int): Int {
        val p = players[seat]
        val before = p.health
        p.health = min(p.maxHealth, p.health + amount)
        val delta = p.health - before
        if (delta != 0) emit(GameEvent.HealthChanged(seat, delta, p.health, null))
        return delta
    }

    private fun canAttemptDeathSave(p: MutablePlayer): Boolean =
        p.hand.any { it.kind == CardKind.BEER } ||
            (abilityFor(p).canDiscardTwoForHealth() && p.hand.size >= 2)

    private fun eliminate(seat: Int, killerSeat: Int?) {
        val p = players[seat]
        if (!p.isAlive) return
        p.isAlive = false
        emit(GameEvent.PlayerEliminated(seat, p.role, killerSeat))
        emit(GameEvent.RoleRevealed(seat, p.role))

        val sam = players.firstOrNull { it.isAlive && it.character == CharacterId.VULTURE_SAM && it.seat != seat }
        if (sam != null) {
            abilityFor(sam).onPlayerEliminated(EngineAbilityContext(sam.seat), seat)
        } else {
            discardAllCards(seat)
        }

        if (killerSeat != null && killerSeat != seat && players[killerSeat].isAlive) {
            if (p.role == Role.OUTLAW) drawCardsToHand(killerSeat, 3) // recompensa
            if (players[killerSeat].isSheriff && p.role == Role.DEPUTY) discardAllCards(killerSeat) // penalización
        }
        checkVictory()
    }

    private fun discardAllCards(seat: Int) {
        val p = players[seat]
        val all = p.hand.toList() + p.inPlay.toList()
        p.hand.clear()
        p.inPlay.clear()
        if (all.isNotEmpty()) {
            all.forEach { discard.addLast(it) }
            emit(GameEvent.CardsDiscarded(seat, all))
        }
    }

    private fun checkVictory() {
        if (result != null) return
        val r = evaluateResult() ?: return
        result = r
        phase = GamePhase.FINISHED
        pending.clear()
        emit(GameEvent.GameEnded(r))
    }

    private fun evaluateResult(): GameResult? {
        val sheriffAlive = players.any { it.isAlive && it.role == Role.SHERIFF }
        if (!sheriffAlive) {
            val alive = players.filter { it.isAlive }
            return if (alive.size == 1 && alive[0].role == Role.RENEGADE) {
                GameResult(Role.RENEGADE, alive.map { it.seat })
            } else {
                GameResult(Role.OUTLAW, players.filter { it.role == Role.OUTLAW }.map { it.seat })
            }
        }
        val badAlive = players.any { it.isAlive && (it.role == Role.OUTLAW || it.role == Role.RENEGADE) }
        if (!badAlive) {
            return GameResult(
                Role.SHERIFF,
                players.filter { it.role == Role.SHERIFF || it.role == Role.DEPUTY }.map { it.seat },
            )
        }
        return null
    }

    // ---- Mazo y manos ----------------------------------------------------------

    private fun reshuffleDeck() {
        if (discard.size <= 1) return
        val top = discard.removeLast()
        val rest = ArrayList(discard)
        discard.clear()
        discard.addLast(top)
        rest.shuffle(rng)
        deck.addAll(rest)
        emit(GameEvent.DeckReshuffled)
    }

    private fun drawOne(): Card? {
        if (deck.isEmpty()) reshuffleDeck()
        return if (deck.isEmpty()) null else deck.removeFirst()
    }

    private fun flipOne(): Card = drawOne() ?: error("Mazo y descartes agotados durante un «¡desenfunda!»")

    private fun discardCard(card: Card) {
        discard.addLast(card)
    }

    private fun drawCardsToHand(seat: Int, count: Int, revealed: Boolean = false): List<Card> {
        val drawn = ArrayList<Card>()
        repeat(count) { drawOne()?.let { players[seat].hand.add(it); drawn.add(it) } }
        if (drawn.isNotEmpty()) emit(GameEvent.CardsDrawn(seat, drawn, drawn.size, revealed))
        return drawn
    }

    private fun removeFromHandToDiscard(seat: Int, card: Card) {
        players[seat].hand.remove(card)
        discardCard(card)
    }

    private fun removeInPlayToDiscard(seat: Int, kind: CardKind) {
        val c = players[seat].inPlay.firstOrNull { it.kind == kind } ?: return
        players[seat].inPlay.remove(c)
        discardCard(c)
    }

    private fun removeExistingWeapon(seat: Int) {
        val w = players[seat].inPlay.firstOrNull { it.kind.isWeapon } ?: return
        players[seat].inPlay.remove(w)
        discardCard(w)
        emit(GameEvent.CardsDiscarded(seat, listOf(w)))
    }

    private fun removeRandomHand(seat: Int): Card? {
        val hand = players[seat].hand
        if (hand.isEmpty()) return null
        return hand.removeAt(rng.nextInt(hand.size))
    }

    private fun stealRandomHand(fromSeat: Int, toSeat: Int): Card? {
        val c = removeRandomHand(fromSeat) ?: return null
        players[toSeat].hand.add(c)
        emit(GameEvent.CardStolen(fromSeat, toSeat, c, fromHand = true))
        afterHandChange(fromSeat)
        return c
    }

    private fun afterHandChange(seat: Int) {
        val p = players[seat]
        if (p.isAlive && p.character == CharacterId.SUZY_LAFAYETTE && p.hand.isEmpty() &&
            (deck.isNotEmpty() || discard.size > 1)
        ) {
            abilityFor(p).onHandEmptied(EngineAbilityContext(seat))
        }
    }

    // ---- Distancias ------------------------------------------------------------

    private fun aliveSeatsInOrder(): List<Int> = players.filter { it.isAlive }.map { it.seat }

    private fun seatDistance(a: Int, b: Int): Int {
        val alive = aliveSeatsInOrder()
        val ia = alive.indexOf(a)
        val ib = alive.indexOf(b)
        if (ia < 0 || ib < 0) return Int.MAX_VALUE
        val d = abs(ia - ib)
        return min(d, alive.size - d)
    }

    /** Distancia efectiva desde [from] hasta [to] con Mustang/Mira/habilidades (mínimo 1). */
    private fun viewDistance(from: Int, to: Int): Int {
        if (from == to) return 0
        val viewer = players[from]
        val target = players[to]
        var d = seatDistance(from, to)
        d += (if (target.hasInPlay(CardKind.MUSTANG)) 1 else 0) + abilityFor(target).distanceModifierAsTarget()
        d += (if (viewer.hasInPlay(CardKind.SCOPE)) -1 else 0) + abilityFor(viewer).distanceModifierAsViewer()
        return max(1, d)
    }

    private fun othersInTurnOrder(source: Int): List<Int> =
        aliveSeatsClockwiseFrom(source).filter { it != source }

    private fun aliveSeatsClockwiseFrom(start: Int): List<Int> {
        val res = ArrayList<Int>()
        var s = start
        var guard = 0
        while (guard < n) {
            if (players[s].isAlive) res.add(s)
            s = (s + 1) % n
            guard++
        }
        return res
    }

    // ---- Construcción de peticiones de decisión (opciones legales) -------------

    private fun buildPlayRequest(): DecisionRequest {
        val p = players[currentSeat]
        val ability = abilityFor(p)
        val options = ArrayList<GameCommand>()

        val allowedBangs = if (p.hasInPlay(CardKind.VOLCANIC)) Int.MAX_VALUE else ability.bangsPerTurn()
        val bangAllowed = bangsPlayedThisTurn < allowedBangs

        for (card in p.hand) {
            when (card.kind) {
                CardKind.BANG -> if (bangAllowed) {
                    bangTargets(currentSeat).forEach { options.add(GameCommand.PlayCard(card.id, it)) }
                }
                CardKind.MISSED -> if (ability.canSwapBangAndMissed() && bangAllowed) {
                    bangTargets(currentSeat).forEach { options.add(GameCommand.PlayCard(card.id, it)) }
                }
                CardKind.BEER -> if (p.health < p.maxHealth && aliveCount() > 2) {
                    options.add(GameCommand.PlayCard(card.id))
                }
                CardKind.PANIC -> playersAtDistanceOne(currentSeat).forEach { t ->
                    if (players[t].hand.isNotEmpty()) options.add(GameCommand.PlayCard(card.id, t))
                    players[t].inPlay.forEach { options.add(GameCommand.PlayCard(card.id, t, it.id)) }
                }
                CardKind.CAT_BALOU -> otherAlive(currentSeat).forEach { t ->
                    if (players[t].hand.isNotEmpty()) options.add(GameCommand.PlayCard(card.id, t))
                    players[t].inPlay.forEach { options.add(GameCommand.PlayCard(card.id, t, it.id)) }
                }
                CardKind.STAGECOACH, CardKind.WELLS_FARGO, CardKind.SALOON, CardKind.GENERAL_STORE ->
                    options.add(GameCommand.PlayCard(card.id))
                CardKind.GATLING, CardKind.INDIANS ->
                    if (othersInTurnOrder(currentSeat).isNotEmpty()) options.add(GameCommand.PlayCard(card.id))
                CardKind.DUEL -> otherAlive(currentSeat).forEach { options.add(GameCommand.PlayCard(card.id, it)) }
                CardKind.JAIL -> jailableTargets(currentSeat).forEach { options.add(GameCommand.PlayCard(card.id, it)) }
                CardKind.DYNAMITE -> if (!p.hasInPlay(CardKind.DYNAMITE)) options.add(GameCommand.PlayCard(card.id))
                CardKind.BARREL -> if (!p.hasInPlay(CardKind.BARREL)) options.add(GameCommand.PlayCard(card.id))
                CardKind.MUSTANG -> if (!p.hasInPlay(CardKind.MUSTANG)) options.add(GameCommand.PlayCard(card.id))
                CardKind.SCOPE -> if (!p.hasInPlay(CardKind.SCOPE)) options.add(GameCommand.PlayCard(card.id))
                CardKind.VOLCANIC, CardKind.SCHOFIELD, CardKind.REMINGTON,
                CardKind.REV_CARABINE, CardKind.WINCHESTER -> options.add(GameCommand.PlayCard(card.id))
            }
        }

        if (ability.canDiscardTwoForHealth() && p.health < p.maxHealth && p.hand.size >= 2) {
            combinations(p.hand, 2).forEach { options.add(GameCommand.UseCharacterAbility(it.map { c -> c.id })) }
        }

        options.add(GameCommand.EndPlayPhase)
        return DecisionRequest.PlayOrPass(options.distinct())
    }

    private fun buildBangReactionRequest(top: BangReactionInteraction): DecisionRequest {
        val remaining = top.missesNeeded - top.missesSoFar
        val p = players[top.awaitingSeat]
        val ability = abilityFor(p)
        val missCards = p.hand.filter { it.kind == CardKind.MISSED } +
            (if (ability.canSwapBangAndMissed()) p.hand.filter { it.kind == CardKind.BANG } else emptyList())
        val options = ArrayList<GameCommand>()
        options.add(GameCommand.TakeHit)
        if (missCards.size >= remaining && remaining > 0) {
            combinations(missCards, remaining).forEach { combo ->
                options.add(GameCommand.Respond(combo.map { it.id }))
            }
        }
        val kind = if (top.fromGatling) CardKind.GATLING else CardKind.BANG
        return DecisionRequest.React(kind, top.sourceSeat, remaining, options.distinct())
    }

    private fun buildIndiansRequest(top: IndiansReactionInteraction): DecisionRequest {
        val p = players[top.awaitingSeat]
        val ability = abilityFor(p)
        val bangCards = p.hand.filter { it.kind == CardKind.BANG } +
            (if (ability.canSwapBangAndMissed()) p.hand.filter { it.kind == CardKind.MISSED } else emptyList())
        val options = ArrayList<GameCommand>()
        options.add(GameCommand.TakeHit)
        bangCards.forEach { options.add(GameCommand.Respond(listOf(it.id))) }
        return DecisionRequest.React(CardKind.INDIANS, top.sourceSeat, 1, options.distinct())
    }

    private fun buildDuelRequest(top: DuelInteraction): DecisionRequest {
        val p = players[top.awaitingSeat]
        val ability = abilityFor(p)
        val bangCards = p.hand.filter { it.kind == CardKind.BANG } +
            (if (ability.canSwapBangAndMissed()) p.hand.filter { it.kind == CardKind.MISSED } else emptyList())
        val options = ArrayList<GameCommand>()
        options.add(GameCommand.TakeHit)
        bangCards.forEach { options.add(GameCommand.Respond(listOf(it.id))) }
        return DecisionRequest.React(CardKind.DUEL, top.opponentSeat, 1, options.distinct())
    }

    private fun buildGeneralStoreRequest(top: GeneralStoreInteraction): DecisionRequest =
        DecisionRequest.PickCard(
            PickPurpose.GENERAL_STORE,
            top.cards,
            top.cards.map { GameCommand.ChooseCard(it.id) },
        )

    private fun buildDeathSaveRequest(top: DeathSaveInteraction): DecisionRequest {
        val p = players[top.awaitingSeat]
        val ability = abilityFor(p)
        val options = ArrayList<GameCommand>()
        p.hand.filter { it.kind == CardKind.BEER }.forEach { options.add(GameCommand.Respond(listOf(it.id))) }
        if (ability.canDiscardTwoForHealth() && p.hand.size >= 2) {
            combinations(p.hand, 2).forEach { options.add(GameCommand.UseCharacterAbility(it.map { c -> c.id })) }
        }
        options.add(GameCommand.TakeHit)
        return DecisionRequest.React(
            CardKind.BEER,
            top.sourceSeat ?: top.awaitingSeat,
            max(1, 1 - p.health),
            options.distinct(),
        )
    }

    private fun buildKitRequest(top: KitCarlsonInteraction): DecisionRequest =
        DecisionRequest.PickCard(
            PickPurpose.KIT_CARLSON,
            top.pool,
            top.pool.map { GameCommand.ChooseCard(it.id) },
        )

    private fun buildJesseRequest(top: JesseJonesInteraction): DecisionRequest {
        val sources = ArrayList<DrawSource>()
        sources.add(DrawSource.Deck)
        otherAlive(top.awaitingSeat).forEach { s ->
            if (players[s].hand.isNotEmpty()) sources.add(DrawSource.PlayerHand(s))
        }
        return DecisionRequest.ChooseDraw(sources, sources.map { GameCommand.ChooseDrawSource(it) })
    }

    private fun buildPedroRequest(top: PedroRamirezInteraction): DecisionRequest {
        val sources = ArrayList<DrawSource>()
        if (discard.isNotEmpty()) sources.add(DrawSource.DiscardPile)
        sources.add(DrawSource.Deck)
        return DecisionRequest.ChooseDraw(sources, sources.map { GameCommand.ChooseDrawSource(it) })
    }

    private fun buildLuckyDukeRequest(top: LuckyDukeInteraction): DecisionRequest =
        DecisionRequest.PickCard(
            PickPurpose.LUCKY_DUKE,
            top.flipped,
            top.flipped.map { GameCommand.ChooseCard(it.id) },
        )

    private fun buildDiscardRequest(): DecisionRequest {
        val p = players[currentSeat]
        val excess = p.hand.size - p.health
        return DecisionRequest.DiscardToHandLimit(excess, p.hand.map { GameCommand.Discard(listOf(it.id)) })
    }

    // ---- Objetivos legales -----------------------------------------------------

    private fun otherAlive(from: Int): List<Int> =
        players.filter { it.isAlive && it.seat != from }.map { it.seat }

    private fun bangTargets(from: Int): List<Int> {
        val range = players[from].weaponRange
        return otherAlive(from).filter { viewDistance(from, it) <= range }
    }

    private fun playersAtDistanceOne(from: Int): List<Int> =
        otherAlive(from).filter { viewDistance(from, it) <= 1 }

    private fun jailableTargets(from: Int): List<Int> =
        otherAlive(from).filter { !players[it].isSheriff && !players[it].hasInPlay(CardKind.JAIL) }

    // ---- Contexto de habilidades ----------------------------------------------

    private inner class EngineAbilityContext(override val selfSeat: Int) : AbilityContext {
        override fun drawFromDeck(count: Int, revealed: Boolean): List<Card> =
            drawCardsToHand(selfSeat, count, revealed)

        override fun stealRandomFromHand(fromSeat: Int): Card? = stealRandomHand(fromSeat, selfSeat)

        override fun takeAllCardsFrom(seat: Int) {
            val victim = players[seat]
            val handCards = victim.hand.toList()
            val playCards = victim.inPlay.toList()
            victim.hand.clear()
            victim.inPlay.clear()
            handCards.forEach {
                players[selfSeat].hand.add(it)
                emit(GameEvent.CardStolen(seat, selfSeat, it, fromHand = true))
            }
            playCards.forEach {
                players[selfSeat].hand.add(it)
                emit(GameEvent.CardStolen(seat, selfSeat, it, fromHand = false))
            }
        }

        override fun deckPeek(count: Int): List<Card> {
            while (deck.size < count && discard.size > 1) reshuffleDeck()
            return deck.take(count)
        }

        override fun requestDecision(interaction: PendingInteraction) {
            pending.addLast(interaction)
        }
    }

    // ---- Utilidades ------------------------------------------------------------

    private fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (items.size < k) return emptyList()
        val result = ArrayList<List<T>>()
        fun rec(start: Int, current: ArrayList<T>) {
            if (current.size == k) {
                result.add(current.toList())
                return
            }
            for (i in start until items.size) {
                current.add(items[i])
                rec(i + 1, current)
                current.removeAt(current.size - 1)
            }
        }
        rec(0, ArrayList())
        return result
    }

    // ---- Construcción / restauración -------------------------------------------

    private fun bootstrapNewGame(config: GameConfig) {
        val playerCount = config.players.size
        val fullDeck = DeckFactory.createBaseDeck().shuffled(rng)
        deck.addAll(fullDeck)

        val roles = rolesFor(playerCount).shuffled(rng)
        val fixed = config.players.mapNotNull { it.character }.toSet()
        val pool = (CharacterId.entries - fixed).shuffled(rng).toMutableList()

        val built = ArrayList<MutablePlayer>(playerCount)
        for (seat in 0 until playerCount) {
            val setup = config.players[seat]
            val character = setup.character ?: pool.removeAt(0)
            val role = roles[seat]
            val maxHealth = character.baseHealth + (if (role == Role.SHERIFF) 1 else 0)
            built.add(
                MutablePlayer(
                    seat = seat,
                    name = setup.name,
                    character = character,
                    role = role,
                    health = maxHealth,
                    maxHealth = maxHealth,
                    hand = ArrayList(),
                    inPlay = ArrayList(),
                ),
            )
        }
        players = built

        // Reparto de manos: tantas cartas como vidas iniciales.
        for (p in players) {
            repeat(p.health) { p.hand.add(deck.removeFirst()) }
        }

        val sheriffSeat = players.first { it.role == Role.SHERIFF }.seat
        currentSeat = sheriffSeat
        turnNumber = 1
        phase = GamePhase.DRAW
        turnStage = TurnStage.START

        emit(
            GameEvent.GameStarted(
                playerNames = players.map { it.name },
                characters = players.map { it.character },
                sheriffSeat = sheriffSeat,
            ),
        )
        emit(GameEvent.TurnStarted(currentSeat, turnNumber))
        advance()
    }

    private fun restoreFrom(gameState: GameState) {
        players = gameState.players.map {
            MutablePlayer(
                it.seat, it.name, it.character, it.role, it.health, it.maxHealth,
                it.hand.toMutableList(), it.inPlay.toMutableList(), it.isAlive,
            )
        }
        deck.addAll(gameState.deck)
        discard.addAll(gameState.discardPile)
        currentSeat = gameState.currentSeat
        phase = gameState.phase
        pending.addAll(gameState.pending)
        bangsPlayedThisTurn = gameState.bangsPlayedThisTurn
        turnNumber = gameState.turnNumber
        result = gameState.result
        // La reanudación secuencial del arranque de turno no se restaura (ver informe):
        // en un punto de decisión limpio con fase DRAW y sin pendientes se asume robo hecho.
        turnStage = TurnStage.AFTER_JAIL
        if (phase == GamePhase.DRAW && pending.isEmpty()) phase = GamePhase.PLAY
        advance()
    }

    companion object {
        internal fun newGame(config: GameConfig): BangGameEngine =
            BangGameEngine(Random(config.seed)).apply { bootstrapNewGame(config) }

        internal fun restore(gameState: GameState, seed: Long): BangGameEngine =
            BangGameEngine(Random(seed)).apply { restoreFrom(gameState) }
    }
}

/**
 * Fábrica por defecto del motor de reglas. Crea partidas nuevas deterministas a partir
 * de [GameConfig] y restaura motores desde un [GameState] (para MCTS, online o guardado).
 */
class DefaultGameEngineFactory : GameEngineFactory {
    override fun create(config: GameConfig): GameEngine = BangGameEngine.newGame(config)
    override fun restore(state: GameState, seed: Long): GameEngine = BangGameEngine.restore(state, seed)
}
