package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    private static final int GRID = 18;

    @Test
    void newGameStartsWithFourSegmentsFacingRight() {
        GameState game = new GameState(GRID);

        assertEquals(4, game.length());
        assertEquals(Direction.RIGHT, game.direction());
        assertEquals(0, game.score());
        assertFalse(game.isGameOver());
        assertNotNull(game.food());
    }

    @Test
    void rejectsBoardTooSmallForInitialSnake() {
        assertThrows(IllegalArgumentException.class, () -> new GameState(3));
    }

    @Test
    void stepMovesHeadOneCellInCurrentDirection() {
        GameState game = new GameState(GRID);
        Cell head = game.head();
        game.placeFoodForTest(new Cell(0, 0)); // keep food out of the way

        GameState.StepResult result = game.step();

        assertEquals(GameState.StepResult.MOVED, result);
        assertEquals(new Cell(head.x() + 1, head.z()), game.head());
        assertEquals(4, game.length());
    }

    @Test
    void cannotReverseDirectly() {
        GameState game = new GameState(GRID);
        game.placeFoodForTest(new Cell(0, 0));

        game.setDirection(Direction.LEFT); // opposite of RIGHT — must be ignored
        game.step();

        assertEquals(Direction.RIGHT, game.direction());
    }

    @Test
    void eatingFoodGrowsSnakeAndIncreasesScore() {
        GameState game = new GameState(GRID);
        Cell head = game.head();
        game.placeFoodForTest(new Cell(head.x() + 1, head.z()));

        GameState.StepResult result = game.step();

        assertEquals(GameState.StepResult.ATE, result);
        assertEquals(5, game.length());
        assertEquals(GameState.FOOD_SCORE, game.score());
    }

    @Test
    void hittingWallEndsGame() {
        GameState game = new GameState(GRID);
        game.placeFoodForTest(new Cell(0, 0));

        // Travelling RIGHT, eventually the head must run off the right edge.
        GameState.StepResult result = GameState.StepResult.MOVED;
        for (int i = 0; i < GRID && result != GameState.StepResult.COLLISION; i++) {
            result = game.step();
        }

        assertEquals(GameState.StepResult.COLLISION, result);
        assertTrue(game.isGameOver());
    }

    @Test
    void runningIntoOwnBodyEndsGame() {
        GameState game = new GameState(GRID);
        game.setSnakeForTest(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(4, 6), new Cell(5, 6), new Cell(6, 6)),
                Direction.DOWN);
        game.placeFoodForTest(new Cell(0, 0));

        GameState.StepResult result = game.step();

        assertEquals(GameState.StepResult.COLLISION, result);
        assertTrue(game.isGameOver());
    }

    @Test
    void chasingTailIntoVacatedCellIsSafe() {
        GameState game = new GameState(GRID);
        // Head moving DOWN onto the cell the tail is leaving this tick.
        game.setSnakeForTest(
                List.of(new Cell(5, 5), new Cell(6, 5), new Cell(6, 6), new Cell(5, 6)),
                Direction.DOWN);
        game.placeFoodForTest(new Cell(0, 0));

        GameState.StepResult result = game.step();

        assertEquals(GameState.StepResult.MOVED, result);
        assertFalse(game.isGameOver());
    }

    @Test
    void steppingAfterGameOverStaysGameOver() {
        GameState game = new GameState(GRID);
        game.placeFoodForTest(new Cell(0, 0));
        while (!game.isGameOver()) {
            game.step();
        }

        assertEquals(GameState.StepResult.COLLISION, game.step());
        assertTrue(game.isGameOver());
    }

    @Test
    void foodNeverSpawnsOnTheSnake() {
        GameState game = new GameState(GRID, new Random(42));

        for (int i = 0; i < 500; i++) {
            Cell food = game.food();
            assertNotNull(food);
            assertFalse(game.snake().contains(food), "food spawned on snake at iteration " + i);
            // Steer the head straight onto the food to force a respawn frequently.
            game.placeFoodForTest(game.head().translate(game.direction()));
            game.step();
            if (game.isGameOver()) {
                game.reset();
            }
        }
    }

    @Test
    void resetReturnsToInitialState() {
        GameState game = new GameState(GRID);
        game.placeFoodForTest(game.head().translate(Direction.RIGHT));
        game.step(); // eat, growing and scoring

        game.reset();

        assertEquals(4, game.length());
        assertEquals(0, game.score());
        assertFalse(game.isGameOver());
        assertEquals(Direction.RIGHT, game.direction());
    }
}
