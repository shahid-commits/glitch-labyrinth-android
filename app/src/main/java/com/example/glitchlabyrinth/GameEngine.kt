package com.example.glitchlabyrinth

/**
 * GAME ENGINE
 * -----------
 * Pure functions — no side effects, no Android dependencies.
 * Every function takes a GameState and returns a new GameState.
 *
 * Turn structure (per player move):
 *   1. applyPlayerMove()      — move player (+ phantom if Mirror Step active)
 *   2. resolvePlayerTile()    — apply tile effects (trap/pit/item/exit) with glitch modifiers
 *   3. resolveEnemies()       — move enemies, apply contact damage
 *   4. tickGlitches()         — decrement remaining turns for active glitches
 *   5. checkWinLose()         — flag runOver / runWon
 */
object GameEngine {

    private const val TRAP_DAMAGE      = 15
    private const val PIT_DAMAGE       = 30
    private const val ENEMY_DAMAGE     = 10
    private const val STABILITY_HEAL   = 20
    private const val START_STABILITY  = 100

    // -----------------------------------------------------------------------
    // INIT
    // -----------------------------------------------------------------------

    /** Create a brand-new GameState for floor 1. */
    fun newRun(seed: Long, totalFloors: Int = 7): GameState {
        val floor = LevelGenerator.generateFloor(1, seed)
        val (sx, sy) = LevelGenerator.startPosition(floor)
        val startGlitch = GlitchRegistry.all.random()
        return GameState(
            currentFloorNumber = 1,
            floorState = floor,
            playerState = PlayerState(
                x = sx, y = sy,
                stability = START_STABILITY,
                maxStability = START_STABILITY,
                heldGlitches = listOf(startGlitch)
            ),
            totalFloorsInRun = totalFloors,
            message = "Floor 1 — find the exit!"
        )
    }

    /** Advance to the next floor after clearing the current one. */
    fun advanceToNextFloor(
        state: GameState,
        chosenGlitch: GlitchDefinition,
        seed: Long
    ): GameState {
        val nextFloor = state.currentFloorNumber + 1
        val floor = LevelGenerator.generateFloor(nextFloor, seed)
        val (sx, sy) = LevelGenerator.startPosition(floor)
        val newGlitches = (state.playerState.heldGlitches + chosenGlitch).takeLast(3)
        return state.copy(
            currentFloorNumber = nextFloor,
            floorState = floor,
            playerState = state.playerState.copy(
                x = sx, y = sy,
                heldGlitches = newGlitches,
                hasKey = false,
                phantom = null
            ),
            activeGlitches = emptyList(),
            message = "Floor $nextFloor — the corruption deepens."
        )
    }

    // -----------------------------------------------------------------------
    // GLITCH HELPERS
    // -----------------------------------------------------------------------

    fun isGlitchActive(state: GameState, effect: GlitchEffectType): Boolean =
        state.activeGlitches.any { it.definition.effectType == effect && it.turnsRemaining > 0 }

    /** Activate a glitch from player's inventory. One-shot glitches are applied immediately. */
    fun activateGlitch(state: GameState, glitch: GlitchDefinition): GameState {
        val newInventory = state.playerState.heldGlitches - glitch
        val newPlayer = state.playerState.copy(heldGlitches = newInventory)
        var newState = state.copy(
            playerState = newPlayer,
            glitchesUsed = state.glitchesUsed + 1
        )

        return when (glitch.effectType) {
            GlitchEffectType.SWAP_MAZE -> applySwapMaze(newState)
            else -> {
                // Add to active glitches (or refresh if already active)
                val existing = newState.activeGlitches.filter {
                    it.definition.effectType != glitch.effectType
                }
                val active = existing + ActiveGlitch(glitch, glitch.durationTurns)
                newState.copy(
                    activeGlitches = active,
                    message = "${glitch.iconLabel} ${glitch.name} activated!"
                )
            }
        }
    }

