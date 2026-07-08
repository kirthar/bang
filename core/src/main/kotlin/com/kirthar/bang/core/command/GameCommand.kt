package com.kirthar.bang.core.command

/**
 * Origen de un robo de carta fuera del mazo estándar (habilidades de Jesse Jones
 * y Pedro Ramírez).
 */
sealed interface DrawSource {
    data object Deck : DrawSource
    data object DiscardPile : DrawSource
    data class PlayerHand(val seat: Int) : DrawSource
}

/**
 * Comando que un jugador (humano, IA o remoto) envía al motor. El motor valida cada
 * comando contra el estado y lo rechaza si es ilegal, por lo que el mismo canal sirve
 * para el juego local y para el online (el servidor nunca confía en el cliente).
 *
 * Las cartas se referencian siempre por su [com.kirthar.bang.core.model.Card.id].
 */
sealed interface GameCommand {

    /**
     * Jugar una carta de la mano durante la fase de juego.
     *
     * @param targetSeat objetivo, si la carta lo requiere (BANG!, Duelo, Cárcel, Panic!, Cat Balou…).
     * @param targetCardId para Panic!/Cat Balou: carta concreta en juego del objetivo, o `null`
     *   para robar/descartar una carta al azar de su mano.
     */
    data class PlayCard(
        val cardId: Int,
        val targetSeat: Int? = null,
        val targetCardId: Int? = null,
    ) : GameCommand

    /**
     * Responder a una interacción pendiente con cartas (¡Fallaste! contra un BANG!,
     * BANG! en un Duelo/Indios, segundo ¡Fallaste! contra Slab the Killer…).
     */
    data class Respond(val cardIds: List<Int>) : GameCommand

    /** No responder a la interacción pendiente y asumir el efecto (perder vida, etc.). */
    data object TakeHit : GameCommand

    /** Dar por terminada la fase de juego y pasar al descarte/fin de turno. */
    data object EndPlayPhase : GameCommand

    /** Descartar cartas (fase de descarte hasta el límite de mano). */
    data class Discard(val cardIds: List<Int>) : GameCommand

    /**
     * Elegir una carta entre varias ofrecidas: Emporio, Kit Carlson,
     * Lucky Duke en «¡desenfunda!», carta en juego robada con Panic!…
     */
    data class ChooseCard(val cardId: Int) : GameCommand

    /**
     * Elegir el origen del robo en la fase de robar (habilidades de Jesse Jones y
     * Pedro Ramírez). [cardId] identifica la carta concreta si el origen lo permite.
     */
    data class ChooseDrawSource(val source: DrawSource, val cardId: Int? = null) : GameCommand

    /**
     * Usar la habilidad activa del personaje. Único caso en el juego base:
     * Sid Ketchum descarta 2 cartas ([cardIds]) para recuperar 1 vida.
     */
    data class UseCharacterAbility(val cardIds: List<Int> = emptyList()) : GameCommand
}
