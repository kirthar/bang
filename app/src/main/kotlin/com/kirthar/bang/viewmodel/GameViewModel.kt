package com.kirthar.bang.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kirthar.bang.ai.AiAgent
import com.kirthar.bang.core.agent.PlayerAgent
import com.kirthar.bang.core.command.DrawSource
import com.kirthar.bang.core.command.GameCommand
import com.kirthar.bang.core.engine.GameEngine
import com.kirthar.bang.core.engine.GameLoop
import com.kirthar.bang.core.event.GameEvent
import com.kirthar.bang.core.model.GameConfig
import com.kirthar.bang.core.model.PlayerSetup
import com.kirthar.bang.core.view.DecisionRequest
import com.kirthar.bang.core.view.PlayerGameView
import com.kirthar.bang.di.GameDependencies
import com.kirthar.bang.fake.FakeAiStrategy
import com.kirthar.bang.fake.FakeGameEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel de la pantalla de partida. Crea el motor (real si
 * [GameDependencies.engineFactory] ya está fijado por la integración, o
 * [FakeGameEngine] mientras se desarrolla en paralelo) y lanza [GameLoop] en
 * [viewModelScope]: el asiento 0 (humano) usa [HumanAgent], que expone la decisión
 * pendiente a esta clase a través de [PlayerGameView.myPendingRequest]; los demás
 * asientos usan [AiAgent] con la dificultad elegida en la configuración.
 *
 * Expone el estado de la partida como `StateFlow` para que [GameScreen] sea una
 * función pura de ese estado, y traduce los toques del jugador (carta, objetivo,
 * botones de acción) en el [GameCommand] exacto —tomado siempre de
 * `DecisionRequest.options`— que se entrega a [HumanAgent.submit].
 */
class GameViewModel(private val setup: GameSetupConfig) : ViewModel() {

    private val humanAgent = HumanAgent()

    private val _humanView = MutableStateFlow<PlayerGameView?>(null)
    val humanView: StateFlow<PlayerGameView?> = _humanView.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _selection = MutableStateFlow(UiSelectionState())
    val selection: StateFlow<UiSelectionState> = _selection.asStateFlow()

    private val _gameOver = MutableStateFlow<GameOverInfo?>(null)
    val gameOver: StateFlow<GameOverInfo?> = _gameOver.asStateFlow()

    private var logIdSeq = 0L
    private lateinit var engine: GameEngine

    init {
        viewModelScope.launch { runGame() }
    }

    private suspend fun runGame() {
        val gameConfig = buildGameConfig(setup)
        engine = (GameDependencies.engineFactory ?: FakeGameEngine.Factory).create(gameConfig)
        _humanView.value = engine.viewFor(HUMAN_SEAT)

        val agents = buildAgents(gameConfig)
        val loop = GameLoop(engine = engine, agents = agents, onEvent = ::handleEvent)
        val result = loop.run()

        val finalView = engine.viewFor(HUMAN_SEAT)
        _humanView.value = finalView
        _gameOver.value = GameOverInfo(result = result, players = finalView.players)
    }

    private fun buildAgents(gameConfig: GameConfig): Map<Int, PlayerAgent> {
        val strategyFactory = GameDependencies.aiStrategyFactory ?: FakeAiStrategy.Factory
        return gameConfig.players.indices.associateWith { seat ->
            if (seat == HUMAN_SEAT) {
                humanAgent
            } else {
                val strategy = strategyFactory.create(setup.difficulty, gameConfig.seed + seat)
                AiAgent(strategy, thinkDelayMs = AI_THINK_DELAY_MS)
            }
        }
    }

    private fun buildGameConfig(setup: GameSetupConfig): GameConfig {
        val human = PlayerSetup(
            name = setup.playerName.ifBlank { "Jugador" },
            character = setup.characterId,
        )
        val ais = (1 until setup.playerCount).map { index ->
            PlayerSetup(name = "IA $index", character = null)
        }
        return GameConfig(players = listOf(human) + ais, seed = Random.nextLong())
    }

    private fun handleEvent(event: GameEvent) {
        val nameOf: (Int) -> String = { seat -> engine.state.players.getOrNull(seat)?.name ?: "Asiento $seat" }
        _log.value = _log.value + LogEntry(id = logIdSeq++, text = describeEvent(event, nameOf))
        _humanView.value = engine.viewFor(HUMAN_SEAT)
    }

    private fun currentRequest(): DecisionRequest? = _humanView.value?.myPendingRequest

    private fun submitCommand(command: GameCommand) {
        humanAgent.submit(command)
        _selection.value = UiSelectionState()
    }

    // ---------------------------------------------------------------------
    // Intenciones del jugador desde la UI
    // ---------------------------------------------------------------------

    /** El jugador toca una carta de su mano. */
    fun onHandCardTap(cardId: Int) {
        val request = currentRequest() ?: return
        val sel = _selection.value
        if (sel.pendingPlay != null) return // hay una jugada esperando confirmación
        when (request) {
            is DecisionRequest.PlayOrPass ->
                if (sel.abilityMode) {
                    toggleMultiSelection(cardId, request.options)
                } else {
                    handlePlayCardTap(cardId, request.options)
                }
            is DecisionRequest.React -> toggleMultiSelection(cardId, request.options)
            is DecisionRequest.DiscardToHandLimit -> toggleMultiSelection(cardId, request.options)
            else -> Unit
        }
    }