    /** SWAP_MAZE: swap all enemy positions with item positions (TRAP/STABILITY/KEY). */
    private fun applySwapMaze(state: GameState): GameState {
        val floor = state.floorState
        val itemPositions = floor.tiles
            .filter { it.type in listOf(TileType.TRAP, TileType.STABILITY, TileType.KEY, TileType.PIT) }
            .map { Pair(it.x, it.y) }
            .toMutableList()
        val enemies = floor.enemies.toMutableList()

        if (enemies.isEmpty() || itemPositions.isEmpty()) {
            return state.copy(message = "🔀 Swap Maze — nothing to swap!")
        }

        // Swap: first N enemies get item positions; items get enemy positions
        val swapCount = minOf(enemies.size, itemPositions.size)
        val oldEnemyPositions = enemies.take(swapCount).map { Pair(it.x, it.y) }

        val newEnemies = enemies.mapIndexed { idx, e ->
            if (idx < swapCount) e.copy(x = itemPositions[idx].first, y = itemPositions[idx].second)
            else e
        }

        var newFloor = floor.copy(enemies = newEnemies)
        // Move items to old enemy positions
        val itemTiles = floor.tiles
            .filter { it.type in listOf(TileType.TRAP, TileType.STABILITY, TileType.KEY, TileType.PIT) }
            .take(swapCount)
        itemTiles.forEachIndexed { idx, tile ->
            val (ox, oy) = oldEnemyPositions[idx]
            newFloor = newFloor
                .replaceTile(tile.copy(x = ox, y = oy))        // move item tile
                .replaceTile(Tile(tile.x, tile.y, TileType.FLOOR)) // clear old position
        }

        return state.copy(floorState = newFloor, message = "🔀 Swap Maze — chaos reshuffled!")
    }

    // -----------------------------------------------------------------------
    // PLAYER MOVEMENT
    // -----------------------------------------------------------------------

    fun applyPlayerMove(state: GameState, direction: Direction): GameState {
        if (state.runOver) return state

        val (dx, dy) = direction.toDelta()
        var newState = state.copy(totalTurns = state.totalTurns + 1)

        // Move player
        newState = moveEntity(newState, dx, dy)

        // Move phantom (Mirror Step)
        if (isGlitchActive(newState, GlitchEffectType.MIRROR_STEP)) {
            newState = movePhantom(newState, direction.mirrored())
        }

        // Resolve tile effects for player
        newState = resolvePlayerTile(newState)

        // Resolve enemies
        newState = resolveEnemies(newState)

        // Tick active glitches
        newState = tickGlitches(newState)

        // Check win/lose
        return checkWinLose(newState)
    }

    /** Try to move player by (dx, dy). Respects walls unless Phase Walls active. */
    private fun moveEntity(state: GameState, dx: Int, dy: Int): GameState {
        val p = state.playerState
        val newX = p.x + dx
        val newY = p.y + dy
        val targetTile = state.floorState.tile(newX, newY)

        val canPassWall = isGlitchActive(state, GlitchEffectType.PHASE_WALLS)

        if (targetTile == null) return state  // out of bounds
        if (targetTile.type == TileType.WALL && !canPassWall) return state
        if (targetTile.type == TileType.LOCKED_EXIT && !p.hasKey) {
            return state.copy(message = "🔒 Exit locked — find the key!")
        }

        return state.copy(playerState = p.copy(x = newX, y = newY))
    }

    /** Move the mirror phantom in mirrored direction. Phantom dies on TRAP/WALL. */
    private fun movePhantom(state: GameState, direction: Direction): GameState {
        val phantom = state.playerState.phantom
            ?: PhantomClone(state.playerState.x, state.playerState.y)

        if (!phantom.alive) return state

        val (dx, dy) = direction.toDelta()
        val nx = phantom.x + dx
        val ny = phantom.y + dy
        val tile = state.floorState.tile(nx, ny) ?: return state

        return when (tile.type) {
            TileType.WALL -> state.copy(
                playerState = state.playerState.copy(phantom = phantom.copy(alive = false)),
                message = "🪞 Phantom shattered on a wall."
            )
            TileType.TRAP, TileType.PIT -> state.copy(
                playerState = state.playerState.copy(phantom = phantom.copy(alive = false)),
                message = "🪞 Phantom dissolved in a trap."
            )
            TileType.STABILITY -> {
                // Phantom picks up stability
                val newFloor = state.floorState.replaceTile(tile.copy(type = TileType.FLOOR))
                val newPlayer = state.playerState.copy(
                    stability = minOf(state.playerState.stability + STABILITY_HEAL, state.playerState.maxStability),
                    phantom = phantom.copy(x = nx, y = ny)
                )
                state.copy(floorState = newFloor, playerState = newPlayer, message = "🪞 Phantom absorbed stability +$STABILITY_HEAL!")
            }
            else -> state.copy(
                playerState = state.playerState.copy(phantom = phantom.copy(x = nx, y = ny))
            )
        }
    }

