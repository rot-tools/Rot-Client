package fi.rotclient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shared runtime cache for HUD calculations.
 *
 * Rendering happens every frame, but most HUD information changes
 * much less frequently. This cache stores calculated HUD data so
 * render code only draws already prepared information.
 */
final class HudRuntimeCache {

    private static final long DEFAULT_REFRESH_MS = 50L;

    private static final Map<String, Entry> CACHE =
            new ConcurrentHashMap<>();

    private HudRuntimeCache() {
    }

    static List<String> get(
            String key,
            Supplier<List<String>> supplier) {

        return get(
                key,
                DEFAULT_REFRESH_MS,
                supplier);
    }

    static List<String> get(
            String key,
            long refreshMs,
            Supplier<List<String>> supplier) {

        long now = System.currentTimeMillis();

        Entry cached = CACHE.get(key);

        if (cached != null
                && now - cached.time < refreshMs) {
            return cached.lines;
        }

        List<String> result =
                supplier == null
                        ? List.of()
                        : supplier.get();

        if (result == null) {
            result = List.of();
        }

        result = List.copyOf(result);

        CACHE.put(
                key,
                new Entry(
                        now,
                        result));

        return result;
    }


    static void invalidate(String key) {
        if (key != null) {
            CACHE.remove(key);
        }
    }


    static void clear() {
        CACHE.clear();
    }


    private record Entry(
            long time,
            List<String> lines) {
    }
}
