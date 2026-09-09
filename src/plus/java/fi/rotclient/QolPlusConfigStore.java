package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Plus settings live in {@code rotclient-plus.json} so toggling JARs in one
 * Prism instance does not depend on legit rewriting {@code rotclient.json}.
 * First Plus launch copies the existing shared config once.
 */
public final class QolPlusConfigStore {
    private QolPlusConfigStore() {
    }

    static Path plusPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rotclient-plus.json");
    }

    static Path sharedPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("rotclient.json");
    }

    static void migrateFromSharedIfNeeded() {
        try {
            Path plus = plusPath();
            Path shared = sharedPath();
            if (!Files.exists(plus) && Files.exists(shared)) {
                Files.copy(shared, plus);
            }
        } catch (Exception ignored) {
        }
    }

    static void copySharedSnapshot() {
        try {
            Path plus = plusPath();
            Path shared = sharedPath();
            if (Files.exists(shared)) {
                Files.copy(shared, plus, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }
}
