package fi.rotclient;

/** Plus-owned NPC dialogue selection settings. */
record AutoConversationSettings(boolean enabled, boolean multi, boolean green, int delayTicks) {
    static AutoConversationSettings from(QolUtilityConfig config) {
        return new AutoConversationSettings(
                PlusOpaqueSettings.bool(config, "autoConversationEnabled", false),
                PlusOpaqueSettings.bool(config, "autoConversationMulti", true),
                PlusOpaqueSettings.bool(config, "autoConversationGreen", true),
                AutoConversationPolicy.clampDelayTicks(PlusOpaqueSettings.integer(
                        config, "autoConversationDelayTicks", AutoConversationPolicy.DEFAULT_DELAY_TICKS)));
    }

    static void write(QolUtilityConfig config, String key, boolean value) {
        PlusOpaqueSettings.write(config, key, value);
    }

    static void delayTicks(QolUtilityConfig config, int value) {
        PlusOpaqueSettings.write(config, "autoConversationDelayTicks",
                AutoConversationPolicy.clampDelayTicks(value));
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config, "autoConversationEnabled", "autoConversationMulti",
                "autoConversationGreen", "autoConversationDelayTicks");
    }
}
