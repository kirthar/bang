package com.kirthar.bang.core.model

/** Fase del turno en curso. */
enum class GamePhase {
    /** Resolución de Dinamita/Cárcel y robo de cartas al inicio del turno. */
    DRAW,

    /** El jugador activo juega cartas (o resuelve reacciones pendientes). */
    PLAY,

    /** Descarte hasta el límite de mano (= vidas actuales) al final del turno. */
    DISCARD,

    /** La partida ha terminado. */
    FINISHED,
}

/**
 * Estado inmutable de un jugador. Las cartas azules en juego (equipo, arma, Cárcel,
 * Dinamita) están en [inPlay]; el arma activa, si la hay, también aparece ahí.
 */
data class PlayerState(
    val seat: Int,
    val name: String,
    val character: CharacterId,
    val role: Role,
    val health: Int,
    val maxHealth: Int,
    val hand: List<Card>,
    val inPlay: List<Card>,
    val isAlive: Boolean = true,
) {
    val weapon: Card? get() = inPlay.firstOrNull { it.kind.isWeapon }
    val weaponRange: Int get() = weapon?.kind?.weaponRange ?: DEFAULT_WEAPON_RANGE
    val isSheriff: Boolean get() = role == Role.SHERIFF
    fun hasInPlay(kind: CardKind): Boolean = inPlay.any { it.kind == kind }
}

/**
 * Interacción pendiente de resolver (reacciones, elecciones, duelo…).
 *
 * Interfaz abierta dentro del módulo: el motor define sus implementaciones concretas
 * en `com.kirthar.bang.core.engine` (no puede ser `sealed`, pues sus subtipos viven en
 * otro paquete). El estado las transporta para que la partida sea serializable/
 * restaurable (clave para el online y para la determinización de MCTS).
 */
interface PendingInteraction {
    /** Asiento que debe actuar ahora para resolver esta interacción. */
    val awaitingSeat: Int
}

/**
 * Estado completo y autoritativo de la partida. Solo el motor (o un futuro servidor)
 * debe verlo entero; los jugadores y las IAs reciben proyecciones [com.kirthar.bang.core.view.PlayerGameView].
 */
data class GameState(
    val players: List<PlayerState>,
    val deck: List<Card>,
    val discardPile: List<Card>,
    val currentSeat: Int,
    val phase: GamePhase,
    /** Pila de interacciones pendientes; la última entrada es la más prioritaria. */
    val pending: List<PendingInteraction>,
    /** BANG! jugados por el jugador activo en este turno (límite 1 salvo Volcanic/Willy the Kid). */
    val bangsPlayedThisTurn: Int = 0,
    val turnNumber: Int = 1,
    val result: GameResult? = null,
) {
    fun player(seat: Int): PlayerState = players[seat]
    val alivePlayers: List<PlayerState> get() = players.filter { it.isAlive }
    val isFinished: Boolean get() = phase == GamePhase.FINISHED
}

/** Configuración inicial de un jugador. */
data class PlayerSetup(
    val name: String,
    /** Personaje concreto o `null` para asignación aleatoria. */
    val character: CharacterId? = null,
)

/**
 * Configuración de la partida. Con la misma [seed] y los mismos comandos, la partida
 * es totalmente reproducible (requisito para el juego online y para las simulaciones).
 */
data class GameConfig(
    val players: List<PlayerSetup>,
    val seed: Long,
) {
    init {
        require(players.size in 4..7) { "BANG! se juega de 4 a 7 jugadores" }
    }
}

/** Resultado final de la partida. */
data class GameResult(
    /** Equipo ganador: SHERIFF cubre a Sheriff+Alguaciles; OUTLAW a los Forajidos; RENEGADE al Renegado. */
    val winningRole: Role,
    val winnerSeats: List<Int>,
)
