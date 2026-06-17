package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CellTest {

    @Test
    void translateMovesAlongTheExpectedAxis() {
        Cell origin = new Cell(5, 5);

        assertEquals(new Cell(5, 4), origin.translate(Direction.UP));
        assertEquals(new Cell(5, 6), origin.translate(Direction.DOWN));
        assertEquals(new Cell(4, 5), origin.translate(Direction.LEFT));
        assertEquals(new Cell(6, 5), origin.translate(Direction.RIGHT));
    }

    @Test
    void translateDoesNotMutateOriginal() {
        Cell origin = new Cell(2, 3);
        origin.translate(Direction.UP);

        assertEquals(new Cell(2, 3), origin);
    }
}
