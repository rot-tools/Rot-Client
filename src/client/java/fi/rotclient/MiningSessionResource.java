package fi.rotclient;

import java.util.Objects;

/**
 * Stable resource identity. Display text is presentation metadata and is not
 * part of equality or hashing.
 */
final class MiningSessionResource {
    enum ResourceKind {
        ITEM,
        CURRENCY
    }

    private final String resourceId;
    private final String displayName;
    private final ResourceKind kind;
    private final TrackedMaterial material;
    private final GemstoneType gemstone;
    private final GemstoneTier gemstoneTier;

    private MiningSessionResource(
            String resourceId,
            String displayName,
            ResourceKind kind,
            TrackedMaterial material,
            GemstoneType gemstone,
            GemstoneTier gemstoneTier) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "Resource ID cannot be null or blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException(
                    "Resource kind cannot be null");
        }
        if (material != null && gemstone != null) {
            throw new IllegalArgumentException(
                    "Material and gemstone identities are mutually exclusive");
        }
        if (gemstoneTier != null && gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier requires a gemstone type");
        }
        if (kind == ResourceKind.CURRENCY
                && (material != null
                || gemstone != null
                || gemstoneTier != null)) {
            throw new IllegalArgumentException(
                    "Currency cannot carry item identities");
        }

        this.resourceId = resourceId;
        this.displayName = optionalText(displayName);
        this.kind = kind;
        this.material = material;
        this.gemstone = gemstone;
        this.gemstoneTier = gemstoneTier;
    }

    static MiningSessionResource genericItem(
            String resourceId,
            String displayName) {
        return new MiningSessionResource(
                resourceId,
                displayName,
                ResourceKind.ITEM,
                null,
                null,
                null);
    }

    static MiningSessionResource material(
            String resourceId,
            String displayName,
            TrackedMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException(
                    "Tracked material cannot be null");
        }
        return new MiningSessionResource(
                resourceId,
                displayName,
                ResourceKind.ITEM,
                material,
                null,
                null);
    }

    static MiningSessionResource gemstone(
            String resourceId,
            String displayName,
            GemstoneType gemstone,
            GemstoneTier tier) {
        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone type cannot be null");
        }
        return new MiningSessionResource(
                resourceId,
                displayName,
                ResourceKind.ITEM,
                null,
                gemstone,
                tier);
    }

    static MiningSessionResource currency(
            String resourceId,
            String displayName) {
        return new MiningSessionResource(
                resourceId,
                displayName,
                ResourceKind.CURRENCY,
                null,
                null,
                null);
    }

    String resourceId() {
        return resourceId;
    }

    String displayName() {
        return displayName;
    }

    ResourceKind kind() {
        return kind;
    }

    TrackedMaterial material() {
        return material;
    }

    GemstoneType gemstone() {
        return gemstone;
    }

    GemstoneTier gemstoneTier() {
        return gemstoneTier;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MiningSessionResource resource)) {
            return false;
        }
        return resourceId.equals(resource.resourceId)
                && kind == resource.kind
                && material == resource.material
                && gemstone == resource.gemstone
                && gemstoneTier == resource.gemstoneTier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                resourceId,
                kind,
                material,
                gemstone,
                gemstoneTier);
    }

    @Override
    public String toString() {
        return kind + ":" + resourceId;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank()
                ? null
                : value;
    }
}
