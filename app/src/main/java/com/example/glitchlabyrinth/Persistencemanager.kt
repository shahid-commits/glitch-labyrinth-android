package com.example.glitchlabyrinth

import android.content.Context
import android.content.SharedPreferences

// ---------------------------------------------------------------------------
// TOP-LEVEL data class — NOT nested inside the object.
// Nesting it inside PersistenceManager caused "Unresolved reference 'SaveData'"
// errors whenever PersistenceManager itself failed to initialise.
// ---------------------------------------------------------------------------
data class SaveData(
    val bestFloor: Int,
    val totalRuns: Int,
    val unlockedGlitchIds: Set<String>
)

/**
 * PERSISTENCE
 * -----------
 * Uses SharedPreferences (simple key-value) to store:
 *   - Best floor reached across all runs
 *   - Total completed runs
 *   - Set of unlocked glitch IDs (comma-separated string)
 *
 * Call load() once at app start; call save() after each run ends.
 */
object PersistenceManager {

    private const val PREFS_NAME     = "glitch_labyrinth_prefs"
    private const val KEY_BEST_FLOOR = "best_floor"
    private const val KEY_TOTAL_RUNS = "total_runs"
    private const val KEY_UNLOCKED   = "unlocked_glitches"

    // Hardcoded as a const — avoids any dependency on GlitchRegistry at
    // object-initialisation time. Kotlin does not guarantee initialisation
    // order between objects, so referencing another object here caused
    // PersistenceManager to silently fail to initialise, making it appear
    // as "Unresolved reference" to every file that imported it.
    private const val DEFAULT_UNLOCKED =
        "phase_walls,reverse_traps,mirror_step,swap_maze,slow_time,invincible"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Load persisted data (returns defaults if this is the first run). */
    fun load(context: Context): SaveData {
        val p = prefs(context)
        val raw = p.getString(KEY_UNLOCKED, DEFAULT_UNLOCKED) ?: DEFAULT_UNLOCKED
        return SaveData(
            bestFloor         = p.getInt(KEY_BEST_FLOOR, 0),
            totalRuns         = p.getInt(KEY_TOTAL_RUNS, 0),
            unlockedGlitchIds = raw.split(",").filter { it.isNotBlank() }.toSet()
        )
    }

    /** Persist results after a run finishes. */
    fun save(context: Context, floorsReached: Int, currentData: SaveData) {
        prefs(context).edit().apply {
            putInt(KEY_BEST_FLOOR, maxOf(currentData.bestFloor, floorsReached))
            putInt(KEY_TOTAL_RUNS, currentData.totalRuns + 1)
            putString(KEY_UNLOCKED, currentData.unlockedGlitchIds.joinToString(","))
            apply()
        }
    }

    /** Permanently unlock a new glitch by ID. */
    fun unlockGlitch(context: Context, glitchId: String, currentData: SaveData): SaveData {
        val newIds = currentData.unlockedGlitchIds + glitchId
        prefs(context).edit()
            .putString(KEY_UNLOCKED, newIds.joinToString(","))
            .apply()
        return currentData.copy(unlockedGlitchIds = newIds)
    }
}