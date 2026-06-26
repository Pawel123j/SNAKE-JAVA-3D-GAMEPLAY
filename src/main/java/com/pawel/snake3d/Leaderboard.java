package com.pawel.snake3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the best handful of scores, persisted one-per-line under the user's
 * home directory. Like the other stores it degrades gracefully on any I/O or
 * parsing error (treated as an empty board).
 */
public class Leaderboard {

    /** How many scores are kept. */
    public static final int CAPACITY = 5;

    private final Path file;
    private final List<Integer> scores = new ArrayList<>();

    public Leaderboard() {
        this(defaultPath());
    }

    public Leaderboard(Path file) {
        this.file = file;
        load();
    }

    private static Path defaultPath() {
        return Paths.get(System.getProperty("user.home", "."), ".snake3d", "scores.txt");
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    scores.add(Integer.parseInt(trimmed));
                }
            }
        } catch (IOException | NumberFormatException e) {
            scores.clear();
            return;
        }
        trim();
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> lines = new ArrayList<>(scores.size());
            for (int score : scores) {
                lines.add(Integer.toString(score));
            }
            Files.write(file, lines);
        } catch (IOException e) {
            // best-effort persistence
        }
    }

    private void trim() {
        scores.sort(Collections.reverseOrder());
        while (scores.size() > CAPACITY) {
            scores.remove(scores.size() - 1);
        }
    }

    /**
     * Records a score.
     *
     * @return {@code true} if it earned a place on the board.
     */
    public boolean submit(int score) {
        if (score <= 0) {
            return false;
        }
        boolean qualifies = scores.size() < CAPACITY || score > scores.get(scores.size() - 1);
        scores.add(score);
        trim();
        save();
        return qualifies && scores.contains(score);
    }

    /** @return the scores, highest first (unmodifiable). */
    public List<Integer> top() {
        return Collections.unmodifiableList(new ArrayList<>(scores));
    }

    public int best() {
        return scores.isEmpty() ? 0 : scores.get(0);
    }
}
