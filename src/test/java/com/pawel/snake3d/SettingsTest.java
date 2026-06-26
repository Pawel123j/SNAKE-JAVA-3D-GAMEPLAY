package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @TempDir
    Path tempDir;

    private Path file() {
        return tempDir.resolve("settings.properties");
    }

    @Test
    void defaultsWhenFileMissing() {
        Settings settings = new Settings(file());
        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertEquals(GameMode.CLASSIC, settings.mode());
        assertTrue(settings.isSoundEnabled());
    }

    @Test
    void persistsAcrossInstances() {
        Settings first = new Settings(file());
        first.setDifficulty(Difficulty.HARD);
        first.setMode(GameMode.OBSTACLES);
        first.setSoundEnabled(false);

        Settings reloaded = new Settings(file());
        assertEquals(Difficulty.HARD, reloaded.difficulty());
        assertEquals(GameMode.OBSTACLES, reloaded.mode());
        assertFalse(reloaded.isSoundEnabled());
    }

    @Test
    void unknownValuesFallBackToDefaults() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "difficulty=INSANE\nmode=HYPERSPACE\n");

        Settings settings = new Settings(file());
        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertEquals(GameMode.CLASSIC, settings.mode());
    }
}
