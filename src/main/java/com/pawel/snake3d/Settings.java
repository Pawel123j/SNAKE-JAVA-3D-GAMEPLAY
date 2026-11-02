package com.pawel.snake3d;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Remembers player preferences (difficulty, board mode, sound) between runs in
 * a properties file under the user's home directory. Like {@link HighScoreStore}
 * it degrades gracefully: any I/O or parsing error falls back to defaults.
 */
public class Settings {

    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_MODE = "mode";
    private static final String KEY_SOUND = "sound";

    private final Path file;
    private final Properties properties = new Properties();

    public Settings() {
        this(defaultPath());
    }

    public Settings(Path file) {
        this.file = file;
        load();
    }

    private static Path defaultPath() {
        String directory = System.getProperty("snake.dataDir");
        return directory == null
                ? Paths.get(System.getProperty("user.home", "."), ".snake3d", "settings.properties")
                : Paths.get(directory, "settings.properties");
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException | IllegalArgumentException e) {
            properties.clear();
            // Preserve malformed input for diagnosis before a later save replaces it.
            if (e instanceof IllegalArgumentException) {
                try {
                    Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + java.util.UUID.randomUUID());
                    Files.copy(file, backup);
                } catch (IOException ignored) { /* read-only data directories still start */ }
            }
        }
    }

    private void save() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            properties.store(out, "Snake 3D Deluxe settings");
            AtomicFiles.write(file, out.toByteArray());
        } catch (IOException e) {
            // best-effort persistence
        }
    }

    public Difficulty difficulty() {
        return enumOrDefault(properties.getProperty(KEY_DIFFICULTY), Difficulty.class, Difficulty.NORMAL);
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) {
            properties.setProperty(KEY_DIFFICULTY, difficulty.name());
            save();
        }
    }

    public GameMode mode() {
        return enumOrDefault(properties.getProperty(KEY_MODE), GameMode.class, GameMode.CLASSIC);
    }

    public void setMode(GameMode mode) {
        if (mode != null) {
            properties.setProperty(KEY_MODE, mode.name());
            save();
        }
    }

    public boolean isSoundEnabled() {
        String value = properties.getProperty(KEY_SOUND);
        return value == null || !value.equalsIgnoreCase("false");
    }

    public void setSoundEnabled(boolean enabled) {
        properties.setProperty(KEY_SOUND, Boolean.toString(enabled));
        save();
    }

    private static <E extends Enum<E>> E enumOrDefault(String value, Class<E> type, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
