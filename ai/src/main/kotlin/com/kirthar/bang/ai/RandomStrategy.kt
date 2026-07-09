package com.kirthar.bang.ai

import com.kirthar.bang.ai.support.amAtFullHealth
import com.kirthar.bang.ai.support.cardInHand
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.model.CardKind
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import kotlin.random.Random

/**
 * IA fácil ([AiDifficulty.EASY]): elige una opción legal **al azar** de
 * [DecisionRequest.options], con dos matices que evitan jugadas absurdas:
 *
 * 1. No elige [GameCommand.TakeHit] si en las opciones hay alguna respuesta con cartas
 *    ([GameCommand.Respond]): prefiere defenderse si puede.
 * 2. No juega Birra ([CardKind.BEER]) estando a la vida máxima (no tendría efecto útil).
 *
 * Si tras aplicar los filtros no queda ninguna opción, se recurre a la lista completa
 * para no bloquear nunca la partida.
 *
 * Es determinista para una [seed] dada, requisito de las simulaciones reproducibles y
 * base de los *rollouts* de [com.kirthar.bang.ai.mcts.MctsStrategy].
 */
class RandomStrategy(seed: Long) : AiStrategy {

    private val random = Random(seed)

    override fun decide(view: PlayerGameView, request: DecisionRequest): GameCommand {
        val legal = request.options
        val filtered = legal.filter { keep(view, legal, it) }
        val pool = filtered.ifEmpty { legal }
        return pool[random.nextInt(pool.size)]
    }

    /** ¿Debe conservarse esta opción tras los filtros heurísticos mínimos? */
    private fun keep(view: PlayerGameView, all: List<GameCommand>, option: GameCommand): Boolean =
        when (option) {
            is GameCommand.TakeHit -> all.none { it is GameCommand.Respond }
            is GameCommand.PlayCard -> !isUselessBeer(view, option)
            else -> true
        }

    /** Birra jugada a la vida máxima (sin efecto útil). */
    private fun isUselessBeer(view: PlayerGameView, play: GameCommand.PlayCard): Boolean {
        val card = view.cardInHand(play.cardId) ?: return false
        return card.kind == CardKind.BEER && view.amAtFullHealth
    }
}
