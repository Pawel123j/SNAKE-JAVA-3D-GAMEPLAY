# Architektura

## Najważniejsza decyzja: logika nie wie nic o JavaFX

Cała mechanika gry siedzi w `SnakeGame` (444 linie) i nie importuje ani
jednej klasy JavaFX. Dzięki temu 40 testów logicznych chodzi w zwykłej
maszynie wirtualnej Javy — bez okna, bez serwera X, bez karty graficznej —
i wykonuje się w ułamku sekundy.

Gdyby ruch węża był liczony w klasie okna, każdy test wymagałby
uruchomienia JavaFX. W praktyce oznaczałoby to brak testów.

```
        ┌──────────────────────────┐
        │  Launcher                │  punkt wejścia grubego JAR-a
        └────────────┬─────────────┘
                     ▼
        ┌──────────────────────────┐
        │  Snake3DApp (886 linii)  │  JavaFX: scena 3D, HUD, pętle,
        │                          │  wejście, cząsteczki, kamera
        └────────────┬─────────────┘
                     │ woła
                     ▼
   ┌────────────────────────────────────────────┐
   │  SnakeGame        — ruch, kolizje, punkty  │   BEZ JavaFX
   │  Leaderboard      — top 5 wyników          │   ← tu są testy
   │  Settings         — zapamiętane ustawienia │
   │  HighScoreStore   — rekord na dysku        │
   │  Direction, Cell, Difficulty, GameMode     │
   └────────────────────────────────────────────┘
                     ▲
        ┌────────────┴─────────────┐
        │  SoundManager            │  syntezuje dźwięki w locie
        └──────────────────────────┘
```

## Dlaczego `Launcher` istnieje

Środowisko uruchomieniowe Javy odmawia startu, gdy `Main-Class` grubego
JAR-a dziedziczy po `javafx.application.Application` — kończy się to
komunikatem „JavaFX runtime components are missing". `Launcher` jest zwykłą
klasą, która woła `Snake3DApp.main()`, i tylko po to istnieje.

## Dlaczego JAR waży 10 MB

Niesie natywne biblioteki JavaFX dla trzech platform naraz (Windows x86-64,
Linux x86-64, macOS Apple Silicon), żeby ten sam plik działał niezależnie od
tego, gdzie został zbudowany.

Jedno ograniczenie warto znać: **natywne pliki dla macOS na Intelu i na
Apple Silicon mają identyczne nazwy**, więc jeden JAR może nieść tylko jeden
z tych zestawów. Wybrany jest `mac-aarch64`. Na Intel macOS, ARM Linux
i systemach 32-bitowych trzeba uruchamiać przez `mvn javafx:run`, które
pobiera pasujące biblioteki samo.

## Wejście gracza jest buforowane

`SnakeGame` trzyma kolejkę do dwóch skrętów (`MAX_QUEUED_TURNS`). Bez tego
szybkie „w prawo, w górę" w obrębie jednej klatki gubiłoby drugi klawisz —
a to jest dokładnie ten rodzaj rzeczy, który gracz odbiera jako „gra nie
reaguje", nie umiejąc nazwać przyczyny.

## Punktacja i combo

- jedzenie: `FOOD_SCORE` = 10,
- co piąte jedzenie (`FOODS_PER_BONUS`) pojawia się złoty bonus wart
  `BONUS_SCORE` = 50, znikający po `BONUS_LIFETIME` = 30 krokach,
- zjedzenie w ciągu `COMBO_WINDOW` = 16 kroków od poprzedniego podbija
  mnożnik, maksymalnie `COMBO_MAX` = 5.

Wszystkie te progi są nazwanymi stałymi, a nie liczbami wklejonymi w kod —
i wszystkie mają testy.

## Testy

40 testów czysto logicznych plus dwie klasy uruchamiające JavaFX, obie
domyślnie wyłączone i włączane osobną właściwością systemową.

| Plik | Co sprawdza |
|---|---|
| `SnakeGameTest` (26) | ruch, kolizje ze ścianą i z ogonem, jedzenie, bonusy, combo, tryby planszy, bufor wejścia |
| `LeaderboardTest` (4) | sortowanie i przycięcie do pięciu wyników |
| `HighScoreStoreTest` (4) | zapis i odczyt rekordu, odporność na uszkodzony plik |
| `SettingsTest` (3) | zapamiętywanie poziomu, trybu i dźwięku |
| `DirectionTest` (3) | zakaz zawrócenia o 180° |
| `SmokeTest` (1) | uruchomienie JavaFX pod `xvfb`; domyślnie pomijany, CI włącza go przez `-Dsnake.smoke.headless=true` |
| `ScreenshotTest` | nie jest testem, tylko generatorem zrzutów do README; włączany przez `-Dsnake.screenshots=true` |

### Czego testy nie pokrywają — i to trzeba powiedzieć wprost

`SmokeTest` **nie sprawdza, czy cokolwiek zostało narysowane**. Pod `xvfb`
nie ma karty graficznej, JavaFX spada na programowy pipeline Prism, a ten
zgłasza:

```
WARNING: System can't support ConditionalFeature.SCENE3D
```

Scena 3D nie jest wtedy rasteryzowana w ogóle. Regresja, która zostawia
pustą planszę, ale nadal poprawnie aktualizuje graf sceny, przeszłaby ten
test.

Co `SmokeTest` naprawdę daje: łapie wyjątki w konstrukcji sceny, w pętlach
renderowania i gry, w interpolacji i w obsłudze cząsteczek — czyli w kodzie,
którego testy logiczne nie dotykają wcale.

To samo ograniczenie dotyczy zrzutów ekranu — patrz
[`docs/screenshots/README.md`](screenshots/README.md).

## Czego świadomie nie zrobiono

`Snake3DApp` ma 886 linii i dałoby się go rozbić na budowę sceny, pętle,
wejście i HUD. Nie zostało to zrobione, bo jedyny test dotykający tej klasy
nie weryfikuje renderowania (patrz wyżej) — czyli nie ma siatki, która
złapałaby regresję po podziale. Kolejność powinna być odwrotna: najpierw
testy renderowania na maszynie z GPU, potem podział.

## Gałąź `claude/fervent-newton-71zji6`

W repozytorium jest starsza gałąź z **równoległą, niezgodną** przebudową:
usuwa `SnakeGame`, `Leaderboard`, `Settings` i `GameMode`, zastępując je
inną strukturą wokół `GameState`. Jest o dzień starsza od `main` i nie ma
tablicy wyników, trybów planszy ani combo.

Nie została zmergowana i nie została usunięta. Przeniesiony z niej został
natomiast fragment, który rozwiązywał realny problem: `Launcher` wraz
z konfiguracją grubego JAR-a. Reszta zostaje tam, gdzie jest — na wypadek,
gdyby ktoś chciał do niej wrócić.
