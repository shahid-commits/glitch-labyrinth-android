package com.example.glitchlabyrinth

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.SharedFlow

// ===========================================================================
// COLOUR PALETTE
// ===========================================================================
object GlitchColors {
    val Background    = Color(0xFF0A0A1A)
    val WallTile      = Color(0xFF1A1A3A)
    val FloorTile     = Color(0xFF12122A)
    val TrapTile      = Color(0xFF8B1A1A)
    val PitTile       = Color(0xFF1A0A0A)
    val ExitTile      = Color(0xFF0A4A2A)
    val KeyTile       = Color(0xFF4A3A0A)
    val StabTile      = Color(0xFF0A2A4A)
    val LockedExit    = Color(0xFF3A2A0A)
    val Player        = Color(0xFF00FFAA)
    val Phantom       = Color(0xFF00FFAA).copy(alpha = 0.4f)
    val Enemy         = Color(0xFFFF4444)
    val Accent        = Color(0xFF7B2FBE)
    val AccentLight   = Color(0xFFBB7FFF)
    val TextPrimary   = Color(0xFFE0E0FF)
    val TextSecondary = Color(0xFF8080AA)
    val HpGreen       = Color(0xFF22CC66)
    val HpRed         = Color(0xFFCC2222)
    val GlitchCard    = Color(0xFF1E103C)
    val Border        = Color(0xFF3A2A6A)
}

// ===========================================================================
// TITLE SCREEN
// ===========================================================================
@Composable
fun TitleScreen(
    onStartRun: () -> Unit,
    onOpenCodex: () -> Unit,
    bestFloor: Int,
    totalRuns: Int
) {
    // Local flag controls the How to Play overlay — no extra ScreenState needed
    var showHowToPlay by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "title_glitch")
    val glitchAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(GlitchColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // Main title column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "GLITCH\nLABYRINTH",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = GlitchColors.AccentLight.copy(alpha = glitchAlpha),
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )

            Text(
                text = "A corrupted dungeon awaits",
                fontSize = 14.sp,
                color = GlitchColors.TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(8.dp))

            GlitchButton(text = "▶  START RUN",    onClick = onStartRun)
            GlitchButton(text = "❓  HOW TO PLAY",  onClick = { showHowToPlay = true }, secondary = true)
            GlitchButton(text = "📖  GLITCH CODEX", onClick = onOpenCodex, secondary = true)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                StatChip("Best Floor", "$bestFloor")
                StatChip("Runs",       "$totalRuns")
            }

            Spacer(Modifier.height(12.dp))

            // Developer credit — styled in the game's cyberpunk monospace theme
            Surface(
                color  = GlitchColors.GlitchCard,
                shape  = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GlitchColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed by",
                        fontSize = 10.sp,
                        color = GlitchColors.TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Shahidkhan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                        color = GlitchColors.AccentLight
                    )
                }
            }
        }

        // How to Play overlay — layered on top via Box, no navigation change
        if (showHowToPlay) {
            HowToPlayOverlay(onDismiss = { showHowToPlay = false })
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = GlitchColors.AccentLight, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 11.sp,
            color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
    }
}

