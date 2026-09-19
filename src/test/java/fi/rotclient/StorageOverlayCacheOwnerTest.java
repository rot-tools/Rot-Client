package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StorageOverlayCacheOwnerTest {
    private static final String UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    @Test
    void ownerKeyCombinesAccountAndProfile() {
        assertEquals(
                "069a79f444e94726a5befca90e38aaf5_banana",
                StorageOverlayPolicy.ownerKey(UUID, "banana"));
        assertEquals(
                "069a79f444e94726a5befca90e38aaf5_banana",
                StorageOverlayPolicy.ownerKey(UUID.toUpperCase(), "  Banana "));
        // Two profiles of one account must never share a file.
        assertEquals(false, StorageOverlayPolicy.ownerKey(UUID, "banana")
                .equals(StorageOverlayPolicy.ownerKey(UUID, "apple")));
    }

    @Test
    void ownerKeyIsEmptyUntilBothPartsAreKnown() {
        assertEquals("", StorageOverlayPolicy.ownerKey(UUID, SkyBlockProfileIdentity.UNKNOWN));
        assertEquals("", StorageOverlayPolicy.ownerKey(UUID, ""));
        assertEquals("", StorageOverlayPolicy.ownerKey(null, "banana"));
        assertEquals("", StorageOverlayPolicy.ownerKey("not-a-uuid", "banana"));
    }

    @Test
    void ownerKeyIsSafeAsAFileName() {
        String key = StorageOverlayPolicy.ownerKey(UUID, "../../etc/pass wd");
        assertEquals(false, key.contains("/") || key.contains("\\") || key.contains(".."));
        assertEquals(true, key.length() <= 32 + 1 + 32);
    }

    @Test
    void ageLabelsAreShortAndCapped() {
        long now = 100_000_000_000L;
        assertEquals("", StorageOverlayPolicy.ageLabel(0L, now));
        assertEquals("now", StorageOverlayPolicy.ageLabel(now - 30_000L, now));
        assertEquals("5m", StorageOverlayPolicy.ageLabel(now - 5 * 60_000L, now));
        assertEquals("3h", StorageOverlayPolicy.ageLabel(now - 3 * 3_600_000L, now));
        assertEquals("2d", StorageOverlayPolicy.ageLabel(now - 2 * 86_400_000L, now));
        assertEquals("99d", StorageOverlayPolicy.ageLabel(now - 500L * 86_400_000L, now));
        // A clock that went backwards must not produce a negative age.
        assertEquals("now", StorageOverlayPolicy.ageLabel(now + 5_000L, now));
    }

    @Test
    void neverOpenedPagesSaySoAndTheOpenPageIsLive() {
        long now = 100_000_000_000L;
        assertEquals("", StorageOverlayPolicy.freshnessLabel(true, true, now - 9_999L, now));
        assertEquals("not opened", StorageOverlayPolicy.freshnessLabel(false, false, 0L, now));
        // An older cache with items but no timestamp stays quiet rather than guessing.
        assertEquals("", StorageOverlayPolicy.freshnessLabel(false, true, 0L, now));
        assertEquals("4h", StorageOverlayPolicy.freshnessLabel(false, true, now - 4 * 3_600_000L, now));
    }

    @Test
    void runtimeKeepsEachOwnersPagesInTheirOwnFile() throws Exception {
        String runtime = Files.readString(
                Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"),
                StandardCharsets.UTF_8);
        // The owner is synced before any load, so pages never load into the wrong profile.
        int load = runtime.indexOf("private static void loadCache()");
        int sync = runtime.indexOf("syncOwner();", load);
        int read = runtime.indexOf("Path path = cachePath();", load);
        assertEquals(true, load >= 0 && sync > load && read > sync);
        // An async write goes to the file of the owner it was built for, not whoever owns the cache
        // by the time the IO thread runs.
        assertEquals(true, runtime.contains("Path target = cachePath();"));
        assertEquals(true, runtime.contains("AtomicFileWriter.writeAtomically(target, json);"));
        // The old owner is saved to its own file before a switch, and the legacy file is migrated once.
        assertEquals(true, runtime.contains("saveOwnerNow(activeOwner);"));
        assertEquals(true, runtime.contains("migrateLegacyCacheTo(desired);"));
        // The automatic page walk is still Plus-only.
        assertEquals(true, runtime.contains("!QolFlavorSupport.isPlus()"));
    }
}
