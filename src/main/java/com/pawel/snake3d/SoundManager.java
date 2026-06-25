package com.pawel.snake3d;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight sound effects synthesised at runtime with {@code javax.sound.sampled}.
 *
 * <p>Tones are generated on the fly, so the project needs no audio assets and no
 * extra JavaFX modules. The manager disables itself gracefully the first time a
 * sound cannot be played — for example on a headless CI machine with no audio
 * device — so gameplay is never affected by the absence of sound.
 *
 * <p>Playback happens on a single daemon thread to keep the JavaFX thread free.
 */
public final class SoundManager {

    private static final float SAMPLE_RATE = 44_100f;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final ExecutorService player = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "snake-sound");
        thread.setDaemon(true);
        return thread;
    });

    private final byte[] eatSound = sweep(660, 990, 90);
    private final byte[] startSound = sweep(440, 880, 160);
    private final byte[] gameOverSound = sweep(440, 120, 420);

    private volatile boolean available = true;
    private volatile boolean muted = false;

    /** Plays the "food eaten" blip. */
    public void playEat() {
        play(eatSound);
    }

    /** Plays the "new game" chime. */
    public void playStart() {
        play(startSound);
    }

    /** Plays the descending "game over" tone. */
    public void playGameOver() {
        play(gameOverSound);
    }

    /** @return {@code true} when sound is currently muted by the player */
    public boolean isMuted() {
        return muted;
    }

    /** Toggles muting and returns the new state. */
    public boolean toggleMute() {
        muted = !muted;
        return muted;
    }

    /** Stops the playback thread; call on application shutdown. */
    public void shutdown() {
        player.shutdownNow();
    }

    private void play(byte[] pcm) {
        if (muted || !available) {
            return;
        }
        try {
            player.submit(() -> render(pcm));
        } catch (RuntimeException ignored) {
            // Executor already shut down — nothing to play.
        }
    }

    private void render(byte[] pcm) {
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception e) {
            // No usable audio device: disable further attempts silently.
            available = false;
        } finally {
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                } catch (Exception ignored) {
                    // Best-effort cleanup.
                }
            }
        }
    }

    /**
     * Synthesises a mono, 16-bit, little-endian PCM tone that linearly sweeps
     * between two frequencies, with a short fade in/out envelope to avoid clicks.
     * Visible for testing.
     *
     * @param fromHz starting frequency in hertz
     * @param toHz   ending frequency in hertz
     * @param millis duration in milliseconds
     * @return the raw PCM buffer in {@link #FORMAT}
     */
    static byte[] sweep(double fromHz, double toHz, int millis) {
        int samples = (int) (SAMPLE_RATE * millis / 1000.0);
        byte[] out = new byte[samples * 2];
        double phase = 0;
        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double freq = fromHz + (toHz - fromHz) * progress;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            double envelope = Math.min(1.0, Math.min(progress * 12, (1 - progress) * 12));
            short value = (short) (Math.sin(phase) * envelope * 0.35 * Short.MAX_VALUE);
            out[i * 2] = (byte) (value & 0xff);
            out[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }
        return out;
    }
}
