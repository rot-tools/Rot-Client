package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

final class TrackerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object BACKUP_LOCK = new Object();
    private static boolean backupTakenForSession;

    private TrackerStore() {
    }

    static TrackerConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) return normalizedDefault();
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root == null || !root.isJsonObject()) {
                return normalizedDefault();
            }
            JsonObject object = root.getAsJsonObject();
            int sourceVersion = intValue(object, "dataVersion", 0);
            if (sourceVersion < TrackerConfig.CURRENT_DATA_VERSION) {
                maybeBackupOnce(path);
            }
            return fromJson(object);
        } catch (Exception ignored) {
            return normalizedDefault();
        }
    }

    static void save(TrackerConfig config) {
        if (config == null
                || config.dataVersion > TrackerConfig.CURRENT_DATA_VERSION) {
            // A newer Rot Client may own fields this build does not understand.
            // Keep the loaded file intact instead of silently downgrading it.
            return;
        }
        try {
            Path path = configPath();
            AtomicFileWriter.writeAtomically(path, GSON.toJson(toJson(config)));
        } catch (Exception ignored) {
        }
    }

    /**
     * Testable deserialization boundary.
     *
     * Versions before v5 stored one flat Gold ledger. Version 5 moves that
     * ledger under {@code materialStates.GOLD}.
     *
     * Version 7 replaces the incorrect Mithril + Tungsten target selection
     * with Mithril + Titanium. Existing Tungsten ledger data remains separate
     * and is never copied into the new Titanium ledger.
     *
     * Version 10 enables Tungsten as its own target. A pre-v10 raw TUNGSTEN
     * selection still means the former compatibility alias and migrates to
     * Mithril + Titanium; the existing Tungsten ledger remains untouched.
     */
    static TrackerConfig fromJson(JsonObject root) {
        if (root == null) return normalizedDefault();

        int sourceVersion = intValue(root, "dataVersion", 0);
        TrackerConfig config;
        try {
            config = GSON.fromJson(root, TrackerConfig.class);
        } catch (Exception ignored) {
            config = new TrackerConfig();
        }
        if (config == null) config = new TrackerConfig();
        if (sourceVersion < 5) {
            MaterialTrackerState legacyGold =
                    legacyGoldState(root);

            Map<String, MaterialTrackerState> states =
                    config.states();

            states.clear();
            states.put(
                    TrackedMaterial.GOLD.id(),
                    legacyGold);

            for (TrackedMaterial material :
                    TrackedMaterial.values()) {
                states.putIfAbsent(
                        material.id(),
                        new MaterialTrackerState());
            }
        }
        if (sourceVersion < 7) {
            String legacyTargetId = stringValue(
                    root,
                    "selectedTargetId",
                    stringValue(
                            root,
                            "selectedMaterialId",
                            config.selectedTargetId));

            if ("MITHRIL_TUNGSTEN".equalsIgnoreCase(legacyTargetId)
                    || "MITHRIL".equalsIgnoreCase(legacyTargetId)
                    || "TITANIUM".equalsIgnoreCase(legacyTargetId)
                    || "TUNGSTEN".equalsIgnoreCase(legacyTargetId)) {
                config.selectedTargetId =
                        TrackingTarget.MITHRIL_TITANIUM.id();
            }

            /*
             * Calling states() creates a new empty Titanium ledger when it is
             * missing. Existing Mithril and Tungsten ledgers remain separate.
             */
            Map<String, MaterialTrackerState> states = config.states();
            states.putIfAbsent(
                    TrackedMaterial.TITANIUM.id(),
                    new MaterialTrackerState());
        }
        if (sourceVersion < 9) {
            if (config.qolUtilities == null) {
                config.qolUtilities = new QolUtilityConfig();
            } else {
                config.qolUtilities.migrateLegacyDefaultPalette();
            }
        }
        if (sourceVersion >= 7 && sourceVersion < 10) {
            String legacyTargetId = stringValue(
                    root,
                    "selectedTargetId",
                    stringValue(
                            root,
                            "selectedMaterialId",
                            config.selectedTargetId));
            if ("TUNGSTEN".equalsIgnoreCase(legacyTargetId)) {
                config.selectedTargetId =
                        TrackingTarget.MITHRIL_TITANIUM.id();
            }
        }

        restorePlayableSecretHitboxDefaults(config, root);
        restorePlayableInventoryOverlayDefaults(config, root);

        config.normalize();
        if (sourceVersion > TrackerConfig.CURRENT_DATA_VERSION) {
            config.dataVersion = sourceVersion;
        }
        return config;
    }

    /**
     * Gson writes missing primitive booleans as {@code false}, which would
     * wipe the playable Secret Hitboxes defaults (lever/button/skull on).
     * Restore those defaults only when the key was absent.
     */
    private static void restorePlayableSecretHitboxDefaults(
            TrackerConfig config, JsonObject root) {
        if (config.qolUtilities == null) {
            config.qolUtilities = new QolUtilityConfig();
        }
        JsonObject qolJson = null;
        if (root.has("qolUtilities") && root.get("qolUtilities").isJsonObject()) {
            qolJson = root.getAsJsonObject("qolUtilities");
        }
        QolUtilityConfig qol = config.qolUtilities;
        if (qolJson == null || !qolJson.has("secretHitboxesLever")) {
            qol.secretHitboxesLever = true;
        }
        if (qolJson == null || !qolJson.has("secretHitboxesButton")) {
            qol.secretHitboxesButton = true;
        }
        if (qolJson == null || !qolJson.has("secretHitboxesSkull")) {
            qol.secretHitboxesSkull = true;
        }
    }

    /**
     * Existing saves have no inventory-overlay keys. Gson would leave the new
     * booleans false; restore the playable on-by-default set when absent.
     */
    private static void restorePlayableInventoryOverlayDefaults(
            TrackerConfig config, JsonObject root) {
        if (config.qolUtilities == null) {
            config.qolUtilities = new QolUtilityConfig();
        }
        JsonObject qolJson = null;
        if (root.has("qolUtilities") && root.get("qolUtilities").isJsonObject()) {
            qolJson = root.getAsJsonObject("qolUtilities");
        }
        QolUtilityConfig qol = config.qolUtilities;
        if (qolJson == null || !qolJson.has("inventoryOverlayEnabled")) {
            qol.inventoryOverlayEnabled = true;
        }
        if (qolJson == null || !qolJson.has("inventoryOverlayEquipment")) {
            qol.inventoryOverlayEquipment = true;
        }
        if (qolJson == null || !qolJson.has("inventoryOverlayHideRecipeBook")) {
            qol.inventoryOverlayHideRecipeBook = true;
        }
        if (qolJson == null || !qolJson.has("inventoryOverlayHideStatusEffects")) {
            qol.inventoryOverlayHideStatusEffects = true;
        }
        if (qolJson == null || !qolJson.has("inventoryOverlayPetSlot")) {
            qol.inventoryOverlayPetSlot = true;
        }
        if (qolJson == null || !qolJson.has("skillLevelsEnabled")) {
            qol.skillLevelsEnabled = true;
        }
        if (qolJson == null || !qolJson.has("skillLevelsBackground")) {
            qol.skillLevelsBackground = true;
        }
        if (qolJson == null || !qolJson.has("petHudEnabled")) {
            qol.petHudEnabled = true;
        }
    }

    /**
     * Serializes only the current schema. The old flat Gold fields no longer
     * exist on {@link TrackerConfig}, so they cannot leak back into the file.
     */
    static JsonObject toJson(TrackerConfig config) {
        TrackerConfig safe = config == null ? normalizedDefault() : config;
        int sourceVersion = safe.dataVersion;
        safe.normalize();
        if (sourceVersion > TrackerConfig.CURRENT_DATA_VERSION) {
            safe.dataVersion = sourceVersion;
        }
        return GSON.toJsonTree(safe).getAsJsonObject();
    }

    private static TrackerConfig normalizedDefault() {
        TrackerConfig config = new TrackerConfig();
        config.normalize();
        return config;
    }

    private static Path configPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("rotclient.json");
    }

    private static void maybeBackupOnce(Path path) {
        synchronized (BACKUP_LOCK) {
            if (backupTakenForSession) {
                return;
            }
            backupTakenForSession = true;
            try {
                Files.copy(
                        path,
                        path.resolveSibling(path.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
    }

    private static MaterialTrackerState legacyGoldState(JsonObject root) {
        MaterialTrackerState state = new MaterialTrackerState();
        state.totalBlocks = longValue(root, "totalBlocks", 0);
        state.totalBaseDrops = safeMultiply(state.totalBlocks, 5);
        state.totalActiveMillis = longValue(root, "totalActiveMillis", 0);
        state.sessionBlocks = longValue(root, "sessionBlocks", 0);
        state.sessionBaseDrops = safeMultiply(state.sessionBlocks, 5);
        state.sessionActiveMillis = longValue(root, "sessionActiveMillis", 0);
        state.lastBreakEpochMillis = longValue(root, "lastBreakEpochMillis", 0);
        state.lastRawPrice = doubleValue(root, "lastGoldPrice", 0);
        state.lastRawSellOfferPrice =
                doubleValue(root, "lastGoldSellOfferPrice", 0);
        state.lastEnchantedPrice =
                doubleValue(root, "lastEnchantedGoldPrice", 0);
        state.lastPriceUpdateEpochMillis =
                longValue(root, "lastPriceUpdateEpochMillis", 0);
        state.sessionActualRawEquivalent =
                longValue(root, "sessionActualGold", 0);
        state.totalActualRawEquivalent =
                longValue(root, "totalActualGold", 0);
        state.sessionInventoryRaw =
                longValue(root, "sessionInventoryRawGold", 0);
        state.totalInventoryRaw =
                longValue(root, "totalInventoryRawGold", 0);
        state.sessionCompactBonusEnchanted =
                longValue(root, "sessionCompactBonusEnchanted", 0);
        state.totalCompactBonusEnchanted =
                longValue(root, "totalCompactBonusEnchanted", 0);
        state.sessionSackRaw = longValue(root, "sessionSackRawGold", 0);
        state.totalSackRaw = longValue(root, "totalSackRawGold", 0);
        state.sessionSackEnchanted =
                longValue(root, "sessionSackEnchantedGold", 0);
        state.totalSackEnchanted =
                longValue(root, "totalSackEnchantedGold", 0);
        state.sessionCompactedEnchanted =
                longValue(root, "sessionCompactedGold", 0);
        state.totalCompactedEnchanted =
                longValue(root, "totalCompactedGold", 0);
        state.sessionSoldRaw = longValue(root, "sessionSoldRawGold", 0);
        state.sessionSoldEnchanted =
                longValue(root, "sessionSoldEnchantedGold", 0);
        state.sessionRealizedGrossCoins =
                doubleValue(root, "sessionRealizedGrossCoins", 0);
        state.compactorRawRemainder =
                longValue(root, "compactorRawRemainder", 0);
        state.lastActualRawEquivalentEpochMillis =
                longValue(root, "lastActualGoldEpochMillis", 0);
        state.actualRawEquivalentSource =
                stringValue(root, "actualGoldSource", "none");
        state.normalize();
        return state;
    }

    private static long longValue(JsonObject root, String key, long fallback) {
        try {
            JsonElement value = root.get(key);
            return value == null || value.isJsonNull()
                    ? fallback
                    : value.getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int intValue(JsonObject root, String key, int fallback) {
        long value = longValue(root, key, fallback);
        return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE
                ? fallback
                : (int) value;
    }

    private static double doubleValue(
            JsonObject root, String key, double fallback) {
        try {
            JsonElement value = root.get(key);
            return value == null || value.isJsonNull()
                    ? fallback
                    : value.getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String stringValue(
            JsonObject root, String key, String fallback) {
        try {
            JsonElement value = root.get(key);
            return value == null || value.isJsonNull()
                    ? fallback
                    : value.getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long safeMultiply(long left, long right) {
        if (left <= 0 || right <= 0) return 0;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

}
