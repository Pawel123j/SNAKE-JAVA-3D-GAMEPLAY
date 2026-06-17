package com.pawel.snake3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Headless, deterministic Snake game engine.
 *
 * <p>This class owns the complete rules of the game — snake movement, growth,
 * food placement, scoring and collision detection — without any dependency on
 * JavaFX or any other rendering technology. Keeping the simulation separate from
 * the view makes the rules unit-testable and lets the renderer stay a thin,
 * stateless projection of {@link #snake()} and {@link #food()}.
 *
 * <p>The engine is intentionally single-threaded: {@link #step()} advances the
 * simulation by exactly one tick and is expected to be driven by the UI's game
 * loop. A {@link Random} can be injected to make food placement reproducible in
 * tests.
 */
public final class GameState {

    /** Base points awarded per food, before the difficulty multiplier. */
    public static final int BASE_FOOD_SCORE = 10;

    /** Number of segments the snake starts with. */
    static final int INITIAL_LENGTH = 4;

    /** Outcome of advancing the simulation by one {@link #step()}. */
    public enum StepResult {
        /** The snake moved into a free cell. */
        MOVED,
        /** The snake ate food, grew by one segment and gained {@link #FOOD_SCORE} points. */
        ATE,
        /** The snake hit a wall or itself; the game is now over. */
        COLLISION
    }

    private final int gridSize;
    private final Random random;

    /** Head is the first element, tail is the last. */
    private final Deque<Cell> snake = new ArrayDeque<>();

    private Difficulty difficulty;
    private Direction currentDirection;
    private Direction queuedDirection;
    private Cell food;
    private int score;
    private boolean gameOver;

    /**
     * Creates a {@link Difficulty#NORMAL} game on a {@code gridSize}×{@code gridSize}
     * board with a non-deterministic random source.
     *
     * @param gridSize the number of cells along each edge of the board
     */
    public GameState(int gridSize) {
        this(gridSize, Difficulty.NORMAL, new Random());
    }

    /**
     * Creates a game with an explicit difficulty and injected random source. The
     * injectable {@link Random} keeps food placement reproducible in tests.
     *
     * @param gridSize   the number of cells along each edge of the board
     * @param difficulty the difficulty preset
     * @param random     the source of randomness used to place food
     * @throws IllegalArgumentException if the board is too small to host the
     *                                  initial snake
     */
    public GameState(int gridSize, Difficulty difficulty, Random random) {
        if (gridSize < INITIAL_LENGTH + 2) {
            throw new IllegalArgumentException("gridSize must be at least " + (INITIAL_LENGTH + 2));
        }
        this.gridSize = gridSize;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        this.random = Objects.requireNonNull(random, "random");
        reset();
    }

    /** Restores the engine to a fresh game: a centred snake, a new food and a zero score. */
    public void reset() {
        snake.clear();
        int z = gridSize / 2;
        int baseX = gridSize / 4;
        // Head first (largest x), tail last — the snake faces RIGHT.
        for (int i = INITIAL_LENGTH - 1; i >= 0; i--) {
            snake.addLast(new Cell(baseX + i, z));
        }
        currentDirection = Direction.RIGHT;
        queuedDirection = Direction.RIGHT;
        score = 0;
        gameOver = false;
        spawnFood();
    }

    /**
     * Records the player's desired heading for the next tick. The change is
     * ignored if it would reverse the snake straight into its own neck.
     *
     * @param direction the requested direction (ignored when {@code null})
     */
    public void setDirection(Direction direction) {
        if (direction == null || currentDirection.isOpposite(direction)) {
            return;
        }
        queuedDirection = direction;
    }

    /**
     * Advances the simulation by one tick.
     *
     * @return the {@link StepResult} describing what happened; repeated calls
     *         after a collision keep returning {@link StepResult#COLLISION}
     */
    public StepResult step() {
        if (gameOver) {
            return StepResult.COLLISION;
        }

        if (!currentDirection.isOpposite(queuedDirection)) {
            currentDirection = queuedDirection;
        }

        Cell next = snake.getFirst().translate(currentDirection);
        boolean willGrow = next.equals(food);

        if (isOutsideBoard(next) || hitsBody(next, willGrow)) {
            gameOver = true;
            return StepResult.COLLISION;
        }

        snake.addFirst(next);
        if (willGrow) {
            score += scorePerFood();
            spawnFood();
            return StepResult.ATE;
        }

        snake.removeLast();
        return StepResult.MOVED;
    }

    private boolean isOutsideBoard(Cell cell) {
        return cell.x() < 0 || cell.x() >= gridSize || cell.z() < 0 || cell.z() >= gridSize;
    }

    /**
     * Tests whether {@code next} overlaps the snake's body. When the snake is not
     * about to grow, the tail cell is vacated this tick and therefore safe to
     * enter.
     */
    private boolean hitsBody(Cell next, boolean willGrow) {
        int lastIndex = snake.size() - 1;
        int index = 0;
        for (Cell segment : snake) {
            boolean isTail = index == lastIndex;
            if (!(isTail && !willGrow) && segment.equals(next)) {
                return true;
            }
            index++;
        }
        return false;
    }

    /**
     * Places food on a random free cell. When the snake fills the entire board
     * (a perfect game) there is nowhere left to spawn and {@link #food()} becomes
     * {@code null}.
     */
    private void spawnFood() {
        if (snake.size() >= gridSize * gridSize) {
            food = null;
            return;
        }
        Cell candidate;
        do {
            candidate = new Cell(random.nextInt(gridSize), random.nextInt(gridSize));
        } while (snake.contains(candidate));
        food = candidate;
    }

    /** @return the number of cells along each edge of the board */
    public int gridSize() {
        return gridSize;
    }

    /** @return the active difficulty preset */
    public Difficulty difficulty() {
        return difficulty;
    }

    /**
     * Selects the difficulty used from the next {@link #reset()} onwards.
     *
     * @param difficulty the preset to apply
     */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    }

    /** @return the number of points the next eaten food is worth */
    public int scorePerFood() {
        return BASE_FOOD_SCORE * difficulty.scoreMultiplier();
    }

    /** @return the snake's segments as an immutable list, head first */
    public List<Cell> snake() {
        return List.copyOf(snake);
    }

    /** @return the snake's head, i.e. the leading segment */
    public Cell head() {
        return snake.getFirst();
    }

    /** @return the current length of the snake */
    public int length() {
        return snake.size();
    }

    /** @return the food cell, or {@code null} when the board is completely full */
    public Cell food() {
        return food;
    }

    /** @return the current score */
    public int score() {
        return score;
    }

    /** @return the direction the snake is currently travelling */
    public Direction direction() {
        return currentDirection;
    }

    /** @return {@code true} once the snake has collided with a wall or itself */
    public boolean isGameOver() {
        return gameOver;
    }

    // --- Test seams -------------------------------------------------------
    // Package-private hooks that let tests construct deterministic scenarios
    // without exposing mutable internals to the rest of the application.

    /**
     * Forces the food onto a specific cell. Visible for testing only.
     *
     * @param cell the cell the food should occupy
     */
    void placeFoodForTest(Cell cell) {
        this.food = cell;
    }

    /**
     * Replaces the entire snake and heading. Visible for testing only.
     *
     * @param cells     the segments, head first
     * @param direction the heading to travel in
     */
    void setSnakeForTest(List<Cell> cells, Direction direction) {
        snake.clear();
        snake.addAll(cells);
        currentDirection = direction;
        queuedDirection = direction;
        gameOver = false;
    }
}
