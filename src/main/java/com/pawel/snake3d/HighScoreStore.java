package com.pawel.snake3d;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists best scores between runs, keyed so each {@link Difficulty} keeps its
 * own record.
 *
 * <p>The scores live in a small {@link Properties} file (by default
 * {@code ~/.snake3d/highscores.properties}) so they survive application
 * restarts. All I/O failures are swallowed deliberately: a missing or unreadable
 * file should never crash the game, it simply means there is no record yet.
 */
public final class HighScoreStore {

    private final Path file;

    /** Creates a store backed by {@code ~/.snake3d/highscores.properties}. */
    public HighScoreStore() {
        this(Path.of(System.getProperty("user.home", "."), ".snake3d", "highscores.properties"));
    }

    /**
     * Creates a store backed by an explicit file, primarily for tests.
     *
     * @param file the file used to read and write best scores
     */
    public HighScoreStore(Path file) {
        this.file = file;
    }

    /**
     * Reads the persisted best score for a key.
     *
     * @param key the score key, e.g. a {@link Difficulty} name
     * @return the stored best score, or {@code 0} when none has been saved yet or
     *         the value cannot be read
     */
    public int load(String key) {
        Properties properties = read();
        try {
            return Math.max(0, Integer.parseInt(properties.getProperty(key, "0").trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * Persists a best score for a key, creating the parent directory if needed.
     * Failures are ignored so a read-only environment cannot break gameplay.
     *
     * @param key   the score key, e.g. a {@link Difficulty} name
     * @param score the score to persist
     */
    public void save(String key, int score) {
        Properties properties = read();
        properties.setProperty(key, Integer.toString(score));
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Snake 3D Deluxe high scores");
            }
        } catch (IOException ignored) {
            // Persisting the high score is best-effort.
        }
    }

    private Properties read() {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException ignored) {
                // Treat an unreadable file as "no high scores yet".
            }
        }
        return properties;
    }
}
