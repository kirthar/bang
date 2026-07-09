package com.kirthar.bang.core.engine

import com.kirthar.bang.core.event.DrawCheckReason
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.PendingInteraction

/**
 * Implementaciones concretas de [PendingInteraction] que usa [BangGameEngine].
 *
 * El contrato declara [PendingInteraction] como interfaz sellada «abierta» dentro del
 * módulo: el motor define aquí sus subtipos para transportar en el estado toda la
 * información necesaria para reanudar una resolución en curso (reacciones a un BANG!,
 * elecciones de habilidad, duelos, emporio, salvación con Birra, etc.).
 *
 * Cada subtipo lleva únicamente datos serializables; la lógica de resolución vive en
 * el motor. Las reanudaciones puramente secuenciales del inicio de turno (Dinamita →
 * Cárcel → robo) se llevan con estado interno del motor, no aquí.
 */

/** Reacción de un jugador a un BANG! (o a un disparo de Gatling) dirigido contra él. */
internal data class BangReactionInteraction(
    override val awaitingSeat: Int,
    val sourceSeat: Int,
    /** ¡Fallaste! necesarios en total para cancelar (2 contra Slab the Killer). */
    val missesNeeded: Int,
    /** ¡Fallaste! ya acumulados (por Barril o respuestas previas). */
    val missesSoFar: Int,
    /** Intentos de «¡desenfunda!» de Barril aún por resolver. */
    val barrelTriesLeft: Int,
    /** `true` si procede de un Gatling (para el texto/animación; no afecta a la regla). */
    val fromGatling: Boolean,
) : PendingInteraction

/** Reacción a ¡Indios!: descartar un BANG! o perder 1 vida. */
internal data class IndiansReactionInteraction(
    override val awaitingSeat: Int,
    val sourceSeat: Int,
) : PendingInteraction

/** Turno de un jugador dentro de un Duelo: jugar un BANG! o perder 1 vida. */
internal data class DuelInteraction(
    override val awaitingSeat: Int,
    val opponentSeat: Int,
) : PendingInteraction

/** Emporio: [awaitingSeat] elige una de las [cards] reveladas. */
internal data class GeneralStoreInteraction(
    override val awaitingSeat: Int,
    /** Asientos que aún deben elegir, empezando por [awaitingSeat]. */
    val remainingSeats: List<Int>,
    val cards: List<Card>,
) : PendingInteraction

/**
 * Golpe mortal: [awaitingSeat] puede jugar Birras (o usar a Sid Ketchum) para volver a
 * ≥1 vida; si no, queda eliminado. [sourceSeat] es quien causó el golpe (para recompensas).
 */
internal data class DeathSaveInteraction(
    override val awaitingSeat: Int,
    val sourceSeat: Int?,
) : PendingInteraction

/** Kit Carlson: mira 3 cartas, se queda 2 y devuelve 1 al mazo (se pide de una en una). */
internal data class KitCarlsonInteraction(
    override val awaitingSeat: Int,
    /** Cartas aún disponibles para elegir de las 3 miradas. */
    val pool: List<Card>,
    /** Cuántas se ha quedado ya (termina al quedarse 2). */
    val keptSoFar: Int,
) : PendingInteraction

/** Jesse Jones: elige si roba su 1ª carta del mazo o de la mano de un jugador. */
internal data class JesseJonesInteraction(
    override val awaitingSeat: Int,
) : PendingInteraction

/** Pedro Ramírez: elige si roba su 1ª carta del mazo o de la cima de los descartes. */
internal data class PedroRamirezInteraction(
    override val awaitingSeat: Int,
) : PendingInteraction

/** Contexto que indica qué efecto reanudar tras un «¡desenfunda!» de Lucky Duke. */
internal sealed interface LuckyDukeContext {
    data object Dynamite : LuckyDukeContext
    data object Jail : LuckyDukeContext
    /** Barril: se aplica a la [BangReactionInteraction] inmediatamente inferior en la pila. */
    data object Barrel : LuckyDukeContext
}

/**
 * Lucky Duke: en cada «¡desenfunda!» voltea 2 cartas ([flipped]) y elige cuál cuenta;
 * ambas se descartan. [context] indica qué resolución reanudar con la carta elegida.
 */
internal data class LuckyDukeInteraction(
    override val awaitingSeat: Int,
    val reason: DrawCheckReason,
    val flipped: List<Card>,
    val context: LuckyDukeContext,
) : PendingInteraction
