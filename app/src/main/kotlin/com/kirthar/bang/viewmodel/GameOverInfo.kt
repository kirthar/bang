package com.kirthar.bang.viewmodel

import com.kirthar.bang.core.model.GameResult
import com.kirthar.bang.core.view.PublicPlayerInfo

/** Información necesaria para [com.kirthar.bang.ui.screens.GameOverScreen]. */
data class GameOverInfo(
    val result: GameResult,
    /** Todos los jugadores con su rol ya revelado. */
    val players: List<PublicPlayerInfo>,
)
