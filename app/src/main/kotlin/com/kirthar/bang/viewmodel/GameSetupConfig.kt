package com.kirthar.bang.viewmodel

import com.kirthar.bang.ai.AiDifficulty
import com.kirthar.bang.core.model.CharacterId

/**
 * Configuración elegida por el jugador humano en [com.kirthar.bang.ui.screens.SetupScreen],
 * antes de crear la partida real ([com.kirthar.bang.core.model.GameConfig]).
 *
 * @param playerCount número total de jugadores (4-7), humano incluido.
 * @param difficulty dificultad aplicada a todas las IAs de la partida.
 * @param characterId personaje elegido por el humano, o `null` para asignación aleatoria.
 * @param playerName nombre del jugador humano (asiento 0).
 */
data class GameSetupConfig(
    val playerCount: Int,
    val difficulty: AiDifficulty,
    val characterId: CharacterId?,
    val playerName: String,
) {
    companion object {
        val DEFAULT = GameSetupConfig(
            playerCount = 5,
            difficulty = AiDifficulty.MEDIUM,
            characterId = null,
            playerName = "",
        )
    }
}
