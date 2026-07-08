package com.kirthar.bang.core.engine

import com.kirthar.bang.core.agent.PlayerAgent
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.event.redactFor
import com.kirthar.bang.core.model.GameResult

/**
 * Bucle de partida: consulta al agente al que le toca decidir, envía su comando al
 * motor y reparte los eventos (redactados por asiento). Sirve tanto para partidas
 * locales (humano + IAs) como para simulaciones headless IA-vs-IA.
 *
 * @param onEvent callback global con el evento sin redactar (para la UI del anfitrión
 *   o el log del servidor); los agentes reciben la versión redactada vía [PlayerAgent.onEvent].
 * @param maxIllegalRetries reintentos permitidos a un agente que devuelve comandos
 *   ilegales antes de forzar la primera opción legal (protege de IAs con errores).
 */
class GameLoop(
    private val engine: GameEngine,
    private val agents: Map<Int, PlayerAgent>,
    private val onEvent: (GameEvent) -> Unit = {},
    private val maxIllegalRetries: Int = 3,
) {

    /** Ejecuta la partida hasta el final y devuelve el resultado. */
    suspend fun run(): GameResult {
        while (true) {
            val (seat, request) = engine.pendingDecision()
                ?: return checkNotNull(engine.state.result) {
                    "La partida terminó sin resultado: estado inconsistente"
                }
            val agent = checkNotNull(agents[seat]) { "No hay agente para el asiento $seat" }
            val view = engine.viewFor(seat)

            var attempts = 0
            var accepted: CommandResult.Accepted? = null
            while (accepted == null) {
                val command = if (attempts < maxIllegalRetries) {
                    agent.decide(view, request)
                } else {
                    request.options.first()
                }
                when (val result = engine.submit(seat, command)) {
                    is CommandResult.Accepted -> accepted = result
                    is CommandResult.Rejected -> attempts++
                }
            }

            accepted.events.forEach { event ->
                onEvent(event)
                agents.forEach { (agentSeat, a) -> a.onEvent(event.redactFor(agentSeat)) }
            }
        }
    }
}
