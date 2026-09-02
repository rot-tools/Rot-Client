package fi.rotclient;

enum TrackerSelection {
    COAL("COAL", TrackingTarget.COAL, null),
    IRON("IRON", TrackingTarget.IRON, null),
    GOLD("GOLD", TrackingTarget.GOLD, null),
    LAPIS("LAPIS", TrackingTarget.LAPIS, null),
    REDSTONE("REDSTONE", TrackingTarget.REDSTONE, null),
    EMERALD("EMERALD", TrackingTarget.EMERALD, null),
    DIAMOND("DIAMOND", TrackingTarget.DIAMOND, null),
    QUARTZ("QUARTZ", TrackingTarget.QUARTZ, null),
    MITHRIL_TITANIUM(
            "MITHRIL_TITANIUM",
            TrackingTarget.MITHRIL_TITANIUM,
            null),
    TUNGSTEN("TUNGSTEN", TrackingTarget.TUNGSTEN, null),
    UMBER("UMBER", TrackingTarget.UMBER, null),
    RUBY("GEMSTONE_RUBY", null, GemstoneType.RUBY),
    AMBER("GEMSTONE_AMBER", null, GemstoneType.AMBER),
    SAPPHIRE("GEMSTONE_SAPPHIRE", null, GemstoneType.SAPPHIRE),
    JADE("GEMSTONE_JADE", null, GemstoneType.JADE),
    AMETHYST("GEMSTONE_AMETHYST", null, GemstoneType.AMETHYST),
    TOPAZ("GEMSTONE_TOPAZ", null, GemstoneType.TOPAZ),
    JASPER("GEMSTONE_JASPER", null, GemstoneType.JASPER),
    OPAL("GEMSTONE_OPAL", null, GemstoneType.OPAL),
    ONYX("GEMSTONE_ONYX", null, GemstoneType.ONYX),
    AQUAMARINE("GEMSTONE_AQUAMARINE", null, GemstoneType.AQUAMARINE),
    CITRINE("GEMSTONE_CITRINE", null, GemstoneType.CITRINE),
    PERIDOT("GEMSTONE_PERIDOT", null, GemstoneType.PERIDOT);

    private final String id;
    private final TrackingTarget materialTarget;
    private final GemstoneType gemstone;

    TrackerSelection(
            String id,
            TrackingTarget materialTarget,
            GemstoneType gemstone) {
        this.id = id;
        this.materialTarget = materialTarget;
        this.gemstone = gemstone;
    }

    String id() {
        return id;
    }

    String displayName() {
        if (isMaterial()) {
            return materialTarget.displayName();
        }
        return gemstone.displayName();
    }

    boolean isMaterial() {
        return materialTarget != null;
    }

    boolean isGemstone() {
        return gemstone != null;
    }

    boolean supportsLiveTracking() {
        return isMaterial()
                || isGemstone();
    }

    TrackingTarget materialTarget() {
        return materialTarget;
    }

    GemstoneType gemstone() {
        return gemstone;
    }

    static TrackerSelection fromId(String id) {
        TrackerSelection resolved = findKnown(id);
        return resolved == null ? GOLD : resolved;
    }

    static TrackerSelection findKnown(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        String normalized = id.trim();

        if ("MITHRIL_TUNGSTEN".equalsIgnoreCase(normalized)
                || "MITHRIL".equalsIgnoreCase(normalized)
                || "TITANIUM".equalsIgnoreCase(normalized)) {
            return MITHRIL_TITANIUM;
        }

        for (TrackerSelection selection : values()) {
            if (selection.id.equalsIgnoreCase(normalized)) {
                return selection;
            }
        }

        return null;
    }

    static TrackerSelection forMaterial(TrackingTarget target) {
        if (target == null) {
            return GOLD;
        }

        for (TrackerSelection selection : values()) {
            if (selection.isMaterial()
                    && selection.materialTarget == target) {
                return selection;
            }
        }

        return GOLD;
    }

    static TrackerSelection forGemstone(GemstoneType gemstone) {
        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone type cannot be null");
        }

        for (TrackerSelection selection : values()) {
            if (selection.isGemstone()
                    && selection.gemstone == gemstone) {
                return selection;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported gemstone: " + gemstone);
    }
}
