package com.kirthar.bang.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirthar.bang.R
import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PickPurpose
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.fake.previewLogEntries
import com.kirthar.bang.fake.previewMidGameView
import com.kirthar.bang.fake.previewReactView
import com.kirthar.bang.ui.components.CardView
import com.kirthar.bang.ui.components.DeckPile
import com.kirthar.bang.ui.components.DiscardPile
import com.kirthar.bang.ui.components.GameTexts
import com.kirthar.bang.ui.components.PlayerBadge
import com.kirthar.bang.ui.components.TableBackground
import com.kirthar.bang.ui.components.WesternButton
import com.kirthar.bang.ui.theme.BangTheme
import com.kirthar.bang.viewmodel.GameViewModel
import com.kirthar.bang.viewmodel.LogEntry
import com.kirthar.bang.viewmodel.UiSelectionState

/**
 * Pantalla de partida: mesa con los rivales, mazo/descartes en el centro, mano propia
 * abajo, zona de acciones contextuales según la decisión pendiente, y log de eventos.
 */
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val view by viewModel.humanView.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    val currentView = view
    if (currentView == null) {
        TableBackground(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.game_loading),
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }

    GameScreenContent(
        view = currentView,
        log = log,
        selection = selection,
        onHandCardTap = viewModel::onHandCardTap,
        onTargetSeatTap = viewModel::onTargetSeatTap,
        onOfferedCardTap = viewModel::onOfferedCardTap,
        onDrawSourceTap = viewModel::onDrawSourceTap,
        onTakeHit = viewModel::onTakeHit,
        onEndPlayPhase = viewModel::onEndPlayPhase,
        onToggleAbilityMode = viewModel::onToggleAbilityMode,
    )
}

