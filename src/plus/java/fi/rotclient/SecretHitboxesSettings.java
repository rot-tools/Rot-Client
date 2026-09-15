package fi.rotclient;

/** Plus-owned Secret Hitboxes settings stored in opaque edition fields. */
record SecretHitboxesSettings(
        boolean enabled,
        boolean onlyDungeons,
        boolean lever,
        boolean oldLever,
        boolean button,
        boolean flatButton,
        boolean skull,
        boolean chests,
        boolean onlyTrappedChests) {

    static SecretHitboxesSettings from(QolUtilityConfig config) {
        return new SecretHitboxesSettings(
                read(config, "secretHitboxesEnabled", false),
                read(config, "secretHitboxesOnlyDungeons", true),
                read(config, "secretHitboxesLever", true),
                read(config, "secretHitboxesOldLever", true),
                read(config, "secretHitboxesButton", true),
                read(config, "secretHitboxesFlatButton", false),
                read(config, "secretHitboxesSkull", true),
                read(config, "secretHitboxesChests", false),
                read(config, "secretHitboxesOnlyTrappedChests", false));
    }

    static boolean read(QolUtilityConfig config, String key, boolean fallback) {
        return PlusOpaqueSettings.bool(config, key, fallback);
    }

    static void write(QolUtilityConfig config, String key, boolean value) {
        PlusOpaqueSettings.write(config, key, value);
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config,
                "secretHitboxesEnabled", "secretHitboxesOnlyDungeons",
                "secretHitboxesLever", "secretHitboxesOldLever",
                "secretHitboxesButton", "secretHitboxesFlatButton",
                "secretHitboxesSkull", "secretHitboxesChests",
                "secretHitboxesOnlyTrappedChests");
    }
}
