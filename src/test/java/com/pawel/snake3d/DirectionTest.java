package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionTest {

    @Test
    void oppositesAreDetected() {
        assertTrue(Direction.UP.isOpposite(Direction.DOWN));
        assertTrue(Direction.DOWN.isOpposite(Direction.UP));
        assertTrue(Direction.LEFT.isOpposite(Direction.RIGHT));
        assertTrue(Direction.RIGHT.isOpposite(Direction.LEFT));
    }

    @Test
    void nonOppositesAreNot() {
        assertFalse(Direction.UP.isOpposite(Direction.UP));
        assertFalse(Direction.UP.isOpposite(Direction.LEFT));
        assertFalse(Direction.RIGHT.isOpposite(Direction.UP));
        assertFalse(Direction.RIGHT.isOpposite(null));
    }

    @Test
    void cellStepsByDirectionDelta() {
        Cell origin = new Cell(5, 5);
        assertTrue(origin.step(Direction.UP).equals(new Cell(5, 4)));
        assertTrue(origin.step(Direction.DOWN).equals(new Cell(5, 6)));
        assertTrue(origin.step(Direction.LEFT).equals(new Cell(4, 5)));
        assertTrue(origin.step(Direction.RIGHT).equals(new Cell(6, 5)));
    }
}
