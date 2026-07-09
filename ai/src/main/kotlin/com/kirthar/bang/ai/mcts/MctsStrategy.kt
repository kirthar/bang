package com.kirthar.bang.ai.mcts

import com.kirthar.bang.ai.HeuristicStrategy
import com.kirthar.bang.ai.RandomStrategy
import com.kirthar.bang.ai.AiStrategy
import com.kirthar.bang.ai.memory.PlayerMemory
import com.kirthar.bang.core.engine.CommandResult
import com.kirthar.bang.core.engine.GameEngine
import com.kirthar.bang.core.engine.GameEngineFactory
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * IA difícil ([com.kirthar.bang.ai.AiDifficulty.HARD]): **ISMCTS** (Information Set
 * Monte Carlo Tree Search) con determinización de la información oculta.
 *
 * En cada iteración:
 * 1. [Determinizer] muestrea un [GameState] completo y legal coherente con la
 *    [PlayerGameView] y la memoria de sospechas.
 * 2. [GameEngineFactory.restore] crea un motor simulado sobre ese estado.
 * 3. Se selecciona una opción de la raíz con **UCB1** y se juega un *rollout* con
 *    [RandomStrategy] hasta el final (o un límite de turnos, evaluado heurísticamente).
 * 4. El resultado (1 = victoria, 0 = derrota, o evaluación intermedia) se propaga a las
 *    estadísticas de esa opción raíz.
 *
 * Se elige finalmente la opción más visitada (*robust child*). El presupuesto está
 * acotado por **iteraciones y tiempo** ([iterations], [maxMillis]) para ser apta en
 * móvil. La determinización solo es reconstruible en jugadas normales sin interacción
 * pendiente; para reacciones, elecciones y descartes delega en [HeuristicStrategy].
 *
 * @param engineFactory fábrica del motor real usada para simular (inyectada; en tests
 *   se usa un motor de prueba que cumple el mismo contrato [GameEngine]).
 */
class MctsStrategy(
    private val seed: Long,
    private val engineFactory: GameEngineFactory,
    private val iterations: Int = 2000,
    private val maxMillis: Long = 1000,
    private val memory: PlayerMemory = PlayerMemory(),
    private val maxRolloutTurns: Int = 40,
) : AiStrategy {

    private val determinizer = Determinizer(memory)
    private val fallback = HeuristicStrategy(seed, memory)
    private val rootRandom = Random(seed)
    private var selfRegistered = false

    override fun onEvent(event: GameEvent) = memory.onEvent(event)

    override fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand {
        if (!selfRegistered) {
            memory.selfRole(view.mySeat, view.myRole)
            selfRegistered = true
        }
        val options = request.options
        if (options.size == 1) return options.first()

        // Solo la jugada normal es determinizable con el contrato actual (pending vacío).
        if (request !is DecisionRequest.PlayOrPass) return fallback.decide(view, request)

        return search(view, request)
    }

    private fun search(view: PlayerGameView, request: DecisionRequest.PlayOrPass): GameCommand {
        val options = request.options
        val counts = IntArray(options.size)
        val values = DoubleArray(options.size)
        val start = System.currentTimeMillis()

        var iter = 0
        while (iter < iterations && System.currentTimeMillis() - start < maxMillis) {
            val iterSeed = rootRandom.nextLong()
            val index = selectUcb(counts, values, iter)
            val value = runIteration(view, options[index], iterSeed)
            counts[index]++
            values[index] += value
            iter++
        }

        // Robust child: la opción más visitada; desempate por valor medio.
        var best = 0
        for (i in options.indices) {
            if (counts[i] > counts[best] ||
                (counts[i] == counts[best] && mean(values, counts, i) > mean(values, counts, best))
            ) {
                best = i
            }
        }
        return options[best]
    }

    private fun mean(values: DoubleArray, counts: IntArray, i: Int): Double =
        if (counts[i] == 0) 0.0 else values[i] / counts[i]

    /** UCB1: explora primero lo no visitado; luego equilibra explotación y exploración. */
    private fun selectUcb(counts: IntArray, values: DoubleArray, totalVisits: Int): Int {
        for (i in counts.indices) if (counts[i] == 0) return i
        val lnT = ln((totalVisits + 1).toDouble())
        var best = 0
        var bestScore = Double.NEGATIVE_INFINITY
        for (i in counts.indices) {
            val exploit = values[i] / counts[i]
            val explore = EXPLORATION * sqrt(lnT / counts[i])
            val score = exploit + explore
            if (score > bestScore) {
                bestScore = score
                best = i
            }
        }
        return best
    }

    private fun runIteration(view: PlayerGameView, rootOption: GameCommand, iterSeed: Long): Double {
        val random = Random(iterSeed)
        val state = determinizer.determinize(view, random)
        val engine = engineFactory.restore(state, iterSeed)

        when (engine.submit(view.mySeat, rootOption)) {
            is CommandResult.Rejected -> return 0.0   // opción incoherente con esta determinización
            is CommandResult.Accepted -> Unit
        }
        return rollout(engine, view.mySeat, iterSeed)
    }

    /** *Rollout* aleatorio hasta el final o el tope de turnos; devuelve valor en [0,1]. */
    private fun rollout(engine: GameEngine, mySeat: Int, iterSeed: Long): Double {
        val rnd = RandomStrategy(iterSeed xor 0x9E3779B97F4A7C15uL.toLong())
        val startTurn = engine.state.turnNumber
        var guard = 0
        val guardLimit = maxRolloutTurns * 200

        while (guard++ < guardLimit) {
            val decision = engine.pendingDecision() ?: break
            if (engine.state.turnNumber - startTurn >= maxRolloutTurns) break
            val (seat, req) = decision
            val cmd = rnd.decide(engine.viewFor(seat), req)
            var accepted = engine.submit(seat, cmd) is CommandResult.Accepted
            var attempts = 0
            while (!accepted && attempts++ < 4) {
                accepted = engine.submit(seat, req.options.first()) is CommandResult.Accepted
            }
            if (!accepted) break   // motor atascado: cortar
        }
        return evaluate(engine.state, mySeat)
    }

    /** Valor terminal (0/1) o evaluación heurística intermedia en (0,1). */
    private fun evaluate(state: GameState, mySeat: Int): Double {
        state.result?.let { return if (mySeat in it.winnerSeats) 1.0 else 0.0 }

        val me = state.player(mySeat)
        if (!me.isAlive) return 0.1
        val living = state.alivePlayers
        val progress = when (me.role) {
            Role.SHERIFF, Role.DEPUTY -> {
                val enemies = living.count { it.role == Role.OUTLAW || it.role == Role.RENEGADE }
                1.0 - enemies.toDouble() / (living.size).coerceAtLeast(1)
            }
            Role.OUTLAW -> {
                val sheriff = living.firstOrNull { it.isSheriff }
                if (sheriff == null) 0.9 else 0.4 + 0.3 * (1.0 - sheriff.health.toDouble() / sheriff.maxHealth)
            }
            Role.RENEGADE -> 1.0 - (living.size - 1).toDouble() / state.players.size
        }
        val healthTerm = 0.2 * (me.health.toDouble() / me.maxHealth)
        return (0.3 + 0.5 * progress + healthTerm).coerceIn(0.1, 0.9)
    }

    companion object {
        private val EXPLORATION = sqrt(2.0)
    }
}

