package com.pawel.snake3d;

/** Lifecycle states of a {@link SnakeGame}. */
public enum GameState {
    /** Fresh game, waiting for the player to start. */
    READY,
    /** Game is being played. */
    RUNNING,
    /** Temporarily halted by the player. */
    PAUSED,
    /** Snake crashed into a wall or itself. */
    GAME_OVER,
    /** Snake filled the entire board — the player won. */
    WON;

    public boolean isFinished() {
        return this == GAME_OVER || this == WON;
    }
}
