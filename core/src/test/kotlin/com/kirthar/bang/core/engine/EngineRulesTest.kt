package com.kirthar.bang.core.engine

import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.GamePhase
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PickPurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineRulesTest {

    // --- Distancias -----------------------------------------------------------

    @Test
    fun `distancias con Mustang, Mira y minimo 1`() {
        val mustang = card(CardKind.MUSTANG, Suit.HEARTS, Rank.EIGHT)
        val scope = card(CardKind.SCOPE, Suit.SPADES, Rank.ACE)
        val players = listOf(
            player(0, Role.SHERIFF, inPlay = listOf(scope)),
            player(1, Role.OUTLAW),
            player(2, Role.OUTLAW, inPlay = listOf(mustang)),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        val view = engine.viewFor(0)
        assertEquals(0, view.players[0].distance)         // uno mismo
        // base 0->1 = 1, con Mira del observador -> 0 -> mínimo 1
        assertEquals(1, view.players[1].distance)
        // base 0->2 = 2, +1 Mustang del objetivo, -1 Mira observador = 2
        assertEquals(2, view.players[2].distance)
        // base 0->3 = 1 (min(3,1)), -1 Mira -> mínimo 1
        assertEquals(1, view.players[3].distance)
    }

    @Test
    fun `Paul Regret suma 1 como objetivo y Rose Doolan resta como observador`() {
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.ROSE_DOOLAN),
            player(1, Role.OUTLAW, character = CharacterId.PAUL_REGRET),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        val view = engine.viewFor(0)
        // 0->1 base 1, +1 Paul, -1 Rose = 1
        assertEquals(1, view.players[1].distance)
        // 0->2 base 2, -1 Rose = 1
        assertEquals(1, view.players[2].distance)
    }

    // --- BANG! / ¡Fallaste! / Barril / Slab -----------------------------------

    @Test
    fun `BANG sin defensa hace perder 1 vida`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 4),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        // Le toca reaccionar al asiento 1
        assertEquals(1, engine.actor())
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(3, engine.state.player(1).health)
    }

    @Test
    fun `Fallaste cancela el BANG`() {
        val bang = card(CardKind.BANG)
        val missed = card(CardKind.MISSED, Suit.CLUBS, Rank.ACE)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, hand = listOf(missed)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.Respond(listOf(missed.id)))
        assertEquals(4, engine.state.player(1).health)
        assertTrue(engine.state.player(1).hand.isEmpty())
    }

    @Test
    fun `Barril con corazon cancela el BANG automaticamente`() {
        val bang = card(CardKind.BANG)
        val barrel = card(CardKind.BARREL, Suit.SPADES, Rank.QUEEN)
        val heart = card(CardKind.BEER, Suit.HEARTS, Rank.SIX) // carta del «¡desenfunda!»
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, inPlay = listOf(barrel)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(heart) + List(10) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        // El barril salió corazón -> BANG cancelado, vuelve a jugar el asiento 0
        assertEquals(0, engine.actor())
        assertEquals(4, engine.state.player(1).health)
    }

    @Test
    fun `Barril sin corazon obliga a reaccionar`() {
        val bang = card(CardKind.BANG)
        val barrel = card(CardKind.BARREL, Suit.SPADES, Rank.QUEEN)
        val spade = card(CardKind.BANG, Suit.SPADES, Rank.FIVE)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, inPlay = listOf(barrel)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(spade) + List(10) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        assertEquals(1, engine.actor())
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(3, engine.state.player(1).health)
    }

    @Test
    fun `Slab the Killer exige dos Fallaste`() {
        val bang = card(CardKind.BANG)
        val m1 = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val m2 = card(CardKind.MISSED, Suit.CLUBS, Rank.JACK)
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.SLAB_THE_KILLER, hand = listOf(bang)),
            player(1, Role.OUTLAW, hand = listOf(m1, m2)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        val req = engine.request() as DecisionRequest.React
        assertEquals(2, req.missesNeeded)
        // Con un solo ¡Fallaste! no se puede cancelar: solo TakeHit + el par completo
        engine.accept(1, GameCommand.Respond(listOf(m1.id, m2.id)))
        assertEquals(4, engine.state.player(1).health)
    }

    @Test
    fun `Slab the Killer con un solo Fallaste no puede cancelar`() {
        val bang = card(CardKind.BANG)
        val m1 = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.SLAB_THE_KILLER, hand = listOf(bang)),
            player(1, Role.OUTLAW, hand = listOf(m1)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        val req = engine.request() as DecisionRequest.React
        // Única opción real: encajar el golpe
        assertEquals(listOf(GameCommand.TakeHit), req.options)
    }

    @Test
    fun `limite de un BANG por turno salvo Volcanic`() {
        val b1 = card(CardKind.BANG)
        val b2 = card(CardKind.BANG, Suit.DIAMONDS, Rank.THREE)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(b1, b2)),
            player(1, Role.OUTLAW),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(b1.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        // Ya jugó su BANG!: no debe ofrecerse un segundo
        val req = engine.request() as DecisionRequest.PlayOrPass
        assertFalse(req.options.any { it is GameCommand.PlayCard && it.cardId == b2.id })
    }

    // --- Duelo, Indios, Gatling ----------------------------------------------

    @Test
    fun `Duelo alternante hasta que alguien no puede responder`() {
        val duel = card(CardKind.DUEL, Suit.DIAMONDS, Rank.QUEEN)
        val bang1 = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, health = 4, hand = listOf(duel)),
            player(1, Role.OUTLAW, health = 4, hand = listOf(bang1)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(duel.id, targetSeat = 1))
        // El retado (1) responde con BANG!
        assertEquals(1, engine.actor())
        engine.accept(1, GameCommand.Respond(listOf(bang1.id)))
        // Ahora responde el asiento 0, que no tiene BANG! -> encaja
        assertEquals(0, engine.actor())
        engine.accept(0, GameCommand.TakeHit)
        assertEquals(3, engine.state.player(0).health)
        assertEquals(4, engine.state.player(1).health)
    }

    @Test
    fun `Indios y Calamity Janet puede descartar Fallaste como BANG`() {
        val indians = card(CardKind.INDIANS, Suit.DIAMONDS, Rank.KING)
        val calamityMissed = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(indians)),
            player(1, Role.OUTLAW, character = CharacterId.CALAMITY_JANET, health = 4, hand = listOf(calamityMissed)),
            player(2, Role.OUTLAW, health = 4, hand = emptyList()),
            player(3, Role.RENEGADE, health = 4),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(indians.id))
        // Reacciona primero el asiento 1 (Calamity): puede descartar el ¡Fallaste! como BANG!
        assertEquals(1, engine.actor())
        val req = engine.request() as DecisionRequest.React
        assertTrue(req.options.any { it is GameCommand.Respond && it.cardIds == listOf(calamityMissed.id) })
        engine.accept(1, GameCommand.Respond(listOf(calamityMissed.id)))
        assertEquals(4, engine.state.player(1).health)
        // El asiento 2 no tiene BANG!: pierde 1 vida
        assertEquals(2, engine.actor())
        engine.accept(2, GameCommand.TakeHit)
        assertEquals(3, engine.state.player(2).health)
    }

    @Test
    fun `Gatling dispara a todos los demas`() {
        val gatling = card(CardKind.GATLING, Suit.HEARTS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(gatling)),
            player(1, Role.OUTLAW, health = 4),
            player(2, Role.OUTLAW, health = 4),
            player(3, Role.RENEGADE, health = 4),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(gatling.id))
        // Cada rival, en orden, encaja el golpe
        repeat(3) {
            val seat = engine.actor()
            engine.accept(seat, GameCommand.TakeHit)
        }
        assertEquals(3, engine.state.player(1).health)
        assertEquals(3, engine.state.player(2).health)
        assertEquals(3, engine.state.player(3).health)
        assertEquals(4, engine.state.player(0).health) // el tirador no se dispara
    }

    // --- Dinamita y Cárcel ----------------------------------------------------

    @Test
    fun `Dinamita explota con picas 2-9`() {
        val dynamite = card(CardKind.DYNAMITE, Suit.HEARTS, Rank.TWO)
        val spade5 = card(CardKind.BANG, Suit.SPADES, Rank.FIVE) // «¡desenfunda!» -> explota
        val players = listOf(
            player(0, Role.SHERIFF, health = 4),
            player(1, Role.OUTLAW, health = 4, inPlay = listOf(dynamite)),
            player(2, Role.OUTLAW, health = 4),
            player(3, Role.RENEGADE, health = 4),
        )
        val engine = engineFrom(gameState(players, deck = listOf(spade5) + List(20) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        // El asiento 0 termina su turno -> arranca el turno del 1 con la Dinamita
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(1, engine.state.player(1).health) // 4 - 3
        assertFalse(engine.state.player(1).hasInPlay(CardKind.DYNAMITE))
    }

    @Test
    fun `Dinamita circula si no explota`() {
        val dynamite = card(CardKind.DYNAMITE, Suit.HEARTS, Rank.TWO)
        val club7 = card(CardKind.BANG, Suit.CLUBS, Rank.SEVEN) // no picas -> no explota
        val players = listOf(
            player(0, Role.SHERIFF, health = 4),
            player(1, Role.OUTLAW, health = 4, inPlay = listOf(dynamite)),
            player(2, Role.OUTLAW, health = 4),
            player(3, Role.RENEGADE, health = 4),
        )
        val engine = engineFrom(gameState(players, deck = listOf(club7) + List(20) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(4, engine.state.player(1).health)
        assertFalse(engine.state.player(1).hasInPlay(CardKind.DYNAMITE))
        assertTrue(engine.state.player(2).hasInPlay(CardKind.DYNAMITE)) // pasó al de la izquierda
    }

    @Test
    fun `Carcel con corazon deja jugar y sin corazon pierde el turno`() {
        val jail = card(CardKind.JAIL, Suit.SPADES, Rank.TEN)
        val heart = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)
        val playersEscape = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, inPlay = listOf(jail)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val escape = engineFrom(gameState(playersEscape, deck = listOf(heart) + List(20) { card(CardKind.BANG) }))
        escape.accept(0, GameCommand.EndPlayPhase)
        assertEquals(1, escape.state.currentSeat) // escapa y juega
        assertEquals(GamePhase.PLAY, escape.state.phase)
        assertFalse(escape.state.player(1).hasInPlay(CardKind.JAIL))

        val jail2 = card(CardKind.JAIL, Suit.SPADES, Rank.TEN)
        val spade = card(CardKind.BANG, Suit.SPADES, Rank.FIVE)
        val playersLose = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, inPlay = listOf(jail2)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val lose = engineFrom(gameState(playersLose, deck = listOf(spade) + List(20) { card(CardKind.BANG) }))
        lose.accept(0, GameCommand.EndPlayPhase)
        assertEquals(2, lose.state.currentSeat) // el 1 pierde el turno
        assertFalse(lose.state.player(1).hasInPlay(CardKind.JAIL))
    }

    @Test
    fun `el Sheriff no puede ser encarcelado`() {
        val jail = card(CardKind.JAIL, Suit.SPADES, Rank.TEN)
        val players = listOf(
            player(0, Role.OUTLAW, hand = listOf(jail)),
            player(1, Role.SHERIFF),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        val req = engine.request() as DecisionRequest.PlayOrPass
        assertFalse(req.options.any { it is GameCommand.PlayCard && it.cardId == jail.id && it.targetSeat == 1 })
        assertTrue(req.options.any { it is GameCommand.PlayCard && it.cardId == jail.id && it.targetSeat == 2 })
    }

    // --- Emporio --------------------------------------------------------------

    @Test
    fun `Emporio reparte una carta a cada jugador en orden`() {
        val store = card(CardKind.GENERAL_STORE, Suit.CLUBS, Rank.NINE)
        val revealed = listOf(
            card(CardKind.BANG, Suit.DIAMONDS, Rank.TWO),
            card(CardKind.BEER, Suit.HEARTS, Rank.SIX),
            card(CardKind.PANIC, Suit.HEARTS, Rank.JACK),
            card(CardKind.MISSED, Suit.CLUBS, Rank.TEN),
        )
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(store)),
            player(1, Role.OUTLAW),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = revealed + List(5) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(store.id))
        // Cada asiento elige, empezando por el 0
        for (seat in 0..3) {
            assertEquals(seat, engine.actor())
            val req = engine.request() as DecisionRequest.PickCard
            assertEquals(PickPurpose.GENERAL_STORE, req.purpose)
            engine.accept(seat, req.options.first() as GameCommand.ChooseCard)
        }
        assertTrue(engine.state.players.all { it.hand.count { c -> c in revealed } == 1 || it.seat == 0 })
        assertEquals(1, engine.state.player(1).hand.size)
    }

    // --- Recompensas y penalizaciones ----------------------------------------

    @Test
    fun `matar a un Forajido da 3 cartas de recompensa`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 1),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = List(10) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        // El asiento 1 muere (sin Birra); recompensa de 3 cartas al asiento 0
        assertFalse(engine.state.player(1).isAlive)
        assertEquals(3, engine.state.player(0).hand.size)
    }

    @Test
    fun `el Sheriff que mata a un Alguacil descarta toda su mano`() {
        val bang = card(CardKind.BANG)
        val extra = card(CardKind.BEER, Suit.HEARTS, Rank.SEVEN)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang, extra)),
            player(1, Role.DEPUTY, health = 1),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        assertFalse(engine.state.player(1).isAlive)
        assertTrue(engine.state.player(0).hand.isEmpty())
        assertTrue(engine.state.player(0).inPlay.isEmpty())
    }

    // --- Birra salvadora ------------------------------------------------------

    @Test
    fun `Birra salva de un golpe mortal`() {
        val bang = card(CardKind.BANG)
        val beer = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 1, maxHealth = 4, hand = listOf(beer)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit) // baja a 0 vidas
        // Ahora el asiento 1 puede salvarse con la Birra
        assertEquals(1, engine.actor())
        val req = engine.request() as DecisionRequest.React
        assertEquals(CardKind.BEER, req.kind)
        engine.accept(1, GameCommand.Respond(listOf(beer.id)))
        assertTrue(engine.state.player(1).isAlive)
        assertEquals(1, engine.state.player(1).health)
    }

    @Test
    fun `con 2 jugadores vivos la Birra no salva`() {
        val bang = card(CardKind.BANG)
        val beer = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 1, maxHealth = 4, hand = listOf(beer)),
            player(2, Role.OUTLAW, alive = false),
            player(3, Role.RENEGADE, alive = false),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        // Con 2 vivos la Birra no tiene efecto: el asiento 1 muere sin poder salvarse
        assertFalse(engine.state.player(1).isAlive)
    }

    // --- Condiciones de victoria ---------------------------------------------

    @Test
    fun `ganan los Forajidos al morir el Sheriff`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, health = 1),
            player(1, Role.OUTLAW, hand = listOf(bang)),
            player(2, Role.RENEGADE),
            player(3, Role.OUTLAW),
        )
        val engine = engineFrom(gameState(players, currentSeat = 1))
        engine.accept(1, GameCommand.PlayCard(bang.id, targetSeat = 0))
        engine.accept(0, GameCommand.TakeHit)
        assertEquals(Role.OUTLAW, engine.state.result?.winningRole)
    }

    @Test
    fun `gana el Renegado si es el ultimo en pie tras caer el Sheriff`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, health = 1),
            player(1, Role.RENEGADE, hand = listOf(bang)),
            player(2, Role.OUTLAW, alive = false),
            player(3, Role.OUTLAW, alive = false),
        )
        val engine = engineFrom(gameState(players, currentSeat = 1))
        engine.accept(1, GameCommand.PlayCard(bang.id, targetSeat = 0))
        engine.accept(0, GameCommand.TakeHit)
        assertEquals(Role.RENEGADE, engine.state.result?.winningRole)
        assertEquals(listOf(1), engine.state.result?.winnerSeats)
    }

    @Test
    fun `ganan Sheriff y Alguaciles al eliminar a Forajidos y Renegado`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 1),
            player(2, Role.DEPUTY),
            player(3, Role.RENEGADE, alive = false),
        )
        val engine = engineFrom(gameState(players, deck = List(6) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(Role.SHERIFF, engine.state.result?.winningRole)
        assertTrue(engine.state.result!!.winnerSeats.containsAll(listOf(0, 2)))
    }

    // --- Límite de mano y rebarajado -----------------------------------------

    @Test
    fun `descarte del exceso al final del turno`() {
        val hand = List(4) { card(CardKind.BANG, Suit.DIAMONDS, Rank.entries[it + 1]) }
        val players = listOf(
            player(0, Role.SHERIFF, health = 2, maxHealth = 2, hand = hand),
            player(1, Role.OUTLAW),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.EndPlayPhase)
        // Exceso de 2 cartas (4 en mano, límite 2)
        var req = engine.request()
        assertTrue(req is DecisionRequest.DiscardToHandLimit)
        engine.accept(0, (req as DecisionRequest.DiscardToHandLimit).options.first() as GameCommand.Discard)
        req = engine.request()
        engine.accept(0, (req as DecisionRequest.DiscardToHandLimit).options.first() as GameCommand.Discard)
        assertEquals(2, engine.state.player(0).hand.size)
        assertEquals(1, engine.state.currentSeat) // pasó el turno
    }

    @Test
    fun `rebarajado de descartes al agotarse el mazo`() {
        val wells = card(CardKind.WELLS_FARGO, Suit.HEARTS, Rank.THREE)
        val onlyCard = card(CardKind.BANG, Suit.DIAMONDS, Rank.TWO)
        val discardStack = List(5) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(wells)),
            player(1, Role.OUTLAW),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(onlyCard), discard = discardStack))
        val events = engine.accept(0, GameCommand.PlayCard(wells.id))
        assertTrue(events.any { it is GameEvent.DeckReshuffled })
        assertEquals(3, engine.state.player(0).hand.size) // robó 3 pese a agotarse el mazo
    }

    // --- Habilidades de robo (elecciones) ------------------------------------

    @Test
    fun `Jesse Jones roba su primera carta de la mano de otro`() {
        val victimCard = card(CardKind.BANG, Suit.DIAMONDS, Rank.KING)
        val players = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, character = CharacterId.JESSE_JONES),
            player(2, Role.OUTLAW, hand = listOf(victimCard)),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = List(10) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        engine.accept(0, GameCommand.EndPlayPhase) // arranca el turno de Jesse (1)
        assertEquals(1, engine.actor())
        val req = engine.request() as DecisionRequest.ChooseDraw
        assertTrue(req.sources.any { it is DrawSource.PlayerHand && it.seat == 2 })
        engine.accept(1, GameCommand.ChooseDrawSource(DrawSource.PlayerHand(2)))
        assertTrue(engine.state.player(2).hand.isEmpty()) // le robó la carta
        assertEquals(2, engine.state.player(1).hand.size) // 1 robada + 1 del mazo
    }

    @Test
    fun `Kit Carlson mira 3 y se queda 2`() {
        val top = listOf(
            card(CardKind.BANG, Suit.DIAMONDS, Rank.TWO),
            card(CardKind.BEER, Suit.HEARTS, Rank.SIX),
            card(CardKind.PANIC, Suit.HEARTS, Rank.JACK),
        )
        val players = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, character = CharacterId.KIT_CARLSON),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = top + List(5) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(1, engine.actor())
        var req = engine.request() as DecisionRequest.PickCard
        assertEquals(PickPurpose.KIT_CARLSON, req.purpose)
        engine.accept(1, GameCommand.ChooseCard(top[0].id))
        req = engine.request() as DecisionRequest.PickCard
        engine.accept(1, GameCommand.ChooseCard(top[1].id))
        assertEquals(2, engine.state.player(1).hand.size)
        assertEquals(top[2].id, engine.state.deck.first().id) // la carta no elegida vuelve a la cima
    }

    @Test
    fun `Lucky Duke elige entre dos cartas del desenfunda del Barril`() {
        val bang = card(CardKind.BANG)
        val barrel = card(CardKind.BARREL, Suit.SPADES, Rank.QUEEN)
        val spade = card(CardKind.BANG, Suit.SPADES, Rank.FIVE)
        val heart = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, character = CharacterId.LUCKY_DUKE, health = 4, inPlay = listOf(barrel)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(spade, heart) + List(10) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        // Lucky Duke voltea 2 y elige: escoge el corazón para cancelar el BANG!
        assertEquals(1, engine.actor())
        val req = engine.request() as DecisionRequest.PickCard
        assertEquals(PickPurpose.LUCKY_DUKE, req.purpose)
        engine.accept(1, GameCommand.ChooseCard(heart.id))
        assertEquals(0, engine.actor()) // BANG! cancelado, vuelve a jugar el 0
        assertEquals(4, engine.state.player(1).health)
    }

    // --- Vulture Sam ----------------------------------------------------------

    @Test
    fun `Vulture Sam toma las cartas del jugador eliminado`() {
        val bang = card(CardKind.BANG)
        val victimHand = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)
        val victimPlay = card(CardKind.SCOPE, Suit.SPADES, Rank.ACE)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 1, hand = listOf(victimHand), inPlay = listOf(victimPlay)),
            player(2, Role.RENEGADE, character = CharacterId.VULTURE_SAM),
            player(3, Role.OUTLAW),
        )
        val engine = engineFrom(gameState(players, deck = List(6) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit) // encaja el BANG! (0 vidas)
        engine.accept(1, GameCommand.TakeHit) // renuncia a salvarse con la Birra
        assertFalse(engine.state.player(1).isAlive)
        // Sam se queda las 2 cartas de la víctima (mano + en juego)
        assertTrue(engine.state.player(2).hand.any { it.id == victimHand.id })
        assertTrue(engine.state.player(2).hand.any { it.id == victimPlay.id })
    }
}
