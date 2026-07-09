package com.kirthar.bang.ai

import com.kirthar.bang.ai.memory.PlayerMemory
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.view.DecisionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests unitarios de [HeuristicStrategy] con fixtures construidos a mano. */
class HeuristicStrategyTest {

    // ------------------------------------------------------------------
    // Fase de juego
    // ------------------------------------------------------------------

    @Test
    fun `equipa mejor arma antes que pasar`() {
        val winchester = Fixtures.card(10, CardKind.WINCHESTER)
        val view = Fixtures.view(
            0, Role.SHERIFF, listOf(winchester),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF)) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val request = DecisionRequest.PlayOrPass(
            listOf(GameCommand.PlayCard(10), GameCommand.EndPlayPhase),
        )
        assertEquals(GameCommand.PlayCard(10), HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `no sustituye su arma por otra peor`() {
        val schofield = Fixtures.card(11, CardKind.SCHOFIELD)
        val winchesterInPlay = Fixtures.card(20, CardKind.WINCHESTER)
        val me = Fixtures.publicInfo(0, role = Role.SHERIFF, inPlay = listOf(winchesterInPlay))
        val view = Fixtures.view(
            0, Role.SHERIFF, listOf(schofield),
            listOf(me) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val request = DecisionRequest.PlayOrPass(
            listOf(GameCommand.PlayCard(11), GameCommand.EndPlayPhase),
        )
        assertEquals(GameCommand.EndPlayPhase, HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `juega Birra cuando esta herido`() {
        val beer = Fixtures.card(9, CardKind.BEER)
        val me = Fixtures.publicInfo(0, health = 1, maxHealth = 5, role = Role.SHERIFF)
        val view = Fixtures.view(
            0, Role.SHERIFF, listOf(beer),
            listOf(me) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val request = DecisionRequest.PlayOrPass(
            listOf(GameCommand.PlayCard(9), GameCommand.EndPlayPhase),
        )
        assertEquals(GameCommand.PlayCard(9), HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `no juega Birra a vida maxima ni con 2 vivos`() {
        val beer = Fixtures.card(9, CardKind.BEER)
        // A vida máxima:
        val meFull = Fixtures.publicInfo(0, health = 5, maxHealth = 5, role = Role.SHERIFF)
        val viewFull = Fixtures.view(
            0, Role.SHERIFF, listOf(beer),
            listOf(meFull) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val request = DecisionRequest.PlayOrPass(
            listOf(GameCommand.PlayCard(9), GameCommand.EndPlayPhase),
        )
        assertEquals(GameCommand.EndPlayPhase, HeuristicStrategy(1).decide(viewFull, request))

        // Herido pero con solo 2 vivos (la Birra no tiene efecto):
        val meHurt = Fixtures.publicInfo(0, health = 1, maxHealth = 5, role = Role.SHERIFF)
        val viewTwoAlive = Fixtures.view(
            0, Role.SHERIFF, listOf(beer),
            listOf(
                meHurt,
                Fixtures.publicInfo(1),
                Fixtures.publicInfo(2, isAlive = false),
                Fixtures.publicInfo(3, isAlive = false),
            ),
        )
        assertEquals(GameCommand.EndPlayPhase, HeuristicStrategy(1).decide(viewTwoAlive, request))
    }

    @Test
    fun `el Forajido dispara al Sheriff si puede`() {
        val bang = Fixtures.card(1, CardKind.BANG)
        val players = listOf(
            Fixtures.publicInfo(0, role = Role.SHERIFF),
            Fixtures.publicInfo(1),                       // yo (Forajido)
            Fixtures.publicInfo(2),
            Fixtures.publicInfo(3),
        )
        val view = Fixtures.view(1, Role.OUTLAW, listOf(bang), players)
        val request = DecisionRequest.PlayOrPass(
            listOf(
                GameCommand.PlayCard(1, targetSeat = 0),
                GameCommand.PlayCard(1, targetSeat = 2),
                GameCommand.PlayCard(1, targetSeat = 3),
                GameCommand.EndPlayPhase,
            ),
        )
        assertEquals(
            GameCommand.PlayCard(1, targetSeat = 0),
            HeuristicStrategy(1).decide(view, request),
        )
    }

    @Test
    fun `el Sheriff no dispara a un Alguacil revelado y si al sospechoso`() {
        val bang = Fixtures.card(1, CardKind.BANG)
        val memory = PlayerMemory()
        val strategy = HeuristicStrategy(1, memory)
        // Eventos: el asiento 3 dispara al Sheriff (asiento 0) dos veces.
        strategy.onEvent(GameEvent.GameStarted(listOf("A", "B", "C", "D"), List(4) { com.kirthar.bang.core.model.CharacterId.WILLY_THE_KID }, sheriffSeat = 0))
        strategy.onEvent(GameEvent.CardPlayed(seat = 3, card = bang, targetSeat = 0))
        strategy.onEvent(GameEvent.HealthChanged(seat = 0, delta = -1, newHealth = 4, sourceSeat = 3))

        val players = listOf(
            Fixtures.publicInfo(0, role = Role.SHERIFF),   // yo
            Fixtures.publicInfo(1, role = Role.DEPUTY),    // Alguacil revelado
            Fixtures.publicInfo(2),
            Fixtures.publicInfo(3),                        // sospechoso
        )
        val view = Fixtures.view(0, Role.SHERIFF, listOf(bang), players)
        val request = DecisionRequest.PlayOrPass(
            listOf(
                GameCommand.PlayCard(1, targetSeat = 1),
                GameCommand.PlayCard(1, targetSeat = 3),
                GameCommand.EndPlayPhase,
            ),
        )
        assertEquals(GameCommand.PlayCard(1, targetSeat = 3), strategy.decide(view, request))
    }

    @Test
    fun `Cat Balou apunta al Barril del enemigo antes que a su mano`() {
        val catBalou = Fixtures.card(2, CardKind.CAT_BALOU)
        val enemyBarrel = Fixtures.card(30, CardKind.BARREL)
        val players = listOf(
            Fixtures.publicInfo(0, role = Role.SHERIFF, handCount = 3, inPlay = listOf(enemyBarrel)),
            Fixtures.publicInfo(1),   // yo (Forajido)
            Fixtures.publicInfo(2),
            Fixtures.publicInfo(3),
        )
        val view = Fixtures.view(1, Role.OUTLAW, listOf(catBalou), players)
        val request = DecisionRequest.PlayOrPass(
            listOf(
                GameCommand.PlayCard(2, targetSeat = 0, targetCardId = 30),   // el Barril
                GameCommand.PlayCard(2, targetSeat = 0, targetCardId = null), // mano al azar
                GameCommand.EndPlayPhase,
            ),
        )
        assertEquals(
            GameCommand.PlayCard(2, targetSeat = 0, targetCardId = 30),
            HeuristicStrategy(1).decide(view, request),
        )
    }

    @Test
    fun `juega Dinamita solo si hay mas enemigos que aliados`() {
        val dynamite = Fixtures.card(4, CardKind.DYNAMITE)
        val request = DecisionRequest.PlayOrPass(
            listOf(GameCommand.PlayCard(4), GameCommand.EndPlayPhase),
        )
        // Forajido en partida de 5: Sheriff conocido + 3 ocultos ≈ más enemigos.
        val outlawView = Fixtures.view(
            1, Role.OUTLAW, listOf(dynamite),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF)) + (1..4).map { Fixtures.publicInfo(it) },
        )
        assertEquals(GameCommand.PlayCard(4), HeuristicStrategy(1).decide(outlawView, request))

        // Sheriff rodeado solo de Alguaciles revelados: no la juega.
        val sheriffView = Fixtures.view(
            0, Role.SHERIFF, listOf(dynamite),
            listOf(
                Fixtures.publicInfo(0, role = Role.SHERIFF),
                Fixtures.publicInfo(1, role = Role.DEPUTY),
                Fixtures.publicInfo(2, role = Role.DEPUTY),
                Fixtures.publicInfo(3, isAlive = false),
            ),
        )
        assertEquals(GameCommand.EndPlayPhase, HeuristicStrategy(1).decide(sheriffView, request))
    }

    // ------------------------------------------------------------------
    // Reacciones
    // ------------------------------------------------------------------

    @Test
    fun `responde con Fallaste si le queda poca vida`() {
        val missed = Fixtures.card(5, CardKind.MISSED)
        val me = Fixtures.publicInfo(1, health = 1, maxHealth = 4)
        val view = Fixtures.view(
            1, Role.OUTLAW, listOf(missed),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF), me, Fixtures.publicInfo(2), Fixtures.publicInfo(3)),
        )
        val request = DecisionRequest.React(
            CardKind.BANG, sourceSeat = 0, missesNeeded = 1,
            options = listOf(GameCommand.Respond(listOf(5)), GameCommand.TakeHit),
        )
        assertEquals(GameCommand.Respond(listOf(5)), HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `no gasta 2 Fallaste contra Slab con vida de sobra y pocos Fallaste`() {
        val missed1 = Fixtures.card(5, CardKind.MISSED)
        val missed2 = Fixtures.card(6, CardKind.MISSED)
        val me = Fixtures.publicInfo(1, health = 4, maxHealth = 4, handCount = 2)
        val view = Fixtures.view(
            1, Role.OUTLAW, listOf(missed1, missed2),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF), me, Fixtures.publicInfo(2), Fixtures.publicInfo(3)),
        )
        val request = DecisionRequest.React(
            CardKind.BANG, sourceSeat = 0, missesNeeded = 2,
            options = listOf(GameCommand.Respond(listOf(5, 6)), GameCommand.TakeHit),
        )
        assertEquals(GameCommand.TakeHit, HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `responde al Duelo solo si tiene municion o esta al limite`() {
        val bang = Fixtures.card(1, CardKind.BANG)
        // Con 1 solo BANG! y vida de sobra: no compensa.
        val healthy = Fixtures.publicInfo(1, health = 4, maxHealth = 4)
        val viewHealthy = Fixtures.view(
            1, Role.OUTLAW, listOf(bang),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF), healthy, Fixtures.publicInfo(2), Fixtures.publicInfo(3)),
        )
        val request = DecisionRequest.React(
            CardKind.DUEL, sourceSeat = 0, missesNeeded = 0,
            options = listOf(GameCommand.Respond(listOf(1)), GameCommand.TakeHit),
        )
        assertEquals(GameCommand.TakeHit, HeuristicStrategy(1).decide(viewHealthy, request))

        // Con poca vida: responde aunque tenga poca munición.
        val hurt = Fixtures.publicInfo(1, health = 1, maxHealth = 4)
        val viewHurt = Fixtures.view(
            1, Role.OUTLAW, listOf(bang),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF), hurt, Fixtures.publicInfo(2), Fixtures.publicInfo(3)),
        )
        assertEquals(GameCommand.Respond(listOf(1)), HeuristicStrategy(1).decide(viewHurt, request))
    }

    // ------------------------------------------------------------------
    // Descartes y elecciones
    // ------------------------------------------------------------------

    @Test
    fun `descarta lo menos valioso`() {
        val missed = Fixtures.card(5, CardKind.MISSED)     // valor 7
        val saloon = Fixtures.card(6, CardKind.SALOON)     // valor 3
        val view = Fixtures.view(
            0, Role.SHERIFF, listOf(missed, saloon),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF)) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val request = DecisionRequest.DiscardToHandLimit(
            excess = 1,
            options = listOf(
                GameCommand.Discard(listOf(5)),
                GameCommand.Discard(listOf(6)),
            ),
        )
        assertEquals(GameCommand.Discard(listOf(6)), HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `en el Emporio elige la carta mas valiosa`() {
        val saloon = Fixtures.card(7, CardKind.SALOON)       // valor 3
        val wells = Fixtures.card(8, CardKind.WELLS_FARGO)   // valor 8
        val view = Fixtures.view(
            0, Role.SHERIFF, emptyList(),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF)) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val request = DecisionRequest.PickCard(
            purpose = com.kirthar.bang.core.view.PickPurpose.GENERAL_STORE,
            cards = listOf(saloon, wells),
            options = listOf(GameCommand.ChooseCard(7), GameCommand.ChooseCard(8)),
        )
        assertEquals(GameCommand.ChooseCard(8), HeuristicStrategy(1).decide(view, request))
    }

    @Test
    fun `siempre devuelve una opcion legal en cualquier peticion`() {
        val bang = Fixtures.card(1, CardKind.BANG)
        val view = Fixtures.view(
            1, Role.RENEGADE, listOf(bang),
            listOf(Fixtures.publicInfo(0, role = Role.SHERIFF)) + (1..3).map { Fixtures.publicInfo(it) },
        )
        val requests = listOf(
            DecisionRequest.PlayOrPass(listOf(GameCommand.PlayCard(1, targetSeat = 0), GameCommand.EndPlayPhase)),
            DecisionRequest.React(CardKind.INDIANS, 0, 0, listOf(GameCommand.Respond(listOf(1)), GameCommand.TakeHit)),
            DecisionRequest.DiscardToHandLimit(1, listOf(GameCommand.Discard(listOf(1)))),
            DecisionRequest.ChooseDraw(
                listOf(com.kirthar.bang.core.command.DrawSource.Deck),
                listOf(GameCommand.ChooseDrawSource(com.kirthar.bang.core.command.DrawSource.Deck)),
            ),
        )
        val strategy = HeuristicStrategy(3)
        for (request in requests) {
            assertTrue(strategy.decide(view, request) in request.options, "ilegal en $request")
        }
    }
}