// ===========================================================================
// HOW TO PLAY OVERLAY
// A scrollable modal card on top of the title screen.
// Implemented with a local boolean — no extra ScreenState or ViewModel change.
// Tap outside the card (the dark scrim) to dismiss.
// ===========================================================================
@Composable
fun HowToPlayOverlay(onDismiss: () -> Unit) {
    // Scrim — full-screen dark background; tapping it closes the overlay
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Card — consumes its own clicks so they don't fall through to the scrim
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.88f)
                .clickable(enabled = false, onClick = {}),
            color  = GlitchColors.GlitchCard,
            shape  = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, GlitchColors.Accent)
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Header ───────────────────────────────────────────────
                Text(
                    "❓  HOW TO PLAY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GlitchColors.AccentLight,
                    fontFamily = FontFamily.Monospace
                )
                HelpDivider()

                // ── Goal ─────────────────────────────────────────────────
                HelpSection("🎯", "GOAL",
                    "Navigate every floor and reach the EXIT 🚪 before your " +
                            "Stability (HP) hits zero. Clear all 7 floors to escape the Glitch Labyrinth."
                )
                HelpDivider()

                // ── Controls ─────────────────────────────────────────────
                HelpSection("🕹", "CONTROLS",
                    "Tap the ▲ ▼ ◀ ▶ buttons at the bottom to move one tile per turn. " +
                            "Enemies and hazards update after every move you make."
                )
                HelpDivider()

                // ── Tiles ────────────────────────────────────────────────
                HelpSection("🗺", "TILES", "")
                TileLegendRow("🚪", "EXIT",          "Reach this to clear the floor")
                TileLegendRow("🔒", "LOCKED EXIT",   "Collect the 🗝 Key first to unlock it")
                TileLegendRow("⚡", "TRAP",           "Step on it → −15 Stability")
                TileLegendRow("🕳", "PIT",            "Step on it → −30 Stability")
                TileLegendRow("💊", "STABILITY",      "Pick up to restore +20 Stability")
                TileLegendRow("🗝", "KEY",            "Unlocks the locked exit on this floor")
                TileLegendRow("👾 🤖", "ENEMY",       "Contact → −10 Stability per turn")
                HelpDivider()

                // ── Glitches ─────────────────────────────────────────────
                HelpSection("✨", "GLITCHES",
                    "Glitches are one-use abilities shown ABOVE the grid. Tap a card to " +
                            "activate it. The number on the card is how many turns the effect lasts."
                )
                TileLegendRow("👻", "Phase Walls",   "Walk through walls for 3 turns")
                TileLegendRow("🔄", "Reverse Traps", "Traps heal you for 2 turns")
                TileLegendRow("🪞", "Mirror Step",   "A phantom clone mirrors your moves for 3 turns")
                TileLegendRow("🔀", "Swap Maze",     "Instantly swaps enemies with items (one-shot)")
                TileLegendRow("⏱", "Slow Time",     "Enemies move every 2nd turn for 4 turns")
                TileLegendRow("🛡", "Invincible",    "Take zero damage for 2 turns")
                HelpDivider()

                // ── Progression ──────────────────────────────────────────
                HelpSection("📈", "PROGRESSION",
                    "After each floor you choose 1 of 2 random glitch rewards " +
                            "(you can hold up to 3). Floors get harder as you descend — " +
                            "more traps, pits, and faster enemies."
                )
                HelpDivider()

                // ── Close button ─────────────────────────────────────────
                GlitchButton(text = "✕  CLOSE", onClick = onDismiss)
            }
        }
    }
}

