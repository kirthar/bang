package com.kirthar.bang.ai

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.DecisionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests unitarios de [RandomStrategy] con vistas y peticiones construidas a mano. */
class RandomStrategyTest {

    private fun players(n: Int = 4) = (0 until n).map { seat ->
        Fixtures.publicInfo(seat, role = if (seat == 0) Role.SHERIFF else null)
    }

    @Test
    fun `elige siempre una opcion legal`() {
        val hand = listOf(
            Fixtures.card(1, CardKind.BANG),
            Fixtures.card(2, CardKind.MISSED),
        )
        val options = listOf<GameCommand>(
            GameCommand.PlayCard(1, targetSeat = 1),
            GameCommand.EndPlayPhase,
        )
        val view = Fixtures.view(0, Role.SHERIFF, hand, players())
        val request = DecisionRequest.PlayOrPass(options)
        val strategy = RandomStrategy(seed = 7)
        repeat(50) {
            assertTrue(strategy.decide(view, request) in options)
        }
    }

    @Test
    fun `es determinista para una misma semilla`() {
        val hand = listOf(Fixtures.card(1, CardKind.BANG))
        val options = listOf<GameCommand>(
            GameCommand.PlayCard(1, targetSeat = 1),
            GameCommand.PlayCard(1, targetSeat = 2),
            GameCommand.PlayCard(1, targetSeat = 3),
            GameCommand.EndPlayPhase,
        )
        val view = Fixtures.view(0, Role.SHERIFF, hand, players())
        val request = DecisionRequest.PlayOrPass(options)
        val a = RandomStrategy(42).let { s -> (1..20).map { s.decide(view, request) } }
        val b = RandomStrategy(42).let { s -> (1..20).map { s.decide(view, request) } }
        assertEquals(a, b)
    }

    @Test
    fun `no elige TakeHit si hay respuesta disponible`() {
        val hand = listOf(Fixtures.card(5, CardKind.MISSED))
        val options = listOf<GameCommand>(
            GameCommand.Respond(listOf(5)),
            GameCommand.TakeHit,
        )
        val view = Fixtures.view(1, Role.OUTLAW, hand, players())
        val request = DecisionRequest.React(CardKind.BANG, sourceSeat = 0, missesNeeded = 1, options = options)
        val strategy = RandomStrategy(seed = 3)
        repeat(100) {
            assertEquals(GameCommand.Respond(listOf(5)), strategy.decide(view, request))
        }
    }

    @Test
    fun `elige TakeHit si es la unica opcion`() {
        val options = listOf<GameCommand>(GameCommand.TakeHit)
        val view = Fixtures.view(1, Role.OUTLAW, emptyList(), players())
        val request = DecisionRequest.React(CardKind.BANG, sourceSeat = 0, missesNeeded = 1, options = options)
        assertEquals(GameCommand.TakeHit, RandomStrategy(1).decide(view, request))
    }

    @Test
    fun `no juega Birra a vida maxima`() {
        val beer = Fixtures.card(9, CardKind.BEER)
        val hand = listOf(beer)
        val options = listOf<GameCommand>(
            GameCommand.PlayCard(9),   // Birra
            GameCommand.EndPlayPhase,
        )
        val me = Fixtures.publicInfo(0, health = 4, maxHealth = 4, role = Role.SHERIFF)
        val others = (1..3).map { Fixtures.publicInfo(it) }
        val view = Fixtures.view(0, Role.SHERIFF, hand, listOf(me) + others)
        val request = DecisionRequest.PlayOrPass(options)
        val strategy = RandomStrategy(seed = 11)
        repeat(100) {
            assertEquals(GameCommand.EndPlayPhase, strategy.decide(view, request))
        }
    }

    @Test
    fun `si juega Birra estando herido`() {
        val beer = Fixtures.card(9, CardKind.BEER)
        val options = listOf<GameCommand>(
            GameCommand.PlayCard(9),
            GameCommand.EndPlayPhase,
        )
        val me = Fixtures.publicInfo(0, health = 2, maxHealth = 4, role = Role.SHERIFF)
        val others = (1..3).map { Fixtures.publicInfo(it) }
        val view = Fixtures.view(0, Role.SHERIFF, listOf(beer), listOf(me) + others)
        val request = DecisionRequest.PlayOrPass(options)
        val strategy = RandomStrategy(seed = 11)
        val chosen = (1..100).map { strategy.decide(view, request) }.toSet()
        assertTrue(GameCommand.PlayCard(9) in chosen, "herido, la Birra debe estar entre las elegibles")
    }

    @Test
    fun `no se bloquea si los filtros vacian las opciones`() {
        // Única opción: Birra a vida máxima → el filtro la eliminaría; debe degradar
        // a la lista completa y devolverla igualmente.
        val beer = Fixtures.card(9, CardKind.BEER)
        val options = listOf<GameCommand>(GameCommand.PlayCard(9))
        val me = Fixtures.publicInfo(0, health = 4, maxHealth = 4, role = Role.SHERIFF)
        val others = (1..3).map { Fixtures.publicInfo(it) }
        val view = Fixtures.view(0, Role.SHERIFF, listOf(beer), listOf(me) + others)
        val request = DecisionRequest.PlayOrPass(options)
        assertEquals(GameCommand.PlayCard(9), RandomStrategy(5).decide(view, request))
    }
}
