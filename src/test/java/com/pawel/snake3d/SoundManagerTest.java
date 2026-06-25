package com.pawel.snake3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundManagerTest {

    @Test
    void sweepProducesMono16BitBufferOfExpectedLength() {
        byte[] buffer = SoundManager.sweep(440, 880, 100);

        // 44.1 kHz * 0.1 s = 4410 samples, 2 bytes each.
        assertEquals(4410 * 2, buffer.length);
        assertEquals(0, buffer.length % 2, "16-bit samples must align to 2 bytes");
    }

    @Test
    void sweepStartsAndEndsNearSilenceFromTheEnvelope() {
        byte[] buffer = SoundManager.sweep(440, 880, 100);

        assertEquals(0, buffer[0]);
        assertEquals(0, buffer[1]);
    }

    @Test
    void managerStartsUnmutedAndTogglesCleanly() {
        SoundManager sound = new SoundManager();
        try {
            assertFalse(sound.isMuted());
            assertTrue(sound.toggleMute());
            assertTrue(sound.isMuted());
            assertFalse(sound.toggleMute());
        } finally {
            sound.shutdown();
        }
    }

    @Test
    void playingNeverThrowsEvenWithoutAnAudioDevice() {
        SoundManager sound = new SoundManager();
        try {
            sound.playStart();
            sound.playEat();
            sound.playGameOver();
        } finally {
            sound.shutdown();
        }
    }
}
