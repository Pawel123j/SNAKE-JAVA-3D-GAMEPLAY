package com.pawel.snake3d;

/**
 * An immutable position on the logical game grid.
 *
 * @param x column index, growing to the right
 * @param z row index, growing into the screen (the 3D world's depth axis)
 */
public record Cell(int x, int z) {

    /**
     * Returns the neighbouring cell one step away in the given direction.
     *
     * @param direction the direction to move towards
     * @return a new {@link Cell} adjacent to this one
     */
    public Cell translate(Direction direction) {
        return switch (direction) {
            case UP -> new Cell(x, z - 1);
            case DOWN -> new Cell(x, z + 1);
            case LEFT -> new Cell(x - 1, z);
            case RIGHT -> new Cell(x + 1, z);
        };
    }
}
