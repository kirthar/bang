package com.kirthar.bang.fake

import com.kirthar.bang.ai.AiDifficulty
import com.kirthar.bang.ai.AiStrategy
import com.kirthar.bang.ai.AiStrategyFactory
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import kotlin.random.Random

/**
 * Estrategia de IA de desarrollo: elige una opción legal al azar. Se usa como
 * respaldo mientras `GameDependencies.aiStrategyFactory` no tenga la implementación
 * real del subagente de IA (equivalente a lo que las reglas llaman [AiDifficulty.EASY]).
 */
class FakeAiStrategy(private val random: Random) : AiStrategy {
    override fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand =
        request.options.random(random)

    override fun onEvent(event: GameEvent) = Unit

    companion object Factory : AiStrategyFactory {
        override fun create(difficulty: AiDifficulty, seed: Long): AiStrategy = FakeAiStrategy(Random(seed))
    }
}
