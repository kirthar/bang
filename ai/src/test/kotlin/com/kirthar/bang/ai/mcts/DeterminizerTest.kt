package com.kirthar.bang.ai.mcts

import com.kirthar.bang.ai.Fixtures
import com.kirthar.bang.ai.memory.PlayerMemory
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.rolesFor
import com.kirthar.bang.core.view.PlayerGameView
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests de consistencia de la determinización de [Determinizer]. */
class DeterminizerTest {

    /** Vista de ejemplo: 4 jugadores, yo en el asiento 1 (Forajido), Sheriff en el 0. */
    private fun sampleView(): PlayerGameView {
        val myHand = listOf(
            Fixtures.card(0, CardKind.BANG),
            Fixtures.card(1, CardKind.MISSED),
            Fixtures.card(2, CardKind.BEER),
        )
        val sheriffWeapon = Fixtures.card(10, CardKind.WINCHESTER)
        val enemyBarrel = Fixtures.card(11, CardKind.BARREL)
        val discardTop = Fixtures.card(12, CardKind.SALOON)
        val players = listOf(
            Fixtures.publicInfo(0, role = Role.SHERIFF, handCount = 4, inPlay = listOf(sheriffWeapon), health = 5, maxHealth = 5),
            Fixtures.publicInfo(1, role = Role.OUTLAW, handCount = 3, health = 3, maxHealth = 4),
            Fixtures.publicInfo(2, handCount = 2, inPlay = listOf(enemyBarrel)),
            Fixtures.publicInfo(3, handCount = 5),
        )
        return Fixtures.view(
            mySeat = 1,
            myRole = Role.OUTLAW,
            myHand = myHand,
            players = players,
            deckSize = 30,
            discardTop = discardTop,
            currentTurnSeat = 1,
        )
    }

    @Test
    fun `la mano propia queda intacta`() {
        val view = sampleView()
        val state = Determinizer(PlayerMemory()).determinize(view, Random(1))
        assertEquals(view.myHand, state.player(1).hand)
    }

    @Test
    fun `los conteos de mano, mazo y descartes son correctos`() {
        val view = sampleView()
        val state = Determinizer(PlayerMemory()).determinize(view, Random(2))

        for (p in view.players) {
            assertEquals(p.handCount, state.player(p.seat).hand.size, "mano del asiento ${p.seat}")
        }
        assertEquals(view.deckSize, state.deck.size)
        // 80 = mazo + descartes + manos + en juego
        val total = state.deck.size + state.discardPile.size +
            state.players.sumOf { it.hand.size } + state.players.sumOf { it.inPlay.size }
        assertEquals(80, total)
        // La carta superior del descarte es la visible.
        assertEquals(view.discardTop, state.discardPile.last())
    }

    @Test
    fun `no hay cartas duplicadas ni cartas vistas en zonas ocultas`() {
        val view = sampleView()
        val state = Determinizer(PlayerMemory()).determinize(view, Random(3))

        val allIds = buildList {
            addAll(state.deck.map { it.id })
            addAll(state.discardPile.map { it.id })
            state.players.forEach { p ->
                addAll(p.hand.map { it.id })
                addAll(p.inPlay.map { it.id })
            }
        }
        assertEquals(allIds.size, allIds.toSet().size, "hay cartas duplicadas")

        // Mis cartas y las cartas en juego no pueden aparecer en manos rivales ni en el mazo.
        val visibleIds = view.myHand.map { it.id } + view.players.flatMap { it.inPlay.map { c -> c.id } }
        val hiddenZones = state.deck.map { it.id } +
            state.players.filter { it.seat != 1 }.flatMap { it.hand.map { c -> c.id } }
        for (id in visibleIds) {
            assertTrue(id !in hiddenZones, "la carta vista $id apareció en una zona oculta")
        }
    }

    @Test
    fun `los roles muestreados son una permutacion legal`() {
        val view = sampleView()
        val determinizer = Determinizer(PlayerMemory())
        repeat(50) { i ->
            val state = determinizer.determinize(view, Random(i.toLong()))
            val roles = state.players.map { it.role }.sorted()
            assertEquals(rolesFor(4).sorted(), roles, "reparto ilegal en la iteración $i")
            // Los roles conocidos se respetan.
            assertEquals(Role.SHERIFF, state.player(0).role)
            assertEquals(Role.OUTLAW, state.player(1).role)
        }
    }

    @Test
    fun `respeta los roles revelados por la memoria`() {
        val memory = PlayerMemory()
        memory.onEvent(
            GameEvent.GameStarted(listOf("A", "B", "C", "D"), List(4) { CharacterId.WILLY_THE_KID }, sheriffSeat = 0),
        )
        memory.onEvent(GameEvent.RoleRevealed(seat = 3, role = Role.RENEGADE))
        val view = sampleView()
        repeat(20) { i ->
            val state = Determinizer(memory).determinize(view, Random(100L + i))
            assertEquals(Role.RENEGADE, state.player(3).role)
            assertEquals(Role.OUTLAW, state.player(2).role)   // único rol restante
        }
    }

    @Test
    fun `las sospechas sesgan el muestreo de roles`() {
        // Partida de 5 (2 Forajidos + Renegado + Alguacil ocultos): el asiento que
        // dispara al Sheriff debe salir Forajido más a menudo que Alguacil.
        val memory = PlayerMemory()
        memory.onEvent(
            GameEvent.GameStarted(List(5) { "P$it" }, List(5) { CharacterId.WILLY_THE_KID }, sheriffSeat = 0),
        )
        val bang = Fixtures.card(1, CardKind.BANG)
        repeat(3) {
            memory.onEvent(GameEvent.CardPlayed(seat = 2, card = bang, targetSeat = 0))
            memory.onEvent(GameEvent.HealthChanged(seat = 0, delta = -1, newHealth = 4, sourceSeat = 2))
        }
        val players = listOf(
            Fixtures.publicInfo(0, role = Role.SHERIFF, handCount = 2),
            Fixtures.publicInfo(1, role = null, handCount = 2),
            Fixtures.publicInfo(2, handCount = 2),
            Fixtures.publicInfo(3, handCount = 2),
            Fixtures.publicInfo(4, handCount = 2),
        )
        // Yo soy el asiento 1 y me conozco (OUTLAW): quedan OUTLAW, RENEGADE y DEPUTY ocultos.
        val view = Fixtures.view(1, Role.OUTLAW, listOf(Fixtures.card(0, CardKind.BANG)), players, deckSize = 40)

        val determinizer = Determinizer(memory)
        var outlaw2 = 0
        var deputy2 = 0
        val runs = 400
        repeat(runs) { i ->
            val roles = determinizer.sampleRoles(view, Random(i.toLong()))
            when (roles.getValue(2)) {
                Role.OUTLAW -> outlaw2++
                Role.DEPUTY -> deputy2++
                else -> Unit
            }
        }
        assertTrue(
            outlaw2 > deputy2,
            "el agresor del Sheriff salió Forajido $outlaw2 veces y Alguacil $deputy2: el sesgo no funciona",
        )
    }

    @Test
    fun `mismo random produce la misma determinizacion (reproducibilidad)`() {
        val view = sampleView()
        val d = Determinizer(PlayerMemory())
        val a = d.determinize(view, Random(9))
        val b = d.determinize(view, Random(9))
        assertEquals(a, b)
    }
}
