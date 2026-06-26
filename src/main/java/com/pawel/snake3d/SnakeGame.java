package com.pawel.snake3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Snake, free of any rendering or JavaFX dependency so it
 * can be unit-tested in isolation. The view layer ({@code Snake3DApp}) drives
 * this model and reads its state to render frames.
 */
public class SnakeGame {

    /** Number of segments the snake starts with. */
    public static final int START_LENGTH = 4;
    /** Points awarded for eating a single food. */
    public static final int FOOD_SCORE = 10;
    /** Points awarded for eating a golden bonus. */
    public static final int BONUS_SCORE = 50;
    /** A bonus appears after this many regular foods are eaten. */
    public static final int FOODS_PER_BONUS = 5;
    /** How many steps a bonus stays on the board before vanishing. */
    public static final int BONUS_LIFETIME = 30;

    /**
     * Strategy for choosing where the next food appears. Receives the list of
     * currently free cells (never empty) and a source of randomness. Extracted
     * as a seam so tests can place food deterministically.
     */
    @FunctionalInterface
    public interface FoodPlacer {
        Cell place(List<Cell> freeCells, Random random);
    }

    /** Outcome of a single {@link #step()}. */
    public enum StepResult {
        /** Step ignored because the game was not running. */
        IGNORED,
        /** Snake moved one cell forward. */
        MOVED,
        /** Snake ate food and grew. */
        ATE,
        /** Snake ate a golden bonus (extra points, no growth). */
        ATE_BONUS,
        /** Snake crashed — game over. */
        GAME_OVER,
        /** Snake ate the last free cell and won. */
        WON
    }

    private static final FoodPlacer RANDOM_PLACER =
            (free, random) -> free.get(random.nextInt(free.size()));

    private final int gridSize;
    private final Random random;
    private final FoodPlacer foodPlacer;
    private final LinkedList<Cell> snake = new LinkedList<>();

    private Difficulty difficulty = Difficulty.NORMAL;
    private Direction currentDirection;
    private Direction queuedDirection;
    private Cell food;
    private Cell bonusFood;
    private int bonusTimer;
    private int applesEaten;
    private int score;
    private GameState state;

    public SnakeGame(int gridSize) {
        this(gridSize, new Random(), RANDOM_PLACER);
    }

    public SnakeGame(int gridSize, Random random) {
        this(gridSize, random, RANDOM_PLACER);
    }

    public SnakeGame(int gridSize, Random random, FoodPlacer foodPlacer) {
        if (gridSize < 4) {
            throw new IllegalArgumentException("gridSize must be >= 4, was " + gridSize);
        }
        this.gridSize = gridSize;
        this.random = random;
        this.foodPlacer = foodPlacer;
        reset();
    }

    /** Resets to a fresh, not-yet-started game. */
    public void reset() {
        snake.clear();
        int startRow = gridSize / 2;
        int headX = gridSize / 2 + 1;
        for (int i = 0; i < START_LENGTH; i++) {
            snake.addLast(new Cell(headX - i, startRow));
        }
        currentDirection = Direction.RIGHT;
        queuedDirection = Direction.RIGHT;
        score = 0;
        bonusFood = null;
        bonusTimer = 0;
        applesEaten = 0;
        state = GameState.READY;
        spawnFood();
    }

    /** Starts a fresh game (resets then runs). */
    public void start() {
        reset();
        state = GameState.RUNNING;
    }

