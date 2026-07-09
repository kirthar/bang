package com.kirthar.bang.ai.sim

import com.kirthar.bang.ai.AiDifficulty
import com.kirthar.bang.ai.FixedLoopFactory
import com.kirthar.bang.core.model.PlayerSetup
import com.kirthar.bang.core.model.Role
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests del simulador headless [GameSimulator] contra el motor de prueba
 * [com.kirthar.bang.ai.FixedLoopEngine] (juego trivial que siempre gana el Sheriff).
 */
class GameSimulatorTest {

    private fun seats(n: Int = 4) = (0 until n).map { seat ->
        SeatSetup(PlayerSetup("Bot$seat"), if (seat % 2 == 0) AiDifficulty.EASY else AiDifficulty.MEDIUM)
    }

    @Test
    fun `ejecuta N partidas y agrega estadisticas`() = runTest {
        val stats = GameSimulator(FixedLoopFactory()).run(seats(), games = 10, baseSeed = 5)

        assertEquals(10, stats.gamesRequested)
        assertEquals(10, stats.gamesCompleted)
        assertEquals(0, stats.errors)
        // En el motor trivial siempre gana el bando del Sheriff.
        assertEquals(10, stats.winsByRole[Role.SHERIFF])
        assertTrue(stats.averageTurns > 0.0)
    }

    @Test
    fun `atribuye las victorias a la dificultad del asiento ganador`() = runTest {
        // En rolesFor(4) el Sheriff es el asiento 0 (EASY) y no hay Alguaciles.
        val stats = GameSimulator(FixedLoopFactory()).run(seats(4), games = 4, baseSeed = 0)
        assertEquals(4, stats.winsByDifficulty[AiDifficulty.EASY])
        assertEquals(null, stats.winsByDifficulty[AiDifficulty.MEDIUM])
    }

    @Test
    fun `rechaza configuraciones fuera de 4-7 asientos`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            GameSimulator(FixedLoopFactory()).run(seats(3), games = 1)
        }
    }
}
