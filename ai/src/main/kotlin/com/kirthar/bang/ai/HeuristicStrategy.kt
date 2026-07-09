package com.kirthar.bang.ai

import com.kirthar.bang.ai.memory.PlayerMemory
import com.kirthar.bang.ai.support.CardValues
import com.kirthar.bang.ai.support.aliveCount
import com.kirthar.bang.ai.support.cardInHand
import com.kirthar.bang.ai.support.hasInPlay
import com.kirthar.bang.ai.support.weapon
import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.DEFAULT_WEAPON_RANGE
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.core.view.PublicPlayerInfo
import kotlin.random.Random

/**
 * IA media ([AiDifficulty.MEDIUM]): reglas expertas por rol sobre una [PlayerMemory]
 * que sospecha los roles ocultos a partir de los eventos.
 *
 * ## Escala de puntuación
 *
 * Toda decisión se reduce a puntuar comandos legales y elegir el mayor. Las escalas:
 *
 * - **Hostilidad** hacia un rival: [hostility] devuelve un valor con signo. `> 0`
 *   significa «es mi enemigo, me interesa dañarle» (más alto = más prioritario);
 *   `<= 0` significa aliado o neutral (nunca se le ataca). Rango típico −3..+3.
 * - **Amenaza** de un rival: [threat] es siempre `>= 0` (peligro objetivo: arma, vida,
 *   cartas, ser Sheriff), independientemente de la alianza.
 * - **Jugar una carta** (fase de juego): [scorePlay] puntúa cada opción; pasar
 *   ([GameCommand.EndPlayPhase]) vale [PASS_SCORE] = 0.5. Solo se juega una carta si
 *   supera ese umbral. Referencias: equiparse 4–8, curarse 2–6, atacar 3–8, control
 *   (Panic!/Cat Balou/Cárcel) 3–7, robo de cartas 4–5.
 * - **Valor de mano**: [CardValues] (0–10) para descartar/elegir cartas.
 *
 * @param seed semilla para desempates aleatorios (reproducibilidad).
 * @param memory memoria de sospechas; se comparte con [com.kirthar.bang.ai.mcts.MctsStrategy].
 */
