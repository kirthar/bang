package com.kirthar.bang.di

import com.kirthar.bang.ai.AiStrategyFactory
import com.kirthar.bang.ai.DefaultAiStrategyFactory
import com.kirthar.bang.core.engine.DefaultGameEngineFactory
import com.kirthar.bang.core.engine.GameEngineFactory

/**
 * Punto único de inyección de dependencias «reales» del juego.
 *
 * Mientras [engineFactory] o [aiStrategyFactory] sean `null`, `GameViewModel`
 * recurre a las implementaciones de desarrollo del paquete `com.kirthar.bang.fake`.
 */
object GameDependencies {
    /** Fábrica del motor de reglas real. */
    var engineFactory: GameEngineFactory? = DefaultGameEngineFactory()

    /** Fábrica de estrategias de IA reales (MCTS usa [engineFactory] para simular). */
    var aiStrategyFactory: AiStrategyFactory? = DefaultAiStrategyFactory(engineFactory)
}
