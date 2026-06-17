# Wkład w projekt

Dzięki za zainteresowanie rozwojem **Snake 3D Deluxe**! Poniżej krótki przewodnik.

## Wymagania

- JDK 21
- Maven 3.9+

## Praca z kodem

```bash
mvn verify        # kompilacja + testy + spakowanie JAR-a
mvn test          # same testy
mvn javafx:run    # uruchomienie gry
```

## Zasady architektury

- **Logika gry trafia do `GameState`** (lub innych klas modelu) i nie może
  zależeć od JavaFX. Dzięki temu pozostaje testowalna bez uruchamiania UI.
- **`Snake3DApp` to wyłącznie warstwa widoku** — renderuje stan i obsługuje
  wejście, nie przechowuje zasad gry.
- Każda zmiana zachowania silnika powinna mieć **test JUnit 5**.

## Styl kodu

- Formatowanie wymusza [`.editorconfig`](.editorconfig) (4 spacje, UTF‑8, LF).
- Publiczne typy i metody opisuj zwięzłym Javadociem.
- Nazwy w języku angielskim; komunikaty dla gracza po polsku.

## Pull requesty

1. Utwórz gałąź tematyczną.
2. Upewnij się, że `mvn verify` przechodzi (CI uruchamia też smoke‑test).
3. Zaktualizuj `CHANGELOG.md` w sekcji `Unreleased`.
4. Opisz zwięźle, co i dlaczego się zmienia.
