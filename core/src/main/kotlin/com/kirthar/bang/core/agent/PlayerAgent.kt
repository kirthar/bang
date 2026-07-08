package com.kirthar.bang.core.agent

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView

/**
 * Un jugador desde el punto de vista del motor: quien decide qué comando enviar
 * cuando le toca actuar. Implementaciones:
 * - IA local (`ai`): estrategia aleatoria/heurística/MCTS.
 * - Humano local (`app`): la UI completa la decisión de forma asíncrona.
 * - Jugador remoto (futuro online): la decisión llega por [com.kirthar.bang.core.transport.GameTransport].
 */
interface PlayerAgent {

    /**
     * Decide el comando a enviar. [request] siempre incluye las opciones legales;
     * devolver un comando ilegal es un error del agente (el motor lo rechazará y se
     * le volverá a preguntar).
     */
    suspend fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand

    /**
     * Notificación de un evento ya redactado para este asiento. Útil para que las IAs
     * mantengan memoria (conteo de cartas, sospechas de rol) y para la UI.
     */
    fun onEvent(event: GameEvent) {}
}
