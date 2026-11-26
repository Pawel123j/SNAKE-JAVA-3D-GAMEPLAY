package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighScoreStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void zeroDoesNotCreateAMissingScoreFile() {
        Path file = tempDir.resolve("nested/highscore.txt");
        HighScoreStore store = new HighScoreStore(file);
        assertEquals(0, store.load());
        store.save(0);
        assertTrue(Files.notExists(file));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 120})
    void firstPositiveScorePersistsAndReloads(int score) throws IOException {
        Path file = tempDir.resolve("nested/highscore.txt");
        new HighScoreStore(file).save(score);
        assertEquals(Integer.toString(score), Files.readString(file));
        assertEquals(score, new HighScoreStore(file).load());
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