class HeuristicStrategy(
    seed: Long,
    private val memory: PlayerMemory = PlayerMemory(),
) : AiStrategy {

    private val random = Random(seed)
    private var selfRegistered = false

    override fun onEvent(event: GameEvent) = memory.onEvent(event)

    override fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand {
        if (!selfRegistered) {
            memory.selfRole(view.mySeat, view.myRole)
            selfRegistered = true
        }
        return when (request) {
            is DecisionRequest.PlayOrPass -> decidePlay(view, request)
            is DecisionRequest.React -> decideReact(view, request)
            is DecisionRequest.DiscardToHandLimit -> decideDiscard(view, request)
            is DecisionRequest.PickCard -> decidePick(view, request)
            is DecisionRequest.ChooseDraw -> decideDraw(view, request)
        }
    }

    // ----------------------------------------------------------------------------
    // Fase de juego
    // ----------------------------------------------------------------------------

    private fun decidePlay(view: PlayerGameView, request: DecisionRequest.PlayOrPass): GameCommand {
        val scored = request.options.map { option -> option to scoreOption(view, option) }
        val bestScore = scored.maxOfOrNull { it.second } ?: return GameCommand.EndPlayPhase
        // Desempate aleatorio (reproducible) entre las opciones igualmente buenas.
        val top = scored.filter { it.second >= bestScore - 1e-9 }.map { it.first }
        return top[random.nextInt(top.size)]
    }

    private fun scoreOption(view: PlayerGameView, option: GameCommand): Double = when (option) {
        is GameCommand.EndPlayPhase -> PASS_SCORE
        is GameCommand.PlayCard -> scorePlay(view, option)
        is GameCommand.UseCharacterAbility -> scoreSidKetchum(view)
        else -> 0.0
    }

    private fun scorePlay(view: PlayerGameView, play: GameCommand.PlayCard): Double {
        val card = view.cardInHand(play.cardId) ?: return 0.0
        return when (card.kind) {
            CardKind.VOLCANIC, CardKind.SCHOFIELD, CardKind.REMINGTON,
            CardKind.REV_CARABINE, CardKind.WINCHESTER -> scoreWeapon(view, card)

            CardKind.BARREL -> if (view.me.hasInPlay(CardKind.BARREL)) 0.0 else 5.0
            CardKind.MUSTANG -> if (view.me.hasInPlay(CardKind.MUSTANG)) 0.0 else 4.5
            CardKind.SCOPE -> if (view.me.hasInPlay(CardKind.SCOPE)) 0.0 else 4.0

            CardKind.BEER -> scoreBeer(view)
            CardKind.SALOON -> scoreSaloon(view)

            CardKind.BANG -> scoreBang(view, play)
            CardKind.GATLING -> scoreArea(view, includeSelf = false)
            CardKind.INDIANS -> scoreArea(view, includeSelf = false) * 0.9
            CardKind.DUEL -> scoreTargeted(view, play, base = 2.5, factor = 1.2)
            CardKind.JAIL -> scoreJail(view, play)
            CardKind.PANIC, CardKind.CAT_BALOU -> scoreDispossess(view, play)

            CardKind.STAGECOACH -> 4.0 + drawUrgency(view)
            CardKind.WELLS_FARGO -> 5.0 + drawUrgency(view)
            CardKind.GENERAL_STORE -> 1.5 + drawUrgency(view)

            CardKind.DYNAMITE -> scoreDynamite(view)
            CardKind.MISSED -> 0.0   // solo se juega como reacción
        }
    }

    private fun scoreWeapon(view: PlayerGameView, card: Card): Double {
        val newRange = card.kind.weaponRange ?: DEFAULT_WEAPON_RANGE
        val currentRange = view.me.weapon?.kind?.weaponRange ?: DEFAULT_WEAPON_RANGE
        val volcanic = card.kind == CardKind.VOLCANIC
        return when {
            newRange > currentRange -> 6.0 + (newRange - currentRange)
            volcanic && view.me.weapon?.kind != CardKind.VOLCANIC -> 5.0 // BANG! ilimitados
            else -> 0.0   // no degradar el arma
        }
    }

    private fun scoreBeer(view: PlayerGameView): Double {
        if (view.aliveCount <= 2) return 0.0          // sin efecto con 2 vivos
        val deficit = view.me.maxHealth - view.me.health
        if (deficit <= 0) return 0.0
        val urgency = if (view.me.health <= 2) 3.0 else 0.0
        return deficit * 1.5 + urgency
    }

    private fun scoreSaloon(view: PlayerGameView): Double {
        if (view.aliveCount <= 2) return 0.0
        val myDeficit = view.me.maxHealth - view.me.health
        if (myDeficit <= 0) return 0.0
        // Cura a todos, también enemigos: solo interesa si estoy realmente tocado.
        return myDeficit * 1.0 - 0.5
    }

    private fun scoreBang(view: PlayerGameView, play: GameCommand.PlayCard): Double {
        val target = play.targetSeat ?: return 0.0
        val h = hostility(view, target)
        if (h <= 0.0) return -1.0        // no disparar a aliados
        return 3.0 + h * 1.5 + finisherBonus(view, target)
    }

    private fun scoreTargeted(
        view: PlayerGameView,
        play: GameCommand.PlayCard,
        base: Double,
        factor: Double,
    ): Double {
        val target = play.targetSeat ?: return 0.0
        val h = hostility(view, target)
        if (h <= 0.0) return -1.0
        return base + h * factor
    }

    /** Gatling/Indios: suma de hostilidades positivas menos daño colateral a aliados. */
    private fun scoreArea(view: PlayerGameView, includeSelf: Boolean): Double {
        var score = 0.0
        for (p in view.players) {
            if (!p.isAlive || p.seat == view.mySeat) continue
            val h = hostility(view, p.seat)
            score += if (h > 0) h * 1.2 else h * 1.5   // castiga golpear aliados
        }
        return score
    }

    private fun scoreJail(view: PlayerGameView, play: GameCommand.PlayCard): Double {
        val target = play.targetSeat ?: return 0.0
        // La Cárcel no puede ir sobre el Sheriff (el motor ya lo veta); apunta al
        // enemigo más fuerte para bloquearle el turno.
        val h = hostility(view, target)
        if (h <= 0.0) return -1.0
        return 3.5 + threat(view, view.players[target]) * 0.4
    }

    /** Panic!/Cat Balou: mejor contra el arma/Barril del enemigo más peligroso. */
    private fun scoreDispossess(view: PlayerGameView, play: GameCommand.PlayCard): Double {
        val target = play.targetSeat ?: return 0.0
        val h = hostility(view, target)
        if (h <= 0.0) return -1.0
        val targetCard = play.targetCardId?.let { id ->
            view.players[target].inPlay.firstOrNull { it.id == id }
        }
        val cardBonus = when (targetCard?.kind) {
            CardKind.BARREL -> 3.0
            CardKind.MUSTANG, CardKind.SCOPE -> 2.0
            null -> 0.5                 // robar/descartar carta de mano al azar
            else -> if (targetCard.kind.isWeapon) 2.5 else 1.5
        }
        return 3.0 + h + cardBonus
    }

    private fun scoreDynamite(view: PlayerGameView): Double {
        // Interesa si hay más enemigos que aliados vivos (la Dinamita circula y
        // acaba dañando a alguien; mejor que ese alguien no sea yo rodeado de aliados).
        var enemies = 0
        var allies = 0
        for (p in view.players) {
            if (!p.isAlive || p.seat == view.mySeat) continue
            if (hostility(view, p.seat) > 0) enemies++ else allies++
        }
        return if (enemies > allies) 2.0 else 0.3
    }

    private fun scoreSidKetchum(view: PlayerGameView): Double {
        val deficit = view.me.maxHealth - view.me.health
        if (deficit <= 0 || view.myHand.size < 2) return 0.0
        // Cuesta 2 cartas por vida: solo cuando la vida escasea.
        val urgency = if (view.me.health <= 2) 3.0 else 0.0
        return deficit * 0.8 + urgency - 1.0
    }

    private fun drawUrgency(view: PlayerGameView): Double =
        if (view.myHand.size <= 1) 1.5 else 0.0

    private fun finisherBonus(view: PlayerGameView, target: Int): Double {
        val p = view.players[target]
        // Rematar a un enemigo a 1 de vida es prioritario.
        return if (p.health <= 1) 2.0 else 0.0
    }

    // ----------------------------------------------------------------------------
    // Reacciones
    // ----------------------------------------------------------------------------

    private fun decideReact(view: PlayerGameView, request: DecisionRequest.React): GameCommand {
        val responds = request.options.filterIsInstance<GameCommand.Respond>()
        val canTakeHit = request.options.any { it is GameCommand.TakeHit }
        if (responds.isEmpty()) return request.options.first()

        val shouldRespond = shouldRespond(view, request)
        if (!shouldRespond && canTakeHit) return GameCommand.TakeHit
        // Responder con las cartas menos valiosas posibles.
        return responds.minByOrNull { respondCost(view, it) } ?: request.options.first()
    }

    private fun shouldRespond(view: PlayerGameView, request: DecisionRequest.React): Boolean {
        val health = view.me.health
        val lethal = health <= 1                       // el golpe me dejaría a 0
        if (lethal) return true
        return when (request.kind) {
            CardKind.BANG, CardKind.GATLING -> {
                val missed = countInHand(view, CardKind.MISSED)
                when {
                    health <= 2 -> true
                    // No gastar 2 ¡Fallaste! por 1 de daño si me sobra vida y escasean.
                    request.missesNeeded >= 2 -> missed >= 4
                    else -> missed >= 2            // hay de sobra, defenderse
                }
            }
            CardKind.INDIANS -> {
                val bangs = countInHand(view, CardKind.BANG)
                health <= 2 || bangs >= 2
            }
            CardKind.DUEL -> {
                val bangs = countInHand(view, CardKind.BANG)
                health <= 2 || bangs >= 2      // aguantar el duelo solo con munición
            }
            else -> health <= 2
        }
    }

    private fun respondCost(view: PlayerGameView, respond: GameCommand.Respond): Double =
        respond.cardIds.sumOf { id -> view.cardInHand(id)?.let { CardValues.handValue(it) } ?: 0 }.toDouble()

    // ----------------------------------------------------------------------------
    // Descartes y elecciones
    // ----------------------------------------------------------------------------

    private fun decideDiscard(view: PlayerGameView, request: DecisionRequest.DiscardToHandLimit): GameCommand {
        val discards = request.options.filterIsInstance<GameCommand.Discard>()
        if (discards.isEmpty()) return request.options.first()
        // Descartar el conjunto de menor valor total (y, a igualdad, menos cartas).
        return discards.minWithOrNull(
            compareBy({ discardValue(view, it) }, { it.cardIds.size }),
        ) ?: request.options.first()
    }

    private fun discardValue(view: PlayerGameView, discard: GameCommand.Discard): Int =
        discard.cardIds.sumOf { id -> view.cardInHand(id)?.let { CardValues.handValue(it) } ?: 0 }

    private fun decidePick(view: PlayerGameView, request: DecisionRequest.PickCard): GameCommand {
        val choices = request.options.filterIsInstance<GameCommand.ChooseCard>()
        if (choices.isEmpty()) return request.options.first()
        // Nos quedamos con la carta de mayor valor de las ofrecidas.
        val byId = request.cards.associateBy { it.id }
        return choices.maxByOrNull { c -> byId[c.cardId]?.let { CardValues.handValue(it) } ?: 0 }
            ?: request.options.first()
    }

    private fun decideDraw(view: PlayerGameView, request: DecisionRequest.ChooseDraw): GameCommand {
        val choices = request.options.filterIsInstance<GameCommand.ChooseDrawSource>()
        if (choices.isEmpty()) return request.options.first()

        // Jesse Jones: robar de la mano del enemigo con más cartas.
        val handSteal = choices
            .filter { it.source is DrawSource.PlayerHand }
            .maxByOrNull { c ->
                val seat = (c.source as DrawSource.PlayerHand).seat
                val p = view.players[seat]
                p.handCount + (if (hostility(view, seat) > 0) 5 else 0)
            }
        if (handSteal != null) {
            val seat = (handSteal.source as DrawSource.PlayerHand).seat
            if (view.players[seat].handCount > 0) return handSteal
        }

        // Pedro Ramírez: coger de los descartes solo si la carta de arriba es valiosa.
        val fromDiscard = choices.firstOrNull { it.source is DrawSource.DiscardPile }
        val discardValue = view.discardTop?.let { CardValues.handValue(it) } ?: 0
        if (fromDiscard != null && discardValue >= 6) return fromDiscard

        return choices.firstOrNull { it.source is DrawSource.Deck } ?: choices.first()
    }

    // ----------------------------------------------------------------------------
    // Modelo de alianzas / amenazas
    // ----------------------------------------------------------------------------

    private fun sheriffSeat(view: PlayerGameView): Int? =
        view.players.firstOrNull { it.role == Role.SHERIFF }?.seat

    /**
     * Grado en que me interesa **dañar** al asiento [seat] según mi rol y las sospechas.
     * `> 0` enemigo (mayor = más prioritario), `<= 0` aliado/neutral.
     */
    private fun hostility(view: PlayerGameView, seat: Int): Double {
        if (seat == view.mySeat) return -10.0
        val p = view.players[seat]
        if (!p.isAlive) return -10.0
        val known = p.role
        val anti = memory.antiSheriffScore(seat)
        val pro = memory.proSheriffScore(seat)
        val attacksMe = memory.aggressionTowards(seat, view.mySeat)

        return when (view.myRole) {
            Role.SHERIFF, Role.DEPUTY -> when (known) {
                Role.SHERIFF -> -3.0
                Role.DEPUTY -> -2.0
                Role.OUTLAW -> 3.0
                Role.RENEGADE -> 2.5
                null -> 0.5 + anti - pro + 0.3 * attacksMe
            }

            Role.OUTLAW -> {
                val sheriff = sheriffSeat(view)
                when {
                    seat == sheriff -> 3.0
                    known == Role.OUTLAW -> -2.0
                    known == Role.DEPUTY -> 2.0
                    known == Role.RENEGADE -> 1.0
                    else -> 0.4 + pro + 0.3 * attacksMe   // sospechosos de Alguacil
                }
            }

            Role.RENEGADE -> hostilityForRenegade(view, seat, known, attacksMe)
        }
    }

    /**
     * El Renegado apoya al bando débil: mientras haya varios jugadores ataca a la
     * facción más numerosa/fuerte; en el mano a mano final el Sheriff es el objetivo.
     */
    private fun hostilityForRenegade(
        view: PlayerGameView,
        seat: Int,
        known: Role?,
        attacksMe: Double,
    ): Double {
        val sheriff = sheriffSeat(view)
        if (view.aliveCount <= 2) {
            // Duelo final: para ganar debe quedar el último; el Sheriff estorba.
            return if (seat == sheriff) 3.0 else 1.0
        }
        // Estimar tamaño de cada bando entre los vivos.
        val sheriffTeam = view.players.count {
            it.isAlive && (it.role == Role.SHERIFF || it.role == Role.DEPUTY)
        }
        val outlaws = view.players.count {
            it.isAlive && (it.role == Role.OUTLAW || (it.role == null && memory.antiSheriffScore(it.seat) > 0))
        }
        val strongerSide = if (outlaws >= sheriffTeam) Role.OUTLAW else Role.SHERIFF
        return when (known) {
            Role.SHERIFF -> if (strongerSide == Role.SHERIFF) 2.0 else 0.5
            Role.DEPUTY -> if (strongerSide == Role.SHERIFF) 1.5 else 0.3
            Role.OUTLAW -> if (strongerSide == Role.OUTLAW) 2.0 else 0.5
            Role.RENEGADE -> 0.0
            null -> 0.5 + 0.3 * attacksMe
        }
    }

    /** Peligro objetivo de un rival (>= 0): arma, vida, cartas, ser Sheriff. */
    private fun threat(view: PlayerGameView, p: PublicPlayerInfo): Double {
        val range = p.weapon?.kind?.weaponRange ?: DEFAULT_WEAPON_RANGE
        return range + p.health * 0.5 + p.handCount * 0.3 + if (p.role == Role.SHERIFF) 2.0 else 0.0
    }

    private fun countInHand(view: PlayerGameView, kind: CardKind): Int =
        view.myHand.count { it.kind == kind }

    companion object {
        /** Puntuación de pasar el turno; solo se juega una carta si supera este umbral. */
        private const val PASS_SCORE = 0.5
    }
}
