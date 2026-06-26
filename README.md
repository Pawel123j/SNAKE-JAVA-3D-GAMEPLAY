# Snake 3D Deluxe

Elegancka gra Snake w prawdziwym 3D zrobiona w **Java + JavaFX 3D + Maven**.

![build](https://github.com/pawel123j/snake-java-3d-gameplay/actions/workflows/build.yml/badge.svg)

## Co jest w środku

- kamera perspektywiczna 3D z lekkim, płynnym ruchem,
- światła i futurystyczna arena (filary, siatka, gwiazdy),
- **płynny ruch węża** — segmenty interpolowane między klatkami,
- animowany, świecący food oraz **złoty bonus** (więcej punktów, znika po chwili),
- **mnożnik combo** — jedz pod rząd, a punkty rosną nawet ×5,
- **bufor wejścia** — szybkie zakręty nie giną (płynne pokonywanie rogów),
- **efekt cząsteczek** przy jedzeniu i **wstrząs ekranu** przy końcu gry,
- **dźwięk** generowany w locie (jedzenie / bonus / koniec / wygrana),
- elegancki HUD (wynik, rekord, combo, prędkość, poziom, plansza),
- **3 poziomy trudności** (łatwy / normalny / trudny),
- **3 tryby planszy** (klasyczny / zawijanie krawędzi / przeszkody),
- **tablica top-5 wyników** pokazywana na ekranie startu i końca,
- **zapisywane ustawienia** — poziom, tryb i dźwięk przeżywają restart gry,
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
| **1 / 2 / 3** | poziom: łatwy / normalny / trudny |
| **B** | zmiana planszy (klasyczna → zawijanie → przeszkody) |
| **M** | włącz / wycisz dźwięk |

### Tryby planszy

- **Klasyczny** — uderzenie w ścianę kończy grę.
- **Zawijanie** — wyjście poza krawędź przenosi węża na przeciwległą stronę.
- **Przeszkody** — na arenie pojawiają się stałe pylony; zderzenie kończy grę.

### Punktacja

- zwykły food: **+10 × mnożnik combo** i wąż rośnie,
- **combo**: każde kolejne jabłko zjedzone szybko po poprzednim zwiększa mnożnik (do ×5); zbyt długa przerwa zeruje serię,
- złoty bonus: **+50**, bez wzrostu — pojawia się co 5 zjedzonych jabłek i znika po chwili.

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

Dodatkowo istnieje **headless smoke test** uruchamiający całą aplikację (scena
3D, pętle, interpolacja, cząsteczki, wstrząs) i wyłapujący wyjątki — domyślnie
pominięty, w CI odpalany pod `xvfb`:

```bash
xvfb-run -a mvn test -Dtest=SmokeTest -Dsnake.smoke.headless=true
```

## Architektura

Logika gry żyje w czystych, testowalnych klasach (bez zależności od JavaFX),
a `Snake3DApp` zajmuje się wyłącznie renderowaniem i wejściem:

| Klasa | Rola |
|---|---|
| `SnakeGame` | model gry: ruch, jedzenie, bonus, kolizje, tryby, warunek wygranej |
| `GameState` | stany: READY / RUNNING / PAUSED / GAME_OVER / WON |
| `GameMode` | tryby planszy: CLASSIC / WRAP / OBSTACLES |
| `Direction` | kierunki ruchu + wektory przesunięcia |
| `Cell` | współrzędna na siatce (rekord) |
| `Difficulty` | presety prędkości (EASY / NORMAL / HARD) |
| `HighScoreStore` | zapis/odczyt rekordu z `~/.snake3d/highscore.txt` |
| `Leaderboard` | top-5 wyników w `~/.snake3d/scores.txt` |
| `Settings` | zapis poziomu, trybu i dźwięku w `~/.snake3d/settings.properties` |
| `SoundManager` | syntezowane efekty dźwiękowe (bez plików audio) |
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
    │   ├── GameMode.java
    │   ├── Difficulty.java
    │   ├── HighScoreStore.java
    │   ├── Leaderboard.java
    │   ├── Settings.java
    │   ├── SoundManager.java
    │   ├── Cell.java
    │   └── Direction.java
    └── test/java/com/pawel/snake3d/
        ├── SnakeGameTest.java
        ├── DirectionTest.java
        ├── HighScoreStoreTest.java
        ├── LeaderboardTest.java
        ├── SettingsTest.java
        └── SmokeTest.java
```
