package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void eatingTheLastTwoCellsWinsOnlyOnTheSecondApple() {
        SnakeGame game = new SnakeGame(4, new Random(0));
        // The head faces the two empty squares in the top row.
        List<Cell> body = List.of(
                new Cell(2, 0), new Cell(3, 0),
                new Cell(3, 1), new Cell(2, 1), new Cell(1, 1), new Cell(0, 1),
                new Cell(0, 2), new Cell(1, 2), new Cell(2, 2), new Cell(3, 2),
                new Cell(3, 3), new Cell(2, 3), new Cell(1, 3), new Cell(0, 3));
        game.loadState(body, Direction.LEFT, new Cell(1, 0), GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.ATE, game.step());
        assertEquals(GameState.RUNNING, game.state());
        assertEquals(15, game.length());
        assertEquals(10, game.score());
        assertEquals(new Cell(0, 0), game.food());

        assertEquals(SnakeGame.StepResult.WON, game.step());
        assertEquals(GameState.WON, game.state());
        assertNull(game.food());
        assertEquals(16, game.length());
        assertEquals(30, game.score()); // the second consecutive apple earns 20
        assertEquals(SnakeGame.StepResult.IGNORED, game.step());
        assertEquals(30, game.score());
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

    @Test
    void wrapModeWrapsAroundTheEdge() {
        SnakeGame game = newGame();
        game.setModeForTest(GameMode.WRAP);
        game.loadState(
                List.of(new Cell(17, 9), new Cell(16, 9), new Cell(15, 9), new Cell(14, 9)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.MOVED, game.step());
        assertEquals(GameState.RUNNING, game.state());
        assertEquals(new Cell(0, 9), game.head(), "leaving the right edge re-enters from the left");
    }

    @Test
    void obstacleEndsGame() {
        SnakeGame game = newGame();
        game.setObstaclesForTest(Set.of(new Cell(6, 5)));
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(0, 0),
                GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.GAME_OVER, game.step());
        assertEquals(GameState.GAME_OVER, game.state());
    }

    @Test
    void obstaclesModeKeepsSnakeAndFoodClear() {
        SnakeGame game = newGame();
        game.setMode(GameMode.OBSTACLES);

        assertFalse(game.obstacles().isEmpty(), "obstacles mode should scatter obstacles");
        assertTrue(java.util.Collections.disjoint(game.obstacles(), game.snakeBody()),
                "obstacles must never overlap the snake");
        assertFalse(game.obstacles().contains(game.food()), "food must not spawn on an obstacle");
    }

    @Test
    void winIgnoresObstacleCells() {
        // 4x4 board, one obstacle at (3,3); snake fills everything except the
        // food cell (3,2), so eating it leaves no free cell -> win.
        SnakeGame game = new SnakeGame(4, new Random(0));
        game.setObstaclesForTest(Set.of(new Cell(3, 3)));

        Set<Cell> blocked = Set.of(new Cell(3, 3), new Cell(3, 2));
        List<Cell> body = new ArrayList<>();
        body.add(new Cell(3, 1)); // head, moves DOWN into food at (3,2)
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                Cell c = new Cell(x, z);
                if (!blocked.contains(c) && !c.equals(new Cell(3, 1))) {
                    body.add(c);
                }
            }
        }
        game.loadState(body, Direction.DOWN, new Cell(3, 2), GameState.RUNNING);

        assertEquals(SnakeGame.StepResult.WON, game.step());
        assertNull(game.food());
    }

    @Test
    void bufferedTurnsApplyInOrder() {
        SnakeGame game = newGame();
        game.start();
        Cell head = game.head();

        game.setDirection(Direction.UP);   // valid vs RIGHT
        game.setDirection(Direction.LEFT); // valid vs the buffered UP

        game.step();
        assertEquals(new Cell(head.x(), head.z() - 1), game.head());
        game.step();
        assertEquals(new Cell(head.x() - 1, head.z() - 1), game.head(),
                "the second buffered turn should apply on the next step");
    }

    @Test
    void inputBufferRejectsReverseAndRespectsCap() {
        SnakeGame game = newGame();
        game.start();
        Cell head = game.head();

        game.setDirection(Direction.UP);    // buffered
        game.setDirection(Direction.DOWN);  // opposite of UP -> ignored
        game.setDirection(Direction.LEFT);  // buffered (now full: [UP, LEFT])
        game.setDirection(Direction.UP);    // buffer full -> ignored

        game.step(); // UP
        game.step(); // LEFT
        game.step(); // nothing buffered -> keeps going LEFT
        assertEquals(new Cell(head.x() - 2, head.z() - 1), game.head());
    }

    @Test
    void comboMultipliesConsecutiveEats() {
        SnakeGame game = newGame();
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(6, 5),
                GameState.RUNNING);
        game.setComboStateForTest(2, 0); // eating again immediately -> streak grows to 3

        assertEquals(SnakeGame.StepResult.ATE, game.step());
        assertEquals(3, game.comboMultiplier());
        assertEquals(SnakeGame.FOOD_SCORE * 3, game.score());
    }

    @Test
    void comboResetsAfterWindow() {
        SnakeGame game = newGame();
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(6, 5),
                GameState.RUNNING);
        game.setComboStateForTest(4, SnakeGame.COMBO_WINDOW); // too slow -> streak breaks

        assertEquals(SnakeGame.StepResult.ATE, game.step());
        assertEquals(1, game.comboMultiplier());
        assertEquals(SnakeGame.FOOD_SCORE, game.score());
    }

    @Test
    void comboMultiplierIsCapped() {
        SnakeGame game = newGame();
        game.loadState(
                List.of(new Cell(5, 5), new Cell(4, 5), new Cell(3, 5), new Cell(2, 5)),
                Direction.RIGHT,
                new Cell(6, 5),
                GameState.RUNNING);
        game.setComboStateForTest(SnakeGame.COMBO_MAX, 0);

        assertEquals(SnakeGame.StepResult.ATE, game.step());
        assertEquals(SnakeGame.COMBO_MAX, game.comboMultiplier());
        assertEquals(SnakeGame.FOOD_SCORE * SnakeGame.COMBO_MAX, game.score());
    }

    @Test
    void switchingModeResetsTheGame() {
        SnakeGame game = newGame();
        game.start();
        game.step();
        int movedScoreless = game.score();

        game.setMode(GameMode.WRAP);

        assertEquals(GameMode.WRAP, game.mode());
        assertEquals(GameState.READY, game.state(), "changing mode should reset to a fresh game");
        assertEquals(SnakeGame.START_LENGTH, game.length());
        assertEquals(0, movedScoreless);
    }
}
