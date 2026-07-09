package com.kirthar.bang.ai.support

import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.Card
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.core.view.PublicPlayerInfo

/**
 * Utilidades de solo lectura sobre [PlayerGameView] compartidas por todas las
 * estrategias. No contienen lógica de decisión, solo consultas cómodas.
 */

/** Busca en la mano propia la carta con el [id] dado, o `null` si no está. */
fun PlayerGameView.cardInHand(id: Int): Card? = myHand.firstOrNull { it.id == id }

/** La carta que juega este comando [GameCommand.PlayCard], resuelta contra la mano propia. */
fun PlayerGameView.playedCard(command: GameCommand.PlayCard): Card? = cardInHand(command.cardId)

/** Número de jugadores vivos (incluido uno mismo). */
val PlayerGameView.aliveCount: Int get() = players.count { it.isAlive }

/** Información pública de un asiento concreto. */
fun PlayerGameView.playerAt(seat: Int): PublicPlayerInfo = players[seat]

/** ¿Está uno mismo a la vida máxima? */
val PlayerGameView.amAtFullHealth: Boolean get() = me.health >= me.maxHealth

/** Rivales vivos (todos menos uno mismo). */
val PlayerGameView.livingOpponents: List<PublicPlayerInfo>
    get() = players.filter { it.isAlive && it.seat != mySeat }

/** ¿Tiene este jugador una carta de este tipo en juego (equipada)? */
fun PublicPlayerInfo.hasInPlay(kind: CardKind): Boolean = inPlay.any { it.kind == kind }

/** El arma equipada (si la hay). */
val PublicPlayerInfo.weapon: Card? get() = inPlay.firstOrNull { it.kind.isWeapon }
