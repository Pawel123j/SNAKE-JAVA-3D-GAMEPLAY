package com.pawel.snake3d;

/** Immutable grid coordinate. */
public record Cell(int x, int z) {

    /** @return a new cell shifted by one step in {@code direction}. */
    public Cell step(Direction direction) {
        return new Cell(x + direction.dx(), z + direction.dz());
    }
}
