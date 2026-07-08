package com.kirthar.bang.core.engine

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.GameState
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView

/** Resultado de enviar un comando al motor. */
sealed interface CommandResult {
    data class Accepted(val events: List<GameEvent>) : CommandResult
    data class Rejected(val reason: String) : CommandResult
}

/**
 * Motor de reglas autoritativo. Determinista: mismo [GameConfig] (misma semilla) y
 * misma secuencia de comandos producen exactamente la misma partida — la base del
 * juego online y de las simulaciones de IA.
 *
 * El motor no conoce a los agentes: expone qué asiento debe decidir y valida los
 * comandos que recibe. El bucle que consulta a los agentes es [GameLoop].
 */
interface GameEngine {

    /** Estado completo. Solo para el propio motor, tests, simulación MCTS y futuro servidor. */
    val state: GameState

    /** Proyección del estado para un asiento (lo único que ven jugadores e IAs). */
    fun viewFor(seat: Int): PlayerGameView

    /** Asiento que debe decidir ahora y su petición, o `null` si la partida terminó. */
    fun pendingDecision(): Pair<Int, DecisionRequest>?

    /**
     * Procesa un comando de [seat]. Si es ilegal o no es su turno de decidir, devuelve
     * [CommandResult.Rejected] sin tocar el estado.
     */
    fun submit(seat: Int, command: GameCommand): CommandResult
}

/**
 * Fábrica del motor. La implementación (`BangGameEngine`) vive en este mismo paquete.
 */
interface GameEngineFactory {

    /** Crea una partida nueva: baraja, reparte roles/personajes/manos según la semilla. */
    fun create(config: GameConfig): GameEngine

    /** Restaura un motor a partir de un estado (simulación MCTS, online, guardado). */
    fun restore(state: GameState, seed: Long): GameEngine
}
