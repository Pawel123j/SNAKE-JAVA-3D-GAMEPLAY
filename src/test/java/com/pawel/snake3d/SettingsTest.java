package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @CsvSource({"'', CLASSIC", "mode=WRAP, WRAP"})
    void loadsOlderSettingsWithOrWithoutABoardMode(String modeLine, GameMode expectedMode) throws IOException {
        // The original writer saved only preferences the player had set.
        String original = "difficulty=HARD\nsound=false\n" + modeLine;
        Files.writeString(file(), original);

        Settings settings = new Settings(file());
        assertEquals(Difficulty.HARD, settings.difficulty());
        assertEquals(expectedMode, settings.mode());
        assertFalse(settings.isSoundEnabled());
        assertEquals(original, Files.readString(file()));
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

    @Test
    void malformedUnicodeFallsBackAndPreservesInputForDiagnosis() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "difficulty=\\uZZZZ\nmode=WRAP\n");

        Settings settings = new Settings(file());

        assertEquals(Difficulty.NORMAL, settings.difficulty());
        assertEquals(GameMode.CLASSIC, settings.mode());
        try (Stream<Path> paths = Files.list(tempDir)) {
            assertTrue(paths.anyMatch(path -> path.getFileName().toString()
                    .startsWith("settings.properties.corrupt-")));
        }
    }
}
