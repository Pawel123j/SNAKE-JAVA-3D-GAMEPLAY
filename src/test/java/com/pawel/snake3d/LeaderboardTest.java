package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardTest {

    @TempDir
    Path tempDir;

    private Path file() {
        return tempDir.resolve("scores.txt");
    }

    @Test
    void keepsTopScoresSortedAndCapped() {
        Leaderboard board = new Leaderboard(file());
        for (int s : new int[]{30, 10, 50, 20, 40, 5, 60}) {
            board.submit(s);
        }
        assertEquals(List.of(60, 50, 40, 30, 20), board.top());
        assertEquals(60, board.best());
    }

    @Test
    void submitReportsWhetherItQualifies() {
        Leaderboard board = new Leaderboard(file());
        for (int s : new int[]{100, 90, 80, 70, 60}) {
            board.submit(s);
        }
        assertFalse(board.submit(10), "below the board should not qualify");
        assertTrue(board.submit(95), "a top score should qualify");
        assertFalse(board.submit(0), "non-positive scores are ignored");
    }

    @Test
    void tieAtTheCutoffQualifiesOnlyWhileThereIsRoom() {
        Leaderboard board = new Leaderboard(file());
        for (int score : List.of(50, 40, 30, 20)) {
            board.submit(score);
        }

        List<Integer> expected = List.of(50, 40, 30, 20, 20);
        assertTrue(board.submit(20));
        assertEquals(expected, board.top());

        Leaderboard reloaded = new Leaderboard(file());
        assertEquals(expected, reloaded.top());
        assertFalse(reloaded.submit(20));
        assertEquals(expected, reloaded.top());
        assertEquals(expected, new Leaderboard(file()).top());
    }

    @Test
    void persistsAcrossInstances() {
        Leaderboard first = new Leaderboard(file());
        first.submit(120);
        first.submit(80);

        Leaderboard reloaded = new Leaderboard(file());
        assertEquals(List.of(120, 80), reloaded.top());
    }

    @Test
    void corruptFileIsTreatedAsEmpty() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "garbage\n???");

        Leaderboard board = new Leaderboard(file());
        assertTrue(board.top().isEmpty());
        assertEquals(0, board.best());
    }
}
