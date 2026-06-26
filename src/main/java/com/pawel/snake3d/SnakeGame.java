package com.pawel.snake3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
    /** How many turns may be buffered, so fast corners are never dropped. */
    public static final int MAX_QUEUED_TURNS = 2;
    /** Eating again within this many steps keeps the combo streak alive. */
    public static final int COMBO_WINDOW = 16;
    /** Combo multiplier is capped at this value. */
    public static final int COMBO_MAX = 5;

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
    private final Set<Cell> obstacles = new HashSet<>();

    private final Deque<Direction> inputQueue = new ArrayDeque<>();

    private Difficulty difficulty = Difficulty.NORMAL;
    private GameMode mode = GameMode.CLASSIC;
    private Direction currentDirection;
    private Cell food;
    private Cell bonusFood;
    private int bonusTimer;
    private int applesEaten;
    private int combo;
    private int stepsSinceEat;
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
        inputQueue.clear();
        score = 0;
        bonusFood = null;
        bonusTimer = 0;
        applesEaten = 0;
        combo = 0;
        stepsSinceEat = COMBO_WINDOW + 1;
        state = GameState.READY;
        obstacles.clear();
        if (mode == GameMode.OBSTACLES) {
            generateObstacles();
        }
        spawnFood();
    }

    /**
     * Scatters static obstacles across the arena, keeping the snake's starting
     * lane clear so the player is never trapped at the first step.
     */
    private void generateObstacles() {
        int startRow = gridSize / 2;
        int target = Math.max(4, gridSize * gridSize / 24);
        int attempts = 0;
        while (obstacles.size() < target && attempts++ < target * 30) {
            Cell candidate = new Cell(random.nextInt(gridSize), random.nextInt(gridSize));
            if (Math.abs(candidate.z() - startRow) <= 1 || snake.contains(candidate)) {
                continue;
            }
            obstacles.add(candidate);
        }
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
     * Buffers a turn for an upcoming step. Up to {@link #MAX_QUEUED_TURNS} turns
     * are remembered so fast corners aren't dropped; each turn is validated
     * against the previously buffered one (or the current heading), so reversing
     * into the neck and redundant presses are ignored.
     */
    public void setDirection(Direction direction) {
        if (direction == null || state.isFinished() || inputQueue.size() >= MAX_QUEUED_TURNS) {
            return;
        }
        Direction reference = inputQueue.isEmpty() ? currentDirection : inputQueue.peekLast();
        if (direction == reference || direction.isOpposite(reference)) {
            return;
        }
        inputQueue.addLast(direction);
    }

    /** Advances the simulation by one tick. */
    public StepResult step() {
        if (state != GameState.RUNNING) {
            return StepResult.IGNORED;
        }

        stepsSinceEat++;

        if (bonusFood != null && --bonusTimer <= 0) {
            bonusFood = null;
        }

        Direction nextDirection = inputQueue.pollFirst();
        if (nextDirection != null && !currentDirection.isOpposite(nextDirection)) {
            currentDirection = nextDirection;
        }

        Cell next = snake.getFirst().step(currentDirection);

        if (mode == GameMode.WRAP) {
            next = wrap(next);
        } else if (isOutOfBounds(next)) {
            state = GameState.GAME_OVER;
            return StepResult.GAME_OVER;
        }

        if (obstacles.contains(next)) {
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
            combo = stepsSinceEat <= COMBO_WINDOW ? combo + 1 : 1;
            stepsSinceEat = 0;
            score += FOOD_SCORE * comboMultiplier();
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

    private Cell wrap(Cell cell) {
        return new Cell(Math.floorMod(cell.x(), gridSize), Math.floorMod(cell.z(), gridSize));
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
        for (Cell c : obstacles) {
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

    /** @return the current combo streak length (number of consecutive eats). */
    public int combo() {
        return combo;
    }

    /** @return the active points multiplier in [1, {@link #COMBO_MAX}]. */
    public int comboMultiplier() {
        return Math.min(Math.max(combo, 1), COMBO_MAX);
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

    public GameMode mode() {
        return mode;
    }

    /** Changes the board mode and resets the game to apply the new layout. */
    public void setMode(GameMode mode) {
        if (mode != null && mode != this.mode) {
            this.mode = mode;
            reset();
        }
    }

    /** @return unmodifiable view of obstacle cells (empty unless in OBSTACLES mode). */
    public Set<Cell> obstacles() {
        return Collections.unmodifiableSet(obstacles);
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
        this.inputQueue.clear();
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

    /** Visible for testing obstacle collisions without random generation. */
    void setObstaclesForTest(Collection<Cell> cells) {
        obstacles.clear();
        obstacles.addAll(cells);
    }

    /** Visible for testing — sets the mode without resetting the board. */
    void setModeForTest(GameMode mode) {
        this.mode = mode;
    }

    /** Visible for testing the combo multiplier. */
    void setComboStateForTest(int combo, int stepsSinceEat) {
        this.combo = combo;
        this.stepsSinceEat = stepsSinceEat;
    }
}
