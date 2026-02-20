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
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/bdb4745a-be24-4889-9196-62957367e6c0" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/da41d85f-5b45-44ea-8f09-9c39a049ec21" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/96c3f1f8-e88a-4b6a-ba71-0ca38ff153a6" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/3a35bd62-2bf3-4341-9553-7346eccf72ba" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/d5d2e05b-8cdd-4e91-aaaa-e7bbd81ad98d" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/88777cde-ee32-415a-a505-bf9f3820ed45" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/90c657df-fc5d-42a4-8328-b49294ddbe05" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/d168af9c-fde2-436a-bf02-bc90ad35c13c" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/a821e4e8-3edb-493a-9c17-1008706e5f2d" />
<img width="720" height="1520" alt="image" src="https://github.com/user-attachments/assets/c5781f73-1ab3-4916-917b-f886f8337c51" />



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
