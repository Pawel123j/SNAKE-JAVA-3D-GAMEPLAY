package com.pawel.snake3d;

/**
 * Cardinal directions the snake can travel on the 2D logical grid.
 *
 * <p>The grid uses {@code x} for the horizontal axis and {@code z} for the depth
 * axis (mapped to the 3D world's Z axis by the renderer), so {@link #UP} and
 * {@link #DOWN} move the snake along {@code z}.
 */
public enum Direction {

    UP,
    DOWN,
    LEFT,
    RIGHT;

    /**
     * @return the direction pointing the opposite way to this one
     */
    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    /**
     * @param other the direction to compare against (may be {@code null})
     * @return {@code true} when {@code other} is the exact reverse of this
     *         direction; a snake is never allowed to reverse into itself
     */
    public boolean isOpposite(Direction other) {
        return other != null && opposite() == other;
    }
}
