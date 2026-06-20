# Asteroids

A faithful recreation of the 1979 Atari arcade classic **Asteroids**, written in pure Java
(Swing/AWT + `javax.sound.sampled`) with no third-party dependencies. It builds into a single,
self-contained executable JAR that runs on **JRE 8 or newer**.

## Features

- Authentic white **vector graphics** on black, with a soft phosphor **glow** (toggle with `G`).
- Procedurally **synthesised sound** matching the arcade effects: the accelerating "thump-thump"
  heartbeat, fire, thrust, three explosion sizes, the saucer warble and the extra-life chime.
  (The sounds are recreated in code — the original ROM audio is copyrighted and is not shipped.)
- Full ship physics: rotation, thrust with inertia and friction, screen wrap, up to 4 shots,
  and **hyperspace** (with the classic small chance of self-destruct).
- Asteroids that split **large → medium → small**, waves that grow over time.
- Alien **flying saucers** (large and small) that traverse the field, change direction, and
  **shoot at the player** while making sound — the small saucer aims, the large one fires wildly.
- Authentic scoring (20 / 50 / 100 for rocks, 200 / 1000 for saucers) and an **extra ship every 10,000 points**.
- A persistent **leaderboard** (top 10) with name entry, shown on the welcome/attract screen.

## Controls

| Key | Action |
| --- | --- |
| Left / Right arrows | Rotate |
| Up arrow | Thrust |
| Space | Fire |
| Down arrow | Hyperspace |
| P | Pause / resume |
| R | End the current game and return to the welcome screen |
| F11 | Toggle fullscreen |
| G | Toggle the phosphor glow |
| Space / Enter | Start the game from the welcome screen |

The leaderboard is saved to `%USERPROFILE%\.asteroids-opus\highscores.txt`.

## Running

Double-click **`run.bat`**, or from a terminal:

```
java -jar dist\Asteroids.jar
```

## Building

This machine has only a JRE 8, so the build uses the JDK bundled with IntelliJ IDEA (the
JetBrains Runtime) and targets Java 8 bytecode via `--release 8`:

```
powershell -ExecutionPolicy Bypass -File build.ps1
```

The output is `dist\Asteroids.jar`. In IntelliJ you can also build via **Build → Build Artifacts →
Asteroids:jar**, after setting the module SDK.
