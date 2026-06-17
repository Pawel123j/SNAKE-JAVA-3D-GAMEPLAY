# Snake 3D Deluxe

[![build](https://github.com/Pawel123j/SNAKE-JAVA-3D-GAMEPLAY/actions/workflows/build.yml/badge.svg)](https://github.com/Pawel123j/SNAKE-JAVA-3D-GAMEPLAY/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Elegancka gra Snake w prawdziwym 3D, zbudowana w **Javie 21 + JavaFX 3D + Maven**.

Projekt rozdziela **logikę gry** od **warstwy renderowania**, dzięki czemu zasady
są w pełni pokryte testami jednostkowymi, a klasa JavaFX pozostaje cienką
projekcją stanu gry. Pełny przepływ uruchomienia jest dodatkowo weryfikowany
bezgłowym smoke‑testem w CI.

## Funkcje

- kamera perspektywiczna z subtelnym, ciągłym ruchem,
- futurystyczna arena: siatka, ściany, filary i dynamiczne światła,
- animowane „food" z poświatą, własnym światłem i efektem „pop" przy zjedzeniu,
- **trzy poziomy trudności** (Łatwy / Normalny / Trudny) — różna prędkość i punktacja,
- czytelny HUD: wynik, najlepszy wynik, prędkość i aktywny poziom,
- **trwały najlepszy wynik osobno dla każdego poziomu**,
- start / pauza / restart oraz ekran *Game Over*,
- rosnąca prędkość wraz z długością węża.

## Sterowanie

| Klawisz                    | Akcja                          |
|----------------------------|--------------------------------|
| `W` `A` `S` `D` / strzałki | ruch                           |
| `Spacja`                   | start / restart                |
| `P`                        | pauza                          |
| `R`                        | szybki restart                 |
| `1` / `2` / `3`            | poziom: Łatwy / Normalny / Trudny |

Poziom trudności można zmieniać w menu startowym oraz na ekranie *Game Over*.

## Wymagania

- **JDK 21**
- **Maven 3.9+**

## Uruchomienie

Najprościej, przez wtyczkę JavaFX (sama dobiera natywne biblioteki):

```bash
mvn javafx:run
```

lub skrypty pomocnicze:

```bash
./run.sh      # Linux / macOS
run.bat       # Windows
```

### Samodzielny, uruchamialny JAR

```bash
mvn package
java -jar target/snake-3d-deluxe-1.0.0-shaded.jar
```

> Plik `*-shaded.jar` zawiera natywne biblioteki JavaFX dla platformy, na której
> został zbudowany, więc do jego uruchomienia wystarczy samo JDK 21.

## Testy

Logika gry jest niezależna od JavaFX i pokryta testami JUnit 5:

```bash
mvn test
```

Pełny build wraz z testami i spakowaniem JAR‑a:

```bash
mvn verify
```

## Architektura

Kod celowo oddziela symulację od prezentacji:

| Klasa             | Odpowiedzialność                                                            |
|-------------------|-----------------------------------------------------------------------------|
| `GameState`       | bezstanowy względem UI silnik gry — ruch, jedzenie, kolizje, punktacja      |
| `Difficulty`      | presety trudności: tempo gry i mnożnik punktów                             |
| `Cell`            | niemutowalna pozycja na siatce z metodą `translate(Direction)`              |
| `Direction`       | kierunki ruchu wraz z logiką przeciwnych zwrotów                            |
| `HighScoreStore`  | trwały zapis/odczyt najlepszych wyników per poziom (`~/.snake3d/`)          |
| `Snake3DApp`      | warstwa JavaFX — kamera, światła, arena, HUD; renderuje stan `GameState`    |
| `Launcher`        | punkt wejścia dla uruchamialnego JAR‑a                                       |

Pętla czasu (`Timeline`) wywołuje `GameState.step()`, a osobna pętla
renderująca (`AnimationTimer`) odpowiada wyłącznie za animacje wizualne.
Taki podział sprawia, że zasady gry można testować bez uruchamiania UI.

Ustawienie właściwości `-Dsnake.autoCloseMillis=<ms>` zamyka okno po zadanym
czasie — wykorzystuje to bezgłowy smoke‑test w CI, który potwierdza, że
aplikacja realnie startuje i renderuje.

## Struktura projektu

```text
snake-3d-deluxe/
├── pom.xml
├── README.md · CHANGELOG.md · CONTRIBUTING.md · LICENSE
├── .editorconfig · .gitignore
├── run.sh · run.bat
├── .github/workflows/build.yml
└── src/
    ├── main/java/com/pawel/snake3d/
    │   ├── Snake3DApp.java       # warstwa widoku (JavaFX)
    │   ├── Launcher.java         # punkt wejścia JAR-a
    │   ├── GameState.java        # silnik gry (logika)
    │   ├── Difficulty.java       # poziomy trudności
    │   ├── HighScoreStore.java   # trwałe najlepsze wyniki
    │   ├── Cell.java
    │   └── Direction.java
    └── test/java/com/pawel/snake3d/
        ├── GameStateTest.java
        ├── DifficultyTest.java
        ├── HighScoreStoreTest.java
        ├── DirectionTest.java
        └── CellTest.java
```

## Wkład

Zasady rozwoju i konwencje opisuje [CONTRIBUTING.md](CONTRIBUTING.md),
a historię zmian [CHANGELOG.md](CHANGELOG.md).

## Licencja

Projekt udostępniony na licencji [MIT](LICENSE).
