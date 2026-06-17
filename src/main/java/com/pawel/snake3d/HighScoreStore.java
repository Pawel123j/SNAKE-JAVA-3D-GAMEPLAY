package com.pawel.snake3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists the best score between runs as a single integer in a small text file.
 *
 * <p>By default the score lives in {@code ~/.snake3d/highscore} so it survives
 * application restarts. All I/O failures are swallowed deliberately: a missing or
 * unreadable high-score file should never crash the game, it simply means there
 * is no record yet.
 */
public final class HighScoreStore {

    private final Path file;

    /** Creates a store backed by {@code ~/.snake3d/highscore}. */
    public HighScoreStore() {
        this(Path.of(System.getProperty("user.home", "."), ".snake3d", "highscore"));
    }

    /**
     * Creates a store backed by an explicit file, primarily for tests.
     *
     * @param file the file used to read and write the best score
     */
    public HighScoreStore(Path file) {
        this.file = file;
    }

    /**
     * Reads the persisted best score.
     *
     * @return the stored best score, or {@code 0} when none has been saved yet or
     *         the file cannot be read
     */
    public int load() {
        try {
            if (Files.exists(file)) {
                return Math.max(0, Integer.parseInt(Files.readString(file).trim()));
            }
        } catch (IOException | NumberFormatException ignored) {
            // Treat any problem as "no high score yet".
        }
        return 0;
    }

    /**
     * Writes the best score, creating the parent directory if necessary. Failures
     * are ignored so a read-only environment cannot break gameplay.
     *
     * @param score the score to persist
     */
    public void save(int score) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, Integer.toString(score));
        } catch (IOException ignored) {
            // Persisting the high score is best-effort.
        }
    }
}
