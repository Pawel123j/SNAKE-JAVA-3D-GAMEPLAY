package com.pawel.snake3d;

/**
 * Difficulty presets. Each level defines how fast the snake moves and how
 * quickly it accelerates as it grows.
 */
public enum Difficulty {
    EASY("ŁATWY", 260, 6.0, 95),
    NORMAL("NORMALNY", 220, 7.5, 78),
    HARD("TRUDNY", 165, 9.0, 60);

    private final String label;
    private final double baseTickMs;
    private final double accelPerSegment;
    private final double minTickMs;

    Difficulty(String label, double baseTickMs, double accelPerSegment, double minTickMs) {
        this.label = label;
        this.baseTickMs = baseTickMs;
        this.accelPerSegment = accelPerSegment;
        this.minTickMs = minTickMs;
    }

    public String label() {
        return label;
    }

    /** @return delay between game ticks (ms) for a snake of the given length. */
    public double tickMs(int snakeLength) {
        int growth = Math.max(0, snakeLength - SnakeGame.START_LENGTH);
        return Math.max(minTickMs, baseTickMs - growth * accelPerSegment);
    }

    /** @return the next difficulty, wrapping around to {@link #EASY}. */
    public Difficulty next() {
        Difficulty[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
