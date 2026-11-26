# Snake 3D Deluxe

Snake written in Java 21 and JavaFX 3D. It has three difficulty levels, three
board modes, buffered movement, combo scoring, generated sound, a leaderboard,
saved settings, pause/restart, and a win state when the arena is full.

## Run and verify

Requirements: JDK 21 and Maven 3.9 or newer.

```bash
./run.sh
mvn -B verify
```

`mvn verify` runs the JUnit logic and persistence tests and builds the
application JAR. `SmokeTest` can also exercise JavaFX startup under Xvfb:

```bash
xvfb-run -a mvn -B test -Dtest=SmokeTest -Dsnake.smoke.headless=true
```

The smoke test checks startup and game-loop behavior; it does not prove that a
renderer drew the 3D world. JavaFX must report `ConditionalFeature.SCENE3D` for
the screenshot test to accept an image as gameplay.

## Controls

| Key | Action |
|---|---|
| `WASD` / arrows | Steer |
| `Space` | Start or restart |
| `R` | Restart |
| `P` | Pause / resume |
| `1` / `2` / `3` | Difficulty |
| `B` | Cycle board mode |
| `M` | Toggle sound |

Choose a difficulty with `1`, `2`, or `3` while paused. The new speed takes
effect when you press `P` to resume.

Classic mode ends at a wall. Wrap mode connects opposite edges. Obstacles mode
adds fixed pylons. A regular food grows the snake and earns combo-scaled points;
the golden bonus adds points without growth.

## Data and screenshots

By default, settings, high score, and leaderboard are stored in `~/.snake3d/`.
Set `-Dsnake.dataDir=/path/to/data` to use a separate directory.

The screenshot test uses the real JavaFX scene and refuses to save a gameplay
image when 3D support is missing:

```bash
mvn -B test -Dtest=ScreenshotTest -Dsnake.screenshots=true \
  -Dsnake.screenshots.dir=docs/screenshots -DfailIfNoTests=false
```

There are no screenshots in this checkout yet. The current environment lacks
the Maven artifacts needed to build JavaFX and reports no Maven Central network
route, so the renderer and sound have not been run for this revision. See
[docs/screenshots/README.md](docs/screenshots/README.md) for renderer details.

## Current validation

The JDK 21 compiler and JVM compiled and exercised the changed persistence
classes against temporary files: malformed settings fell back and retained a
diagnostic copy; injected pre-commit failure preserved the previous file and
removed its temporary file; high score and leaderboard data survived reload.
The new regression test bodies were also run in a temporary local harness.

`mvn verify` could not run because `maven-resources-plugin:3.3.1` is not cached
and the environment cannot reach Maven Central. This is not a full Maven test or
build result. JavaFX 3D rendering, audio, Windows, and macOS remain unverified.

## Project layout

- `SnakeGame` — game rules, movement, collisions, scoring, and win state.
- `Snake3DApp` — JavaFX scene, input, HUD, animation, and sound.
- `AtomicFiles`, `Settings`, `HighScoreStore`, `Leaderboard` — persistent data.
- `src/test/java` — JUnit logic, settings, persistence, smoke, and screenshot tests.
- `docs/ARCHITECTURE.md` — design and platform notes.

MIT — see [LICENSE](LICENSE).
