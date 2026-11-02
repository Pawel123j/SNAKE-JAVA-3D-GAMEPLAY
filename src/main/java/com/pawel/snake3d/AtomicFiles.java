package com.pawel.snake3d;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Same-directory durable staging. If atomic replacement is unsupported, keep the old file. */
final class AtomicFiles {
    private AtomicFiles() { }
    static void write(Path destination, byte[] bytes) throws IOException {
        write(destination, bytes, () -> { });
    }
    // Failure injection at the exact pre-commit boundary for regression tests.
    static void write(Path destination, byte[] bytes, BeforeCommit beforeCommit) throws IOException {
        Path target = destination.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".snake-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            beforeCommit.run();
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
    @FunctionalInterface interface BeforeCommit { void run() throws IOException; }
}
