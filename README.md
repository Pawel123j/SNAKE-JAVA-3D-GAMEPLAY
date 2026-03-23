# Snake 3D Deluxe

Elegancka gra Snake w prawdziwym 3D zrobiona w **Java + JavaFX 3D + Maven**.

## Co jest w środku

- kamera perspektywiczna 3D,
- światła i futurystyczna arena,
- animowany food,
- elegancki HUD,
- start / restart / pauza,
- płynny lekki ruch kamery.

## Sterowanie

- **WASD** albo **strzałki** — ruch
- **SPACJA** — start / restart
- **P** — pauza
- **R** — szybki restart

## Wymagania

- **JDK 21**
- **Maven 3.9+**

## Jak odpalić

W katalogu projektu uruchom:

```bash
mvn javafx:run
```

## Struktura projektu

```text
snake-3d-deluxe/
├── pom.xml
├── README.md
├── run.bat
└── src/
    └── main/
        └── java/
            └── com/
                └── pawel/
                    └── snake3d/
                        ├── Snake3DApp.java
                        ├── Cell.java
                        └── Direction.java
```
