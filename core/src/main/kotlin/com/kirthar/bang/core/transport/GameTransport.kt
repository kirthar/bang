package com.kirthar.bang.core.transport

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.view.PlayerGameView
import kotlinx.coroutines.flow.Flow

/**
 * Canal de comunicación entre un cliente (un asiento) y la partida autoritativa.
 *
 * Hoy solo existe la implementación local en proceso; en el online futuro habrá una
 * implementación WebSocket/Firebase con exactamente este mismo contrato, de modo que
 * ni la UI ni las IAs noten la diferencia entre partida local y remota.
 */
interface GameTransport {

    /** Asiento que este canal representa. */
    val seat: Int

    /** Envía un comando del jugador a la partida. */
    suspend fun sendCommand(command: GameCommand)

    /** Eventos de la partida, ya redactados para este asiento. */
    val events: Flow<GameEvent>

    /** Vista actualizada tras cada cambio de estado, proyectada para este asiento. */
    val views: Flow<PlayerGameView>
}
