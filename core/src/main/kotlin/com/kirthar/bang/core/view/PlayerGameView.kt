package com.kirthar.bang.core.view

import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.model.Role

/**
 * Información pública de un jugador tal y como la ve otro jugador concreto.
 */
data class PublicPlayerInfo(
    val seat: Int,
    val name: String,
    val character: CharacterId,
    val health: Int,
    val maxHealth: Int,
    val handCount: Int,
    val inPlay: List<Card>,
    /** Rol si es público para el observador: Sheriff siempre, eliminados, o uno mismo. */
    val role: Role?,
    val isAlive: Boolean,
    /**
     * Distancia efectiva desde el observador hasta este jugador (asientos + Mustang/
     * Mira/habilidades ya aplicados). 0 para uno mismo; `null` si alguno está eliminado.
     */
    val distance: Int?,
)

/**
 * Proyección del estado de la partida para un asiento concreto: lo único que un
 * jugador (humano, IA o cliente remoto) tiene derecho a ver. Nunca contiene las
 * manos ni los roles ocultos de los demás.
 */
data class PlayerGameView(
    val mySeat: Int,
    val myRole: Role,
    val myHand: List<Card>,
    /** Todos los jugadores en orden de asiento (incluido el propio). */
    val players: List<PublicPlayerInfo>,
    val deckSize: Int,
    val discardTop: Card?,
    val currentTurnSeat: Int,
    val phase: GamePhase,
    val turnNumber: Int,
    /** Decisión que este asiento tiene pendiente, o `null` si no le toca actuar. */
    val myPendingRequest: DecisionRequest?,
    val result: GameResult? = null,
) {
    val me: PublicPlayerInfo get() = players[mySeat]
}

/**
 * Petición de decisión que el motor dirige a un asiento. Toda petición incluye
 * [options], la lista completa de comandos legales en ese momento: una IA aleatoria
 * puede limitarse a elegir uno, y la UI puede habilitar exactamente esas acciones.
 */
sealed interface DecisionRequest {
    val options: List<GameCommand>

    /** Es tu fase de juego: jugar una carta, usar habilidad o pasar. */
    data class PlayOrPass(override val options: List<GameCommand>) : DecisionRequest

    /**
     * Debes reaccionar a un ataque: BANG!/Gatling/Indios/Duelo.
     *
     * @param missesNeeded ¡Fallaste! necesarios (2 contra Slab the Killer).
     */
    data class React(
        val kind: CardKind,
        val sourceSeat: Int,
        val missesNeeded: Int,
        override val options: List<GameCommand>,
    ) : DecisionRequest

    /** Debes descartar hasta el límite de mano. */
    data class DiscardToHandLimit(
        val excess: Int,
        override val options: List<GameCommand>,
    ) : DecisionRequest

    /** Elige una carta entre las ofrecidas (Emporio, Kit Carlson, Lucky Duke…). */
    data class PickCard(
        val purpose: PickPurpose,
        val cards: List<Card>,
        override val options: List<GameCommand>,
    ) : DecisionRequest

    /** Elige de dónde robar en la fase de robar (Jesse Jones, Pedro Ramírez). */
    data class ChooseDraw(
        val sources: List<DrawSource>,
        override val options: List<GameCommand>,
    ) : DecisionRequest
}

enum class PickPurpose {
    GENERAL_STORE,   // Emporio
    KIT_CARLSON,     // elegir 2 de 3 (se pide de una en una)
    LUCKY_DUKE,      // elegir la carta del «¡desenfunda!»
    PANIC_TARGET,    // elegir qué carta en juego robar/descartar (Panic!/Cat Balou)
}
