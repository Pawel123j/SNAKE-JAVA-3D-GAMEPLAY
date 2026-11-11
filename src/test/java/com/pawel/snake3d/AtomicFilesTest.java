package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void failureBeforeReplaceKeepsPreviousFileAndCleansTemporaryFile() throws IOException {
        Path destination = tempDir.resolve("scores.txt");
        Files.writeString(destination, "previous complete scores");

        assertThrows(IOException.class, () -> AtomicFiles.write(
                destination,
                "partial replacement".getBytes(StandardCharsets.UTF_8),
                () -> { throw new IOException("injected failure before commit"); }
        ));

        assertEquals("previous complete scores", Files.readString(destination));
        try (Stream<Path> files = Files.list(tempDir)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
