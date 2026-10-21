package com.pawel.snake3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persists the best score between runs in a small text file under the user's
 * home directory. All I/O failures are swallowed — a missing or unreadable
 * high score simply counts as zero so the game never crashes over it.
 */
public class HighScoreStore {

    private final Path file;

    public HighScoreStore() {
        this(defaultPath());
    }

    public HighScoreStore(Path file) {
        this.file = file;
    }

    private static Path defaultPath() {
        String directory = System.getProperty("snake.dataDir");
        return directory == null
                ? Paths.get(System.getProperty("user.home", "."), ".snake3d", "highscore.txt")
                : Paths.get(directory, "highscore.txt");
    }

    /** @return the stored best score, or 0 if none/unreadable. */
    public int load() {
        try {
            if (!Files.exists(file)) {
                return 0;
            }
            String content = Files.readString(file).trim();
            return Math.max(0, Integer.parseInt(content));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    /** Persists {@code score} only if it beats the stored value. */
    public void save(int score) {
        if (score <= load()) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, Integer.toString(score));
        } catch (IOException e) {
            // Best-effort persistence; ignore failures.
        }
    }
}
