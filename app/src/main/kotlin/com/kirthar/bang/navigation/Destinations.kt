package com.kirthar.bang.navigation

/** Destinos de la navegación de la aplicación: Menú -> Configurar -> Partida -> Fin de partida. */
sealed class Destinations(val route: String) {
    data object Menu : Destinations("menu")
    data object Setup : Destinations("setup")
    data object Game : Destinations("game")
    data object GameOver : Destinations("game_over")
}
