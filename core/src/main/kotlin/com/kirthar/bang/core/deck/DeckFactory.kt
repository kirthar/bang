package com.kirthar.bang.core.deck

import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CardKind.BANG
import com.kirthar.bang.core.model.CardKind.BARREL
import com.kirthar.bang.core.model.CardKind.BEER
import com.kirthar.bang.core.model.CardKind.CAT_BALOU
import com.kirthar.bang.core.model.CardKind.DUEL
import com.kirthar.bang.core.model.CardKind.DYNAMITE
import com.kirthar.bang.core.model.CardKind.GATLING
import com.kirthar.bang.core.model.CardKind.GENERAL_STORE
import com.kirthar.bang.core.model.CardKind.INDIANS
import com.kirthar.bang.core.model.CardKind.JAIL
import com.kirthar.bang.core.model.CardKind.MISSED
import com.kirthar.bang.core.model.CardKind.MUSTANG
import com.kirthar.bang.core.model.CardKind.PANIC
import com.kirthar.bang.core.model.CardKind.REMINGTON
import com.kirthar.bang.core.model.CardKind.REV_CARABINE
import com.kirthar.bang.core.model.CardKind.SALOON
import com.kirthar.bang.core.model.CardKind.SCHOFIELD
import com.kirthar.bang.core.model.CardKind.SCOPE
import com.kirthar.bang.core.model.CardKind.STAGECOACH
import com.kirthar.bang.core.model.CardKind.VOLCANIC
import com.kirthar.bang.core.model.CardKind.WELLS_FARGO
import com.kirthar.bang.core.model.CardKind.WINCHESTER
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Rank.ACE
import com.kirthar.bang.core.model.Rank.EIGHT
import com.kirthar.bang.core.model.Rank.FIVE
import com.kirthar.bang.core.model.Rank.FOUR
import com.kirthar.bang.core.model.Rank.JACK
import com.kirthar.bang.core.model.Rank.KING
import com.kirthar.bang.core.model.Rank.NINE
import com.kirthar.bang.core.model.Rank.QUEEN
import com.kirthar.bang.core.model.Rank.SEVEN
import com.kirthar.bang.core.model.Rank.SIX
import com.kirthar.bang.core.model.Rank.TEN
import com.kirthar.bang.core.model.Rank.THREE
import com.kirthar.bang.core.model.Rank.TWO
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.model.Suit.CLUBS
import com.kirthar.bang.core.model.Suit.DIAMONDS
import com.kirthar.bang.core.model.Suit.HEARTS
import com.kirthar.bang.core.model.Suit.SPADES

/**
 * Composición exacta del mazo base de 80 cartas, con palo y valor de cada una
 * (los palos importan para «¡desenfunda!»: Barril = corazón, Cárcel = corazón,
 * Dinamita = picas 2-9).
 */
object DeckFactory {

    private val composition: List<Triple<CardKind, Suit, Rank>> = buildList {
        // BANG! x25: A♠, 2♦-A♦, 2♣-9♣, Q♥ K♥ A♥
        add(Triple(BANG, SPADES, ACE))
        Rank.entries.forEach { add(Triple(BANG, DIAMONDS, it)) }
        Rank.entries.takeWhile { it <= NINE }.forEach { add(Triple(BANG, CLUBS, it)) }
        listOf(QUEEN, KING, ACE).forEach { add(Triple(BANG, HEARTS, it)) }

        // ¡Fallaste! x12: 2♠-8♠, 10♣-A♣
        Rank.entries.takeWhile { it <= EIGHT }.forEach { add(Triple(MISSED, SPADES, it)) }
        Rank.entries.dropWhile { it < TEN }.forEach { add(Triple(MISSED, CLUBS, it)) }

        // Birra x6: 6♥-J♥
        listOf(SIX, SEVEN, EIGHT, NINE, TEN, JACK).forEach { add(Triple(BEER, HEARTS, it)) }

        // ¡Pánico! x4
        add(Triple(PANIC, HEARTS, JACK)); add(Triple(PANIC, HEARTS, QUEEN))
        add(Triple(PANIC, HEARTS, ACE)); add(Triple(PANIC, DIAMONDS, EIGHT))

        // Cat Balou x4
        add(Triple(CAT_BALOU, HEARTS, KING)); add(Triple(CAT_BALOU, DIAMONDS, NINE))
        add(Triple(CAT_BALOU, DIAMONDS, TEN)); add(Triple(CAT_BALOU, DIAMONDS, JACK))

        // Diligencia x2, Wells Fargo x1
        add(Triple(STAGECOACH, SPADES, NINE)); add(Triple(STAGECOACH, SPADES, NINE))
        add(Triple(WELLS_FARGO, HEARTS, THREE))

        // Emporio x2, Gatling x1, ¡Indios! x2, Duelo x3, Salón x1
        add(Triple(GENERAL_STORE, CLUBS, NINE)); add(Triple(GENERAL_STORE, SPADES, QUEEN))
        add(Triple(GATLING, HEARTS, TEN))
        add(Triple(INDIANS, DIAMONDS, KING)); add(Triple(INDIANS, DIAMONDS, ACE))
        add(Triple(DUEL, DIAMONDS, QUEEN)); add(Triple(DUEL, SPADES, JACK)); add(Triple(DUEL, CLUBS, EIGHT))
        add(Triple(SALOON, HEARTS, FIVE))

        // Cárcel x3, Dinamita x1
        add(Triple(JAIL, SPADES, TEN)); add(Triple(JAIL, SPADES, JACK)); add(Triple(JAIL, HEARTS, FOUR))
        add(Triple(DYNAMITE, HEARTS, TWO))

        // Barril x2, Mustang x2, Mira x1
        add(Triple(BARREL, SPADES, QUEEN)); add(Triple(BARREL, SPADES, KING))
        add(Triple(MUSTANG, HEARTS, EIGHT)); add(Triple(MUSTANG, HEARTS, NINE))
        add(Triple(SCOPE, SPADES, ACE))

        // Armas: Volcanic x2, Schofield x3, Remington x1, Rev. Carabine x1, Winchester x1
        add(Triple(VOLCANIC, SPADES, TEN)); add(Triple(VOLCANIC, CLUBS, TEN))
        add(Triple(SCHOFIELD, CLUBS, JACK)); add(Triple(SCHOFIELD, CLUBS, QUEEN))
        add(Triple(SCHOFIELD, SPADES, KING))
        add(Triple(REMINGTON, CLUBS, KING))
        add(Triple(REV_CARABINE, CLUBS, ACE))
        add(Triple(WINCHESTER, SPADES, EIGHT))
    }

    /** Crea el mazo completo de 80 cartas, sin barajar y con ids estables 0..79. */
    fun createBaseDeck(): List<Card> {
        check(composition.size == 80) { "El mazo base debe tener 80 cartas (hay ${composition.size})" }
        return composition.mapIndexed { index, (kind, suit, rank) -> Card(index, kind, suit, rank) }
    }
}
