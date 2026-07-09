package com.kirthar.bang.ai.sim

import com.kirthar.bang.ai.AiAgent
import com.kirthar.bang.ai.AiDifficulty
import com.kirthar.bang.ai.AiStrategyFactory
import com.kirthar.bang.ai.DefaultAiStrategyFactory
import com.kirthar.bang.core.agent.PlayerAgent
import com.kirthar.bang.core.engine.GameEngineFactory
import com.kirthar.bang.core.engine.GameLoop
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.PlayerSetup
import com.kirthar.bang.core.model.Role

/**
 * Configuración de un asiento en la simulación: su dificultad de IA.
 */
data class SeatSetup(
    val player: PlayerSetup,
    val difficulty: AiDifficulty,
)

/**
 * Estadísticas agregadas de una tanda de simulaciones headless.
 *
 * @property gamesRequested partidas solicitadas.
 * @property gamesCompleted partidas terminadas con resultado válido.
 * @property errors partidas abortadas por excepción o estado inconsistente.
 * @property winsByRole victorias por bando ganador ([Role]).
 * @property winsByDifficulty asientos ganadores agrupados por dificultad de su IA.
 * @property averageTurns turnos medios por partida completada.
 */
data class SimulationStats(
    val gamesRequested: Int,
    val gamesCompleted: Int,
    val errors: Int,
    val winsByRole: Map<Role, Int>,
    val winsByDifficulty: Map<AiDifficulty, Int>,
    val averageTurns: Double,
    val errorMessages: List<String>,
)

/**
 * Simulador headless de partidas IA-vs-IA. Monta un [GameLoop] con [AiAgent]s sobre el
 * motor inyectado y ejecuta N partidas reproducibles, agregando estadísticas de balance.
 *
 * Es la herramienta de validación de la Fase 2 (comprobar que el motor termina y que el
 * balance entre roles/dificultades es razonable). No depende de ninguna implementación
 * concreta del motor, solo del contrato [GameEngineFactory].
 *
 * @param engineFactory fábrica del motor real.
 * @param strategyFactory fábrica de estrategias (por defecto [DefaultAiStrategyFactory]).
 */
class GameSimulator(
    private val engineFactory: GameEngineFactory,
    private val strategyFactory: AiStrategyFactory = DefaultAiStrategyFactory(engineFactory),
) {

    /**
     * Ejecuta [games] partidas con los asientos [seats]. Cada partida usa una semilla
     * derivada de [baseSeed] para ser reproducible. Las excepciones de una partida se
     * capturan y cuentan como error sin abortar la tanda.
     */
    suspend fun run(seats: List<SeatSetup>, games: Int, baseSeed: Long = 0L): SimulationStats {
        require(seats.size in 4..7) { "BANG! se juega de 4 a 7 jugadores" }

        val winsByRole = mutableMapOf<Role, Int>()
        val winsByDifficulty = mutableMapOf<AiDifficulty, Int>()
        val errorMessages = mutableListOf<String>()
        var completed = 0
        var totalTurns = 0L

        for (g in 0 until games) {
            val gameSeed = baseSeed + g
            try {
                val engine = engineFactory.create(
                    GameConfig(players = seats.map { it.player }, seed = gameSeed),
                )
                val agents: Map<Int, PlayerAgent> = seats.indices.associateWith { seat ->
                    val strategy = strategyFactory.create(seats[seat].difficulty, seatSeed(gameSeed, seat))
                    AiAgent(strategy, thinkDelayMs = 0)
                }
                val result = GameLoop(engine, agents).run()

                completed++
                totalTurns += engine.state.turnNumber
                winsByRole.merge(result.winningRole, 1, Int::plus)
                for (winnerSeat in result.winnerSeats) {
                    winsByDifficulty.merge(seats[winnerSeat].difficulty, 1, Int::plus)
                }
            } catch (e: Exception) {
                errorMessages.add("Partida $g (semilla $gameSeed): ${e.message}")
            }
        }

        return SimulationStats(
            gamesRequested = games,
            gamesCompleted = completed,
            errors = errorMessages.size,
            winsByRole = winsByRole,
            winsByDifficulty = winsByDifficulty,
            averageTurns = if (completed > 0) totalTurns.toDouble() / completed else 0.0,
            errorMessages = errorMessages,
        )
    }

    private fun seatSeed(gameSeed: Long, seat: Int): Long = gameSeed * 1_000_003L + seat
}
