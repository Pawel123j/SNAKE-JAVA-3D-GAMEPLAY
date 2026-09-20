# Zrzuty ekranu

Ten katalog jest pusty i to jest świadoma decyzja, a nie zaniedbanie.

## Dlaczego nie ma tu obrazków

W repozytorium jest narzędzie, które potrafi je wygenerować z prawdziwej,
uruchomionej gry (`ScreenshotTest`). Uruchomione w środowisku bez karty
graficznej — czyli pod `xvfb`, tak jak chodzi CI — produkuje jednak obrazki,
które **kłamią**: widać na nich nakładkę z wynikiem i podpowiedziami, a
plansza, wąż i jedzenie są niewidoczne.

Powód jest konkretny i da się go zobaczyć w logu:

```
Prism pipeline init order: sw
*** Fallback to Prism SW pipeline
WARNING: System can't support ConditionalFeature.SCENE3D
```

JavaFX bez GPU spada na programowy pipeline Prism, który **nie obsługuje
sceny 3D w ogóle**. Cała warstwa 3D tej gry po prostu się nie rysuje.

Wrzucenie takich zrzutów do README byłoby gorsze niż ich brak: oglądający
zobaczyłby pustą planszę i uznał, że gra nie działa.

## Jak je wygenerować

Na maszynie z kartą graficzną (zwykły komputer z pulpitem wystarczy):

```bash
mvn -B test -Dtest=ScreenshotTest \
    -Dsnake.screenshots=true \
    -Dsnake.screenshots.dir=docs/screenshots \
    -DfailIfNoTests=false
```

Powstanie pięć plików:

| Plik | Co pokazuje |
|---|---|
| `01-menu.png` | ekran startowy |
| `02-gameplay.png` | rozgrywka |
| `03-pauza.png` | pauza nałożona na planszę |
| `04-tryb-planszy.png` | inny tryb planszy (klawisz `B`) |
| `05-koniec-gry.png` | ekran końca gry z wynikiem |

Narzędzie steruje grą tak, jak zrobiłby to człowiek — wysyła zdarzenia
klawiszy do sceny — i zapisuje to, co wyrenderował silnik. Nie ma tam
żadnej inscenizacji.

Jedna rzecz warta uwagi w kodzie: `Scene.snapshot()` **nie renderuje
zawartości `SubScene` 3D**. Dlatego narzędzie robi dwa zrzuty (świat 3D
i nakładkę 2D) i składa je w tej samej kolejności, w jakiej leżą w
`StackPane`.

## GIF z rozgrywki

Nie ma go i nie zostanie dorobiony automatycznie. Nagranie rozgrywki wymaga
kolejnych klatek renderowanych przez GPU — ten sam blocker co wyżej, tylko
razy kilkadziesiąt. Jeżeli ma powstać, to z prawdziwego nagrania ekranu
podczas grania, a nie z wygenerowanej animacji.
