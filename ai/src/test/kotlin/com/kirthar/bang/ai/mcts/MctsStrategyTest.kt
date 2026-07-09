package com.kirthar.bang.ai.mcts

import com.kirthar.bang.ai.Fixtures
import com.kirthar.bang.ai.WinOnCardFactory
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.DecisionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de la búsqueda UCT de [MctsStrategy] usando un motor STUB mínimo
 * ([com.kirthar.bang.ai.WinOnCardEngine]) que cumple el contrato [com.kirthar.bang.core.engine.GameEngine]:
 * jugar la carta «buena» gana la partida al instante; cualquier otra opción la pierde.
 */
class MctsStrategyTest {

    private val goodCardId = 0

    private fun view() = Fixtures.view(
        mySeat = 1,
        myRole = Role.OUTLAW,
        myHand = listOf(
            Fixtures.card(0, CardKind.BANG),
            Fixtures.card(1, CardKind.BEER),
        ),
        players = listOf(
            Fixtures.publicInfo(0, role = Role.SHERIFF, handCount = 2),
            Fixtures.publicInfo(1, role = null, handCount = 2),
            Fixtures.publicInfo(2, handCount = 2),
            Fixtures.publicInfo(3, handCount = 2),
        ),
        deckSize = 30,
        discardTop = Fixtures.card(50, CardKind.SALOON),
        currentTurnSeat = 1,
    )

    private fun request() = DecisionRequest.PlayOrPass(
        listOf(
            GameCommand.PlayCard(goodCardId, targetSeat = 0),   // ganadora en el stub
            GameCommand.PlayCard(1),
            GameCommand.EndPlayPhase,
        ),
    )

    @Test
    fun `UCT converge a la opcion ganadora`() {
        val strategy = MctsStrategy(
            seed = 1,
            engineFactory = WinOnCardFactory(goodCardId),
            iterations = 300,
            maxMillis = 5_000,
        )
        val decision = strategy.decide(view(), request())
        assertEquals(GameCommand.PlayCard(goodCardId, targetSeat = 0), decision)
    }

    @Test
    fun `respeta el presupuesto de tiempo`() {
        val strategy = MctsStrategy(
            seed = 1,
            engineFactory = WinOnCardFactory(goodCardId),
            iterations = Int.MAX_VALUE,
            maxMillis = 200,
        )
        val start = System.currentTimeMillis()
        strategy.decide(view(), request())
        val elapsed = System.currentTimeMillis() - start
        assertTrue(elapsed < 2_000, "tardó ${elapsed}ms con presupuesto de 200ms")
    }

    @Test
    fun `con una sola opcion la devuelve sin buscar`() {
        val strategy = MctsStrategy(
            seed = 1,
            engineFactory = WinOnCardFactory(goodCardId),
            iterations = 10,
            maxMillis = 100,
        )
        val single = DecisionRequest.PlayOrPass(listOf(GameCommand.EndPlayPhase))
        assertEquals(GameCommand.EndPlayPhase, strategy.decide(view(), single))
    }

    @Test
    fun `las peticiones no determinizables delegan en la heuristica y son legales`() {
        val strategy = MctsStrategy(
            seed = 1,
            engineFactory = WinOnCardFactory(goodCardId),
            iterations = 10,
            maxMillis = 100,
        )
        val react = DecisionRequest.React(
            CardKind.BANG, sourceSeat = 0, missesNeeded = 1,
            options = listOf(GameCommand.Respond(listOf(1)), GameCommand.TakeHit),
        )
        assertTrue(strategy.decide(view(), react) in react.options)

        val discard = DecisionRequest.DiscardToHandLimit(
            excess = 1,
            options = listOf(GameCommand.Discard(listOf(0)), GameCommand.Discard(listOf(1))),
        )
        assertTrue(strategy.decide(view(), discard) in discard.options)
    }

    @Test
    fun `es determinista para una misma semilla`() {
        fun run(): GameCommand = MctsStrategy(
            seed = 77,
            engineFactory = WinOnCardFactory(goodCardId),
            iterations = 100,
            maxMillis = 60_000,
        ).decide(view(), request())
        assertEquals(run(), run())
    }
}
