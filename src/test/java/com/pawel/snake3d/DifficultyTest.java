package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifficultyTest {

    @Test
    void snakeAcceleratesAsItGrows() {
        Difficulty normal = Difficulty.NORMAL;

        double atStart = normal.tickMillisFor(GameState.INITIAL_LENGTH);
        double whenLonger = normal.tickMillisFor(GameState.INITIAL_LENGTH + 10);

        assertTrue(whenLonger < atStart, "a longer snake should tick faster");
    }

    @Test
    void tickNeverDropsBelowTheFloor() {
        Difficulty hard = Difficulty.HARD;

        double veryLong = hard.tickMillisFor(10_000);

        assertTrue(veryLong > 0, "tick must stay positive");
        // The floor is constant regardless of how long the snake gets.
        assertEquals(hard.tickMillisFor(10_000), hard.tickMillisFor(20_000));
    }

    @Test
    void harderModesAreFasterAndScoreHigher() {
        assertTrue(Difficulty.HARD.tickMillisFor(20) < Difficulty.EASY.tickMillisFor(20));
        assertTrue(Difficulty.HARD.scoreMultiplier() > Difficulty.EASY.scoreMultiplier());
    }

    @Test
    void nextCyclesThroughAllPresets() {
        assertEquals(Difficulty.NORMAL, Difficulty.EASY.next());
        assertEquals(Difficulty.HARD, Difficulty.NORMAL.next());
        assertEquals(Difficulty.EASY, Difficulty.HARD.next());
    }
}
