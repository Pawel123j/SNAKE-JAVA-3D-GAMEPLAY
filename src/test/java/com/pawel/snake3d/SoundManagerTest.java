package com.pawel.snake3d;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SoundManagerTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cuesAndMuteToggleWorkWithoutAnOutputLine(boolean requested) {
        AudioFormat format = new AudioFormat(44_100, 16, 1, true, false);
        assumeFalse(AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format)),
                "Requires an unavailable PCM output");

        SoundManager sound = new SoundManager();
        try {
            sound.setEnabled(requested);
            assertFalse(sound.isEnabled());
            sound.eat();
            sound.bonus();
            sound.gameOver();
            sound.win();

            assertEquals(!requested, sound.toggleMute());
            assertFalse(sound.isEnabled());
        } finally {
            sound.shutdown();
        }
    }
}
