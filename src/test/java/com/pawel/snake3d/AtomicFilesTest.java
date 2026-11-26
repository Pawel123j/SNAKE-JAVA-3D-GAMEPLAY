package com.pawel.snake3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AtomicFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void failedStagingKeepsHighScoreAndAllowsRetry() throws IOException {
        assumeTrue(Files.getFileStore(tempDir).supportsFileAttributeView(PosixFileAttributeView.class));
        Path file = tempDir.resolve("highscore.txt");
        HighScoreStore store = new HighScoreStore(file);
        store.save(10);
        assertEquals("10", Files.readString(file));

        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(tempDir);
        try {
            // Keep the record writable so an in-place overwrite would be detected.
            Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("r-x------"));
            assumeFalse(Files.isWritable(tempDir), "Process can bypass directory permissions");
            store.save(50);
            assertEquals("10", Files.readString(file));
            assertEquals(10, new HighScoreStore(file).load());
            try (Stream<Path> files = Files.list(tempDir)) {
                assertEquals(List.of(file), files.toList());
            }
        } finally {
            Files.setPosixFilePermissions(tempDir, originalPermissions);
        }

        store.save(50);
        assertEquals("50", Files.readString(file));
        assertEquals(50, new HighScoreStore(file).load());
    }

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
