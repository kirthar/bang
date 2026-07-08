package com.kirthar.bang.ai

import com.kirthar.bang.core.agent.PlayerAgent
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import kotlinx.coroutines.delay

/** Niveles de dificultad de la IA. */
enum class AiDifficulty {
    EASY,    // RandomStrategy: elige una opción legal al azar
    MEDIUM,  // HeuristicStrategy: reglas expertas por rol
    HARD,    // MctsStrategy: ISMCTS con determinización
}

/**
 * Estrategia de decisión de una IA. Solo ve [PlayerGameView] (información legal para
 * su asiento) — nunca el estado completo. Puede mantener memoria interna alimentada
 * por [onEvent] (conteo de cartas, sospechas de rol...).
 */
interface AiStrategy {

    /** Elige un comando; debe ser una de las [DecisionRequest.options]. */
    fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand

    /** Evento redactado para este asiento, por si la estrategia mantiene memoria. */
    fun onEvent(event: GameEvent) {}
}

/** Fábrica de estrategias, punto de extensión para añadir nuevas IAs. */
interface AiStrategyFactory {
    fun create(difficulty: AiDifficulty, seed: Long): AiStrategy
}

/**
 * Adaptador de una [AiStrategy] al contrato [PlayerAgent] del motor.
 *
 * @param thinkDelayMs pausa artificial por decisión para que en la UI se perciba que
 *   la IA «piensa» (0 en simulaciones headless).
 */
class AiAgent(
    private val strategy: AiStrategy,
    private val thinkDelayMs: Long = 0,
) : PlayerAgent {

    override suspend fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand {
        if (thinkDelayMs > 0) delay(thinkDelayMs)
        return strategy.decide(view, request)
    }

    override fun onEvent(event: GameEvent) = strategy.onEvent(event)
}
