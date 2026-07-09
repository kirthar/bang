package com.kirthar.bang.core.ability

import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CharacterId

/**
 * Habilidad de personaje como estrategia intercambiable (extensible a expansiones).
 *
 * Las habilidades «pasivas» se expresan como modificadores estáticos; las reactivas,
 * como hooks que reciben un [AbilityContext] con las operaciones que el motor les
 * permite realizar. El motor registra una implementación por [CharacterId].
 */
interface CharacterAbility {
    val character: CharacterId

    // ---- Modificadores pasivos ----

    /** Cuánto más lejos te ven los demás (Paul Regret: +1). */
    fun distanceModifierAsTarget(): Int = 0

    /** Cuánto más cerca ves a los demás (Rose Doolan: -1). */
    fun distanceModifierAsViewer(): Int = 0

    /** BANG! máximos por turno ([Int.MAX_VALUE] para Willy the Kid). */
    fun bangsPerTurn(): Int = 1

    /** ¡Fallaste! necesarios para cancelar sus BANG! (Slab the Killer: 2). */
    fun missesRequiredForHisBang(): Int = 1

    /** Calamity Janet: jugar BANG! como ¡Fallaste! y viceversa. */
    fun canSwapBangAndMissed(): Boolean = false

    /** Jourdonnais: Barril innato. */
    fun hasInnateBarrel(): Boolean = false

    /** Lucky Duke: voltea 2 cartas en cada «¡desenfunda!» y elige. */
    fun drawCheckCardCount(): Int = 1

    /** Sid Ketchum: puede descartar 2 cartas para recuperar 1 vida. */
    fun canDiscardTwoForHealth(): Boolean = false

    // ---- Hooks reactivos (el motor los invoca en el momento adecuado) ----

    /**
     * Sustituye la fase de robar estándar si devuelve `true` (Black Jack, Jesse Jones,
     * Kit Carlson, Pedro Ramírez). Si devuelve `false`, el motor roba 2 del mazo.
     */
    fun onDrawPhase(ctx: AbilityContext): Boolean = false

    /** Tras perder [amount] puntos de vida a manos de [sourceSeat] (Bart Cassidy, El Gringo). */
    fun onDamaged(ctx: AbilityContext, amount: Int, sourceSeat: Int?) {}

    /** Cuando su mano queda vacía (Suzy Lafayette). */
    fun onHandEmptied(ctx: AbilityContext) {}

    /** Cuando cualquier otro jugador es eliminado (Vulture Sam). */
    fun onPlayerEliminated(ctx: AbilityContext, eliminatedSeat: Int) {}
}

/**
 * Operaciones que el motor pone a disposición de las habilidades. La implementación
 * vive en el motor (`core/engine`); las habilidades nunca tocan el estado directamente.
 */
interface AbilityContext {
    /** Asiento del dueño de la habilidad. */
    val selfSeat: Int

    /** Roba [count] cartas del mazo a la mano del dueño. [revealed] las muestra a todos (Black Jack). */
    fun drawFromDeck(count: Int, revealed: Boolean = false): List<Card>

    /** Roba una carta al azar de la mano de [fromSeat] (El Gringo). */
    fun stealRandomFromHand(fromSeat: Int): Card?

    /** Toma todas las cartas (mano + en juego) del jugador eliminado (Vulture Sam). */
    fun takeAllCardsFrom(seat: Int)

    /**
     * Mira (sin retirarlas) las [count] cartas de la cima del mazo, rebarajando los
     * descartes si hiciera falta (Kit Carlson). Extensión de Fase 1 respecto al
     * contrato original de [AbilityContext]; documentada en el informe final.
     */
    fun deckPeek(count: Int): List<Card>

    /**
     * Pide al dueño una decisión intermedia (Jesse Jones: ¿de quién robo?;
     * Kit Carlson: ¿qué 2 me quedo?). El motor la encola como interacción pendiente.
     */
    fun requestDecision(interaction: com.kirthar.bang.core.model.PendingInteraction)
}
