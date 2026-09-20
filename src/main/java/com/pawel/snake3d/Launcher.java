package com.pawel.snake3d;

/**
 * Plain entry point used when the application is launched from an executable
 * (shaded) JAR.
 *
 * <p>A JavaFX {@link javafx.application.Application} cannot be the {@code Main-Class}
 * of a fat JAR — the runtime refuses to start with "JavaFX runtime components are
 * missing". Delegating through this non-{@code Application} launcher sidesteps that
 * restriction while keeping {@link Snake3DApp} as the real application class.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Snake3DApp.main(args);
    }
}
