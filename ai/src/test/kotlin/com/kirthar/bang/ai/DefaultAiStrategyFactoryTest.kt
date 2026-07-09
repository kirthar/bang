package com.kirthar.bang.ai

import com.kirthar.bang.ai.mcts.MctsStrategy
import kotlin.test.Test
import kotlin.test.assertTrue

class DefaultAiStrategyFactoryTest {

    @Test
    fun `crea la estrategia correspondiente a cada dificultad`() {
        val factory = DefaultAiStrategyFactory(engineFactory = WinOnCardFactory(0))
        assertTrue(factory.create(AiDifficulty.EASY, 1) is RandomStrategy)
        assertTrue(factory.create(AiDifficulty.MEDIUM, 1) is HeuristicStrategy)
        assertTrue(factory.create(AiDifficulty.HARD, 1) is MctsStrategy)
    }

    @Test
    fun `HARD degrada a heuristica sin fabrica de motor`() {
        val factory = DefaultAiStrategyFactory(engineFactory = null)
        assertTrue(factory.create(AiDifficulty.HARD, 1) is HeuristicStrategy)
    }
}
