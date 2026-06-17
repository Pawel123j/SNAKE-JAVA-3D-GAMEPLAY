package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionTest {

    @Test
    void oppositePairsAreSymmetric() {
        assertEquals(Direction.DOWN, Direction.UP.opposite());
        assertEquals(Direction.UP, Direction.DOWN.opposite());
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite());
    }

    @Test
    void isOppositeDetectsReverseDirections() {
        assertTrue(Direction.UP.isOpposite(Direction.DOWN));
        assertTrue(Direction.LEFT.isOpposite(Direction.RIGHT));
    }

    @Test
    void isOppositeIsFalseForPerpendicularOrSameDirection() {
        assertFalse(Direction.UP.isOpposite(Direction.LEFT));
        assertFalse(Direction.UP.isOpposite(Direction.UP));
    }

    @Test
    void isOppositeHandlesNull() {
        assertFalse(Direction.UP.isOpposite(null));
    }
}