    // -----------------------------------------------------------------------
    // TILE RESOLUTION
    // -----------------------------------------------------------------------

    /** Apply the effect of the tile the player is now standing on. */
    private fun resolvePlayerTile(state: GameState): GameState {
        val p = state.playerState
        val tile = state.floorState.tile(p.x, p.y) ?: return state
        val reverseTraps = isGlitchActive(state, GlitchEffectType.REVERSE_TRAPS)
        val invincible   = isGlitchActive(state, GlitchEffectType.INVINCIBLE)

        return when (tile.type) {
            TileType.TRAP -> {
                if (reverseTraps) {
                    // Heals instead of damages
                    val healed = minOf(p.stability + TRAP_DAMAGE, p.maxStability)
                    state.copy(
                        playerState = p.copy(stability = healed),
                        message = "🔄 Trap reversed — healed $TRAP_DAMAGE stability!"
                    )
                } else if (invincible) {
                    state.copy(message = "🛡 Trap blocked by invincibility!")
                } else {
                    state.copy(
                        playerState = p.copy(stability = p.stability - TRAP_DAMAGE),
                        message = "⚠ Trap! -$TRAP_DAMAGE stability."
                    )
                }
            }
            TileType.PIT -> {
                if (invincible) {
                    state.copy(message = "🛡 Pit blocked by invincibility!")
                } else {
                    state.copy(
                        playerState = p.copy(stability = p.stability - PIT_DAMAGE),
                        message = "🕳 Fell in a pit! -$PIT_DAMAGE stability."
                    )
                }
            }
            TileType.STABILITY -> {
                // Consume the pickup
                val newFloor = state.floorState.replaceTile(tile.copy(type = TileType.FLOOR))
                val healed = minOf(p.stability + STABILITY_HEAL, p.maxStability)
                state.copy(
                    floorState = newFloor,
                    playerState = p.copy(stability = healed),
                    message = "💊 Stability restored +$STABILITY_HEAL!"
                )
            }
            TileType.KEY -> {
                val newFloor = state.floorState.replaceTile(tile.copy(type = TileType.FLOOR))
                // Also upgrade LOCKED_EXIT to EXIT
                val unlockedFloor = newFloor.copy(tiles = newFloor.tiles.map {
                    if (it.type == TileType.LOCKED_EXIT) it.copy(type = TileType.EXIT) else it
                })
                state.copy(
                    floorState = unlockedFloor,
                    playerState = p.copy(hasKey = true),
                    message = "🗝 Key collected — exit unlocked!"
                )
            }
            TileType.EXIT -> state  // handled in checkWinLose
            else -> state
        }
    }

    // -----------------------------------------------------------------------
    // ENEMY RESOLUTION
    // -----------------------------------------------------------------------

    private fun resolveEnemies(state: GameState): GameState {
        val slowTime = isGlitchActive(state, GlitchEffectType.SLOW_TIME)
        val invincible = isGlitchActive(state, GlitchEffectType.INVINCIBLE)
        val floor = state.floorState
        val p = state.playerState

        val newEnemies = floor.enemies.map { enemy ->
            // SlowTime: only move on even turns
            if (slowTime && floor.turnCounter % 2 == 0) return@map enemy

            when (enemy.type) {
                EnemyType.PATROL -> patrolMove(enemy, floor)
                EnemyType.CHASER -> chaserMove(enemy, p, floor)
            }
        }

        // Check enemy-player contact damage
        var newStability = p.stability
        var contactMsg = state.message
        val hitEnemy = newEnemies.any { it.x == p.x && it.y == p.y }
        if (hitEnemy && !invincible) {
            newStability -= ENEMY_DAMAGE
            contactMsg = "👾 Enemy hit! -$ENEMY_DAMAGE stability."
        }

        return state.copy(
            floorState = floor.copy(enemies = newEnemies, turnCounter = floor.turnCounter + 1),
            playerState = p.copy(stability = newStability),
            message = if (hitEnemy) contactMsg else state.message
        )
    }

