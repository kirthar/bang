package com.kirthar.bang.core.deck

import com.kirthar.bang.core.model.CardKind
import kotlin.test.Test
import kotlin.test.assertEquals

class DeckFactoryTest {

    @Test
    fun `el mazo base tiene 80 cartas con ids unicos`() {
        val deck = DeckFactory.createBaseDeck()
        assertEquals(80, deck.size)
        assertEquals(80, deck.map { it.id }.distinct().size)
    }

    @Test
    fun `la composicion por tipo coincide con las reglas`() {
        val byKind = DeckFactory.createBaseDeck().groupingBy { it.kind }.eachCount()
        val expected = mapOf(
            CardKind.BANG to 25, CardKind.MISSED to 12, CardKind.BEER to 6,
            CardKind.PANIC to 4, CardKind.CAT_BALOU to 4, CardKind.STAGECOACH to 2,
            CardKind.WELLS_FARGO to 1, CardKind.GATLING to 1, CardKind.INDIANS to 2,
            CardKind.DUEL to 3, CardKind.GENERAL_STORE to 2, CardKind.SALOON to 1,
            CardKind.JAIL to 3, CardKind.DYNAMITE to 1, CardKind.BARREL to 2,
            CardKind.MUSTANG to 2, CardKind.SCOPE to 1, CardKind.VOLCANIC to 2,
            CardKind.SCHOFIELD to 3, CardKind.REMINGTON to 1,
            CardKind.REV_CARABINE to 1, CardKind.WINCHESTER to 1,
        )
        assertEquals(expected, byKind)
    }
}
