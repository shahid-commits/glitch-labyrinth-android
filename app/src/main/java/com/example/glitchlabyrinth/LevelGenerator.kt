package com.example.glitchlabyrinth

import kotlin.random.Random

/**
 * LEVEL GENERATOR
 * ---------------
 * Uses a simple cellular-automaton / room-corridor approach:
 *   1. Fill grid with WALL.
 *   2. Carve a random walk (drunkard's walk) from centre to create open spaces.
 *   3. Place player start (top-left open area), exit (bottom-right open area).
 *   4. Scatter traps, keys, stability pickups, and enemies based on floor number.
 *
 * Each run is deterministic for a given seed — pass System.currentTimeMillis() for variety.
 */
object LevelGenerator {

    private const val GRID_W = 10
    private const val GRID_H = 10

    fun generateFloor(
        floorNumber: Int,
        seed: Long = System.currentTimeMillis()
    ): FloorState {
        val rng = Random(seed + floorNumber * 31337L)

        // Step 1 — fill with walls
        val grid = Array(GRID_W) { x -> Array(GRID_H) { y -> TileType.WALL to Pair(x, y) } }

        // Step 2 — drunkard's walk to carve floors
        var cx = GRID_W / 2
        var cy = GRID_H / 2
        val steps = (GRID_W * GRID_H * 0.55).toInt()
        repeat(steps) {
            grid[cx][cy] = TileType.FLOOR to Pair(cx, cy)
            val dirs = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
            val (dx, dy) = dirs[rng.nextInt(dirs.size)]
            cx = (cx + dx).coerceIn(1, GRID_W - 2)
            cy = (cy + dy).coerceIn(1, GRID_H - 2)
        }
        // Always carve centre
        grid[GRID_W / 2][GRID_H / 2] = TileType.FLOOR to Pair(GRID_W / 2, GRID_H / 2)

        // Step 3 — find player start: first open cell scanning top-left
        var startX = 1; var startY = 1
        outer@ for (y in 1 until GRID_H - 1) {
            for (x in 1 until GRID_W - 1) {
                if (grid[x][y].first == TileType.FLOOR) { startX = x; startY = y; break@outer }
            }
        }

        // Exit: first open cell scanning bottom-right
        var exitX = GRID_W - 2; var exitY = GRID_H - 2
        outer@ for (y in GRID_H - 2 downTo 1) {
            for (x in GRID_W - 2 downTo 1) {
                if (grid[x][y].first == TileType.FLOOR && !(x == startX && y == startY)) {
                    exitX = x; exitY = y; break@outer
                }
            }
        }

        // Collect all floor positions excluding start and exit
        val openCells = mutableListOf<Pair<Int,Int>>()
        for (y in 0 until GRID_H) for (x in 0 until GRID_W) {
            if (grid[x][y].first == TileType.FLOOR && !(x == startX && y == startY) && !(x == exitX && y == exitY))
                openCells.add(Pair(x, y))
        }
        openCells.shuffle(rng)

        // Step 4 — scatter items based on difficulty
        val difficulty = minOf(floorNumber, 8)
        val trapCount    = 2 + difficulty
        val stabilityCount = maxOf(1, 3 - floorNumber / 3)
        val hasKey       = floorNumber > 1 && rng.nextBoolean()
        var placed = 0

        // Traps
        repeat(minOf(trapCount, openCells.size - placed - 4)) {
            val (tx, ty) = openCells[placed++]
            grid[tx][ty] = TileType.TRAP to Pair(tx, ty)
        }
        // Pits (on higher floors)
        if (floorNumber >= 3) {
            val pitCount = floorNumber / 3
            repeat(minOf(pitCount, openCells.size - placed - 3)) {
                val (px, py) = openCells[placed++]
                grid[px][py] = TileType.PIT to Pair(px, py)
            }
        }
        // Stability pickups
        repeat(minOf(stabilityCount, openCells.size - placed - 2)) {
            val (sx, sy) = openCells[placed++]
            grid[sx][sy] = TileType.STABILITY to Pair(sx, sy)
        }
        // Key & locked exit
        if (hasKey && openCells.size - placed >= 2) {
            val (kx, ky) = openCells[placed++]
            grid[kx][ky] = TileType.KEY to Pair(kx, ky)
            grid[exitX][exitY] = TileType.LOCKED_EXIT to Pair(exitX, exitY)
        } else {
            grid[exitX][exitY] = TileType.EXIT to Pair(exitX, exitY)
        }

        // Build tile list
        val tiles = mutableListOf<Tile>()
        for (y in 0 until GRID_H) for (x in 0 until GRID_W) {
            tiles.add(Tile(x, y, grid[x][y].first))
        }

        // Step 5 — enemies
        val enemyCount = minOf(difficulty / 2, openCells.size - placed)
        val enemies = mutableListOf<Enemy>()
        repeat(enemyCount) { idx ->
            if (placed < openCells.size) {
                val (ex, ey) = openCells[placed++]
                val type = if (floorNumber >= 4 && rng.nextBoolean()) EnemyType.CHASER else EnemyType.PATROL
                val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
                val (pdx, pdy) = dirs[rng.nextInt(dirs.size)]
                enemies.add(Enemy(id = idx, x = ex, y = ey, type = type, patrolDx = pdx, patrolDy = pdy))
            }
        }

        return FloorState(width = GRID_W, height = GRID_H, tiles = tiles, enemies = enemies)
    }

    /** Returns the player's starting position for a given floor. */
    fun startPosition(floor: FloorState): Pair<Int,Int> {
        // Find first FLOOR tile top-left
        for (y in 0 until floor.height) for (x in 0 until floor.width) {
            val t = floor.tile(x, y)
            if (t?.type == TileType.FLOOR) return Pair(x, y)
        }
        return Pair(1, 1)
    }
}