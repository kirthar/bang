package com.kirthar.bang.ai.mcts

import com.kirthar.bang.ai.memory.PlayerMemory
import com.kirthar.bang.core.deck.DeckFactory
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.model.PlayerState
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.rolesFor
import com.kirthar.bang.core.view.PlayerGameView
import kotlin.random.Random

/**
 * Convierte una [PlayerGameView] (información parcial) en un [GameState] completo y
 * **legal** («determinización»), muestreando de forma coherente todo lo oculto:
 *
 * - **Mano propia**: se copia intacta de la vista.
 * - **Cartas en juego**: son públicas, se copian tal cual.
 * - **Manos rivales, mazo y montón de descartes**: el resto de las 80 cartas del mazo
 *   base (las no vistas) se barajan y se reparten respetando los **conteos conocidos**
 *   (`handCount` de cada rival, `deckSize`, y el tamaño del descarte deducido del total).
 * - **Roles ocultos**: se muestrean del multiset legal de roles restante, ponderando
 *   con las sospechas de [PlayerMemory]. Los roles conocidos (Sheriff, revelados, propio)
 *   se respetan siempre.
 *
 * Supuestos sobre el motor (a arbitrar en la Fase 2 de integración):
 * - El montón de descartes se modela con la carta superior ([PlayerGameView.discardTop])
 *   como **último** elemento de la lista.
 * - [GameState.pending] se deja vacío: la determinización solo se usa cuando la decisión
 *   raíz es una jugada normal sin interacción pendiente reconstruible (ver [MctsStrategy]).
 * - [GameState.bangsPlayedThisTurn] no es observable desde la vista; se asume 0.
 */
class Determinizer(private val memory: PlayerMemory) {

    private val fullDeck: List<Card> = DeckFactory.createBaseDeck()

    /** Genera un [GameState] coherente con [view] usando [random] para lo oculto. */
    fun determinize(view: PlayerGameView, random: Random): GameState {
        val hidden = sampleHiddenCards(view, random)
        val roles = sampleRoles(view, random)

        val players = view.players.map { p ->
            val hand = if (p.seat == view.mySeat) view.myHand else hidden.hands.getValue(p.seat)
            PlayerState(
                seat = p.seat,
                name = p.name,
                character = p.character,
                role = roles.getValue(p.seat),
                health = p.health,
                maxHealth = p.maxHealth,
                hand = hand,
                inPlay = p.inPlay,
                isAlive = p.isAlive,
            )
        }

        return GameState(
            players = players,
            deck = hidden.deck,
            discardPile = hidden.discardPile,
            currentSeat = view.currentTurnSeat,
            phase = view.phase.takeIf { it != GamePhase.FINISHED } ?: GamePhase.PLAY,
            pending = emptyList(),
            bangsPlayedThisTurn = 0,
            turnNumber = view.turnNumber,
            result = view.result,
        )
    }

    private class HiddenCards(
        val deck: List<Card>,
        val hands: Map<Int, List<Card>>,
        val discardPile: List<Card>,
    )

    private fun sampleHiddenCards(view: PlayerGameView, random: Random): HiddenCards {
        val knownIds = buildSet {
            view.myHand.forEach { add(it.id) }
            view.players.forEach { p -> p.inPlay.forEach { add(it.id) } }
            view.discardTop?.let { add(it.id) }
        }
        val pool = fullDeck.filter { it.id !in knownIds }.shuffled(random).toMutableList()

        // Tamaño del descarte deducido: 80 = mazo + descartes + manos + cartas en juego.
        val handsTotal = view.players.sumOf { it.handCount }
        val inPlayTotal = view.players.sumOf { it.inPlay.size }
        val discardBelowTop = (fullDeck.size - view.deckSize - handsTotal - inPlayTotal - topCount(view))
            .coerceAtLeast(0)

        val deck = takeN(pool, view.deckSize)

        val hands = mutableMapOf<Int, List<Card>>()
        for (p in view.players) {
            if (p.seat == view.mySeat) continue
            hands[p.seat] = takeN(pool, p.handCount)
        }

        val discardPile = buildList {
            addAll(takeN(pool, discardBelowTop))
            view.discardTop?.let { add(it) }   // superior = último elemento
        }
        return HiddenCards(deck, hands, discardPile)
    }

    private fun topCount(view: PlayerGameView): Int = if (view.discardTop != null) 1 else 0

    private fun takeN(pool: MutableList<Card>, n: Int): List<Card> {
        val count = n.coerceIn(0, pool.size)
        val taken = ArrayList<Card>(count)
        repeat(count) { taken.add(pool.removeAt(pool.size - 1)) }
        return taken
    }

    /**
     * Asigna un rol a cada asiento: los conocidos se respetan; los ocultos se muestrean
     * del multiset legal restante, ponderando por sospechas. El resultado es siempre una
     * permutación legal de [rolesFor].
     */
    fun sampleRoles(view: PlayerGameView, random: Random): Map<Int, Role> {
        val result = mutableMapOf<Int, Role>()
        val unknownSeats = mutableListOf<Int>()

        for (p in view.players) {
            val known = p.role ?: memory.knownRole(p.seat)
            if (known != null) result[p.seat] = known else unknownSeats.add(p.seat)
        }

        val remaining = rolesFor(view.players.size).toMutableList()
        result.values.forEach { remaining.remove(it) }

        // Asignar por asientos en orden aleatorio para no sesgar por posición.
        unknownSeats.shuffle(random)
        for (seat in unknownSeats) {
            if (remaining.isEmpty()) break
            val weights = memory.roleWeights(seat)
            val chosen = weightedPick(remaining, random) { role -> (weights[role] ?: 1.0).coerceAtLeast(1e-6) }
            remaining.remove(chosen)
            result[seat] = chosen
        }
        return result
    }

    private fun weightedPick(roles: List<Role>, random: Random, weight: (Role) -> Double): Role {
        val total = roles.sumOf(weight)
        var pick = random.nextDouble() * total
        for (role in roles) {
            pick -= weight(role)
            if (pick <= 0.0) return role
        }
        return roles.last()
    }
}
