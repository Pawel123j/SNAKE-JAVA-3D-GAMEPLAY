# Snake 3D Deluxe

Elegancka gra Snake w prawdziwym 3D, zbudowana w **Javie 21 + JavaFX 3D + Maven**.

Projekt rozdziela **logikę gry** od **warstwy renderowania**, dzięki czemu zasady
są w pełni pokryte testami jednostkowymi, a klasa JavaFX pozostaje cienką
projekcją stanu gry.

## Funkcje

- kamera perspektywiczna z subtelnym, ciągłym ruchem,
- futurystyczna arena: siatka, ściany, filary i dynamiczne światła,
- animowane „food" z poświatą i własnym źródłem światła,
- czytelny HUD (wynik, najlepszy wynik, prędkość),
- start / pauza / restart oraz ekran *Game Over*,
- **trwały najlepszy wynik** zapisywany między uruchomieniami,
- rosnąca prędkość wraz z długością węża.

## Sterowanie

| Klawisz                | Akcja              |
|------------------------|--------------------|
| `W` `A` `S` `D` / strzałki | ruch           |
| `Spacja`               | start / restart    |
| `P`                    | pauza              |
| `R`                    | szybki restart     |

## Wymagania

- **JDK 21**
- **Maven 3.9+**

## Uruchomienie

```bash
mvn javafx:run
```

lub skrypty pomocnicze:

```bash
./run.sh      # Linux / macOS
run.bat       # Windows
```

## Testy

Logika gry jest niezależna od JavaFX i pokryta testami JUnit 5:

```bash
mvn test
```

Pełny build wraz z testami:

```bash
mvn verify
```

## Architektura

Kod celowo oddziela symulację od prezentacji:

| Klasa             | Odpowiedzialność                                                            |
|-------------------|-----------------------------------------------------------------------------|
| `GameState`       | bezstanowy względem UI silnik gry — ruch, jedzenie, kolizje, punktacja      |
| `Cell`            | niemutowalna pozycja na siatce z metodą `translate(Direction)`              |
| `Direction`       | kierunki ruchu wraz z logiką przeciwnych zwrotów                            |
| `HighScoreStore`  | trwały zapis/odczyt najlepszego wyniku (`~/.snake3d/highscore`)             |
| `Snake3DApp`      | warstwa JavaFX — kamera, światła, arena, HUD; renderuje stan `GameState`    |

Pętla czasu (`Timeline`) wywołuje `GameState.step()`, a osobna pętla
renderująca (`AnimationTimer`) odpowiada wyłącznie za animacje wizualne.
Taki podział sprawia, że zasady gry można testować bez uruchamiania UI.

## Struktura projektu

```text
snake-3d-deluxe/
├── pom.xml
├── README.md
├── LICENSE
├── run.sh
├── run.bat
├── .github/
│   └── workflows/
│       └── build.yml
└── src/
    ├── main/java/com/pawel/snake3d/
    │   ├── Snake3DApp.java       # warstwa widoku (JavaFX)
    │   ├── GameState.java        # silnik gry (logika)
    │   ├── HighScoreStore.java   # trwały najlepszy wynik
    │   ├── Cell.java
    │   └── Direction.java
    └── test/java/com/pawel/snake3d/
        ├── GameStateTest.java
        ├── HighScoreStoreTest.java
        ├── DirectionTest.java
        └── CellTest.java
```

## Licencja

Projekt udostępniony na licencji [MIT](LICENSE).
