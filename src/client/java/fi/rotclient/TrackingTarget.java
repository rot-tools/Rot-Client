package fi.rotclient;

import java.util.List;

/**
 * A dashboard selection can route events to one or more independent material
 * ledgers. Mithril and Titanium share one target because Titanium can appear
 * while mining Mithril. Their quantities and Bazaar values remain separate.
 *
 * The ordinary and Pure variants of one ore share the same material ledger.
 * Tungsten and Umber remain separate because neither is a by-product of the
 * other.
 */
enum TrackingTarget {
    COAL(
            "COAL",
            "Coal",
            List.of(TrackedMaterial.COAL)),
    IRON(
            "IRON",
            "Iron",
            List.of(TrackedMaterial.IRON)),
    GOLD(
            "GOLD",
            "Pure Gold",
            List.of(TrackedMaterial.GOLD)),
    LAPIS(
            "LAPIS",
            "Lapis",
            List.of(TrackedMaterial.LAPIS)),
    REDSTONE(
            "REDSTONE",
            "Redstone",
            List.of(TrackedMaterial.REDSTONE)),
    EMERALD(
            "EMERALD",
            "Emerald",
            List.of(TrackedMaterial.EMERALD)),
    DIAMOND(
            "DIAMOND",
            "Pure Diamond",
            List.of(TrackedMaterial.DIAMOND)),
    QUARTZ(
            "QUARTZ",
            "Quartz",
            List.of(TrackedMaterial.QUARTZ)),
    MITHRIL_TITANIUM(
            "MITHRIL_TITANIUM",
            "Mithril + Titanium",
            List.of(
                    TrackedMaterial.MITHRIL,
                    TrackedMaterial.TITANIUM)),
    TUNGSTEN(
            "TUNGSTEN",
            "Tungsten",
            List.of(TrackedMaterial.TUNGSTEN)),
    UMBER(
            "UMBER",
            "Umber",
            List.of(TrackedMaterial.UMBER));

    private final String id;
    private final String displayName;
    private final List<TrackedMaterial> materials;

    TrackingTarget(
            String id,
            String displayName,
            List<TrackedMaterial> materials) {
        this.id = id;
        this.displayName = displayName;
        this.materials = List.copyOf(materials);
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    List<TrackedMaterial> materials() {
        return materials;
    }

    TrackedMaterial primaryMaterial() {
        return materials.getFirst();
    }

    boolean includes(TrackedMaterial material) {
        return material != null && materials.contains(material);
    }

    boolean isCombined() {
        return materials.size() > 1;
    }

    static TrackingTarget fromId(String id) {
        if (id == null) return GOLD;

        String normalized = id.trim();

        for (TrackingTarget target : values()) {
            if (target.id.equalsIgnoreCase(normalized)) {
                return target;
            }
        }

        /*
         * Legacy selections:
         *
         * MITHRIL_TUNGSTEN was the old, incorrect combined target.
         * Separate material IDs may also exist in older configuration files.
         *
         * Tungsten ledger data is not converted here. This method only chooses
         * a valid current dashboard target.
         */
        if ("MITHRIL_TUNGSTEN".equalsIgnoreCase(normalized)
                || TrackedMaterial.MITHRIL.id().equalsIgnoreCase(normalized)
                || TrackedMaterial.TITANIUM.id().equalsIgnoreCase(normalized)) {
            return MITHRIL_TITANIUM;
        }

        return GOLD;
    }

    static TrackingTarget forMaterial(TrackedMaterial material) {
        if (material == null
                || material == TrackedMaterial.HARD_STONE
                || material == TrackedMaterial.COBBLESTONE) {
            return null;
        }
        if (material == TrackedMaterial.MITHRIL
                || material == TrackedMaterial.TITANIUM) {
            return MITHRIL_TITANIUM;
        }
        for (TrackingTarget target : values()) {
            if (!target.isCombined() && target.includes(material)) {
                return target;
            }
        }
        return null;
    }
}
