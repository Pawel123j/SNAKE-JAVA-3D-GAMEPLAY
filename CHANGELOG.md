# Changelog

Wszystkie istotne zmiany w projekcie są dokumentowane w tym pliku.
Format jest oparty na [Keep a Changelog](https://keepachangelog.com/pl/1.1.0/),
a wersjonowanie na [SemVer](https://semver.org/lang/pl/).

## [Unreleased]

### Added
- Silnik gry `GameState` w pełni oddzielony od JavaFX i pokryty testami JUnit 5.
- Trzy poziomy trudności (`Difficulty`: Łatwy / Normalny / Trudny) wpływające na
  tempo gry oraz mnożnik punktów, przełączane klawiszami `1`/`2`/`3`.
- Trwały najlepszy wynik zapamiętywany osobno dla każdego poziomu
  (`HighScoreStore`, plik `~/.snake3d/highscores.properties`).
- Efekt „pop" przy zjedzeniu jedzenia oraz karta `MODE` w HUD.
- Syntezowane efekty dźwiękowe (`SoundManager`, bez plików audio), wyciszane
  klawiszem `M`, samo‑wyłączające się przy braku urządzenia audio.
- Tryb demo (`-Dsnake.demo=true`) automatycznie startujący rozgrywkę.
- Uruchamialny „fat JAR" (`maven-shade-plugin`) z osobnym `Launcher`.
- Bezgłowy smoke‑test uruchomienia w CI (Xvfb) oraz publikacja artefaktu JAR.
- Dokumentacja: `CONTRIBUTING.md`, `CHANGELOG.md`, `.editorconfig`, licencja MIT.

### Changed
- `Snake3DApp` odchudzona do roli czystej warstwy widoku renderującej stan gry.
- Wzbogacony model: `Direction.opposite()/isOpposite()`, `Cell.translate(...)`.
- Zmodernizowany `pom.xml` (JUnit 5, Surefire, Shade, nowsze wtyczki).

## [1.0.0]

### Added
- Pierwsza wersja gry Snake 3D w JavaFX: arena 3D, oświetlenie, HUD,
  start / pauza / restart.