    /** Toggles between {@link GameState#RUNNING} and {@link GameState#PAUSED}. */
    public void togglePause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
    }

    /**
     * Queues a turn for the next step. Reversing straight into the neck is
     * ignored, and turns are dropped once the game has finished.
     */
    public void setDirection(Direction direction) {
        if (direction == null || state.isFinished()) {
            return;
        }
        if (!currentDirection.isOpposite(direction)) {
            queuedDirection = direction;
        }
    }

    /** Advances the simulation by one tick. */
    public StepResult step() {
        if (state != GameState.RUNNING) {
            return StepResult.IGNORED;
        }

        if (bonusFood != null && --bonusTimer <= 0) {
            bonusFood = null;
        }

        if (!currentDirection.isOpposite(queuedDirection)) {
            currentDirection = queuedDirection;
        }

        Cell next = snake.getFirst().step(currentDirection);

        if (isOutOfBounds(next)) {
            state = GameState.GAME_OVER;
            return StepResult.GAME_OVER;
        }

        boolean eatsApple = next.equals(food);
        boolean eatsBonus = bonusFood != null && next.equals(bonusFood);
        boolean grow = eatsApple; // bonuses reward points but do not grow the snake
        boolean hitsSelf = grow ? snake.contains(next) : bodyExceptTailContains(next);
        if (hitsSelf) {
            state = GameState.GAME_OVER;
            return StepResult.GAME_OVER;
        }

        snake.addFirst(next);

        if (eatsApple) {
            score += FOOD_SCORE;
            applesEaten++;
            spawnFood();
            maybeSpawnBonus();
            return state == GameState.WON ? StepResult.WON : StepResult.ATE;
        }
        if (eatsBonus) {
            score += BONUS_SCORE;
            bonusFood = null;
            bonusTimer = 0;
            snake.removeLast();
            return StepResult.ATE_BONUS;
        }
        snake.removeLast();
        return StepResult.MOVED;
    }

    /** Spawns a golden bonus every {@link #FOODS_PER_BONUS} apples, if there is room. */
    private void maybeSpawnBonus() {
        if (bonusFood != null || state == GameState.WON) {
            return;
        }
        if (applesEaten == 0 || applesEaten % FOODS_PER_BONUS != 0) {
            return;
        }
        List<Cell> free = freeCells();
        free.remove(food);
        if (free.isEmpty()) {
            return;
        }
        bonusFood = foodPlacer.place(free, random);
        bonusTimer = BONUS_LIFETIME;
    }

    private boolean isOutOfBounds(Cell cell) {
        return cell.x() < 0 || cell.x() >= gridSize || cell.z() < 0 || cell.z() >= gridSize;
    }

    /** True if any segment except the (moving) tail occupies {@code cell}. */
    private boolean bodyExceptTailContains(Cell cell) {
        int index = 0;
        int last = snake.size() - 1;
        for (Cell segment : snake) {
            if (index++ == last) {
                break;
            }
            if (segment.equals(cell)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places the next food on a free cell. When the board is completely filled
     * the player has won — this also fixes the original infinite loop that span
     * forever looking for a free cell that no longer existed.
     */
    private void spawnFood() {
        List<Cell> free = freeCells();
        if (free.isEmpty()) {
            food = null;
            state = GameState.WON;
            return;
        }
        food = foodPlacer.place(free, random);
    }

    private List<Cell> freeCells() {
        boolean[][] occupied = new boolean[gridSize][gridSize];
        for (Cell c : snake) {
            occupied[c.x()][c.z()] = true;
        }
        List<Cell> free = new ArrayList<>();
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                if (!occupied[x][z]) {
                    free.add(new Cell(x, z));
                }
            }
        }
        return free;
    }

    // --- accessors -----------------------------------------------------------

    public int gridSize() {
        return gridSize;
    }

    /** @return unmodifiable view of the snake, head first. */
    public List<Cell> snakeBody() {
        return Collections.unmodifiableList(snake);
    }

    public Cell head() {
        return snake.getFirst();
    }

    public int length() {
        return snake.size();
    }

    /** @return current food cell, or {@code null} after a win. */
    public Cell food() {
        return food;
    }

    /** @return the active golden bonus cell, or {@code null} when none is on the board. */
    public Cell bonusFood() {
        return bonusFood;
    }

    /** @return remaining bonus lifetime as a fraction in [0, 1], or 0 when inactive. */
    public double bonusFraction() {
        return bonusFood == null ? 0.0 : (double) bonusTimer / BONUS_LIFETIME;
    }

    public int score() {
        return score;
    }

    public GameState state() {
        return state;
    }

    public Direction currentDirection() {
        return currentDirection;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
    }

    /** @return delay before the next tick (ms) for the current difficulty. */
    public double tickMs() {
        return difficulty.tickMs(snake.size());
    }

    // --- testing seam --------------------------------------------------------

    /**
     * Loads an arbitrary state. Visible for testing collision and win scenarios
     * that are awkward to reach through normal play.
     */
    void loadState(List<Cell> body, Direction direction, Cell food, GameState state) {
        snake.clear();
        snake.addAll(body);
        this.currentDirection = direction;
        this.queuedDirection = direction;
        this.food = food;
        this.state = state;
    }

    /** Visible for testing the golden bonus. */
    void setBonusForTest(Cell bonus, int timer) {
        this.bonusFood = bonus;
        this.bonusTimer = timer;
    }

    /** Visible for testing bonus spawn cadence. */
    void setApplesEatenForTest(int count) {
        this.applesEaten = count;
    }
}
