package com.pawel.snake3d;

/** Board rules for a {@link SnakeGame}. */
public enum GameMode {
    /** Walls are deadly — the classic game. */
    CLASSIC("KLASYCZNY"),
    /** Edges wrap around: leaving one side re-enters from the opposite one. */
    WRAP("ZAWIJANIE"),
    /** Walls are deadly and the arena is sprinkled with static obstacles. */
    OBSTACLES("PRZESZKODY");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** @return the next mode, wrapping around to {@link #CLASSIC}. */
    public GameMode next() {
        GameMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
