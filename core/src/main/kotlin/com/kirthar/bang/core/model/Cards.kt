package com.kirthar.bang.core.model

/** Palo de la carta (relevante para la mecánica de «¡desenfunda!»). */
enum class Suit {
    SPADES, HEARTS, DIAMONDS, CLUBS,
}

/** Valor de la carta, ordenado de menor a mayor. */
enum class Rank(val display: String) {
    TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"),
    EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K"), ACE("A"),
}

/**
 * Categoría de carta según las reglas:
 * - [BROWN]: efecto inmediato, se juega y se descarta (borde marrón).
 * - [BLUE]: permanece en juego delante del jugador (borde azul: equipo, armas, Cárcel, Dinamita).
 */
enum class CardCategory {
    BROWN, BLUE,
}

/**
 * Todos los tipos de carta del juego base.
 *
 * @property category marrón (efecto inmediato) o azul (permanece en juego).
 * @property weaponRange alcance si la carta es un arma; `null` si no lo es.
 */
enum class CardKind(val category: CardCategory, val weaponRange: Int? = null) {
    // Cartas marrones
    BANG(CardCategory.BROWN),
    MISSED(CardCategory.BROWN),
    BEER(CardCategory.BROWN),
    PANIC(CardCategory.BROWN),          // roba una carta a un jugador a distancia 1
    CAT_BALOU(CardCategory.BROWN),      // descarta una carta de cualquier jugador
    STAGECOACH(CardCategory.BROWN),     // Diligencia: roba 2 cartas
    WELLS_FARGO(CardCategory.BROWN),    // roba 3 cartas
    GATLING(CardCategory.BROWN),        // un BANG! a todos los demás
    INDIANS(CardCategory.BROWN),        // ¡Indios!: todos descartan un BANG! o pierden 1 vida
    DUEL(CardCategory.BROWN),           // Duelo: alternar BANG! o perder 1 vida
    GENERAL_STORE(CardCategory.BROWN),  // Emporio: cada jugador elige una carta
    SALOON(CardCategory.BROWN),         // Salón: todos recuperan 1 vida

    // Cartas azules (permanecen en juego)
    JAIL(CardCategory.BLUE),            // Cárcel: se juega sobre otro jugador
    DYNAMITE(CardCategory.BLUE),        // Dinamita: circula entre los jugadores
    BARREL(CardCategory.BLUE),          // Barril: «¡desenfunda!» corazón = Fallaste
    MUSTANG(CardCategory.BLUE),         // los demás te ven a distancia +1
    SCOPE(CardCategory.BLUE),           // Mira: ves a los demás a distancia -1

    // Armas (azules). El arma por defecto es el Colt .45 (alcance 1), que no es una carta.
    VOLCANIC(CardCategory.BLUE, weaponRange = 1),   // permite BANG! ilimitados
    SCHOFIELD(CardCategory.BLUE, weaponRange = 2),
    REMINGTON(CardCategory.BLUE, weaponRange = 3),
    REV_CARABINE(CardCategory.BLUE, weaponRange = 4),
    WINCHESTER(CardCategory.BLUE, weaponRange = 5),
    ;

    val isWeapon: Boolean get() = weaponRange != null
}

/** Alcance del arma por defecto (Colt .45), que todo jugador tiene siempre. */
const val DEFAULT_WEAPON_RANGE: Int = 1

/**
 * Una carta concreta del mazo.
 *
 * @property id identificador único y estable dentro de la partida (0..79 en el mazo base).
 */
data class Card(
    val id: Int,
    val kind: CardKind,
    val suit: Suit,
    val rank: Rank,
)
