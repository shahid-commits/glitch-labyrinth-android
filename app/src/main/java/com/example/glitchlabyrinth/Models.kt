package com.example.glitchlabyrinth

// ---------------------------------------------------------------------------
// TILE TYPES
// ---------------------------------------------------------------------------
enum class TileType {
    WALL,       // Impassable solid tile
    FLOOR,      // Normal passable tile
    TRAP,       // Damages player on step (spikes / corrupted tile)
    PIT,        // Instant -large stability on step
    EXIT,       // Win condition tile
    KEY,        // Pick up to unlock exit
    STABILITY,  // Pick up to restore stability (HP)
    LOCKED_EXIT // Exit blocked until key is collected
}

// ---------------------------------------------------------------------------
// TILE
// ---------------------------------------------------------------------------
data class Tile(
    val x: Int,
    val y: Int,
    val type: TileType,
    val isVisible: Boolean = true   // future: fog-of-war
)

// ---------------------------------------------------------------------------
// ENEMY
// ---------------------------------------------------------------------------
enum class EnemyType { PATROL, CHASER }

data class Enemy(
    val id: Int,
    val x: Int,
    val y: Int,
    val type: EnemyType = EnemyType.PATROL,
    val hp: Int = 1,
    // For patrol: direction cycles; for chaser: moves toward player
    val patrolDx: Int = 1,
    val patrolDy: Int = 0,
    val moveCountdown: Int = 0  // Used by SlowTime glitch
)

// ---------------------------------------------------------------------------
// GLITCH EFFECT TYPES
// These drive the rules engine — no hard-coded magic outside the engine.
// ---------------------------------------------------------------------------
enum class GlitchEffectType {
    PHASE_WALLS,    // Walls become passable
    REVERSE_TRAPS,  // Traps heal instead of damage
    MIRROR_STEP,    // Every move spawns mirrored phantom
    SWAP_MAZE,      // One-shot: swap enemies & items
    SLOW_TIME,      // Enemies move every 2nd turn
    INVINCIBLE      // Player takes no damage
}

// ---------------------------------------------------------------------------
// GLITCH DEFINITION (data-driven — no behaviour hardcoded here)
// ---------------------------------------------------------------------------
data class GlitchDefinition(
    val id: String,
    val name: String,
    val description: String,
    val durationTurns: Int,      // 0 = instant / one-shot
    val effectType: GlitchEffectType,
    val iconLabel: String = "?"  // emoji or short symbol for UI
)

// All available glitches in the game
object GlitchRegistry {
    val all: List<GlitchDefinition> = listOf(
        GlitchDefinition(
            id = "phase_walls",
            name = "Phase Walls",
            description = "Walls become passable for 3 turns. Walk through the maze itself.",
            durationTurns = 3,
            effectType = GlitchEffectType.PHASE_WALLS,
            iconLabel = "👻"
        ),
        GlitchDefinition(
            id = "reverse_traps",
            name = "Reverse Traps",
            description = "Traps restore stability instead of draining it for 2 turns.",
            durationTurns = 2,
            effectType = GlitchEffectType.REVERSE_TRAPS,
            iconLabel = "🔄"
        ),
        GlitchDefinition(
            id = "mirror_step",
            name = "Mirror Step",
            description = "Every move also moves a phantom clone in mirrored directions. It can collect items.",
            durationTurns = 3,
            effectType = GlitchEffectType.MIRROR_STEP,
            iconLabel = "🪞"
        ),
        GlitchDefinition(
            id = "swap_maze",
            name = "Swap Maze",
            description = "Instantly swaps positions of all enemies and items on the floor.",
            durationTurns = 0,  // one-shot
            effectType = GlitchEffectType.SWAP_MAZE,
            iconLabel = "🔀"
        ),
        GlitchDefinition(
            id = "slow_time",
            name = "Slow Time",
            description = "Enemies only move every 2nd turn for the next 4 turns.",
            durationTurns = 4,
            effectType = GlitchEffectType.SLOW_TIME,
            iconLabel = "⏱"
        ),
        GlitchDefinition(
            id = "invincible",
            name = "Invincible",
            description = "You cannot lose stability for 2 turns. Pure safety.",
            durationTurns = 2,
            effectType = GlitchEffectType.INVINCIBLE,
            iconLabel = "🛡"
        )
    )

