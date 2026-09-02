package fi.rotclient;

/**
 * Immutable live SkyBlock location snapshot: parent island/area plus an
 * optional more-precise sub-area. Renderers format this; they must not
 * re-parse scoreboard text.
 */
final class SkyBlockLocation {
    static final SkyBlockLocation UNKNOWN = new SkyBlockLocation(
            SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
            null,
            0L);

    /** Representative HUD-editor preview when no live location is known. */
    static final SkyBlockLocation EDITOR_PREVIEW = of(
            SkyBlockArea.DWARVEN_MINES,
            SkyBlockSubArea.THE_FORGE);

    private static final String SEPARATOR = " · ";

    private final SkyBlockArea parentArea;
    private final SkyBlockSubArea subArea;
    private final long observedAtMillis;

    private SkyBlockLocation(
            SkyBlockArea parentArea,
            SkyBlockSubArea subArea,
            long observedAtMillis) {
        this.parentArea = parentArea == null
                ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA
                : parentArea;
        if (subArea != null && subArea.parentArea() != this.parentArea) {
            this.subArea = null;
        } else {
            this.subArea = subArea;
        }
        this.observedAtMillis = Math.max(0L, observedAtMillis);
    }

    static SkyBlockLocation of(SkyBlockArea parentArea) {
        return of(parentArea, null, 0L);
    }

    static SkyBlockLocation of(
            SkyBlockArea parentArea,
            SkyBlockSubArea subArea) {
        return of(parentArea, subArea, 0L);
    }

    static SkyBlockLocation of(
            SkyBlockArea parentArea,
            SkyBlockSubArea subArea,
            long observedAtMillis) {
        SkyBlockArea safeParent = parentArea == null
                ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA
                : parentArea;
        if (safeParent == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) {
            return UNKNOWN;
        }
        return new SkyBlockLocation(safeParent, subArea, observedAtMillis);
    }

    SkyBlockArea parentArea() {
        return parentArea;
    }

    /** Nullable: absent when only the parent island is known. */
    SkyBlockSubArea subArea() {
        return subArea;
    }

    long observedAtMillis() {
        return observedAtMillis;
    }

    boolean isUnknown() {
        return parentArea == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA;
    }

    boolean hasSubArea() {
        return subArea != null;
    }

    String displayParent() {
        return parentArea.displayName();
    }

    String displaySubArea() {
        return subArea == null ? "" : subArea.displayName();
    }

    /**
     * Compact HUD / status value. Parent only when the child is unknown;
     * never fabricates a sub-area.
     */
    String hudDisplay() {
        if (isUnknown()) {
            return SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.displayName();
        }
        if (subArea == null) {
            return parentArea.displayName();
        }
        return parentArea.displayName() + SEPARATOR + subArea.displayName();
    }

    SkyBlockLocation withObservedAt(long observedAtMillis) {
        return new SkyBlockLocation(parentArea, subArea, observedAtMillis);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SkyBlockLocation that)) {
            return false;
        }
        return parentArea == that.parentArea && subArea == that.subArea;
    }

    @Override
    public int hashCode() {
        int result = parentArea.hashCode();
        result = 31 * result + (subArea == null ? 0 : subArea.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return hudDisplay();
    }
}
