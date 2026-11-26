package com.pawel.snake3d;

import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless boot smoke test for the JavaFX view. Disabled by default — the CI
 * "smoke" job enables it with {@code -Dsnake.smoke.headless=true} under xvfb.
 *
 * <p>It boots the real {@link Snake3DApp}, starts a run and lets the render and
 * game loops churn for a few seconds (the snake runs into a wall, triggering the
 * game-over shake). Any exception on the JavaFX thread fails the test.
 *
 * <p><b>What this does and does not cover.</b> It exercises scene construction,
 * the render and game loops, interpolation, particle and shake bookkeeping, and
 * the node-graph updates that the pure-logic unit tests never touch — so a null
 * dereference or a bad cast in that code fails the build.
 *
 * <p>It does <b>not</b> verify that anything is actually drawn. Under xvfb
 * there is no GPU, JavaFX falls back to the software Prism pipeline, and that
 * pipeline reports {@code System can't support ConditionalFeature.SCENE3D}:
 * the 3D scene is not rasterised at all. A regression that leaves the board
 * blank while still updating the node graph would pass this test. Saying so
 * here is the point — a test whose description promises more than it delivers
 * is worse than no test.
 */
@EnabledIfSystemProperty(named = "snake.smoke.headless", matches = "true")
class SmokeTest {

    @Test
    void bootsAndRunsWithoutExceptions(@TempDir Path dataDir) throws Exception {
        String previousDir = System.setProperty("snake.dataDir", dataDir.toString());
        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> error.compareAndSet(null, e));
        try {
            Platform.startup(() -> {
                Thread.currentThread().setUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
                try {
                    Stage stage = new Stage();
                    Snake3DApp app = new Snake3DApp();
                    app.start(stage);
                    Scene scene = stage.getScene();
                    press(scene, KeyCode.DIGIT1);
                    press(scene, KeyCode.SPACE);
                    assertEquals(260.0, loop(app).getCycleDuration().toMillis());
                    press(scene, KeyCode.P);
                    press(scene, KeyCode.DIGIT3);
                    assertEquals(Animation.Status.PAUSED, loop(app).getStatus());
                    press(scene, KeyCode.P);
                    assertEquals(Animation.Status.RUNNING, loop(app).getStatus());
                    assertEquals(165.0, loop(app).getCycleDuration().toMillis());
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    started.countDown();
                }
            });
            assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start in time");
            Thread.sleep(3000); // let the loops run through a full game-over cycle
            assertNull(error.get(), () -> "FX thread threw: " + error.get());
        } finally {
            Platform.exit();
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
            if (previousDir == null) {
                System.clearProperty("snake.dataDir");
            } else {
                System.setProperty("snake.dataDir", previousDir);
            }
        }
    }

    private static void press(Scene scene, KeyCode key) {
        Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
    }

    private static Timeline loop(Snake3DApp app) throws ReflectiveOperationException {
        Field field = Snake3DApp.class.getDeclaredField("gameLoop");
        field.setAccessible(true);
        return (Timeline) field.get(app);
    }
}
