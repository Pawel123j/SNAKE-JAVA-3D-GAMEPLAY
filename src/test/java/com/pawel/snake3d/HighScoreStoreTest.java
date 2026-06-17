package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighScoreStoreTest {

    @Test
    void loadReturnsZeroWhenNoFileExists(@TempDir Path dir) {
        HighScoreStore store = new HighScoreStore(dir.resolve("missing/highscore"));

        assertEquals(0, store.load());
    }

    @Test
    void savedScoreCanBeReadBack(@TempDir Path dir) {
        HighScoreStore store = new HighScoreStore(dir.resolve("nested/highscore"));

        store.save(250);

        assertEquals(250, store.load());
    }

    @Test
    void corruptFileIsTreatedAsNoScore(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("highscore");
        Files.writeString(file, "not-a-number");
        HighScoreStore store = new HighScoreStore(file);

        assertEquals(0, store.load());
    }
}
