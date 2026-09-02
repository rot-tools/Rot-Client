package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ClientBoundaryGuardTest {
    @BeforeEach
    void reset() {
        ClientBoundaryGuard.resetFailureCountForTests();
    }

    @Test
    void runtimeExceptionIsContainedAndCounted() {
        long before = ClientBoundaryGuard.failureCount();
        ClientBoundaryGuard.run("HUD_RENDER", () -> {
            throw new IllegalArgumentException("Unit price scale exceeds maximum");
        });
        assertEquals(before + 1L, ClientBoundaryGuard.failureCount());
    }

    @Test
    void successfulActionDoesNotCountFailure() {
        AtomicBoolean ran = new AtomicBoolean(false);
        ClientBoundaryGuard.run("AREA_DETECT", () -> ran.set(true));
        assertTrue(ran.get());
        assertEquals(0L, ClientBoundaryGuard.failureCount());
    }

    @Test
    void linkageErrorIsContainedLikeRuntimeFailure() {
        long before = ClientBoundaryGuard.failureCount();
        ClientBoundaryGuard.run("INVENTORY_OVERLAY", () -> {
            throw new NoClassDefFoundError("simulated corrupt jar");
        });
        assertEquals(before + 1L, ClientBoundaryGuard.failureCount());
    }

    @Test
    void errorIsNotSwallowed() {
        assertThrows(OutOfMemoryError.class, () ->
                ClientBoundaryGuard.run("TICK", () -> {
                    throw new OutOfMemoryError("simulated");
                }));
        assertEquals(0L, ClientBoundaryGuard.failureCount());
    }

    @Test
    void callReturnsFallbackOnRuntimeFailure() {
        AtomicInteger sideEffect = new AtomicInteger();
        Integer value = ClientBoundaryGuard.call(
                "BAZAAR_APPLY",
                () -> {
                    sideEffect.incrementAndGet();
                    throw new IllegalStateException("bad price object");
                },
                42);
        assertEquals(42, value);
        assertEquals(1, sideEffect.get());
        assertEquals(1L, ClientBoundaryGuard.failureCount());
    }

    @Test
    void sanitizeBoundaryBoundsAndDefaults() {
        assertEquals("UNKNOWN", ClientBoundaryGuard.sanitizeBoundary(null));
        assertEquals("UNKNOWN", ClientBoundaryGuard.sanitizeBoundary("  "));
        assertEquals("HUD_RENDER", ClientBoundaryGuard.sanitizeBoundary("HUD_RENDER"));
        assertEquals(
                64,
                ClientBoundaryGuard.sanitizeBoundary("X".repeat(80)).length());
    }

    @Test
    void safeExceptionLabelUsesSimpleName() {
        assertEquals(
                "IllegalArgumentException",
                ClientBoundaryGuard.safeExceptionLabel(
                        new IllegalArgumentException("x")));
        assertEquals("unknown", ClientBoundaryGuard.safeExceptionLabel(null));
    }

    @Test
    void productionBoundariesNeverMaskThrowable() throws IOException {
        try (Stream<Path> sources = Files.walk(Path.of("src"))) {
            sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("test"))
                    .forEach(path -> {
                        String source;
                        try {
                            source = Files.readString(
                                    path, StandardCharsets.UTF_8);
                        } catch (IOException failure) {
                            throw new AssertionError(
                                    "Unable to read " + path, failure);
                        }
                        assertTrue(
                                !source.contains("catch (Throwable")
                                        && !source.contains("catch(Throwable"),
                                () -> "Broad Throwable catch in " + path);
                    });
        }
    }

    @Test
    void diagnosticTraceDoesNotInitializeTheWholeClientForStatusReads()
            throws IOException {
        String source = Files.readString(
                Path.of(
                        "src/client/java/fi/rotclient/TrackingRuntimeTrace.java"),
                StandardCharsets.UTF_8);
        assertTrue(
                !source.contains("RotClientClient."),
                "Pure diagnostic helpers must use an installed state provider");
    }
}