// Helper composables used only inside the overlay
@Composable
private fun HelpSection(icon: String, title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "$icon  $title",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GlitchColors.AccentLight,
            fontFamily = FontFamily.Monospace
        )
        if (body.isNotEmpty()) {
            Text(
                body,
                fontSize = 12.sp,
                color = GlitchColors.TextSecondary,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun TileLegendRow(icon: String, name: String, desc: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(GlitchColors.Background.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, fontSize = 14.sp, modifier = Modifier.width(40.dp))
        Column {
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = GlitchColors.TextPrimary, fontFamily = FontFamily.Monospace)
            Text(desc, fontSize = 10.sp,
                color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun HelpDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GlitchColors.Border)
    )
}

// ===========================================================================
// GAME SCREEN
// ──────────────────────────────────────────────────────────────────────────
// Layout order, top → bottom — nothing overlaps, nothing clips:
//   1. HudBar              fixed wrap-content height
//   2. ActiveGlitchRow     fixed wrap-content height (conditional)
//   3. Message banner      fixed one line (conditional)
//   4. GlitchInventory     fixed wrap-content height
//   5. DungeonGrid         weight(1f) → takes ALL remaining vertical space
//                          BoxWithConstraints reads actual remaining W and H
//                          grid = minOf(W, H) so it is always a square that fits
//   6. DirectionalControls fixed wrap-content height (measured before grid)
// ===========================================================================
@Composable
fun GameScreen(
    gameState: GameState,
    gameEvent: SharedFlow<GameEvent>,
    onMoveRequested: (Direction) -> Unit,
    onActivateGlitch: (GlitchDefinition) -> Unit
) {
    // Sound effect collection — runs once, survives recompositions
    LaunchedEffect(Unit) {
        gameEvent.collect { event ->
            when (event) {
                GameEvent.Moved           -> AudioManager.playMove()
                GameEvent.TrapHit         -> AudioManager.playTrap()
                GameEvent.EnemyHit        -> AudioManager.playEnemyHit()
                GameEvent.GlitchActivated -> AudioManager.playGlitch()
                GameEvent.FloorCleared    -> AudioManager.playFloorClear()
                GameEvent.RunOver         -> AudioManager.playRunOver()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(GlitchColors.Background)
    ) {
        // ── 1. HUD ────────────────────────────────────────────────────────
        HudBar(gameState)

        // ── 2. Active glitch timers (wrap-content, optional) ──────────────
        if (gameState.activeGlitches.isNotEmpty()) {
            ActiveGlitchRow(gameState.activeGlitches)
        }

        // ── 3. Message banner (one line, wrap-content) ────────────────────
        if (gameState.message.isNotBlank()) {
            Text(
                text      = gameState.message,
                fontSize  = 11.sp,
                color     = GlitchColors.AccentLight,
                fontFamily = FontFamily.Monospace,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                textAlign = TextAlign.Center,
                maxLines  = 1
            )
        }

        // ── 4. Glitch inventory (wrap-content) ────────────────────────────
        GlitchInventory(
            glitches   = gameState.playerState.heldGlitches,
            onActivate = onActivateGlitch
        )

        // ── 5. Dungeon grid ───────────────────────────────────────────────
        // onSizeChanged measures the actual available width AND height after
        // Compose places every other element. minOf(w, h) gives a square that
        // always fits — no BoxWithConstraints scope needed at all.
        var gridSizePx by remember { mutableStateOf(0) }
        val density    = LocalDensity.current

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size ->
                    gridSizePx = minOf(size.width, size.height)
                },
            contentAlignment = Alignment.Center
        ) {
            if (gridSizePx > 0) {
                val gridSizeDp = with(density) { gridSizePx.toDp() } - 12.dp
                val tileSize   = (gridSizeDp / gameState.floorState.width).value

                Box(
                    modifier         = Modifier.size(gridSizeDp),
                    contentAlignment = Alignment.TopStart
                ) {
                    DungeonGrid(
                        floorState = gameState.floorState,
                        player     = gameState.playerState,
                        tileSize   = tileSize
                    )
                }
            }
        }

        // ── 6. D-pad (wrap-content, always below grid) ────────────────────
        DirectionalControls(onMoveRequested)

        Spacer(Modifier.height(8.dp))
    }
}

// ===========================================================================
// HUD BAR
// ===========================================================================
@Composable
fun HudBar(state: GameState) {
    val hpFraction = state.playerState.stability.toFloat() / state.playerState.maxStability
    val barColor   = if (hpFraction > 0.4f) GlitchColors.HpGreen else GlitchColors.HpRed

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF11112A))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("STABILITY", fontSize = 9.sp,
                color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A2A4A))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(hpFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(barColor)
                )
            }
            Text(
                "${state.playerState.stability}/${state.playerState.maxStability}",
                fontSize = 11.sp,
                color = GlitchColors.TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("FLOOR", fontSize = 9.sp,
                color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
            Text(
                "${state.currentFloorNumber}/${state.totalFloorsInRun}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GlitchColors.AccentLight,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ===========================================================================
// ACTIVE GLITCH ROW
// ===========================================================================
@Composable
fun ActiveGlitchRow(glitches: List<ActiveGlitch>) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        glitches.forEach { ag ->
            Surface(
                color  = GlitchColors.Accent.copy(alpha = 0.35f),
                shape  = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, GlitchColors.Accent)
            ) {
                Row(
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ag.definition.iconLabel, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${ag.definition.name} (${ag.turnsRemaining})",
                        fontSize = 10.sp,
                        color = GlitchColors.AccentLight,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ===========================================================================
// DUNGEON GRID RENDERER
// ===========================================================================
@Composable
fun DungeonGrid(floorState: FloorState, player: PlayerState, tileSize: Float) {
    val tileDp = tileSize.dp

    Box {
        // Tiles
        floorState.tiles.forEach { tile ->
            Box(
                Modifier
                    .offset(x = tileDp * tile.x, y = tileDp * tile.y)
                    .size(tileDp - 1.dp)
                    .background(tileColor(tile.type), RoundedCornerShape(2.dp))
            ) {
                val icon = tileIcon(tile.type)
                if (icon.isNotEmpty()) {
                    Text(icon, fontSize = (tileSize * 0.45f).sp,
                        modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // Enemies
        floorState.enemies.forEach { enemy ->
            Box(
                Modifier
                    .offset(x = tileDp * enemy.x, y = tileDp * enemy.y)
                    .size(tileDp - 1.dp)
                    .background(GlitchColors.Enemy.copy(alpha = 0.85f), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (enemy.type == EnemyType.CHASER) "👾" else "🤖",
                    fontSize = (tileSize * 0.45f).sp
                )
            }
        }

        // Phantom clone (Mirror Step glitch)
        player.phantom?.let { ph ->
            if (ph.alive) {
                Box(
                    Modifier
                        .offset(x = tileDp * ph.x, y = tileDp * ph.y)
                        .size(tileDp - 1.dp)
                        .background(GlitchColors.Phantom, RoundedCornerShape(tileDp / 2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◈", fontSize = (tileSize * 0.45f).sp, color = GlitchColors.Player)
                }
            }
        }

        // Player
        Box(
            Modifier
                .offset(x = tileDp * player.x, y = tileDp * player.y)
                .size(tileDp - 1.dp)
                .background(GlitchColors.Player, RoundedCornerShape(tileDp / 2)),
            contentAlignment = Alignment.Center
        ) {
            Text("◉", fontSize = (tileSize * 0.5f).sp, color = GlitchColors.Background)
        }
    }
}

fun tileColor(type: TileType): Color = when (type) {
    TileType.WALL        -> GlitchColors.WallTile
    TileType.FLOOR       -> GlitchColors.FloorTile
    TileType.TRAP        -> GlitchColors.TrapTile
    TileType.PIT         -> GlitchColors.PitTile
    TileType.EXIT        -> GlitchColors.ExitTile
    TileType.KEY         -> GlitchColors.KeyTile
    TileType.STABILITY   -> GlitchColors.StabTile
    TileType.LOCKED_EXIT -> GlitchColors.LockedExit
}

fun tileIcon(type: TileType): String = when (type) {
    TileType.TRAP        -> "⚡"
    TileType.PIT         -> "🕳"
    TileType.EXIT        -> "🚪"
    TileType.KEY         -> "🗝"
    TileType.STABILITY   -> "💊"
    TileType.LOCKED_EXIT -> "🔒"
    else                 -> ""
}

// ===========================================================================
// GLITCH INVENTORY  (shown ABOVE the grid, never overlaps)
// ===========================================================================
@Composable
fun GlitchInventory(glitches: List<GlitchDefinition>, onActivate: (GlitchDefinition) -> Unit) {
    if (glitches.isEmpty()) return
    Column(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(
            "GLITCHES — tap to activate",
            fontSize = 9.sp,
            color = GlitchColors.TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            glitches.forEach { g -> GlitchChip(g, onActivate) }
        }
    }
}

@Composable
fun GlitchChip(glitch: GlitchDefinition, onActivate: (GlitchDefinition) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(if (pressed) 0.92f else 1f, label = "chip_scale")

    Surface(
        modifier = Modifier
            .scale(scale)
            .clickable { pressed = true; onActivate(glitch) },
        color  = GlitchColors.GlitchCard,
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GlitchColors.Border)
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(glitch.iconLabel, fontSize = 18.sp)
            Text(glitch.name, fontSize = 8.sp, color = GlitchColors.AccentLight,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
            Text(
                if (glitch.durationTurns > 0) "${glitch.durationTurns}T" else "NOW",
                fontSize = 8.sp,
                color = if (glitch.durationTurns > 0) GlitchColors.TextSecondary
                else GlitchColors.HpRed,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ===========================================================================
// DIRECTIONAL CONTROLS — proper cross-shaped D-pad
// Layout:
//         [▲]
//    [◀]  [  ]  [▶]
//         [▼]
// Each button is 60×60 dp. The centre gap is also 60 dp so the cross
// is perfectly symmetrical. Total height = 3×60 + 2×8 = 196 dp.
// ===========================================================================
@Composable
fun DirectionalControls(onMove: (Direction) -> Unit) {
    Box(
        modifier          = Modifier.fillMaxWidth(),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top row — UP only
            DPadButton("▲") { onMove(Direction.UP) }

            // Middle row — LEFT, gap, RIGHT
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                DPadButton("◀") { onMove(Direction.LEFT) }
                // Centre placeholder keeps the cross shape
                Spacer(Modifier.size(60.dp))
                DPadButton("▶") { onMove(Direction.RIGHT) }
            }

            // Bottom row — DOWN only
            DPadButton("▼") { onMove(Direction.DOWN) }
        }
    }
}

@Composable
fun DPadButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick        = onClick,
        modifier       = modifier.size(60.dp),   // larger tap target
        shape          = RoundedCornerShape(12.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1040)),
        contentPadding = PaddingValues(0.dp),
        border         = BorderStroke(1.5.dp, GlitchColors.AccentLight.copy(alpha = 0.6f))
    ) {
        Text(
            text       = label,
            fontSize   = 24.sp,
            color      = GlitchColors.AccentLight,
            fontWeight = FontWeight.Bold
        )
    }
}

// ===========================================================================
// GLITCH REWARD SCREEN  (between floors)
// ===========================================================================
@Composable
fun GlitchRewardScreen(
    options: List<GlitchDefinition>,
    onChoose: (GlitchDefinition) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(GlitchColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("FLOOR CLEARED", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, color = Color(0xFF00CC66))
            Text("Choose your reward glitch:", fontSize = 14.sp,
                color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
            options.forEach { glitch ->
                GlitchRewardCard(glitch) { onChoose(glitch) }
            }
        }
    }
}

@Composable
fun GlitchRewardCard(glitch: GlitchDefinition, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color  = GlitchColors.GlitchCard,
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, GlitchColors.Accent)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(glitch.iconLabel, fontSize = 32.sp)
            Column {
                Text(glitch.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = GlitchColors.AccentLight, fontFamily = FontFamily.Monospace)
                Text(glitch.description, fontSize = 12.sp,
                    color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
                Text(
                    if (glitch.durationTurns > 0) "Duration: ${glitch.durationTurns} turns"
                    else "Instant effect",
                    fontSize = 11.sp,
                    color = GlitchColors.TextSecondary.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ===========================================================================
// RUN SUMMARY SCREEN
// ===========================================================================
@Composable
fun RunSummaryScreen(
    stats: RunStats,
    onPlayAgain: () -> Unit,
    onBackToTitle: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(GlitchColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                if (stats.survived) "ESCAPED!" else "SYSTEM FAILURE",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (stats.survived) GlitchColors.HpGreen else GlitchColors.HpRed
            )
            Text(
                "\"${stats.summaryQuote()}\"",
                fontSize = 14.sp,
                color = GlitchColors.TextSecondary,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            SummaryRow("Floors Cleared", "${stats.floorsCleared}")
            SummaryRow("Glitches Used",  "${stats.glitchesUsed}")
            SummaryRow("Total Turns",    "${stats.totalTurns}")

            Spacer(Modifier.height(16.dp))

            GlitchButton("▶  PLAY AGAIN", onPlayAgain)
            GlitchButton("⌂  TITLE", onBackToTitle, secondary = true)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(GlitchColors.GlitchCard, RoundedCornerShape(8.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
        Text(value,  fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = GlitchColors.TextPrimary, fontFamily = FontFamily.Monospace)
    }
}

// ===========================================================================
// GLITCH CODEX SCREEN
// ===========================================================================
@Composable
fun GlitchCodexScreen(
    unlockedGlitches: List<GlitchDefinition>,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(GlitchColors.Background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← BACK", fontSize = 12.sp,
                    color = GlitchColors.AccentLight, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(8.dp))
            Text("GLITCH CODEX", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = GlitchColors.TextPrimary, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(unlockedGlitches) { glitch ->
                Surface(
                    color  = GlitchColors.GlitchCard,
                    shape  = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GlitchColors.Border)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(glitch.iconLabel, fontSize = 28.sp)
                        Column {
                            Text(glitch.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = GlitchColors.AccentLight, fontFamily = FontFamily.Monospace)
                            Text(glitch.description, fontSize = 12.sp,
                                color = GlitchColors.TextSecondary, fontFamily = FontFamily.Monospace)
                            Text(
                                if (glitch.durationTurns > 0) "• ${glitch.durationTurns}-turn effect"
                                else "• One-shot",
                                fontSize = 11.sp,
                                color = GlitchColors.TextSecondary.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===========================================================================
// SHARED WIDGET — reusable full-width button
// ===========================================================================
@Composable
fun GlitchButton(text: String, onClick: () -> Unit, secondary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) GlitchColors.GlitchCard else GlitchColors.Accent
        ),
        border = BorderStroke(
            1.dp,
            if (secondary) GlitchColors.Border else GlitchColors.AccentLight
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (secondary) GlitchColors.TextSecondary else GlitchColors.TextPrimary
        )
    }
}