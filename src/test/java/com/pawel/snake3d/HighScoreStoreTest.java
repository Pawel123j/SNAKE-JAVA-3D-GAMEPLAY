package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighScoreStoreTest {

    @Test
    void loadReturnsZeroWhenNoFileExists(@TempDir Path dir) {
        HighScoreStore store = new HighScoreStore(dir.resolve("missing/highscores.properties"));

        assertEquals(0, store.load("NORMAL"));
    }

    @Test
    void savedScoreCanBeReadBack(@TempDir Path dir) {
        HighScoreStore store = new HighScoreStore(dir.resolve("nested/highscores.properties"));

        store.save("NORMAL", 250);

        assertEquals(250, store.load("NORMAL"));
    }

    @Test
    void scoresArePersistedPerKeyIndependently(@TempDir Path dir) {
        HighScoreStore store = new HighScoreStore(dir.resolve("highscores.properties"));

        store.save("EASY", 80);
        store.save("HARD", 510);

        assertEquals(80, store.load("EASY"));
        assertEquals(510, store.load("HARD"));
        assertEquals(0, store.load("NORMAL"));
    }

    @Test
    void corruptValueIsTreatedAsNoScore(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("highscores.properties");
        Files.writeString(file, "NORMAL=not-a-number\n");
        HighScoreStore store = new HighScoreStore(file);

        assertEquals(0, store.load("NORMAL"));
    }
}
