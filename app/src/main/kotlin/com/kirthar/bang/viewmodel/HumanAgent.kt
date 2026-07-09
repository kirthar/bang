package com.kirthar.bang.viewmodel

import com.kirthar.bang.core.agent.PlayerAgent
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import kotlinx.coroutines.CompletableDeferred

/**
 * Agente del jugador humano: en vez de decidir por su cuenta, expone la decisión
 * pendiente a la UI (a través de [PlayerGameView.myPendingRequest], que
 * [GameViewModel] republica como `StateFlow`) y se queda esperando en un
 * [CompletableDeferred] hasta que la UI llama a [submit] con el comando elegido.
 *
 * No implementa [onEvent]: [GameViewModel] ya recibe todos los eventos globales por el
 * callback de [com.kirthar.bang.core.engine.GameLoop] y actualiza su propio estado ahí,
 * así que este agente no necesita memoria propia.
 */
class HumanAgent : PlayerAgent {

    private var pending: CompletableDeferred<GameCommand>? = null

    override suspend fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand {
        val deferred = CompletableDeferred<GameCommand>()
        pending = deferred
        return deferred.await()
    }

    override fun onEvent(event: GameEvent) = Unit

    /**
     * Entrega el comando elegido por la UI para la decisión pendiente actual.
     * Devuelve `false` si no hay ninguna decisión pendiente en este momento (p. ej.
     * doble toque accidental mientras el motor ya está procesando la anterior).
     */
    fun submit(command: GameCommand): Boolean {
        val deferred = pending ?: return false
        pending = null
        return deferred.complete(command)
    }
}
