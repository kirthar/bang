package com.kirthar.bang.core.engine

import com.kirthar.bang.core.agent.PlayerAgent
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.PlayerSetup
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Agente trivial: elige una de las opciones legales al azar (con RNG sembrado para que
 * la partida completa sea reproducible).
 */
private class RandomAgent(seed: Long) : PlayerAgent {
    private val rng = Random(seed)
    override suspend fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand {
        val options = request.options
        return options[rng.nextInt(options.size)]
    }
}

class SmokeTest {

    private fun configFor(playerCount: Int, seed: Long): GameConfig {
        val players = (0 until playerCount).map { PlayerSetup(name = "P$it") }
        return GameConfig(players, seed)
    }

    @Test
    fun `200 partidas con agentes aleatorios terminan con resultado valido`() = runTest {
        val factory = DefaultGameEngineFactory()
        for (seed in 0L until 200L) {
            val playerCount = 4 + (seed % 4).toInt() // 4..7
            val engine = factory.create(configFor(playerCount, seed))
            val agents = (0 until playerCount).associateWith { seat ->
                RandomAgent(seed * 100 + seat) as PlayerAgent
            }
            val loop = GameLoop(engine, agents, maxIllegalRetries = 2)
            val result = loop.run()

            assertNotNull(result, "La partida $seed debe terminar con resultado")
            assertTrue(engine.state.isFinished, "El estado de la partida $seed debe estar FINISHED")
            // El resultado corresponde a una facción válida y con ganadores.
            assertTrue(result.winnerSeats.isNotEmpty(), "La partida $seed debe tener ganadores")
            assertTrue(
                result.winningRole in setOf(Role.SHERIFF, Role.OUTLAW, Role.RENEGADE),
                "Facción ganadora inválida en la partida $seed",
            )
            // Coherencia de la condición de victoria con el estado final.
            val players = engine.state.players
            when (result.winningRole) {
                Role.OUTLAW -> assertTrue(players.none { it.isAlive && it.role == Role.SHERIFF })
                Role.RENEGADE -> {
                    val alive = players.filter { it.isAlive }
                    assertTrue(alive.size == 1 && alive[0].role == Role.RENEGADE)
                }
                Role.SHERIFF -> assertTrue(
                    players.none { it.isAlive && (it.role == Role.OUTLAW || it.role == Role.RENEGADE) },
                )
                Role.DEPUTY -> error("La facción DEPUTY no puede ganar por sí sola")
            }
        }
    }

    @Test
    fun `misma semilla produce la misma partida (determinismo)`() = runTest {
        val factory = DefaultGameEngineFactory()

        suspend fun playOnce(): List<Int> {
            val engine = factory.create(configFor(5, 12345L))
            val agents = (0 until 5).associateWith { seat -> RandomAgent(777L + seat) as PlayerAgent }
            GameLoop(engine, agents, maxIllegalRetries = 2).run()
            return engine.state.players.map { it.health } + listOf(engine.state.turnNumber)
        }

        assertTrue(playOnce() == playOnce(), "Dos ejecuciones con la misma semilla deben coincidir")
    }
}