    private fun handlePlayCardTap(cardId: Int, options: List<GameCommand>) {
        val sel = _selection.value
        if (sel.selectedCardId == cardId) {
            // Segundo toque sobre la misma carta: deshacer selección.
            _selection.value = UiSelectionState()
            return
        }
        val matches = options.filterIsInstance<GameCommand.PlayCard>().filter { it.cardId == cardId }
        if (matches.isEmpty()) return // carta no jugable ahora mismo

        val withoutTarget = matches.firstOrNull { it.targetSeat == null }
        val targetSeats = matches.mapNotNull { it.targetSeat }.toSet()
        if (targetSeats.isEmpty() && withoutTarget != null) {
            _selection.value = UiSelectionState(pendingPlay = withoutTarget)
        } else {
            _selection.value = UiSelectionState(selectedCardId = cardId, targetableSeats = targetSeats)
        }
    }

    /** El jugador toca a un rival (o a sí mismo) como objetivo de la carta seleccionada. */
    fun onTargetSeatTap(seat: Int) {
        val request = currentRequest() as? DecisionRequest.PlayOrPass ?: return
        val sel = _selection.value
        if (sel.pendingPlay != null) return // hay una jugada esperando confirmación
        val cardId = sel.selectedCardId ?: return
        if (seat !in sel.targetableSeats) return
        val option = request.options.filterIsInstance<GameCommand.PlayCard>()
            .firstOrNull { it.cardId == cardId && it.targetSeat == seat } ?: return
        _selection.value = sel.copy(targetableSeats = emptySet(), pendingPlay = option)
    }

    /** Confirma en el diálogo la jugada pendiente y la envía al motor. */
    fun onConfirmPlay() {
        val pending = _selection.value.pendingPlay ?: return
        submitCommand(pending)
    }

    /** El jugador toca una de las cartas ofrecidas (Emporio, Kit Carlson, «¡desenfunda!» de Lucky Duke...). */
    fun onOfferedCardTap(cardId: Int) {
        val request = currentRequest() as? DecisionRequest.PickCard ?: return
        val option = request.options.filterIsInstance<GameCommand.ChooseCard>()
            .firstOrNull { it.cardId == cardId } ?: return
        submitCommand(option)
    }

    /** El jugador elige de dónde robar (habilidad de Jesse Jones o Pedro Ramírez). */
    fun onDrawSourceTap(source: DrawSource) {
        val request = currentRequest() as? DecisionRequest.ChooseDraw ?: return
        val option = request.options.filterIsInstance<GameCommand.ChooseDrawSource>()
            .firstOrNull { it.source == source } ?: return
        submitCommand(option)
    }

    /** Botón «no responder / encajar el golpe» ante un ataque. */
    fun onTakeHit() {
        val request = currentRequest() ?: return
        val option = request.options.firstOrNull { it is GameCommand.TakeHit } ?: return
        submitCommand(option)
    }

    /** Botón «pasar / terminar turno». */
    fun onEndPlayPhase() {
        val request = currentRequest() ?: return
        val option = request.options.firstOrNull { it is GameCommand.EndPlayPhase } ?: return
        submitCommand(option)
    }

    /** Botón «usar habilidad» (p. ej. Sid Ketchum): entra o sale del modo de selección para ella. */
    fun onToggleAbilityMode() {
        val hasAbilityOption = currentRequest()?.options.orEmpty().any { it is GameCommand.UseCharacterAbility }
        if (!hasAbilityOption) return
        val sel = _selection.value
        _selection.value = UiSelectionState(abilityMode = !sel.abilityMode)
    }

    /** Cancela cualquier selección en curso (carta, objetivo o selección múltiple). */
    fun onCancelSelection() {
        _selection.value = UiSelectionState()
    }

    private fun toggleMultiSelection(cardId: Int, options: List<GameCommand>) {
        val relevantIds = options.mapNotNull(::cardIdsOf).flatten().toSet()
        if (cardId !in relevantIds) return

        val sel = _selection.value
        val newSelection = sel.multiSelected.toMutableSet().also { set ->
            if (!set.add(cardId)) set.remove(cardId)
        }
        val match = options.firstOrNull { cardIdsOf(it)?.toSet() == newSelection }
        if (match != null) {
            submitCommand(match)
        } else {
            _selection.value = sel.copy(multiSelected = newSelection)
        }
    }

    private fun cardIdsOf(command: GameCommand): List<Int>? = when (command) {
        is GameCommand.Respond -> command.cardIds
        is GameCommand.Discard -> command.cardIds
        is GameCommand.UseCharacterAbility -> command.cardIds
        else -> null
    }

    companion object {
        private const val HUMAN_SEAT = 0
        private const val AI_THINK_DELAY_MS = 600L

        fun factory(setup: GameSetupConfig): ViewModelProvider.Factory = GameViewModelFactory(setup)
    }
}

/** Factoría estándar de `androidx.lifecycle` para construir [GameViewModel] con su configuración. */
class GameViewModelFactory(private val setup: GameSetupConfig) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameViewModel::class.java)) {
            "GameViewModelFactory solo puede crear GameViewModel, no $modelClass"
        }
        return GameViewModel(setup) as T
    }
}
