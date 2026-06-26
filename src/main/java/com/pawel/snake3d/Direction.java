package com.pawel.snake3d;

/**
 * Movement direction on the grid. {@code z} grows "downwards" (towards the
 * camera), so {@link #UP} decreases z and {@link #DOWN} increases it.
 */
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dz;

    Direction(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    /** @return {@code true} when {@code other} points the opposite way. */
    public boolean isOpposite(Direction other) {
        return other != null && dx == -other.dx && dz == -other.dz;
    }
}