@Composable
private fun GameScreenContent(
    view: PlayerGameView,
    log: List<LogEntry>,
    selection: UiSelectionState,
    onHandCardTap: (Int) -> Unit,
    onTargetSeatTap: (Int) -> Unit,
    onOfferedCardTap: (Int) -> Unit,
    onDrawSourceTap: (DrawSource) -> Unit,
    onTakeHit: () -> Unit,
    onEndPlayPhase: () -> Unit,
    onToggleAbilityMode: () -> Unit,
) {
    val request = view.myPendingRequest
    val playableIds = playableCardIds(request, selection.abilityMode)

    TableBackground(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(8.dp),
            ) {
                // Rivales
                val rivals = view.players.filter { it.seat != view.mySeat }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rivals) { player ->
                        PlayerBadge(
                            player = player,
                            isCurrentTurn = player.seat == view.currentTurnSeat,
                            isTargetable = player.seat in selection.targetableSeats,
                            onClick = { onTargetSeatTap(player.seat) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TurnBanner(view)
                Spacer(modifier = Modifier.height(8.dp))

                // Mazo y descartes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    DeckPile(count = view.deckSize)
                    Spacer(modifier = Modifier.width(24.dp))
                    DiscardPile(top = view.discardTop)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    ActionArea(
                        request = request,
                        selection = selection,
                        view = view,
                        onOfferedCardTap = onOfferedCardTap,
                        onDrawSourceTap = onDrawSourceTap,
                        onTakeHit = onTakeHit,
                        onEndPlayPhase = onEndPlayPhase,
                        onToggleAbilityMode = onToggleAbilityMode,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Mano propia
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerBadge(player = view.me, isCurrentTurn = view.currentTurnSeat == view.mySeat)
                    Spacer(modifier = Modifier.width(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(view.myHand) { card ->
                            CardView(
                                card = card,
                                selected = card.id == selection.selectedCardId || card.id in selection.multiSelected,
                                enabled = card.id in playableIds,
                                onClick = { onHandCardTap(card.id) },
                            )
                        }
                    }
                }
            }

            // Log de eventos
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
            ) {
                Text(text = stringResource(R.string.game_log_title), style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(log, key = { it.id }) { entry ->
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TurnBanner(view: PlayerGameView) {
    val text = if (view.currentTurnSeat == view.mySeat) {
        stringResource(R.string.game_your_turn)
    } else {
        val name = view.players.firstOrNull { it.seat == view.currentTurnSeat }?.name.orEmpty()
        stringResource(R.string.game_turn_of, name)
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ActionArea(
    request: DecisionRequest?,
    selection: UiSelectionState,
    view: PlayerGameView,
    onOfferedCardTap: (Int) -> Unit,
    onDrawSourceTap: (DrawSource) -> Unit,
    onTakeHit: () -> Unit,
    onEndPlayPhase: () -> Unit,
    onToggleAbilityMode: () -> Unit,
) {
    when (request) {
        null -> Text(
            text = stringResource(R.string.game_waiting_others),
            color = MaterialTheme.colorScheme.onBackground,
        )

        is DecisionRequest.PlayOrPass -> {
            val hasAbility = request.options.any { it is GameCommand.UseCharacterAbility }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WesternButton(text = stringResource(R.string.game_end_turn), onClick = onEndPlayPhase)
                if (hasAbility) {
                    WesternButton(
                        text = stringResource(R.string.game_use_ability),
                        onClick = onToggleAbilityMode,
                        enabled = true,
                    )
                }
            }
            if (selection.abilityMode) {
                Text(
                    text = stringResource(R.string.game_ability_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        is DecisionRequest.React -> Column {
            Text(
                text = stringResource(
                    R.string.game_react_prompt,
                    GameTexts.cardName(request.kind),
                    view.players.firstOrNull { it.seat == request.sourceSeat }?.name.orEmpty(),
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            WesternButton(text = stringResource(R.string.game_take_hit), onClick = onTakeHit)
        }

        is DecisionRequest.DiscardToHandLimit -> Text(
            text = stringResource(R.string.game_discard_prompt, request.excess),
        )

        is DecisionRequest.PickCard -> Column {
            Text(text = pickPurposeText(request.purpose))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                request.cards.forEach { card ->
                    CardView(card = card, onClick = { onOfferedCardTap(card.id) })
                }
            }
        }

        is DecisionRequest.ChooseDraw -> Column {
            Text(text = stringResource(R.string.game_choose_draw_source))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                request.sources.forEach { source ->
                    val label = when (source) {
                        is DrawSource.Deck -> stringResource(R.string.game_draw_from_deck)
                        is DrawSource.DiscardPile -> stringResource(R.string.game_draw_from_discard)
                        is DrawSource.PlayerHand -> stringResource(
                            R.string.game_draw_from_player,
                            view.players.firstOrNull { it.seat == source.seat }?.name.orEmpty(),
                        )
                    }
                    WesternButton(text = label, onClick = { onDrawSourceTap(source) })
                }
            }
        }
    }
}

@Composable
private fun pickPurposeText(purpose: PickPurpose): String = when (purpose) {
    PickPurpose.GENERAL_STORE -> stringResource(R.string.game_pick_general_store)
    PickPurpose.KIT_CARLSON -> stringResource(R.string.game_pick_kit_carlson)
    PickPurpose.LUCKY_DUKE -> stringResource(R.string.game_pick_lucky_duke)
    PickPurpose.PANIC_TARGET -> stringResource(R.string.game_pick_panic_target)
}

/** Ids de carta de la mano que se pueden tocar ahora mismo, según la decisión pendiente. */
private fun playableCardIds(request: DecisionRequest?, abilityMode: Boolean): Set<Int> = when (request) {
    is DecisionRequest.PlayOrPass ->
        if (abilityMode) {
            request.options.filterIsInstance<GameCommand.UseCharacterAbility>().flatMap { it.cardIds }.toSet()
        } else {
            request.options.filterIsInstance<GameCommand.PlayCard>().map { it.cardId }.toSet()
        }
    is DecisionRequest.React -> request.options.filterIsInstance<GameCommand.Respond>().flatMap { it.cardIds }.toSet()
    is DecisionRequest.DiscardToHandLimit ->
        request.options.filterIsInstance<GameCommand.Discard>().flatMap { it.cardIds }.toSet()
    else -> emptySet()
}

@Preview(widthDp = 1000, heightDp = 500)
@Composable
private fun GameScreenMidGamePreview() {
    BangTheme {
        GameScreenContent(
            view = previewMidGameView(),
            log = previewLogEntries(),
            selection = UiSelectionState(),
            onHandCardTap = {},
            onTargetSeatTap = {},
            onOfferedCardTap = {},
            onDrawSourceTap = {},
            onTakeHit = {},
            onEndPlayPhase = {},
            onToggleAbilityMode = {},
        )
    }
}

@Preview(widthDp = 1000, heightDp = 500)
@Composable
private fun GameScreenReactPreview() {
    BangTheme {
        GameScreenContent(
            view = previewReactView(),
            log = previewLogEntries(),
            selection = UiSelectionState(),
            onHandCardTap = {},
            onTargetSeatTap = {},
            onOfferedCardTap = {},
            onDrawSourceTap = {},
            onTakeHit = {},
            onEndPlayPhase = {},
            onToggleAbilityMode = {},
        )
    }
}
