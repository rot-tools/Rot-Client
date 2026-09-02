package fi.rotclient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes a complete payload to a sibling temporary file, flushes and forces
 * durable storage, then replaces the destination. Never truncates the
 * destination before the replacement payload is ready.
 */
final class AtomicFileWriter {
    private AtomicFileWriter() {
    }

    static void writeAtomically(Path target, String contents)
            throws IOException {
        if (target == null) {
            throw new IllegalArgumentException("Target path cannot be null");
        }
        if (contents == null) {
            throw new IllegalArgumentException("Contents cannot be null");
        }

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path directory = parent != null ? parent : Path.of(".");
        Path temp = Files.createTempFile(
                directory,
                target.getFileName().toString() + ".",
                ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    temp,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(
                                    Channels.newOutputStream(channel),
                                    StandardCharsets.UTF_8))) {
                writer.write(contents);
                writer.flush();
                channel.force(true);
            }

            try {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            temp = null;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
