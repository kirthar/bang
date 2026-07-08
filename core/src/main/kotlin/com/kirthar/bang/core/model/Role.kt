package com.kirthar.bang.core.model

/**
 * Rol secreto de cada jugador. Solo el del Sheriff es público desde el inicio;
 * los demás se revelan al morir el jugador (o al terminar la partida).
 */
enum class Role {
    SHERIFF,   // gana si mueren todos los Forajidos y el Renegado
    DEPUTY,    // Alguacil: gana con el Sheriff
    OUTLAW,    // Forajido: gana si muere el Sheriff
    RENEGADE,  // Renegado: gana si es el último en pie
}

/**
 * Reparto de roles según el número de jugadores (4-7), tal y como indican las reglas.
 */
fun rolesFor(playerCount: Int): List<Role> = when (playerCount) {
    4 -> listOf(Role.SHERIFF, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW)
    5 -> listOf(Role.SHERIFF, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW, Role.DEPUTY)
    6 -> listOf(Role.SHERIFF, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW, Role.OUTLAW, Role.DEPUTY)
    7 -> listOf(
        Role.SHERIFF, Role.RENEGADE, Role.OUTLAW, Role.OUTLAW, Role.OUTLAW,
        Role.DEPUTY, Role.DEPUTY,
    )
    else -> throw IllegalArgumentException("BANG! se juega de 4 a 7 jugadores (recibido: $playerCount)")
}
