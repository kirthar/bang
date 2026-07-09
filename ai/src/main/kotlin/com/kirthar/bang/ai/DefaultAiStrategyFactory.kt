package com.kirthar.bang.ai

import com.kirthar.bang.ai.mcts.MctsStrategy
import com.kirthar.bang.core.engine.GameEngineFactory

/**
 * Fábrica por defecto de estrategias de IA.
 *
 * - [AiDifficulty.EASY] → [RandomStrategy].
 * - [AiDifficulty.MEDIUM] → [HeuristicStrategy].
 * - [AiDifficulty.HARD] → [MctsStrategy], que **requiere** un [GameEngineFactory] para
 *   simular. Si no se proporciona ([engineFactory] `null`), el nivel HARD degrada a
 *   MEDIUM (heurística), de modo que la app siga funcionando aunque no se inyecte motor.
 *
 * @param engineFactory fábrica del motor real; obligatoria para HARD, opcional para el resto.
 */
class DefaultAiStrategyFactory(
    private val engineFactory: GameEngineFactory? = null,
) : AiStrategyFactory {

    override fun create(difficulty: AiDifficulty, seed: Long): AiStrategy = when (difficulty) {
        AiDifficulty.EASY -> RandomStrategy(seed)
        AiDifficulty.MEDIUM -> HeuristicStrategy(seed)
        AiDifficulty.HARD ->
            if (engineFactory != null) MctsStrategy(seed, engineFactory)
            else HeuristicStrategy(seed)   // degradación segura sin motor para simular
    }
}
