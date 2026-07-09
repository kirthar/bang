package com.kirthar.bang.core.engine

import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.PlayerSetup
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.rolesFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests de creación de partida y de la proyección [com.kirthar.bang.core.view.PlayerGameView]. */
class GameSetupTest {

    private fun config(playerCount: Int, seed: Long, fixed: Map<Int, CharacterId> = emptyMap()) =
        GameConfig((0 until playerCount).map { PlayerSetup("P$it", fixed[it]) }, seed)

    @Test
    fun `la creacion reparte roles, vidas y manos segun las reglas`() {
        for (count in 4..7) {
            val engine = DefaultGameEngineFactory().create(config(count, seed = 42L))
            val state = engine.state
            // Roles según la tabla oficial
            assertEquals(
                rolesFor(count).groupingBy { it }.eachCount(),
                state.players.groupingBy { it.role }.eachCount(),
                "Reparto de roles incorrecto con $count jugadores",
            )
            // Personajes únicos
            assertEquals(count, state.players.map { it.character }.distinct().size)
            state.players.forEach { p ->
                val expected = p.character.baseHealth + (if (p.role == Role.SHERIFF) 1 else 0)
                assertEquals(expected, p.maxHealth, "Vidas de ${p.character}")
                // El Sheriff ya ha robado sus 2 cartas del turno 1 (o las de su habilidad);
                // los demás tienen mano inicial = vidas (salvo robos de habilidad al Sheriff).
                if (p.role != Role.SHERIFF) {
                    assertTrue(p.hand.size >= p.maxHealth, "Mano inicial de P${p.seat}")
                }
            }
            // Empieza el Sheriff
            val sheriff = state.players.first { it.role == Role.SHERIFF }
            assertEquals(sheriff.seat, state.currentSeat)
            // Mazo + descartes + manos + en juego = 80 cartas
            val total = state.deck.size + state.discardPile.size +
                state.players.sumOf { it.hand.size + it.inPlay.size } +
                state.pending.filterIsInstance<GeneralStoreInteraction>().sumOf { it.cards.size }
            assertEquals(80, total)
        }
    }

    @Test
    fun `los personajes fijados en PlayerSetup se respetan`() {
        val engine = DefaultGameEngineFactory().create(
            config(4, seed = 7L, fixed = mapOf(0 to CharacterId.WILLY_THE_KID, 2 to CharacterId.ROSE_DOOLAN)),
        )
        assertEquals(CharacterId.WILLY_THE_KID, engine.state.player(0).character)
        assertEquals(CharacterId.ROSE_DOOLAN, engine.state.player(2).character)
        assertEquals(4, engine.state.players.map { it.character }.distinct().size)
    }

    @Test
    fun `misma semilla produce el mismo estado inicial`() {
        val a = DefaultGameEngineFactory().create(config(6, seed = 99L)).state
        val b = DefaultGameEngineFactory().create(config(6, seed = 99L)).state
        assertEquals(a.players, b.players)
        assertEquals(a.deck, b.deck)
    }

    @Test
    fun `viewFor oculta roles y manos ajenas`() {
        val engine = DefaultGameEngineFactory().create(config(5, seed = 3L))
        val state = engine.state
        val sheriffSeat = state.players.first { it.role == Role.SHERIFF }.seat
        val observer = (sheriffSeat + 1) % 5

        val view = engine.viewFor(observer)
        assertEquals(observer, view.mySeat)
        assertEquals(state.player(observer).hand, view.myHand)
        view.players.forEach { info ->
            when (info.seat) {
                observer -> assertEquals(state.player(observer).role, info.role)
                sheriffSeat -> assertEquals(Role.SHERIFF, info.role)
                else -> assertNull(info.role, "El rol de un vivo no Sheriff debe estar oculto")
            }
            // La distancia está precalculada y es >= 1 para los demás
            if (info.seat == observer) assertEquals(0, info.distance)
            else assertTrue(info.distance!! >= 1)
        }
    }

    @Test
    fun `el rol de un jugador muerto es publico`() {
        val bang = card(com.kirthar.bang.core.model.CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 1),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = List(6) { card(com.kirthar.bang.core.model.CardKind.BEER) }))
        engine.accept(0, com.kirthar.bang.core.command.GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, com.kirthar.bang.core.command.GameCommand.TakeHit)
        val view = engine.viewFor(3)
        assertEquals(Role.OUTLAW, view.players[1].role)
        assertNull(view.players[2].role)
        assertNull(view.players[1].distance) // eliminado: sin distancia
    }
}
