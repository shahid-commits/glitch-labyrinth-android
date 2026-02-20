package com.example.glitchlabyrinth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// GAME EVENT — one-shot audio signals emitted after each player action.
// Collected by GameScreen via LaunchedEffect → calls AudioManager.
// GameEngine stays 100% pure (no Android imports).
// ---------------------------------------------------------------------------
sealed class GameEvent {
    object Moved           : GameEvent()  // normal step, no hazard triggered
    object TrapHit         : GameEvent()  // stepped on TRAP or PIT
    object EnemyHit        : GameEvent()  // enemy contact damage taken
    object GlitchActivated : GameEvent()  // player activated a glitch
    object FloorCleared    : GameEvent()  // reached EXIT
    object RunOver         : GameEvent()  // stability depleted — game over
}

// ---------------------------------------------------------------------------
// SCREEN STATE — sealed class drives navigation between composables
// ---------------------------------------------------------------------------
sealed class ScreenState {
    object Title   : ScreenState()
    object Game    : ScreenState()
    object Summary : ScreenState()
    object Codex   : ScreenState()
    // Shown between floors: pick one of two glitch rewards
    data class GlitchReward(val options: List<GlitchDefinition>) : ScreenState()
}

// ---------------------------------------------------------------------------
// GAME VIEW MODEL
// ---------------------------------------------------------------------------
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Capture the Context once at construction — avoids calling the generic
    // getApplication<T>() repeatedly and eliminates all type-inference errors.
    private val appContext: Context = application.applicationContext

    // UI-visible screen state
    private val _screen = MutableStateFlow<ScreenState>(ScreenState.Title)
    val screen: StateFlow<ScreenState> = _screen.asStateFlow()

    // Core game state
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    // SaveData is now a top-level data class (not nested inside PersistenceManager),
    // which eliminates all "Unresolved reference" cascade errors.
    private var saveData: SaveData = PersistenceManager.load(appContext)

    // Random seed per run (changes each run)
    private var runSeed: Long = System.currentTimeMillis()

    // Game events — SharedFlow so each emission is received exactly once
    // (unlike StateFlow which only keeps the latest value).
    // extraBufferCapacity = 8 prevents drops if the collector is briefly slow.
    private val _gameEvent = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val gameEvent: SharedFlow<GameEvent> = _gameEvent.asSharedFlow()

    // ---------------------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------------------

    fun startNewRun() {
        runSeed = System.currentTimeMillis()
        _gameState.value = GameEngine.newRun(seed = runSeed)
        _screen.value = ScreenState.Game
    }

    fun openCodex() {
        _screen.value = ScreenState.Codex
    }

    fun backToTitle() {
        _screen.value = ScreenState.Title
    }

    // ---------------------------------------------------------------------------
    // GAME ACTIONS
    // ---------------------------------------------------------------------------

    /** Called when the player taps a directional button or swipes. */
    fun onMove(direction: Direction) {
        val state = _gameState.value ?: return
        val oldStability = state.playerState.stability
        val updated = GameEngine.applyPlayerMove(state, direction)
        _gameState.value = updated

        // Emit the sound event that best describes what just happened.
        // We check stability change and state flags to classify the event.
        viewModelScope.launch {
            when {
                updated.runOver ->
                    _gameEvent.emit(GameEvent.RunOver)

                GameEngine.isFloorComplete(updated) || updated.runWon ->
                    _gameEvent.emit(GameEvent.FloorCleared)

                updated.playerState.stability < oldStability -> {
                    // Damage taken — enemy contact or hazard tile?
                    val hitByEnemy = updated.floorState.enemies.any { enemy ->
                        enemy.x == updated.playerState.x && enemy.y == updated.playerState.y
                    }
                    if (hitByEnemy) _gameEvent.emit(GameEvent.EnemyHit)
                    else            _gameEvent.emit(GameEvent.TrapHit)
                }

                else -> _gameEvent.emit(GameEvent.Moved)
            }
        }

        when {
            updated.runOver -> onRunEnded(updated)
            GameEngine.isFloorComplete(updated) -> onFloorComplete(updated)
        }
    }

    /** Called when the player activates a glitch from their inventory. */
    fun onActivateGlitch(glitch: GlitchDefinition) {
        val state = _gameState.value ?: return
        val updated = GameEngine.activateGlitch(state, glitch)
        _gameState.value = updated

        // Always play the glitch sound — activation is always intentional.
        viewModelScope.launch { _gameEvent.emit(GameEvent.GlitchActivated) }
    }

    /** Player chose a glitch reward between floors. */
    fun onChooseGlitchReward(chosen: GlitchDefinition) {
        val state = _gameState.value ?: return
        val updated = GameEngine.advanceToNextFloor(
            state = state,
            chosenGlitch = chosen,
            seed = runSeed + state.currentFloorNumber
        )
        _gameState.value = updated
        _screen.value = ScreenState.Game
    }

    // ---------------------------------------------------------------------------
    // INTERNAL TRANSITIONS
    // ---------------------------------------------------------------------------

    private fun onFloorComplete(state: GameState) {
        if (state.runWon) {
            onRunEnded(state)
        } else {
            // Offer glitch reward
            val options = GameEngine.glitchRewardChoices(state.playerState.heldGlitches)
            _screen.value = ScreenState.GlitchReward(options)
        }
    }

    private fun onRunEnded(state: GameState) {
        // Persist results — use the captured appContext, no generics needed.
        PersistenceManager.save(
            context = appContext,
            floorsReached = state.currentFloorNumber,
            currentData = saveData
        )
        saveData = PersistenceManager.load(appContext)
        _screen.value = ScreenState.Summary
    }

    // ---------------------------------------------------------------------------
    // ACCESSORS FOR UI
    // ---------------------------------------------------------------------------

    fun runStats(): RunStats? {
        val state = _gameState.value ?: return null
        return GameEngine.buildRunStats(state)
    }

    fun unlockedGlitches(): List<GlitchDefinition> =
        GlitchRegistry.all.filter { it.id in saveData.unlockedGlitchIds }

    fun bestFloor(): Int = saveData.bestFloor
    fun totalRuns(): Int = saveData.totalRuns
}