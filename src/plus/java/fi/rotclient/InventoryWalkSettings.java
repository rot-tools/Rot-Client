package fi.rotclient;

/** Plus-owned Inventory Walk switch and ping window. */
record InventoryWalkSettings(boolean enabled, int pingMs) {
    static InventoryWalkSettings from(QolUtilityConfig config) {
        return new InventoryWalkSettings(
                PlusOpaqueSettings.bool(config, "inventoryWalkEnabled", false),
                InventoryWalkPolicy.clampPingMs(PlusOpaqueSettings.integer(
                        config, "inventoryWalkPingMs", InventoryWalkPolicy.DEFAULT_PING_MS)));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        PlusOpaqueSettings.write(config, "inventoryWalkEnabled", value);
    }

    static void pingMs(QolUtilityConfig config, int value) {
        PlusOpaqueSettings.write(config, "inventoryWalkPingMs",
                InventoryWalkPolicy.clampPingMs(value));
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config, "inventoryWalkEnabled", "inventoryWalkPingMs");
    }
}
