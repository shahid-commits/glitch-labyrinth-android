package com.example.glitchlabyrinth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.SharedFlow

/**
 * MAIN ACTIVITY
 * -------------
 * Single-activity architecture. Compose manages all navigation.
 * AudioManager lifecycle is tied here so music/SFX follow the app foreground state.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Full immersion: hide system bars ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { ctrl ->
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // --- Audio setup ---
        // init() loads the SoundPool and all SFX raw resources.
        AudioManager.init(this)
        // startMusic() creates the MediaPlayer and begins the looping track.
        AudioManager.startMusic(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = GlitchColors.Background) {
                    GlitchLabyrinthApp(viewModel)
                }
            }
        }
    }

    // Music pauses when the app goes to the background (home button, call, etc.)
    override fun onPause() {
        super.onPause()
        AudioManager.pauseMusic()
    }

    // Music resumes when the app comes back to the foreground.
    override fun onResume() {
        super.onResume()
        AudioManager.resumeMusic()
    }

    // All native audio resources are released when the Activity is destroyed.
    // This prevents memory leaks on low-end devices.
    override fun onDestroy() {
        super.onDestroy()
        AudioManager.release()
    }
}

// ===========================================================================
// ROOT COMPOSABLE — screen state machine + sound event collection
// ===========================================================================
@Composable
fun GlitchLabyrinthApp(viewModel: GameViewModel) {
    val screen    by viewModel.screen.collectAsState()
    val gameState by viewModel.gameState.collectAsState()

    when (val s = screen) {

        is ScreenState.Title -> TitleScreen(
            onStartRun  = { viewModel.startNewRun() },
            onOpenCodex = { viewModel.openCodex() },
            bestFloor   = viewModel.bestFloor(),
            totalRuns   = viewModel.totalRuns()
        )

        is ScreenState.Game -> {
            val gs = gameState
            if (gs != null) {
                // Pass the gameEvent SharedFlow so GameScreen can collect it
                // and call the correct AudioManager SFX method per event.
                GameScreen(
                    gameState        = gs,
                    gameEvent        = viewModel.gameEvent,
                    onMoveRequested  = { dir -> viewModel.onMove(dir) },
                    onActivateGlitch = { g   -> viewModel.onActivateGlitch(g) }
                )
            }
        }

        is ScreenState.GlitchReward -> GlitchRewardScreen(
            options  = s.options,
            onChoose = { chosen -> viewModel.onChooseGlitchReward(chosen) }
        )

        is ScreenState.Summary -> {
            val stats = viewModel.runStats()
            if (stats != null) {
                RunSummaryScreen(
                    stats         = stats,
                    onPlayAgain   = { viewModel.startNewRun() },
                    onBackToTitle = { viewModel.backToTitle() }
                )
            }
        }

        is ScreenState.Codex -> GlitchCodexScreen(
            unlockedGlitches = viewModel.unlockedGlitches(),
            onBack           = { viewModel.backToTitle() }
        )
    }
}