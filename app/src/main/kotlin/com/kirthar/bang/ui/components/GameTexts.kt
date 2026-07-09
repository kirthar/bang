package com.kirthar.bang.ui.components

import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit

/**
 * Todos los textos del juego en español, fieles a `docs/REGLAS.md`. Centralizados
 * aquí para que los componentes visuales (y las pantallas de otros subagentes)
 * nunca tengan que traducir ni inventar nombres de cartas, personajes o roles.
 */
object GameTexts {

    /** Nombre de la carta tal y como aparece en las reglas oficiales en español. */
    fun cardName(kind: CardKind): String = when (kind) {
        CardKind.BANG -> "¡BANG!"
        CardKind.MISSED -> "¡Fallaste!"
        CardKind.BEER -> "Birra"
        CardKind.PANIC -> "¡Pánico!"
        CardKind.CAT_BALOU -> "Cat Balou"
        CardKind.STAGECOACH -> "Diligencia"
        CardKind.WELLS_FARGO -> "Wells Fargo"
        CardKind.GATLING -> "Gatling"
        CardKind.INDIANS -> "¡Indios!"
        CardKind.DUEL -> "Duelo"
        CardKind.GENERAL_STORE -> "Emporio"
        CardKind.SALOON -> "Salón"
        CardKind.JAIL -> "Cárcel"
        CardKind.DYNAMITE -> "Dinamita"
        CardKind.BARREL -> "Barril"
        CardKind.MUSTANG -> "Mustang"
        CardKind.SCOPE -> "Mira"
        CardKind.VOLCANIC -> "Volcanic"
        CardKind.SCHOFIELD -> "Schofield"
        CardKind.REMINGTON -> "Remington"
        CardKind.REV_CARABINE -> "Rev. Carabine"
        CardKind.WINCHESTER -> "Winchester"
    }

    /** Descripción breve del efecto de la carta, resumida de `docs/REGLAS.md`. */
    fun cardDescription(kind: CardKind): String = when (kind) {
        CardKind.BANG -> "Dispara a un jugador a distancia igual o menor que tu alcance. Se cancela con ¡Fallaste!"
        CardKind.MISSED -> "Cancela un BANG! (o un disparo de Gatling) dirigido a ti. Solo como reacción."
        CardKind.BEER -> "Recupera 1 vida. Sin efecto si solo quedan 2 jugadores vivos."
        CardKind.PANIC -> "Roba una carta (mano al azar o en juego, a tu elección) a un jugador a distancia 1."
        CardKind.CAT_BALOU -> "Obliga a un jugador a descartar una carta (mano al azar o en juego, a tu elección). Sin límite de distancia."
        CardKind.STAGECOACH -> "Roba 2 cartas del mazo."
        CardKind.WELLS_FARGO -> "Roba 3 cartas del mazo."
        CardKind.GATLING -> "Un disparo BANG! contra todos los demás jugadores a la vez."
        CardKind.INDIANS -> "Todos los demás descartan un BANG! o pierden 1 vida. El Barril y el ¡Fallaste! no sirven."
        CardKind.DUEL -> "Reta a un jugador: alternáis descartando BANG! hasta que uno no pueda y pierda 1 vida."
        CardKind.GENERAL_STORE -> "Se revelan tantas cartas como jugadores vivos; cada uno escoge una, empezando por ti."
        CardKind.SALOON -> "Todos los jugadores vivos recuperan 1 vida (no salva de un golpe mortal)."
        CardKind.JAIL -> "Se juega delante de otro jugador (no el Sheriff). En su turno hará «¡desenfunda!»: con Corazones escapa, si no pierde el turno."
        CardKind.DYNAMITE -> "Circula entre jugadores. Al inicio de tu turno haces «¡desenfunda!»: Picas 2-9 explota (3 vidas), si no pasa a tu izquierda."
        CardKind.BARREL -> "Al recibir un BANG!, haces «¡desenfunda!»: con Corazones cuenta como un ¡Fallaste!. Un intento por disparo."
        CardKind.MUSTANG -> "Los demás jugadores te ven a distancia +1."
        CardKind.SCOPE -> "Ves a los demás jugadores a distancia -1."
        CardKind.VOLCANIC -> "Arma de alcance 1. Permite jugar tantos BANG! como quieras en tu turno."
        CardKind.SCHOFIELD -> "Arma de alcance 2."
        CardKind.REMINGTON -> "Arma de alcance 3."
        CardKind.REV_CARABINE -> "Arma de alcance 4."
        CardKind.WINCHESTER -> "Arma de alcance 5."
    }

    /** Nombre visible del personaje. */
    fun characterName(c: CharacterId): String = c.displayName

    /** Resumen de la habilidad especial del personaje, según `docs/REGLAS.md` §2. */
    fun characterDescription(c: CharacterId): String = when (c) {
        CharacterId.BART_CASSIDY -> "Cada vez que pierde un punto de vida, roba 1 carta del mazo."
        CharacterId.BLACK_JACK -> "Al robar, enseña la 2ª carta: si es Corazones o Diamantes, roba una carta más."
        CharacterId.CALAMITY_JANET -> "Puede jugar sus BANG! como ¡Fallaste! y sus ¡Fallaste! como BANG!."
        CharacterId.EL_GRINGO -> "Cada vez que un jugador le hace perder una vida, le roba 1 carta al azar de la mano."
        CharacterId.JESSE_JONES -> "Puede robar su primera carta de la mano de otro jugador, elegido al azar."
        CharacterId.JOURDONNAIS -> "Se considera que siempre tiene un Barril en juego, incluso sin tenerlo."
        CharacterId.KIT_CARLSON -> "Al robar, mira las 3 primeras cartas del mazo, se queda 2 y devuelve 1 boca abajo."
        CharacterId.LUCKY_DUKE -> "En cada «¡desenfunda!» voltea 2 cartas y elige el resultado que prefiera."
        CharacterId.PAUL_REGRET -> "Los demás jugadores lo ven a distancia +1."
        CharacterId.PEDRO_RAMIREZ -> "Puede robar su primera carta de la cima de la pila de descartes."
        CharacterId.ROSE_DOOLAN -> "Ve a los demás jugadores a distancia -1."
        CharacterId.SID_KETCHUM -> "En cualquier momento puede descartar 2 cartas de la mano para recuperar 1 vida."
        CharacterId.SLAB_THE_KILLER -> "Hacen falta 2 cartas ¡Fallaste! para cancelar uno de sus BANG!."
        CharacterId.SUZY_LAFAYETTE -> "En cuanto se queda sin cartas en la mano, roba 1 carta del mazo."
        CharacterId.VULTURE_SAM -> "Cuando un personaje es eliminado, toma todas sus cartas (mano y en juego)."
        CharacterId.WILLY_THE_KID -> "Puede jugar tantas cartas BANG! como quiera en su turno."
    }

    /** Nombre del rol secreto (o público, en el caso del Sheriff). */
    fun roleName(r: Role): String = when (r) {
        Role.SHERIFF -> "Sheriff"
        Role.DEPUTY -> "Alguacil"
        Role.OUTLAW -> "Forajido"
        Role.RENEGADE -> "Renegado"
    }

    /** Símbolo tipográfico del palo. */
    fun suitSymbol(s: Suit): String = when (s) {
        Suit.SPADES -> "♠"
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
    }
}
