package com.kirthar.bang.ai.memory

import com.kirthar.bang.ai.Fixtures
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests del modelo de sospechas [PlayerMemory]. */
class PlayerMemoryTest {

    private fun started(sheriffSeat: Int = 0, n: Int = 4) = GameEvent.GameStarted(
        playerNames = (0 until n).map { "P$it" },
        characters = List(n) { CharacterId.WILLY_THE_KID },
        sheriffSeat = sheriffSeat,
    )

    @Test
    fun `registra el Sheriff al empezar la partida`() {
        val memory = PlayerMemory()
        memory.onEvent(started(sheriffSeat = 2))
        assertEquals(Role.SHERIFF, memory.knownRole(2))
        assertEquals(2, memory.sheriffSeatOrNull())
    }

    @Test
    fun `quien dispara al Sheriff acumula sospecha anti-Sheriff`() {
        val memory = PlayerMemory()
        memory.onEvent(started())
        val bang = Fixtures.card(1, CardKind.BANG)
        memory.onEvent(GameEvent.CardPlayed(seat = 3, card = bang, targetSeat = 0))
        assertTrue(memory.antiSheriffScore(3) > 0.0)
        assertEquals(0.0, memory.antiSheriffScore(1))
    }

    @Test
    fun `el dano confirmado pesa mas que la carta jugada`() {
        val memory = PlayerMemory()
        memory.onEvent(started())
        val bang = Fixtures.card(1, CardKind.BANG)
        memory.onEvent(GameEvent.CardPlayed(seat = 3, card = bang, targetSeat = 0))
        val onlyCard = memory.antiSheriffScore(3)
        memory.onEvent(GameEvent.HealthChanged(seat = 0, delta = -1, newHealth = 4, sourceSeat = 3))
        assertTrue(memory.antiSheriffScore(3) > onlyCard * 2)
    }

    @Test
    fun `quien ataca a un agresor del Sheriff acumula sospecha pro-Sheriff`() {
        val memory = PlayerMemory()
        memory.onEvent(started())
        val bang = Fixtures.card(1, CardKind.BANG)
        // El asiento 3 ataca al Sheriff; luego el 1 ataca al 3.
        memory.onEvent(GameEvent.CardPlayed(seat = 3, card = bang, targetSeat = 0))
        memory.onEvent(GameEvent.CardPlayed(seat = 1, card = bang, targetSeat = 3))
        assertTrue(memory.proSheriffScore(1) > 0.0)
        assertEquals(0.0, memory.proSheriffScore(3))
    }

    @Test
    fun `la Dinamita no genera sospechas (sin autor)`() {
        val memory = PlayerMemory()
        memory.onEvent(started())
        memory.onEvent(GameEvent.HealthChanged(seat = 0, delta = -3, newHealth = 2, sourceSeat = null))
        for (seat in 0..3) assertEquals(0.0, memory.antiSheriffScore(seat))
    }

    @Test
    fun `los roles revelados al morir quedan registrados`() {
        val memory = PlayerMemory()
        memory.onEvent(started())
        memory.onEvent(GameEvent.PlayerEliminated(seat = 2, role = Role.OUTLAW, bySeat = 0))
        assertEquals(Role.OUTLAW, memory.knownRole(2))
        memory.onEvent(GameEvent.RoleRevealed(seat = 3, role = Role.RENEGADE))
        assertEquals(Role.RENEGADE, memory.knownRole(3))
    }

    @Test
    fun `los pesos de rol reflejan las sospechas`() {
        val memory = PlayerMemory()
        memory.onEvent(started())
        val bang = Fixtures.card(1, CardKind.BANG)
        memory.onEvent(GameEvent.CardPlayed(seat = 3, card = bang, targetSeat = 0))
        memory.onEvent(GameEvent.HealthChanged(seat = 0, delta = -1, newHealth = 4, sourceSeat = 3))

        val weights3 = memory.roleWeights(3)
        val weights1 = memory.roleWeights(1)
        // El agresor del Sheriff debe pesar más como Forajido que un neutral.
        assertTrue(weights3.getValue(Role.OUTLAW) > weights1.getValue(Role.OUTLAW))
        // El Sheriff nunca se muestrea.
        assertEquals(0.0, weights3.getValue(Role.SHERIFF))
    }
}
