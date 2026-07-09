package com.kirthar.bang.core.engine

import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.view.DecisionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests de las habilidades de personaje no cubiertas ya por [EngineRulesTest]. */
class CharacterAbilityTest {

    @Test
    fun `Bart Cassidy roba una carta por cada vida perdida`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, character = CharacterId.BART_CASSIDY, health = 4),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = List(8) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(3, engine.state.player(1).health)
        assertEquals(1, engine.state.player(1).hand.size) // robó 1 al perder 1 vida
    }

    @Test
    fun `Bart Cassidy roba 3 cartas si le explota la Dinamita`() {
        val dynamite = card(CardKind.DYNAMITE, Suit.HEARTS, Rank.TWO)
        val spade5 = card(CardKind.BANG, Suit.SPADES, Rank.FIVE)
        val players = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, character = CharacterId.BART_CASSIDY, health = 4, inPlay = listOf(dynamite)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(
            gameState(players, deck = listOf(spade5) + List(10) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }),
        )
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(1, engine.state.player(1).health) // 4 - 3
        // 3 por la habilidad + 2 de su fase de robar
        assertEquals(5, engine.state.player(1).hand.size)
    }

    @Test
    fun `Black Jack roba una extra si la segunda es corazones o diamantes`() {
        val c1 = card(CardKind.BANG, Suit.SPADES, Rank.KING)
        val c2 = card(CardKind.BEER, Suit.HEARTS, Rank.SIX) // 2ª: corazones -> extra
        val c3 = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, character = CharacterId.BLACK_JACK),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(c1, c2, c3) + List(5) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(3, engine.state.player(1).hand.size)
    }

    @Test
    fun `Black Jack no roba extra si la segunda es negra`() {
        val c1 = card(CardKind.BANG, Suit.SPADES, Rank.KING)
        val c2 = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN) // 2ª: tréboles -> sin extra
        val players = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, character = CharacterId.BLACK_JACK),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(c1, c2) + List(5) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(2, engine.state.player(1).hand.size)
    }

    @Test
    fun `El Gringo roba una carta de la mano de quien le hiere`() {
        val bang = card(CardKind.BANG)
        val extra = card(CardKind.BEER, Suit.HEARTS, Rank.SEVEN)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang, extra)),
            player(1, Role.OUTLAW, character = CharacterId.EL_GRINGO, health = 3),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(2, engine.state.player(1).health)
        assertTrue(engine.state.player(1).hand.any { it.id == extra.id })
        assertTrue(engine.state.player(0).hand.isEmpty())
    }

    @Test
    fun `El Gringo no roba nada cuando el dano viene de la Dinamita`() {
        val dynamite = card(CardKind.DYNAMITE, Suit.HEARTS, Rank.TWO)
        val spade5 = card(CardKind.BANG, Suit.SPADES, Rank.FIVE)
        val sheriffCard = card(CardKind.BEER, Suit.HEARTS, Rank.SEVEN)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(sheriffCard)),
            player(1, Role.OUTLAW, character = CharacterId.EL_GRINGO, health = 4, inPlay = listOf(dynamite)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(
            gameState(players, deck = listOf(spade5) + List(8) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }),
        )
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(1, engine.state.player(1).health)
        // Nadie causó el daño: el Sheriff conserva su mano y El Gringo solo tiene lo robado en su fase
        assertEquals(1, engine.state.player(0).hand.size)
        assertEquals(2, engine.state.player(1).hand.size)
    }

    @Test
    fun `Jourdonnais tiene un Barril innato`() {
        val bang = card(CardKind.BANG)
        val heart = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, character = CharacterId.JOURDONNAIS, health = 4),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(heart) + List(5) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        // Sin Barril real, su habilidad hace el «¡desenfunda!»: corazón -> BANG! cancelado
        assertEquals(0, engine.actor())
        assertEquals(4, engine.state.player(1).health)
    }

    @Test
    fun `Jourdonnais con Barril real tiene dos intentos`() {
        val bang = card(CardKind.BANG)
        val barrel = card(CardKind.BARREL, Suit.SPADES, Rank.QUEEN)
        val spade = card(CardKind.BANG, Suit.SPADES, Rank.FIVE) // 1er intento falla
        val heart = card(CardKind.BEER, Suit.HEARTS, Rank.SIX)  // 2º intento acierta
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, character = CharacterId.JOURDONNAIS, health = 4, inPlay = listOf(barrel)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = listOf(spade, heart) + List(5) { card(CardKind.BANG) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        assertEquals(0, engine.actor()) // cancelado al segundo intento
        assertEquals(4, engine.state.player(1).health)
    }

    @Test
    fun `Pedro Ramirez puede robar de la cima de los descartes`() {
        val discarded = card(CardKind.PANIC, Suit.HEARTS, Rank.JACK)
        val players = listOf(
            player(0, Role.SHERIFF),
            player(1, Role.OUTLAW, character = CharacterId.PEDRO_RAMIREZ),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(
            gameState(players, deck = List(8) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }, discard = listOf(discarded)),
        )
        engine.accept(0, GameCommand.EndPlayPhase)
        assertEquals(1, engine.actor())
        val req = engine.request() as DecisionRequest.ChooseDraw
        assertTrue(req.sources.contains(DrawSource.DiscardPile))
        engine.accept(1, GameCommand.ChooseDrawSource(DrawSource.DiscardPile))
        assertTrue(engine.state.player(1).hand.any { it.id == discarded.id })
        assertEquals(2, engine.state.player(1).hand.size)
        assertTrue(engine.state.discardPile.isEmpty())
    }

    @Test
    fun `Sid Ketchum descarta 2 cartas para recuperar 1 vida`() {
        val c1 = card(CardKind.BANG, Suit.DIAMONDS, Rank.TWO)
        val c2 = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.SID_KETCHUM, health = 2, maxHealth = 5, hand = listOf(c1, c2)),
            player(1, Role.OUTLAW),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        val req = engine.request() as DecisionRequest.PlayOrPass
        assertTrue(req.options.any { it is GameCommand.UseCharacterAbility })
        engine.accept(0, GameCommand.UseCharacterAbility(listOf(c1.id, c2.id)))
        assertEquals(3, engine.state.player(0).health)
        assertTrue(engine.state.player(0).hand.isEmpty())
    }

    @Test
    fun `Sid Ketchum puede salvarse de un golpe mortal descartando 2 cartas`() {
        val bang = card(CardKind.BANG)
        val c1 = card(CardKind.BANG, Suit.DIAMONDS, Rank.TWO)
        val c2 = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, character = CharacterId.SID_KETCHUM, health = 1, maxHealth = 4, hand = listOf(c1, c2)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit) // baja a 0
        assertEquals(1, engine.actor())
        engine.accept(1, GameCommand.UseCharacterAbility(listOf(c1.id, c2.id)))
        assertTrue(engine.state.player(1).isAlive)
        assertEquals(1, engine.state.player(1).health)
    }

    @Test
    fun `Suzy Lafayette roba al quedarse sin cartas`() {
        val bang = card(CardKind.BANG)
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.SUZY_LAFAYETTE, hand = listOf(bang)),
            player(1, Role.OUTLAW, health = 4),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players, deck = List(6) { card(CardKind.BEER, Suit.HEARTS, Rank.SIX) }))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        // Jugó su última carta: roba 1 inmediatamente
        assertEquals(1, engine.state.player(0).hand.size)
    }

    @Test
    fun `Willy the Kid puede jugar varios BANG en el mismo turno`() {
        val b1 = card(CardKind.BANG)
        val b2 = card(CardKind.BANG, Suit.DIAMONDS, Rank.THREE)
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.WILLY_THE_KID, hand = listOf(b1, b2)),
            player(1, Role.OUTLAW, health = 4),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(b1.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        val req = engine.request() as DecisionRequest.PlayOrPass
        assertTrue(req.options.any { it is GameCommand.PlayCard && it.cardId == b2.id })
        engine.accept(0, GameCommand.PlayCard(b2.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(2, engine.state.player(1).health)
    }

    @Test
    fun `Calamity Janet juega un Fallaste como BANG`() {
        val missed = card(CardKind.MISSED, Suit.CLUBS, Rank.TEN)
        val players = listOf(
            player(0, Role.SHERIFF, character = CharacterId.CALAMITY_JANET, hand = listOf(missed)),
            player(1, Role.OUTLAW, health = 4),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        val req = engine.request() as DecisionRequest.PlayOrPass
        assertTrue(req.options.any { it is GameCommand.PlayCard && it.cardId == missed.id && it.targetSeat == 1 })
        engine.accept(0, GameCommand.PlayCard(missed.id, targetSeat = 1))
        engine.accept(1, GameCommand.TakeHit)
        assertEquals(3, engine.state.player(1).health)
    }

    @Test
    fun `Calamity Janet responde a un BANG con otro BANG como Fallaste`() {
        val bang = card(CardKind.BANG)
        val janetBang = card(CardKind.BANG, Suit.DIAMONDS, Rank.FIVE)
        val players = listOf(
            player(0, Role.SHERIFF, hand = listOf(bang)),
            player(1, Role.OUTLAW, character = CharacterId.CALAMITY_JANET, health = 4, hand = listOf(janetBang)),
            player(2, Role.OUTLAW),
            player(3, Role.RENEGADE),
        )
        val engine = engineFrom(gameState(players))
        engine.accept(0, GameCommand.PlayCard(bang.id, targetSeat = 1))
        val req = engine.request() as DecisionRequest.React
        assertTrue(req.options.any { it is GameCommand.Respond && it.cardIds == listOf(janetBang.id) })
        engine.accept(1, GameCommand.Respond(listOf(janetBang.id)))
        assertEquals(4, engine.state.player(1).health)
    }
}
