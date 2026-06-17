package com.pawel.snake3d;

/**
 * Difficulty presets that tune both how fast the snake moves and how generously
 * eating food is rewarded.
 *
 * <p>The timing fields are consumed by the view's game loop, while
 * {@link #scoreMultiplier()} is applied by {@link GameState}. Keeping the values
 * in one enum makes the trade-offs between presets easy to read and adjust.
 */
public enum Difficulty {

    EASY("Łatwy", 260, 130, 5.0, 1),
    NORMAL("Normalny", 200, 88, 7.5, 2),
    HARD("Trudny", 150, 62, 9.0, 3);

    private final String displayName;
    private final double baseTickMs;
    private final double minTickMs;
    private final double acceleration;
    private final int scoreMultiplier;

    Difficulty(String displayName, double baseTickMs, double minTickMs,
               double acceleration, int scoreMultiplier) {
        this.displayName = displayName;
        this.baseTickMs = baseTickMs;
        this.minTickMs = minTickMs;
        this.acceleration = acceleration;
        this.scoreMultiplier = scoreMultiplier;
    }

    /** @return the human-readable name shown in the UI */
    public String displayName() {
        return displayName;
    }

    /** @return points awarded per food, before this multiplier is applied */
    public int scoreMultiplier() {
        return scoreMultiplier;
    }

    /**
     * Computes the tick duration for a snake of the given length: the snake
     * accelerates as it grows, but never faster than {@link #minTickMs}.
     *
     * @param snakeLength the current number of segments
     * @return the delay between simulation steps, in milliseconds
     */
    public double tickMillisFor(int snakeLength) {
        int growth = Math.max(0, snakeLength - GameState.INITIAL_LENGTH);
        return Math.max(minTickMs, baseTickMs - growth * acceleration);
    }

    /** @return the next preset in the cycle, wrapping around to the first */
    public Difficulty next() {
        Difficulty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
