package fi.rotclient;

import java.util.Locale;
import java.util.Objects;

/**
 * Stable SkyBlock item identity. Unknown IDs are allowed; equality is by
 * normalized id only.
 */
final class SkyBlockItemId {
    private final String id;
    private final String displayName;

    SkyBlockItemId(String id, String displayName) {
        this.id = normalize(id);
        if (this.id.isEmpty()) {
            throw new IllegalArgumentException("Item id cannot be blank");
        }
        String safeName = displayName == null ? "" : displayName.trim();
        this.displayName = safeName.isEmpty() ? this.id : safeName;
    }

    static SkyBlockItemId of(String id) {
        return new SkyBlockItemId(id, id);
    }

    static SkyBlockItemId of(String id, String displayName) {
        return new SkyBlockItemId(id, displayName);
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SkyBlockItemId that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
