package com.kirthar.bang.ai.sim

import com.kirthar.bang.ai.AiDifficulty
import com.kirthar.bang.core.engine.DefaultGameEngineFactory
import com.kirthar.bang.core.model.PlayerSetup
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verificación de integración puntual (NO forma parte del encargo de ningún
 * subagente): confirma que el motor real ([DefaultGameEngineFactory], del
 * subagente de motor) funciona con las IAs reales ([GameSimulator], del
 * subagente de IA) de extremo a extremo, con las 3 dificultades y 4-7 jugadores.
 */
class RealEngineSimulationCheck {

    @Test
    fun `partidas con motor real y IAs facil-media terminan sin error, 4 a 7 jugadores`() = runTest {
        val simulator = GameSimulator(DefaultGameEngineFactory())
        for (playerCount in 4..7) {
            val seats = (0 until playerCount).map { i ->
                val difficulty = if (i % 2 == 0) AiDifficulty.EASY else AiDifficulty.MEDIUM
                SeatSetup(PlayerSetup("P$i"), difficulty)
            }
            val stats = simulator.run(seats, games = 25, baseSeed = 5000L + playerCount * 1000)
            assertTrue(
                stats.errors == 0,
                "$playerCount jugadores: ${stats.errors} errores de ${stats.gamesRequested} -> ${stats.errorMessages.take(3)}",
            )
            assertTrue(stats.gamesCompleted == stats.gamesRequested)
            assertTrue(stats.winsByRole.values.sum() == stats.gamesCompleted)
        }
    }

    @Test
    fun `una partida a 5 jugadores con un asiento dificil (MCTS) termina sin error`() = runTest {
        val simulator = GameSimulator(DefaultGameEngineFactory())
        val seats = listOf(
            SeatSetup(PlayerSetup("P0"), AiDifficulty.HARD),
            SeatSetup(PlayerSetup("P1"), AiDifficulty.EASY),
            SeatSetup(PlayerSetup("P2"), AiDifficulty.MEDIUM),
            SeatSetup(PlayerSetup("P3"), AiDifficulty.EASY),
            SeatSetup(PlayerSetup("P4"), AiDifficulty.MEDIUM),
        )
        val stats = simulator.run(seats, games = 2, baseSeed = 9000L)
        assertTrue(stats.errors == 0, "errores: ${stats.errorMessages}")
        assertTrue(stats.gamesCompleted == 2)
    }
}
