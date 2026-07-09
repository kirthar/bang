package com.kirthar.bang.di

import com.kirthar.bang.ai.AiStrategyFactory
import com.kirthar.bang.core.engine.GameEngineFactory

/**
 * Punto único de inyección de dependencias «reales» del juego.
 *
 * Los subagentes del motor y de la IA (fase de integración) fijarán aquí sus
 * implementaciones concretas (`BangGameEngine.Factory`, `HeuristicAiStrategyFactory`,
 * etc.). Mientras [engineFactory] o [aiStrategyFactory] sean `null`, `GameViewModel`
 * recurre a las implementaciones de desarrollo del paquete `com.kirthar.bang.fake`.
 */
object GameDependencies {
    /** Fábrica del motor de reglas real. `null` mientras no esté integrado. */
    var engineFactory: GameEngineFactory? = null

    /** Fábrica de estrategias de IA reales. `null` mientras no estén integradas. */
    var aiStrategyFactory: AiStrategyFactory? = null
}
