package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighScoreStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsZeroWhenMissing() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("nested/highscore.txt"));
        assertEquals(0, store.load());
    }

    @Test
    void savePersistsAndReloads() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("nested/highscore.txt"));
        store.save(120);
        assertEquals(120, store.load());
    }

    @Test
    void saveKeepsHigherExistingScore() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("highscore.txt"));
        store.save(200);
        store.save(50); // lower — must not overwrite
        assertEquals(200, store.load());
    }

    @Test
    void corruptFileIsTreatedAsZero() throws IOException {
        Path file = tempDir.resolve("highscore.txt");
        Files.writeString(file, "not-a-number");

        HighScoreStore store = new HighScoreStore(file);
        assertEquals(0, store.load());
    }
}
