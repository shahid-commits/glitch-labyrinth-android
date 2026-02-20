# Glitch Labyrinth

Turn-based glitchy dungeon crawler built with Kotlin and Jetpack Compose.  
No external game engine – pure Android SDK.

## Features

- Procedural dungeon floors with increasing difficulty
- Turn-based movement with enemies, traps, pits, keys, and exits
- Collectible "glitches" that temporarily change the rules
- Persistent stats: best floor reached, total runs, unlocked glitches
- Custom UI, sound effects, and background music

## Tech Stack

- Kotlin, Jetpack Compose, Android ViewModel
- Pure functional GameEngine with immutable `GameState`
- SharedPreferences-based persistence
- Audio: SoundPool (SFX) + MediaPlayer (music)

## Screenshots

<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/54e7f163-0b94-4aa2-a224-1343d988b5aa" />



## How to Play

- Use the on-screen arrows to move one tile at a time.
- Reach the 🚪 exit tile to clear the floor.
- Avoid ⚡ traps, 🕳 pits, and enemies – they reduce your stability.
- Keys 🗝 unlock locked exits, 💊 pills restore stability.
- Tap a glitch card to activate a special power (limited turns).

## Download

Release APKs are in the `release-apk` folder.

Latest version: [glitch_labyrinth_v1.apk](release-apk/glitch_labyrinth_v1.apk)

To install:
1. Download the APK to your Android device.
2. Enable installation from unknown sources when prompted.
3. Open the file to install and launch the game.

## Building from Source

1. Clone the repo:
   ```bash
   git clone https://github.com/YOUR_USERNAME/glitch-labyrinth-android.git