    private fun patrolMove(enemy: Enemy, floor: FloorState): Enemy {
        val nx = enemy.x + enemy.patrolDx
        val ny = enemy.y + enemy.patrolDy
        val tile = floor.tile(nx, ny)
        return if (tile != null && tile.type != TileType.WALL) {
            enemy.copy(x = nx, y = ny)
        } else {
            // Reverse direction
            val reversed = enemy.copy(patrolDx = -enemy.patrolDx, patrolDy = -enemy.patrolDy)
            val rx = reversed.x + reversed.patrolDx
            val ry = reversed.y + reversed.patrolDy
            val rTile = floor.tile(rx, ry)
            if (rTile != null && rTile.type != TileType.WALL) reversed.copy(x = rx, y = ry)
            else enemy
        }
    }

    private fun chaserMove(enemy: Enemy, player: PlayerState, floor: FloorState): Enemy {
        // Simple greedy: move one step toward player
        val dx = (player.x - enemy.x).coerceIn(-1, 1)
        val dy = (player.y - enemy.y).coerceIn(-1, 1)

        // Try horizontal first, then vertical
        for ((tdx, tdy) in listOf(Pair(dx, 0), Pair(0, dy))) {
            if (tdx == 0 && tdy == 0) continue
            val nx = enemy.x + tdx
            val ny = enemy.y + tdy
            val tile = floor.tile(nx, ny)
            if (tile != null && tile.type != TileType.WALL) {
                return enemy.copy(x = nx, y = ny)
            }
        }
        return enemy
    }

    // -----------------------------------------------------------------------
    // GLITCH TICK
    // -----------------------------------------------------------------------

    private fun tickGlitches(state: GameState): GameState {
        val updated = state.activeGlitches
            .map { it.copy(turnsRemaining = it.turnsRemaining - 1) }
            .filter { it.turnsRemaining > 0 }

        // If Mirror Step expired, remove phantom
        val mirrorExpired = state.activeGlitches.any {
            it.definition.effectType == GlitchEffectType.MIRROR_STEP && it.turnsRemaining == 1
        }
        val newPlayer = if (mirrorExpired) state.playerState.copy(phantom = null)
        else state.playerState

        return state.copy(activeGlitches = updated, playerState = newPlayer)
    }

    // -----------------------------------------------------------------------
    // WIN / LOSE
    // -----------------------------------------------------------------------

    private fun checkWinLose(state: GameState): GameState {
        if (state.playerState.stability <= 0) {
            return state.copy(
                playerState = state.playerState.copy(stability = 0),
                runOver = true,
                runWon = false,
                message = "💀 Stability depleted. Run over."
            )
        }
        val tile = state.floorState.tile(state.playerState.x, state.playerState.y)
        if (tile?.type == TileType.EXIT) {
            val clearedAll = state.currentFloorNumber >= state.totalFloorsInRun
            return state.copy(
                runOver = clearedAll,
                runWon = clearedAll,
                message = if (clearedAll) "🏆 You escaped the Glitch Labyrinth!" else "🚪 Floor cleared!"
            )
        }
        return state
    }

    /** Returns true if the current floor is complete (player on EXIT, not dead). */
    fun isFloorComplete(state: GameState): Boolean {
        val tile = state.floorState.tile(state.playerState.x, state.playerState.y)
        return !state.runOver && tile?.type == TileType.EXIT
    }

    /** Compile run stats for the summary screen. */
    fun buildRunStats(state: GameState) = RunStats(
        floorsCleared = state.currentFloorNumber - if (state.runWon) 0 else 1,
        glitchesUsed = state.glitchesUsed,
        totalTurns = state.totalTurns,
        survived = state.runWon
    )

    /** Pick 2 random glitch rewards for the post-floor choice. */
    fun glitchRewardChoices(excludeOwned: List<GlitchDefinition>): List<GlitchDefinition> {
        val pool = GlitchRegistry.all.filter { it !in excludeOwned }
        return if (pool.size >= 2) pool.shuffled().take(2)
        else GlitchRegistry.all.shuffled().take(2)
    }
}