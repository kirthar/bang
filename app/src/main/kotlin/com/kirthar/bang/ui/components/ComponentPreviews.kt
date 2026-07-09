package com.kirthar.bang.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.model.CharacterId
import com.kirthar.bang.core.model.Rank
import com.kirthar.bang.core.model.Role
import com.kirthar.bang.core.model.Suit
import com.kirthar.bang.core.view.PublicPlayerInfo
import com.kirthar.bang.ui.theme.BangTheme

/**
 * Previews de todos los componentes visuales para Android Studio. No forman parte
 * de la app final; sirven para revisar el arte sin arrancar una partida.
 */

private fun sampleCard(kind: CardKind, suit: Suit = Suit.SPADES, rank: Rank = Rank.ACE, id: Int = 0) =
    Card(id = id, kind = kind, suit = suit, rank = rank)

private fun samplePlayer(
    seat: Int = 0,
    name: String = "Kirthar",
    character: CharacterId = CharacterId.WILLY_THE_KID,
    health: Int = 3,
    maxHealth: Int = 5,
    handCount: Int = 4,
    inPlay: List<Card> = emptyList(),
    role: Role? = null,
    isAlive: Boolean = true,
) = PublicPlayerInfo(
    seat = seat,
    name = name,
    character = character,
    health = health,
    maxHealth = maxHealth,
    handCount = handCount,
    inPlay = inPlay,
    role = role,
    isAlive = isAlive,
    distance = if (seat == 0) 0 else 1,
)

@Preview(name = "Cartas marrones", widthDp = 760, heightDp = 420, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewBrownCards() {
    BangTheme {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardView(sampleCard(CardKind.BANG, Suit.SPADES, Rank.ACE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.MISSED, Suit.CLUBS, Rank.TEN), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.BEER, Suit.HEARTS, Rank.SIX), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.PANIC, Suit.HEARTS, Rank.QUEEN), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.CAT_BALOU, Suit.DIAMONDS, Rank.KING), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.GATLING, Suit.HEARTS, Rank.TEN), modifier = Modifier.width(100.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardView(sampleCard(CardKind.INDIANS, Suit.DIAMONDS, Rank.ACE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.DUEL, Suit.SPADES, Rank.JACK), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.STAGECOACH, Suit.SPADES, Rank.NINE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.WELLS_FARGO, Suit.HEARTS, Rank.THREE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.GENERAL_STORE, Suit.CLUBS, Rank.NINE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.SALOON, Suit.HEARTS, Rank.FIVE), modifier = Modifier.width(100.dp))
            }
        }
    }
}

@Preview(name = "Cartas azules y armas", widthDp = 760, heightDp = 420, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewBlueCards() {
    BangTheme {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardView(sampleCard(CardKind.BARREL, Suit.SPADES, Rank.QUEEN), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.MUSTANG, Suit.HEARTS, Rank.EIGHT), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.SCOPE, Suit.SPADES, Rank.ACE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.JAIL, Suit.SPADES, Rank.TEN), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.DYNAMITE, Suit.HEARTS, Rank.TWO), modifier = Modifier.width(100.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardView(sampleCard(CardKind.VOLCANIC, Suit.SPADES, Rank.TEN), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.SCHOFIELD, Suit.CLUBS, Rank.KING), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.REMINGTON, Suit.CLUBS, Rank.KING), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.REV_CARABINE, Suit.CLUBS, Rank.ACE), modifier = Modifier.width(100.dp))
                CardView(sampleCard(CardKind.WINCHESTER, Suit.SPADES, Rank.EIGHT), modifier = Modifier.width(100.dp))
            }
        }
    }
}

@Preview(name = "Estados de carta", widthDp = 620, heightDp = 220, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewCardStates() {
    BangTheme {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardView(sampleCard(CardKind.BANG, Suit.HEARTS, Rank.KING), modifier = Modifier.width(100.dp))
            CardView(sampleCard(CardKind.BANG, Suit.HEARTS, Rank.KING), selected = true, modifier = Modifier.width(100.dp))
            CardView(sampleCard(CardKind.BANG, Suit.HEARTS, Rank.KING), enabled = false, modifier = Modifier.width(100.dp))
            CardView(card = null, modifier = Modifier.width(100.dp))
            CardView(sampleCard(CardKind.BANG), faceUp = false, modifier = Modifier.width(100.dp))
            CardView(sampleCard(CardKind.WINCHESTER, Suit.SPADES, Rank.EIGHT), modifier = Modifier.width(60.dp))
        }
    }
}

@Preview(name = "Pilas de mazo y descartes", widthDp = 320, heightDp = 220, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewPiles() {
    BangTheme {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DeckPile(count = 42)
            DiscardPile(top = sampleCard(CardKind.CAT_BALOU, Suit.DIAMONDS, Rank.JACK))
            DiscardPile(top = null)
        }
    }
}

@Preview(name = "PlayerBadge en cada estado", widthDp = 640, heightDp = 460, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewPlayerBadges() {
    BangTheme {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerBadge(
                    player = samplePlayer(name = "Sheriff Ana", character = CharacterId.ROSE_DOOLAN, role = Role.SHERIFF, health = 5, maxHealth = 5),
                    isCurrentTurn = true,
                )
                PlayerBadge(
                    player = samplePlayer(
                        seat = 1,
                        name = "Bandido Bill",
                        character = CharacterId.SLAB_THE_KILLER,
                        health = 2,
                        maxHealth = 4,
                        inPlay = listOf(
                            sampleCard(CardKind.MUSTANG, Suit.HEARTS, Rank.EIGHT, id = 1),
                            sampleCard(CardKind.WINCHESTER, Suit.SPADES, Rank.EIGHT, id = 2),
                        ),
                    ),
                    isCurrentTurn = false,
                    isTargetable = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerBadge(
                    player = samplePlayer(seat = 2, name = "Suzy", character = CharacterId.SUZY_LAFAYETTE, health = 4, maxHealth = 4, handCount = 1),
                    isCurrentTurn = false,
                )
                PlayerBadge(
                    player = samplePlayer(seat = 3, name = "Difunto Dan", character = CharacterId.BART_CASSIDY, health = 0, maxHealth = 4, handCount = 0, role = Role.OUTLAW, isAlive = false),
                    isCurrentTurn = false,
                )
            }
        }
    }
}

@Preview(name = "Avatares de los 16 personajes", widthDp = 520, heightDp = 160, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewAvatars() {
    BangTheme {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CharacterId.entries.take(8).forEach { CharacterAvatar(character = it, size = 48.dp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CharacterId.entries.drop(8).forEach { CharacterAvatar(character = it, size = 48.dp) }
            }
        }
    }
}

@Preview(name = "Botón western", widthDp = 400, heightDp = 140, showBackground = true, backgroundColor = 0xFF221108)
@Composable
private fun PreviewWesternButton() {
    BangTheme {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            WesternButton(text = "Jugar", onClick = {})
            WesternButton(text = "Pasar turno", enabled = false, onClick = {})
        }
    }
}

@Preview(name = "Mesa de juego", widthDp = 800, heightDp = 480, showBackground = true)
@Composable
private fun PreviewTable() {
    BangTheme {
        TableBackground(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = androidx.compose.ui.graphics.Color.Transparent,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DeckPile(count = 61)
                    DiscardPile(top = sampleCard(CardKind.BEER, Suit.HEARTS, Rank.SIX))
                }
            }
            Text(
                text = "¡BANG!",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
        }
    }
}