    fun byId(id: String) = all.first { it.id == id }
}

// ---------------------------------------------------------------------------
// ACTIVE GLITCH (instance of a glitch currently running)
// ---------------------------------------------------------------------------
data class ActiveGlitch(
    val definition: GlitchDefinition,
    val turnsRemaining: Int
)

// ---------------------------------------------------------------------------
// PHANTOM CLONE (for Mirror Step glitch)
// ---------------------------------------------------------------------------
data class PhantomClone(
    val x: Int,
    val y: Int,
    val alive: Boolean = true
)

// ---------------------------------------------------------------------------
// PLAYER STATE
// ---------------------------------------------------------------------------
data class PlayerState(
    val x: Int,
    val y: Int,
    val stability: Int,         // Current HP
    val maxStability: Int,
    val heldGlitches: List<GlitchDefinition>,  // Inventory (max ~3)
    val hasKey: Boolean = false,
    val phantom: PhantomClone? = null           // Active phantom for Mirror Step
)

// ---------------------------------------------------------------------------
// FLOOR STATE
// ---------------------------------------------------------------------------
data class FloorState(
    val width: Int,
    val height: Int,
    // Flat list of tiles. Access via tile(x,y) helper.
    val tiles: List<Tile>,
    val enemies: List<Enemy> = emptyList(),
    val turnCounter: Int = 0    // increments each player move
) {
    fun tile(x: Int, y: Int): Tile? =
        if (x in 0 until width && y in 0 until height)
            tiles.find { it.x == x && it.y == y }
        else null

    fun replaceTile(newTile: Tile): FloorState =
        copy(tiles = tiles.map {
            if (it.x == newTile.x && it.y == newTile.y) newTile else it
        })
}

// ---------------------------------------------------------------------------
// GAME STATE (top-level, single source of truth)
// ---------------------------------------------------------------------------
data class GameState(
    val currentFloorNumber: Int,
    val floorState: FloorState,
    val playerState: PlayerState,
    val activeGlitches: List<ActiveGlitch> = emptyList(),
    val runOver: Boolean = false,
    val runWon: Boolean = false,       // true if all floors cleared
    val totalFloorsInRun: Int = 7,
    val totalTurns: Int = 0,
    val glitchesUsed: Int = 0,
    val message: String = ""           // transient feedback text shown in UI
)

// ---------------------------------------------------------------------------
// DIRECTION
// ---------------------------------------------------------------------------
enum class Direction { UP, DOWN, LEFT, RIGHT }

fun Direction.toDelta(): Pair<Int, Int> = when (this) {
    Direction.UP    -> Pair(0, -1)
    Direction.DOWN  -> Pair(0, 1)
    Direction.LEFT  -> Pair(-1, 0)
    Direction.RIGHT -> Pair(1, 0)
}

fun Direction.mirrored(): Direction = when (this) {
    Direction.UP    -> Direction.DOWN
    Direction.DOWN  -> Direction.UP
    Direction.LEFT  -> Direction.RIGHT
    Direction.RIGHT -> Direction.LEFT
}

// ---------------------------------------------------------------------------
// RUN STATS (for summary screen)
// ---------------------------------------------------------------------------
data class RunStats(
    val floorsCleared: Int,
    val glitchesUsed: Int,
    val totalTurns: Int,
    val survived: Boolean
) {
    fun summaryQuote(): String = when {
        survived         -> "You mastered the corruption."
        floorsCleared >= 5 -> "You embraced chaos and nearly won."
        floorsCleared >= 3 -> "The labyrinth respected your effort."
        else              -> "The glitch swallowed you whole."
    }
}