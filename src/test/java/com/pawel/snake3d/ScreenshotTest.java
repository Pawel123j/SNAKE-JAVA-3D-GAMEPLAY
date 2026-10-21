package com.pawel.snake3d;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates the README screenshots from the real, running game.
 *
 * <p>Disabled by default, exactly like {@link SmokeTest} — run it on demand:
 *
 * <pre>
 * xvfb-run -a mvn -B test -Dtest=ScreenshotTest -Dsnake.screenshots=true \
 *          -Dsnake.screenshots.dir=docs/screenshots -DfailIfNoTests=false
 * </pre>
 *
 * <p>The images come from {@code Scene.snapshot()}, so they are the renderer's
 * own output rather than a capture of somebody's desktop: no window chrome, no
 * cursor, identical on every machine. That matters here for one reason — a
 * screenshot of a game is a claim that the game runs, and a claim that can be
 * regenerated on demand is worth more than one that cannot.
 */
@EnabledIfSystemProperty(named = "snake.screenshots", matches = "true")
class ScreenshotTest {

    private static final int BOOT_TIMEOUT_SECONDS = 20;

    @Test
    void captureGameplay() throws Exception {
        Path outDir = Path.of(System.getProperty("snake.screenshots.dir", "docs/screenshots"));
        Files.createDirectories(outDir);

        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Scene> sceneRef = new AtomicReference<>();
        AtomicReference<Snake3DApp> appRef = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> error.compareAndSet(null, e));

        CountDownLatch booted = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                if (!Platform.isSupported(javafx.application.ConditionalFeature.SCENE3D)) {
                    throw new IllegalStateException("SCENE3D unavailable: refusing to label a blank 2D overlay as gameplay");
                }
                Snake3DApp app = new Snake3DApp();
                Stage stage = new Stage();
                app.start(stage);
                appRef.set(app);
                sceneRef.set(stage.getScene());
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            } finally {
                booted.countDown();
            }
        });
        assertTrue(booted.await(BOOT_TIMEOUT_SECONDS, TimeUnit.SECONDS), "JavaFX did not start in time");
        if (error.get() != null) {
            Platform.exit();
            throw new AssertionError("Renderer did not provide a real 3D scene", error.get());
        }
        Scene scene = sceneRef.get();

        // 1. Ekran startowy — gra czeka na naciśnięcie klawisza.
        settle(800);
        shoot(scene, outDir, "01-menu");

        // 2. Rozgrywka. Zrzut robimy krótko po starcie: plansza jest niewielka,
        //    a wąż porusza się sam, więc czekanie kilku sekund kończy się
        //    ekranem "GAME OVER" zamiast gry.
        press(scene, KeyCode.SPACE);
        settle(700);
        shoot(scene, outDir, "02-gameplay");

        // 3. Pauza — widać planszę wraz z komunikatem nałożonym na grę.
        press(scene, KeyCode.P);
        settle(400);
        shoot(scene, outDir, "03-pauza");
        press(scene, KeyCode.P);

        // 4. Inny tryb planszy (klawisz B przełącza tryby po kolei).
        press(scene, KeyCode.SPACE);
        settle(300);
        press(scene, KeyCode.B);
        settle(700);
        shoot(scene, outDir, "04-tryb-planszy");

        // 5. Koniec gry: wąż idzie prosto, aż w coś uderzy.
        press(scene, KeyCode.SPACE);
        settle(6000);
        shoot(scene, outDir, "05-koniec-gry");

        Platform.exit();

        Throwable thrown = error.get();
        if (thrown != null) {
            throw new AssertionError("FX thread threw while taking screenshots", thrown);
        }
    }

    /** Wysyła zdarzenie klawisza tak, jak zrobiłby to użytkownik. */
    private static void press(Scene scene, KeyCode code) {
        runAndWait(() -> scene.getOnKeyPressed().handle(
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false)));
    }

    /** Pozwala pętli renderowania i pętli gry przemielić kilka klatek. */
    private static void settle(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * Zapisuje zrzut sceny.
     *
     * <p>Trzeba tu obejść ograniczenie JavaFX: {@code Scene.snapshot()} nie
     * renderuje zawartości {@link SubScene} 3D — wychodzi z niej sama nakładka
     * 2D na pustym tle, czyli obrazek, który kłamie na temat gry. Zrzut
     * {@code SubScene} wykonany osobno renderuje świat 3D poprawnie, więc
     * robimy dwa zrzuty i składamy je w tej samej kolejności, w jakiej leżą
     * w {@link StackPane}: najpierw świat, na wierzch nakładka.
     */
    private static void shoot(Scene scene, Path dir, String name) {
        AtomicReference<WritableImage> worldShot = new AtomicReference<>();
        AtomicReference<WritableImage> overlayShot = new AtomicReference<>();

        runAndWait(() -> {
            StackPane root = (StackPane) scene.getRoot();
            SubScene world = (SubScene) root.getChildren().get(0);
            Node overlay = root.getChildren().get(1);

            worldShot.set(world.snapshot(new SnapshotParameters(), null));

            SnapshotParameters overlayParams = new SnapshotParameters();
            overlayParams.setFill(javafx.scene.paint.Color.TRANSPARENT);
            overlayShot.set(overlay.snapshot(overlayParams, null));
        });

        try {
            BufferedImage composed = toBufferedImage(worldShot.get());
            java.util.Set<Integer> colors = new java.util.HashSet<>();
            for (int y = 0; y < composed.getHeight(); y += 4) {
                for (int x = 0; x < composed.getWidth(); x += 4) colors.add(composed.getRGB(x, y));
            }
            assertTrue(colors.size() > 64, "3D world must contain rendered geometry, not a uniform clear color");
            BufferedImage overlay = toBufferedImage(overlayShot.get());
            java.awt.Graphics2D g = composed.createGraphics();
            g.drawImage(overlay, 0, 0, null);
            g.dispose();

            File out = dir.resolve(name + ".png").toFile();
            ImageIO.write(composed, "png", out);
            System.out.println("Zapisano: " + out + " (" + composed.getWidth() + "x" + composed.getHeight() + ")");
        } catch (Exception e) {
            throw new IllegalStateException("Nie udało się zapisać zrzutu " + name, e);
        }
    }

    /** Wykonuje zadanie na wątku JavaFX i czeka na jego zakończenie. */
    private static void runAndWait(Runnable task) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(BOOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Zadanie na wątku JavaFX nie zakończyło się w czasie");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        if (failure.get() != null) {
            throw failure.get();
        }
    }


    /**
     * Przepisuje obraz JavaFX do obrazu AWT bez {@code SwingFXUtils}.
     *
     * <p>Ta klasa siedzi w module {@code javafx-swing}, którego projekt nie
     * używa. Dokładanie zależności do całego projektu po to, żeby narzędzie
     * pomocnicze miało jedną metodę konwersji, byłoby złą wymianą — pętla po
     * pikselach jest krótsza niż uzasadnienie dla nowej zależności.
     */
    private static BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return out;
    }

    @SuppressWarnings("unused")
    private static void invokePrivate(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }
}
