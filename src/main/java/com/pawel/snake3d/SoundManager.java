package com.pawel.snake3d;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays short, synthesized sound effects. No audio assets are bundled — every
 * effect is generated as raw PCM at construction time. If no audio device is
 * available (e.g. on a headless machine) sound silently disables itself so the
 * game keeps running.
 */
public class SoundManager {

    private static final float SAMPLE_RATE = 44_100f;
    private final AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "snake-sound");
        thread.setDaemon(true);
        return thread;
    });

    private final byte[] eatClip;
    private final byte[] bonusClip;
    private final byte[] gameOverClip;
    private final byte[] winClip;

    private volatile boolean enabled = true;
    private volatile boolean available;

    public SoundManager() {
        eatClip = tone(new double[]{660, 990}, 0.10, 0.35);
        bonusClip = tone(new double[]{880, 1320, 1760}, 0.18, 0.32);
        gameOverClip = tone(new double[]{392, 311, 233}, 0.36, 0.40);
        winClip = tone(new double[]{523, 659, 784, 1047}, 0.44, 0.34);
        available = probe();
    }

    private boolean probe() {
        try {
            return AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Builds a mono 16-bit PCM clip that plays each frequency in sequence. */
    private byte[] tone(double[] frequencies, double totalSeconds, double gain) {
        int perNote = (int) (SAMPLE_RATE * totalSeconds / frequencies.length);
        int totalSamples = perNote * frequencies.length;
        byte[] data = new byte[totalSamples * 2];
        double fade = SAMPLE_RATE * 0.012; // ~12 ms attack/decay to avoid clicks

        int idx = 0;
        for (double freq : frequencies) {
            for (int i = 0; i < perNote; i++) {
                double envelope = Math.max(0, Math.min(1, Math.min(i, perNote - i) / fade));
                double sample = Math.sin(2 * Math.PI * freq * (i / SAMPLE_RATE)) * gain * envelope;
                short value = (short) (sample * Short.MAX_VALUE);
                data[idx++] = (byte) (value & 0xff);
                data[idx++] = (byte) ((value >> 8) & 0xff);
            }
        }
        return data;
    }

    public void eat() {
        play(eatClip);
    }

    public void bonus() {
        play(bonusClip);
    }

    public void gameOver() {
        play(gameOverClip);
    }

    public void win() {
        play(winClip);
    }

    private void play(byte[] data) {
        if (!enabled || !available) {
            return;
        }
        executor.submit(() -> {
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
                line.stop();
            } catch (Exception e) {
                available = false; // give up quietly after the first failure
            }
        });
    }

    /** @return {@code true} if sound is now on. */
    public boolean toggleMute() {
        enabled = !enabled;
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled && available;
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
