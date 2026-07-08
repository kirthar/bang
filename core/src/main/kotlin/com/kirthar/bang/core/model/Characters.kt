package com.kirthar.bang.core.model

/**
 * Los 16 personajes del juego base.
 *
 * @property baseHealth puntos de vida (balas) del personaje. El Sheriff juega con +1.
 */
enum class CharacterId(val displayName: String, val baseHealth: Int) {
    BART_CASSIDY("Bart Cassidy", 4),         // cada vez que pierde vida, roba 1 carta
    BLACK_JACK("Black Jack", 4),             // muestra la 2ª carta robada; si es ♥ o ♦, roba 1 más
    CALAMITY_JANET("Calamity Janet", 4),     // usa BANG! como ¡Fallaste! y viceversa
    EL_GRINGO("El Gringo", 3),               // cuando un jugador le hace daño, le roba 1 carta de la mano
    JESSE_JONES("Jesse Jones", 4),           // puede robar su 1ª carta de la mano de un jugador
    JOURDONNAIS("Jourdonnais", 4),           // tiene un Barril innato
    KIT_CARLSON("Kit Carlson", 4),           // mira las 3 primeras cartas y elige 2
    LUCKY_DUKE("Lucky Duke", 4),             // en cada «¡desenfunda!» voltea 2 cartas y elige
    PAUL_REGRET("Paul Regret", 3),           // los demás lo ven a distancia +1
    PEDRO_RAMIREZ("Pedro Ramírez", 4),       // puede robar su 1ª carta de los descartes
    ROSE_DOOLAN("Rose Doolan", 4),           // ve a los demás a distancia -1
    SID_KETCHUM("Sid Ketchum", 4),           // puede descartar 2 cartas para recuperar 1 vida
    SLAB_THE_KILLER("Slab the Killer", 4),   // sus BANG! necesitan 2 ¡Fallaste!
    SUZY_LAFAYETTE("Suzy Lafayette", 4),     // si se queda sin cartas en mano, roba 1
    VULTURE_SAM("Vulture Sam", 4),           // cuando muere un personaje, toma todas sus cartas
    WILLY_THE_KID("Willy the Kid", 4),       // puede jugar cualquier número de BANG!
}
