package com.example.glitchlabyrinth

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * AUDIO MANAGER
 * -------------
 * Singleton that owns:
 *   • SoundPool  — low-latency SFX (move, trap, enemy, glitch, floor clear, run over)
 *   • MediaPlayer — looping background music track
 *
 * Lifecycle contract (call from MainActivity):
 *   onCreate  → AudioManager.init(context)   loads SFX pool
 *   onCreate  → AudioManager.startMusic(context)
 *   onPause   → AudioManager.pauseMusic()
 *   onResume  → AudioManager.resumeMusic()
 *   onDestroy → AudioManager.release()       frees all native resources
 *
 * Audio files expected in res/raw/:
 *   sfx_move.wav  | sfx_trap.wav  | sfx_enemy_hit.wav
 *   sfx_glitch.wav | sfx_floor_clear.wav | sfx_run_over.wav
 *   music_loop.mp3
 *
 * GameEngine.kt stays 100% pure — it never imports or calls anything here.
 */
object AudioManager {

    // -----------------------------------------------------------------------
    // SoundPool — SFX
    // -----------------------------------------------------------------------

    private var soundPool: SoundPool? = null

    // Sound IDs returned by SoundPool.load().  0 = not loaded yet.
    private var idMove       = 0
    private var idTrap       = 0
    private var idEnemyHit   = 0
    private var idGlitch     = 0
    private var idFloorClear = 0
    private var idRunOver    = 0

    // -----------------------------------------------------------------------
    // MediaPlayer — background music
    // -----------------------------------------------------------------------

    private var mediaPlayer: MediaPlayer? = null

    // Guards against double-initialisation (e.g. screen rotation)
    private var soundPoolReady = false

    // -----------------------------------------------------------------------
    // INIT — call once in MainActivity.onCreate()
    // -----------------------------------------------------------------------

    /**
     * Builds the SoundPool and loads all SFX from res/raw.
     * Safe to call multiple times — skips work if already initialised.
     */
    fun init(context: Context) {
        if (soundPoolReady) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)           // up to 4 SFX simultaneously
            .setAudioAttributes(attrs)
            .build()

        val sp = soundPool ?: return

        // Each load() is asynchronous internally; SoundPool handles "not ready yet"
        // gracefully by simply skipping the play — no crash risk.
        idMove       = tryLoad(sp, context, R.raw.sfx_move)
        idTrap       = tryLoad(sp, context, R.raw.sfx_trap)
        idEnemyHit   = tryLoad(sp, context, R.raw.sfx_enemy_hit)
        idGlitch     = tryLoad(sp, context, R.raw.sfx_glitch)
        idFloorClear = tryLoad(sp, context, R.raw.sfx_floor_clear)
        idRunOver    = tryLoad(sp, context, R.raw.sfx_run_over)

        soundPoolReady = true
    }

    /**
     * Wraps SoundPool.load() in a try-catch so a missing raw file
     * doesn't crash the app — it simply returns 0 (silent).
     */
    private fun tryLoad(sp: SoundPool, context: Context, resId: Int): Int {
        return try {
            sp.load(context, resId, 1)
        } catch (e: Exception) {
            0  // file missing → play() with id 0 is a silent no-op
        }
    }

    // -----------------------------------------------------------------------
    // SFX PLAY METHODS
    // Called from GameScreen via LaunchedEffect on GameEvent.
    // vol: left/right volume [0.0 – 1.0], loop 0 = no loop, rate 1.0 = normal
    // -----------------------------------------------------------------------

    fun playMove()       = play(idMove,       leftVol = 0.6f, rightVol = 0.6f)
    fun playTrap()       = play(idTrap,       leftVol = 1.0f, rightVol = 1.0f)
    fun playEnemyHit()   = play(idEnemyHit,   leftVol = 1.0f, rightVol = 1.0f)
    fun playGlitch()     = play(idGlitch,     leftVol = 0.9f, rightVol = 0.9f)
    fun playFloorClear() = play(idFloorClear, leftVol = 1.0f, rightVol = 1.0f)
    fun playRunOver()    = play(idRunOver,    leftVol = 1.0f, rightVol = 1.0f)

    private fun play(soundId: Int, leftVol: Float, rightVol: Float) {
        if (soundId == 0) return            // not loaded or file missing
        soundPool?.play(soundId, leftVol, rightVol, 1, 0, 1.0f)
    }

    // -----------------------------------------------------------------------
    // BACKGROUND MUSIC
    // -----------------------------------------------------------------------

    /**
     * Creates and starts the looping music MediaPlayer.
     * Safe to call multiple times — skips if already playing.
     */
    fun startMusic(context: Context) {
        if (mediaPlayer != null) return     // already running

        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.music_loop)?.apply {
                isLooping = true
                // Music volume lower than SFX so SFX stay audible
                setVolume(0.35f, 0.35f)
                start()
            }
        } catch (e: Exception) {
            // Missing file or audio focus problem — game continues without music
            mediaPlayer = null
        }
    }

    /** Call in Activity.onPause() */
    fun pauseMusic() {
        try { mediaPlayer?.pause() } catch (_: Exception) {}
    }

    /** Call in Activity.onResume() */
    fun resumeMusic() {
        try {
            val mp = mediaPlayer ?: return
            if (!mp.isPlaying) mp.start()
        } catch (_: Exception) {}
    }

    /** Call in Activity.onDestroy() — frees all native resources */
    fun release() {
        try { soundPool?.release() } catch (_: Exception) {}
        soundPool    = null
        soundPoolReady = false

        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }
}