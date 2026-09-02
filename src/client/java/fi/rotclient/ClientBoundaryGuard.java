package fi.rotclient;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Narrow client-thread containment for Rot Client boundaries that ingest
 * external Minecraft / Hypixel runtime data.
 *
 * <p>Catches {@link RuntimeException} and {@link LinkageError} (corrupt JAR
 * classload, missing mixin target) — never {@link Throwable} or
 * {@link OutOfMemoryError}, so programmer failures still surface. Does not
 * mutate accounting state.
 */
public final class ClientBoundaryGuard {
    private static final AtomicLong FAILURES = new AtomicLong();

    private ClientBoundaryGuard() {
    }

    public static void run(String boundary, Runnable action) {
        Objects.requireNonNull(action, "action");
        try {
            action.run();
        } catch (RuntimeException failure) {
            recordFailure(boundary, failure);
        } catch (LinkageError failure) {
            recordFailure(boundary, failure);
        }
    }

    public static <T> T call(String boundary, java.util.concurrent.Callable<T> action, T fallback) {
        Objects.requireNonNull(action, "action");
        try {
            return action.call();
        } catch (RuntimeException failure) {
            recordFailure(boundary, failure);
            return fallback;
        } catch (LinkageError failure) {
            recordFailure(boundary, failure);
            return fallback;
        } catch (Exception failure) {
            // Checked exceptions from Callable are treated like boundary I/O.
            recordFailure(boundary, failure);
            return fallback;
        }
    }

    static long failureCount() {
        return FAILURES.get();
    }

    static void resetFailureCountForTests() {
        FAILURES.set(0L);
    }

    private static void recordFailure(String boundary, Throwable failure) {
        FAILURES.incrementAndGet();
        String safeBoundary = sanitizeBoundary(boundary);
        String reason = safeExceptionLabel(failure);
        DiagnosticRecorder.record(
                "CLIENT_BOUNDARY_FAILED",
                "BOUNDARY=" + safeBoundary + " REASON=" + reason + " ACTION=SKIPPED");
        TrackingRuntimeTrace.event(
                "CLIENT_BOUNDARY_FAILED",
                Map.of(
                        "boundary", safeBoundary,
                        "reason", reason,
                        "action", "SKIPPED"));
    }

    static String sanitizeBoundary(String boundary) {
        if (boundary == null || boundary.isBlank()) {
            return "UNKNOWN";
        }
        String trimmed = boundary.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    static String safeExceptionLabel(Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        String name = failure.getClass().getSimpleName();
        return name == null || name.isBlank() ? "RuntimeException" : name;
    }
}
