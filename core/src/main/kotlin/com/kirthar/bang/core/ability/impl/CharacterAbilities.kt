package com.kirthar.bang.core.ability.impl

import com.kirthar.bang.core.ability.AbilityContext
import com.kirthar.bang.core.ability.CharacterAbility
import com.kirthar.bang.core.engine.JesseJonesInteraction
import com.kirthar.bang.core.engine.KitCarlsonInteraction
import com.kirthar.bang.core.engine.PedroRamirezInteraction
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Suit

/**
 * Implementaciones de las 16 habilidades del juego base. Las pasivas se expresan como
 * modificadores estáticos que el motor consulta; las reactivas, como hooks que el motor
 * invoca con un [AbilityContext].
 */

/** Bart Cassidy: cada punto de vida perdido le hace robar 1 carta del mazo. */
internal object BartCassidyAbility : CharacterAbility {
    override val character = CharacterId.BART_CASSIDY
    override fun onDamaged(ctx: AbilityContext, amount: Int, sourceSeat: Int?) {
        if (amount > 0) ctx.drawFromDeck(amount)
    }
}

/** Black Jack: enseña la 2ª carta robada; si es ♥ o ♦, roba una carta más (oculta). */
internal object BlackJackAbility : CharacterAbility {
    override val character = CharacterId.BLACK_JACK
    override fun onDrawPhase(ctx: AbilityContext): Boolean {
        ctx.drawFromDeck(1)
        val second = ctx.drawFromDeck(1, revealed = true).firstOrNull()
        if (second != null && (second.suit == Suit.HEARTS || second.suit == Suit.DIAMONDS)) {
            ctx.drawFromDeck(1)
        }
        return true
    }
}

/** Calamity Janet: puede jugar sus BANG! como ¡Fallaste! y viceversa. */
internal object CalamityJanetAbility : CharacterAbility {
    override val character = CharacterId.CALAMITY_JANET
    override fun canSwapBangAndMissed(): Boolean = true
}

/** El Gringo: cuando un jugador le hace daño, le roba 1 carta de la mano por cada vida perdida. */
internal object ElGringoAbility : CharacterAbility {
    override val character = CharacterId.EL_GRINGO
    override fun onDamaged(ctx: AbilityContext, amount: Int, sourceSeat: Int?) {
        if (sourceSeat == null || sourceSeat == ctx.selfSeat) return
        repeat(amount) { ctx.stealRandomFromHand(sourceSeat) }
    }
}

/** Jesse Jones: puede robar su 1ª carta de la mano de un jugador cualquiera. */
internal object JesseJonesAbility : CharacterAbility {
    override val character = CharacterId.JESSE_JONES
    override fun onDrawPhase(ctx: AbilityContext): Boolean {
        ctx.requestDecision(JesseJonesInteraction(ctx.selfSeat))
        return true
    }
}

/** Jourdonnais: se considera que tiene siempre un Barril en juego. */
internal object JourdonnaisAbility : CharacterAbility {
    override val character = CharacterId.JOURDONNAIS
    override fun hasInnateBarrel(): Boolean = true
}

/** Kit Carlson: mira las 3 primeras cartas del mazo, se queda 2 y devuelve 1. */
internal object KitCarlsonAbility : CharacterAbility {
    override val character = CharacterId.KIT_CARLSON
    override fun onDrawPhase(ctx: AbilityContext): Boolean {
        val pool = ctx.deckPeek(3)
        if (pool.isEmpty()) return false // mazo agotado: robo estándar (robará lo que pueda)
        ctx.requestDecision(KitCarlsonInteraction(ctx.selfSeat, pool, keptSoFar = 0))
        return true
    }
}

/** Lucky Duke: en cada «¡desenfunda!» voltea 2 cartas y elige la que prefiera. */
internal object LuckyDukeAbility : CharacterAbility {
    override val character = CharacterId.LUCKY_DUKE
    override fun drawCheckCardCount(): Int = 2
}

/** Paul Regret: los demás lo ven a distancia +1. */
internal object PaulRegretAbility : CharacterAbility {
    override val character = CharacterId.PAUL_REGRET
    override fun distanceModifierAsTarget(): Int = 1
}

/** Pedro Ramírez: puede robar su 1ª carta de la cima de los descartes. */
internal object PedroRamirezAbility : CharacterAbility {
    override val character = CharacterId.PEDRO_RAMIREZ
    override fun onDrawPhase(ctx: AbilityContext): Boolean {
        ctx.requestDecision(PedroRamirezInteraction(ctx.selfSeat))
        return true
    }
}

/** Rose Doolan: ve a los demás a distancia -1. */
internal object RoseDoolanAbility : CharacterAbility {
    override val character = CharacterId.ROSE_DOOLAN
    override fun distanceModifierAsViewer(): Int = -1
}

/** Sid Ketchum: puede descartar 2 cartas para recuperar 1 vida. */
internal object SidKetchumAbility : CharacterAbility {
    override val character = CharacterId.SID_KETCHUM
    override fun canDiscardTwoForHealth(): Boolean = true
}

/** Slab the Killer: cancelar sus BANG! exige 2 ¡Fallaste! (el Barril cuenta como uno). */
internal object SlabTheKillerAbility : CharacterAbility {
    override val character = CharacterId.SLAB_THE_KILLER
    override fun missesRequiredForHisBang(): Int = 2
}

/** Suzy Lafayette: en cuanto se queda sin cartas en mano, roba 1 del mazo. */
internal object SuzyLafayetteAbility : CharacterAbility {
    override val character = CharacterId.SUZY_LAFAYETTE
    override fun onHandEmptied(ctx: AbilityContext) {
        ctx.drawFromDeck(1)
    }
}

/** Vulture Sam: cuando un personaje es eliminado, toma todas sus cartas. */
internal object VultureSamAbility : CharacterAbility {
    override val character = CharacterId.VULTURE_SAM
    override fun onPlayerEliminated(ctx: AbilityContext, eliminatedSeat: Int) {
        ctx.takeAllCardsFrom(eliminatedSeat)
    }
}

/** Willy the Kid: puede jugar tantos BANG! como quiera en su turno. */
internal object WillyTheKidAbility : CharacterAbility {
    override val character = CharacterId.WILLY_THE_KID
    override fun bangsPerTurn(): Int = Int.MAX_VALUE
}

/** Habilidad neutra por defecto (sin efectos), por si faltara un registro. */
internal class DefaultAbility(override val character: CharacterId) : CharacterAbility

/** Registro de habilidades por personaje. */
internal object AbilityRegistry {
    private val abilities: Map<CharacterId, CharacterAbility> = listOf(
        BartCassidyAbility, BlackJackAbility, CalamityJanetAbility, ElGringoAbility,
        JesseJonesAbility, JourdonnaisAbility, KitCarlsonAbility, LuckyDukeAbility,
        PaulRegretAbility, PedroRamirezAbility, RoseDoolanAbility, SidKetchumAbility,
        SlabTheKillerAbility, SuzyLafayetteAbility, VultureSamAbility, WillyTheKidAbility,
    ).associateBy { it.character }

    fun forCharacter(character: CharacterId): CharacterAbility =
        abilities[character] ?: DefaultAbility(character)
}
