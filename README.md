# Snake 3D Deluxe

Elegancka gra Snake w prawdziwym 3D zrobiona w **Java + JavaFX 3D + Maven**.

![build](https://github.com/pawel123j/snake-java-3d-gameplay/actions/workflows/build.yml/badge.svg)

## Co jest w środku

- kamera perspektywiczna 3D z lekkim, płynnym ruchem,
- światła i futurystyczna arena (filary, siatka, gwiazdy),
- animowany, świecący food,
- elegancki HUD (wynik, rekord, prędkość, tryb),
- **3 poziomy trudności** (łatwy / normalny / trudny),
- **zapisywany rekord** — najlepszy wynik przeżywa zamknięcie gry,
- **ekran wygranej** po wypełnieniu całej areny,
- start / restart / pauza.

## Sterowanie

| Klawisz | Akcja |
|---|---|
| **WASD** / **strzałki** | ruch węża |
| **SPACJA** | start / restart |
| **R** | szybki restart |
| **P** | pauza |
| **1 / 2 / 3** | tryb: łatwy / normalny / trudny |

## Wymagania

- **JDK 21**
- **Maven 3.9+**

## Jak odpalić

**Linux / macOS:**

```bash
./run.sh        # lub: mvn javafx:run
```

**Windows:**

```bat
run.bat
```

…albo bezpośrednio w katalogu projektu:

```bash
mvn javafx:run
```

## Testy

Logika gry jest oddzielona od warstwy graficznej, więc daje się testować bez
uruchamiania JavaFX:

```bash
mvn test
```

## Architektura

Logika gry żyje w czystych, testowalnych klasach (bez zależności od JavaFX),
a `Snake3DApp` zajmuje się wyłącznie renderowaniem i wejściem:

| Klasa | Rola |
|---|---|
| `SnakeGame` | model gry: ruch, jedzenie, kolizje, warunek wygranej |
| `GameState` | stany: READY / RUNNING / PAUSED / GAME_OVER / WON |
| `Direction` | kierunki ruchu + wektory przesunięcia |
| `Cell` | współrzędna na siatce (rekord) |
| `Difficulty` | presety prędkości (EASY / NORMAL / HARD) |
| `HighScoreStore` | zapis/odczyt rekordu z `~/.snake3d/highscore.txt` |
| `Snake3DApp` | warstwa widoku JavaFX (renderowanie, HUD, input) |

Renderowanie korzysta z puli węzłów i cache materiałów — segmenty węża są
ponownie używane między klatkami zamiast tworzone od nowa, co ogranicza presję
na garbage collector.

## Struktura projektu

```text
snake-3d-deluxe/
├── pom.xml
├── README.md
├── run.bat
├── run.sh
├── .github/workflows/build.yml
└── src/
    ├── main/java/com/pawel/snake3d/
    │   ├── Snake3DApp.java
    │   ├── SnakeGame.java
    │   ├── GameState.java
    │   ├── Difficulty.java
    │   ├── HighScoreStore.java
    │   ├── Cell.java
    │   └── Direction.java
    └── test/java/com/pawel/snake3d/
        ├── SnakeGameTest.java
        ├── DirectionTest.java
        └── HighScoreStoreTest.java
```
