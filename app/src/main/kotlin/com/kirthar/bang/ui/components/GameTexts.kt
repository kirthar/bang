package com.kirthar.bang.ui.components

import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit

// PLACEHOLDER: lo sustituye el agente de arte con los textos/ilustraciones definitivos
// (misma firma pública, para que el resto de la UI no cambie).
/** Textos en español del vocabulario del juego (cartas, personajes, roles, palos). */
object GameTexts {

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
        CardKind.REV_CARABINE -> "Carabina Rev."
        CardKind.WINCHESTER -> "Winchester"
    }

    fun cardDescription(kind: CardKind): String = when (kind) {
        CardKind.BANG -> "Dispara a un jugador a tu alcance. Se cancela con ¡Fallaste!"
        CardKind.MISSED -> "Cancela un ¡BANG! recibido. Solo se juega como reacción."
        CardKind.BEER -> "Recupera 1 vida (sin efecto con solo 2 jugadores vivos)."
        CardKind.PANIC -> "Roba una carta a un jugador a distancia 1."
        CardKind.CAT_BALOU -> "Obliga a descartar una carta a cualquier jugador."
        CardKind.STAGECOACH -> "Roba 2 cartas del mazo."
        CardKind.WELLS_FARGO -> "Roba 3 cartas del mazo."
        CardKind.GATLING -> "¡BANG! contra todos los demás jugadores."
        CardKind.INDIANS -> "Todos descartan un ¡BANG! o pierden 1 vida."
        CardKind.DUEL -> "Reta a duelo: se alterna descartando ¡BANG! hasta que alguien no pueda."
        CardKind.GENERAL_STORE -> "Se revelan tantas cartas como jugadores vivos; cada uno toma una."
        CardKind.SALOON -> "Todos los jugadores vivos recuperan 1 vida."
        CardKind.JAIL -> "Se juega sobre otro jugador: en su turno hace «¡desenfunda!» para actuar."
        CardKind.DYNAMITE -> "Circula entre jugadores; en «¡desenfunda!» puede explotar."
        CardKind.BARREL -> "En «¡desenfunda!», con Corazones cancela un ¡BANG!."
        CardKind.MUSTANG -> "Los demás te ven a distancia +1."
        CardKind.SCOPE -> "Ves a los demás a distancia -1."
        CardKind.VOLCANIC -> "Arma de alcance 1. Permite jugar ¡BANG! ilimitados."
        CardKind.SCHOFIELD -> "Arma de alcance 2."
        CardKind.REMINGTON -> "Arma de alcance 3."
        CardKind.REV_CARABINE -> "Arma de alcance 4."
        CardKind.WINCHESTER -> "Arma de alcance 5."
    }

    fun characterName(c: CharacterId): String = c.displayName

    fun characterDescription(c: CharacterId): String = when (c) {
        CharacterId.BART_CASSIDY -> "Cada vez que pierde una vida, roba 1 carta del mazo."
        CharacterId.BLACK_JACK -> "Enseña la 2ª carta que roba; si es Corazones o Diamantes, roba una más."
        CharacterId.CALAMITY_JANET -> "Puede jugar sus ¡BANG! como ¡Fallaste! y viceversa."
        CharacterId.EL_GRINGO -> "Cuando un jugador le hace perder vida, le roba 1 carta de la mano."
        CharacterId.JESSE_JONES -> "Puede robar su 1ª carta de la mano de otro jugador al azar."
        CharacterId.JOURDONNAIS -> "Se considera que siempre tiene un Barril en juego."
        CharacterId.KIT_CARLSON -> "Mira las 3 primeras cartas del mazo y se queda con 2."
        CharacterId.LUCKY_DUKE -> "En cada «¡desenfunda!» voltea 2 cartas y elige la mejor."
        CharacterId.PAUL_REGRET -> "Los demás lo ven a distancia +1."
        CharacterId.PEDRO_RAMIREZ -> "Puede robar su 1ª carta de la cima de los descartes."
        CharacterId.ROSE_DOOLAN -> "Ve a los demás a distancia -1."
        CharacterId.SID_KETCHUM -> "Puede descartar 2 cartas para recuperar 1 vida."
        CharacterId.SLAB_THE_KILLER -> "Hacen falta 2 ¡Fallaste! para cancelar sus ¡BANG!."
        CharacterId.SUZY_LAFAYETTE -> "En cuanto se queda sin cartas en mano, roba 1 del mazo."
        CharacterId.VULTURE_SAM -> "Cuando un jugador es eliminado, toma todas sus cartas."
        CharacterId.WILLY_THE_KID -> "Puede jugar tantos ¡BANG! como quiera en su turno."
    }

    fun roleName(r: Role): String = when (r) {
        Role.SHERIFF -> "Sheriff"
        Role.DEPUTY -> "Alguacil"
        Role.OUTLAW -> "Forajido"
        Role.RENEGADE -> "Renegado"
    }

    fun suitSymbol(s: Suit): String = when (s) {
        Suit.SPADES -> "♠"
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
    }
}
