package com.pawel.snake3d;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    void bootsAndRunsWithoutExceptions() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> error.compareAndSet(null, e));

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> error.compareAndSet(null, e));
            try {
                Snake3DApp app = new Snake3DApp();
                app.start(new Stage());
                Platform.runLater(() -> invokePrivate(app, "startOrRestart", error));
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            } finally {
                started.countDown();
            }
        });

        assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX did not start in time");
        Thread.sleep(3000); // let the loops run through a full game-over cycle
        Platform.exit();

        assertNull(error.get(), () -> "FX thread threw: " + error.get());
    }

    private static void invokePrivate(Object target, String method, AtomicReference<Throwable> error) {
        try {
            Method m = target.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            m.invoke(target);
        } catch (Throwable t) {
            error.compareAndSet(null, t);
        }
    }
}
