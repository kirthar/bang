package com.kirthar.bang.ai.memory

import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.Role

/**
 * Memoria interna que una IA mantiene a partir de los eventos redactados de su asiento.
 *
 * Concentra dos cosas:
 * - **Roles conocidos**: el del Sheriff (público desde el inicio) y los que se revelan
 *   al morir cada jugador. El propio rol se registra con [selfRole].
 * - **Modelo de sospecha**: quién dispara/molesta a quién. De ahí se derivan dos
 *   puntuaciones por asiento (escala acumulativa, sin techo fijo):
 *     - [antiSheriffScore]: cuánto ha agredido al Sheriff → indicio de Forajido/Renegado.
 *     - [proSheriffScore]: cuánto ha agredido a quienes agreden al Sheriff → indicio de Alguacil.
 *
 * El modelo es puramente observacional: nunca ve manos ni roles ocultos. Es la base de
 * los objetivos de [com.kirthar.bang.ai.HeuristicStrategy] y del muestreo de roles en la
 * determinización de [com.kirthar.bang.ai.mcts.MctsStrategy].
 */
class PlayerMemory {

    private var sheriffSeat: Int = -1

    private val knownRoles: MutableMap<Int, Role> = mutableMapOf()

    /** aggression[atacante][objetivo] = intensidad acumulada de agresiones observadas. */
    private val aggression: MutableMap<Int, MutableMap<Int, Double>> = mutableMapOf()

    /** Registra el rol propio (no llega por evento redactado como oculto). */
    fun selfRole(seat: Int, role: Role) {
        knownRoles[seat] = role
    }

    /** Rol conocido de un asiento (Sheriff, revelados o el propio), o `null` si es oculto. */
    fun knownRole(seat: Int): Role? = knownRoles[seat]

    /** El asiento del Sheriff, o -1 si aún no se ha observado el inicio de partida. */
    fun sheriffSeatOrNull(): Int = sheriffSeat

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.GameStarted -> {
                sheriffSeat = event.sheriffSeat
                knownRoles[event.sheriffSeat] = Role.SHERIFF
            }
            is GameEvent.RoleRevealed -> knownRoles[event.seat] = event.role
            is GameEvent.PlayerEliminated -> knownRoles[event.seat] = event.role
            is GameEvent.CardPlayed -> registerCardPlayed(event)
            is GameEvent.HealthChanged -> registerHealthChanged(event)
            else -> Unit
        }
    }

    private fun registerCardPlayed(event: GameEvent.CardPlayed) {
        val target = event.targetSeat ?: return
        if (target == event.seat) return
        val weight = when (event.card.kind) {
            CardKind.BANG -> 1.0
            CardKind.DUEL -> 1.0
            CardKind.PANIC, CardKind.CAT_BALOU -> 0.6   // hostilidad más leve (robo/descarte)
            CardKind.JAIL -> 0.8
            else -> 0.0
        }
        if (weight > 0.0) addAggression(event.seat, target, weight)
    }

    private fun registerHealthChanged(event: GameEvent.HealthChanged) {
        if (event.delta >= 0) return
        val source = event.sourceSeat ?: return   // Dinamita y similares no tienen autor
        if (source == event.seat) return
        // Daño confirmado: señal fuerte de hostilidad.
        addAggression(source, event.seat, 1.5 * -event.delta)
    }

    private fun addAggression(attacker: Int, target: Int, amount: Double) {
        val row = aggression.getOrPut(attacker) { mutableMapOf() }
        row[target] = (row[target] ?: 0.0) + amount
    }

    private fun aggressionOf(attacker: Int, target: Int): Double =
        aggression[attacker]?.get(target) ?: 0.0

    /** Agresión total observada del asiento hacia el Sheriff (indicio de anti-Sheriff). */
    fun antiSheriffScore(seat: Int): Double {
        if (sheriffSeat < 0 || seat == sheriffSeat) return 0.0
        return aggressionOf(seat, sheriffSeat)
    }

    /**
     * Agresión del asiento hacia jugadores que a su vez han agredido al Sheriff
     * (indicio de Alguacil que defiende).
     */
    fun proSheriffScore(seat: Int): Double {
        if (sheriffSeat < 0 || seat == sheriffSeat) return 0.0
        val row = aggression[seat] ?: return 0.0
        return row.entries.sumOf { (target, amount) ->
            if (target != sheriffSeat && antiSheriffScore(target) > 0.0) amount else 0.0
        }
    }

    /** Cuánto ha agredido [seat] a [victim] (para saber quién nos ataca a nosotros). */
    fun aggressionTowards(seat: Int, victim: Int): Double = aggressionOf(seat, victim)

    /**
     * Pesos relativos (>0) de cada rol candidato para un asiento de rol oculto, dadas
     * las sospechas. Se usan para muestrear roles legales en la determinización de MCTS.
     */
    fun roleWeights(seat: Int): Map<Role, Double> {
        val anti = antiSheriffScore(seat)
        val pro = proSheriffScore(seat)
        return mapOf(
            Role.OUTLAW to 1.0 + 1.5 * anti,
            Role.RENEGADE to 1.0 + 0.7 * anti,
            Role.DEPUTY to 1.0 + 1.5 * pro,
            Role.SHERIFF to 0.0,   // el Sheriff siempre es conocido; nunca se muestrea
        )
    }
}
