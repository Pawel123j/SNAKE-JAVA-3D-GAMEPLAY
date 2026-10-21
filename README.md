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

### Gotowy plik do pobrania

Każde wydanie zawiera samodzielny JAR — nie trzeba Mavena ani osobno
instalowanego JavaFX, wystarczy JDK 21:

```bash
java -jar snake-3d-deluxe.jar
```

Do wydania dołączony jest plik `SHA256SUMS.txt` do sprawdzenia, czy pobrany
plik jest tym, który wyszedł z CI.

JAR niesie natywne biblioteki JavaFX dla Windowsa (x86-64), Linuksa (x86-64)
i macOS na Apple Silicon. Na **Intel macOS, ARM Linux i systemach
32-bitowych** trzeba uruchamiać przez `mvn javafx:run` — powód opisany
w [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Testy

Logika gry jest oddzielona od warstwy graficznej, więc daje się testować bez
uruchamiania JavaFX:

```bash
mvn test        # 40 testów logicznych, ułamek sekundy
```

Dodatkowo istnieje **headless smoke test** uruchamiający całą aplikację —
domyślnie pominięty, w CI odpalany pod `xvfb`:

```bash
xvfb-run -a mvn test -Dtest=SmokeTest -Dsnake.smoke.headless=true
```

> **Co ten test daje, a czego nie daje.** Łapie wyjątki w konstrukcji sceny,
> w pętlach renderowania i gry, w interpolacji i w cząsteczkach — czyli
> w kodzie, którego testy logiczne nie dotykają. **Nie sprawdza natomiast,
> czy cokolwiek zostało narysowane**: bez karty graficznej JavaFX spada na
> programowy pipeline Prism, który zgłasza
> `System can't support ConditionalFeature.SCENE3D` i nie rasteryzuje sceny
> 3D w ogóle. Regresja zostawiająca pustą planszę przeszłaby ten test.

CI uruchamia jeszcze trzecie zadanie: buduje samodzielny JAR **i sprawdza,
że faktycznie startuje**. Build, którego nikt nie uruchomił, nie jest
dowodem na nic — a w grubym JAR-ze z JavaFX najłatwiej o błąd, który wychodzi
dopiero przy starcie.

## Zrzuty ekranu

W repozytorium jest narzędzie generujące je z prawdziwej, uruchomionej gry:

```bash
mvn -B test -Dtest=ScreenshotTest -Dsnake.screenshots=true \
    -Dsnake.screenshots.dir=docs/screenshots -DfailIfNoTests=false
```

Katalog `docs/screenshots/` jest na razie pusty — z tego samego powodu, co
wyżej: w środowisku bez karty graficznej powstają z tego obrazki, na których
plansza jest niewidoczna. Szczegóły i instrukcja:
[docs/screenshots/README.md](docs/screenshots/README.md).

## Architektura

Pełny opis wraz z uzasadnieniami decyzji: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

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

## Licencja

MIT — patrz [LICENSE](LICENSE).

## Weryfikacja środowiska — 2026-10-17

`mvn -B verify` z JDK 21: 40 testów PASS, 2 testy GUI pominięte; JAR został zbudowany. To nie potwierdza renderowania sceny 3D ani jakości klatek.

Istniejący workflow uruchamia smoke przez Xvfb. Lokalnie Xvfb nie jest dostępny, a pobranie pakietów z skonfigurowanego snapshotu Debiana było zablokowane. Pozostaje sprawdzenie startu JAR-a, sterowania, pauzy, restartu i zapisu wyników w sesji graficznej. Wydania nie opublikowano. Rollback: poprzedni JAR oraz zachowane pliki ustawień/wyników.

## Walidacja etapu 2 — 2026-10-18

Rzeczywisty test JavaFX SmokeTest uruchomiono pod własnym Xvfb: 1 PASS, bez skip. Wcześniejsze 40 testów logiki i Maven/JAR pozostają dowodem bazowym. Renderer programowy zgłasza brak SCENE3D; sam test sceny/pętli nie dowodzi wyświetlenia geometrii 3D. Do zakończenia potrzeba GPU/środowiska obsługującego JavaFX 3D oraz testów sterowania, ustawień i wyników.

## Etap 3 — 2026-10-21

Status PARTIAL. `mvn verify` przechodzi (40 testów logiki; 2 testy GUI opt-in
pomijane), powstaje shaded JAR. Osobny SmokeTest pod Xvfb/Prism SW PASS.
Gotowy JAR rzeczywiście uruchomiono: klawisze 1/B/M zapisują ustawienia,
Space/P obsługują start/pauzę. To test interfejsu i ustawień, nie renderowania 3D.

ScreenshotTest odrzuca brak `ConditionalFeature.SCENE3D` i jednobarwny obraz
świata, zanim zapisze go jako zrzut rozgrywki. Próba ES2 z Mesa software nie
przeszła kwalifikacji renderera (`no suitable pipeline`). Nie wymuszano
przełączników omijających tę kontrolę. Potrzebny jest rzeczywisty obsługiwany
renderer 3D; screenshot test należy uruchomić na tej maszynie z
`-Dprism.order=es2 -Dsnake.screenshots=true -Dtest=ScreenshotTest`.

`-Dsnake.dataDir=/osobny/katalog` izoluje ustawienia, highscore i leaderboard
bez zmiany katalogu użytkownika. Domyślne ścieżki gracza zachowano.
Audyt Maven nie został zakończony: źródło advisory api.osv.dev zwraca 403.
Nie deklarujemy zerowej liczby podatności ani walidacji Windows/macOS.
