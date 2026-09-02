package fi.rotclient;

/**
 * Prevents blaze daggers and fishing rods from activating twice in one click.
 */
public final class DoubleUseFixPolicy {
    public enum Kind {
        NONE,
        DAGGER,
        ROD
    }

    private DoubleUseFixPolicy() {
    }

    public static Kind kind(boolean fishingRod, String abilityText) {
        if (fishingRod) {
            return Kind.ROD;
        }
        String text = abilityText == null ? "" : abilityText.toLowerCase();
        if (text.contains("attunement")) {
            return Kind.DAGGER;
        }
        return Kind.NONE;
    }

    public static boolean cancelItemUseOnBlock(boolean enabled, Kind kind, boolean lookingAtBlock) {
        return enabled && lookingAtBlock && kind == Kind.DAGGER;
    }

    public static boolean replaceBlockUseWithItemUse(boolean enabled, Kind kind) {
        return enabled && kind == Kind.ROD;
    }
}
