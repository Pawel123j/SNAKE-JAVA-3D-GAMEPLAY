package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnakeGameTest {

    private SnakeGame newGame() {
        return new SnakeGame(18, new Random(42));
    }

    @Test
    void newGameStartsReadyWithFullStartingSnake() {
        SnakeGame game = newGame();

        assertEquals(GameState.READY, game.state());
        assertEquals(SnakeGame.START_LENGTH, game.length());
        assertEquals(0, game.score());
        assertNotNull(game.food());
    }

    @Test
    void rejectsTooSmallGrid() {
        assertThrows(IllegalArgumentException.class, () -> new SnakeGame(3));
    }

    @Test
    void stepIsIgnoredUntilStarted() {
        SnakeGame game = newGame();
        Cell head = game.head();

        assertEquals(SnakeGame.StepResult.IGNORED, game.step());
        assertEquals(head, game.head());
    }

    @Test
    void movingRightAdvancesHead() {
        SnakeGame game = newGame();
        Cell head = game.head();
        game.start();

        assertEquals(SnakeGame.StepResult.MOVED, game.step());

        assertEquals(new Cell(head.x() + 1, head.z()), game.head());
        assertEquals(SnakeGame.START_LENGTH, game.length());
    }

    @Test
    void eatingFoodGrowsSnakeAndScores() {
        SnakeGame game = newGame();
        Cell head = new Cell(5, 5);
        game.loadState(
                List.of(head, new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(6, 5),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.ATE, game.step());

        assertEquals(SnakeGame.START_LENGTH + 1, game.length());
        assertEquals(SnakeGame.FOOD_SCORE, game.score());
        assertEquals(new Cell(6, 5), game.head());
        assertNotNull(game.food());
    }

    @Test
    void hittingWallEndsGame() {
        SnakeGame game = new SnakeGame(10, new Random(1));
        game.loadState(
                List.of(new Cell(9, 5), new Cell(8, 5), new Cell(7, 5), new Cell(6, 5)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.GAME_OVER, game.step());
        assertEquals(GameState.GAME_OVER, game.state());
    }

    @Test
    void runningIntoOwnBodyEndsGame() {
        SnakeGame game = newGame();
        // U-shape: moving RIGHT from (5,5) runs into the (6,5) body segment.
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(4, 6),
                        new Cell(5, 6), new Cell(6, 6), new Cell(6, 5), new Cell(6, 4)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.GAME_OVER, game.step());
        assertEquals(GameState.GAME_OVER, game.state());
    }

    @Test
    void followingTheTailIsAllowed() {
        SnakeGame game = newGame();
        // Moving into the tail cell is safe because the tail vacates it.
        game.loadState(
                List.of(new Cell(5, 5), new Cell(5, 6), new Cell(6, 6), new Cell(6, 5)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.MOVED, game.step());
        assertEquals(GameState.RUNNING, game.state());
        assertEquals(new Cell(6, 5), game.head());
    }

    @Test
    void cannotReverseDirectly() {
        SnakeGame game = newGame();
        Cell head = game.head();
        game.start();

        game.setDirection(Direction.LEFT); // opposite of initial RIGHT — ignored

        assertEquals(SnakeGame.StepResult.MOVED, game.step());
        assertEquals(new Cell(head.x() + 1, head.z()), game.head());
    }

    @Test
    void pausedGameDoesNotStep() {
        SnakeGame game = newGame();
        game.start();
        game.togglePause();
        Cell head = game.head();

        assertEquals(SnakeGame.StepResult.IGNORED, game.step());
        assertEquals(head, game.head());

        game.togglePause();
        assertEquals(SnakeGame.StepResult.MOVED, game.step());
    }

    @Test
    void fillingTheBoardWinsAndDoesNotHang() {
        // 4x4 board; snake occupies every cell except (3,3), with food there.
        SnakeGame game = new SnakeGame(4, new Random(0));
        List<Cell> body = new ArrayList<>();
        body.add(new Cell(3, 2)); // head, will move DOWN into (3,3)
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                Cell c = new Cell(x, z);
                if (!c.equals(new Cell(3, 2)) && !c.equals(new Cell(3, 3))) {
                    body.add(c);
                }
            }
        }
        game.loadState(body, Direction.DOWN, new Cell(3, 3), GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.WON, game.step());
        assertEquals(GameState.WON, game.state());
        assertNull(game.food());
        assertEquals(16, game.length());
    }

    @Test
    void tickSpeedsUpAsSnakeGrowsAndWithDifficulty() {
        SnakeGame game = newGame();
        game.setDifficulty(Difficulty.NORMAL);
        double atStart = game.tickMs();

        game.setDifficulty(Difficulty.HARD);
        assertTrue(game.tickMs() < atStart, "HARD should tick faster than NORMAL");
    }

    @Test
    void setDifficultyIgnoresNull() {
        SnakeGame game = newGame();
        game.setDifficulty(Difficulty.HARD);
        game.setDifficulty(null);
        assertSame(Difficulty.HARD, game.difficulty());
    }

    @Test
    void eatingBonusScoresWithoutGrowing() {
        SnakeGame game = newGame();
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);
        game.setBonusForTest(new Cell(6, 5), 10);

        assertEquals(SnakeGame.StepResult.ATE_BONUS, game.step());

        assertEquals(SnakeGame.BONUS_SCORE, game.score());
        assertEquals(SnakeGame.START_LENGTH, game.length(), "bonus must not grow the snake");
        assertNull(game.bonusFood());
    }

    @Test
    void bonusExpiresAfterLifetime() {
        SnakeGame game = newGame();
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);
        game.setBonusForTest(new Cell(12, 12), 1); // one step left

        assertEquals(SnakeGame.StepResult.MOVED, game.step());
        assertNull(game.bonusFood(), "bonus should vanish once its timer runs out");
    }

    @Test
    void bonusAppearsAfterEnoughApples() {
        SnakeGame game = newGame();
        game.setApplesEatenForTest(SnakeGame.FOODS_PER_BONUS - 1);
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(6, 5),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.ATE, game.step());
        assertNotNull(game.bonusFood(), "a golden bonus should appear on the Nth apple");
    }
}
