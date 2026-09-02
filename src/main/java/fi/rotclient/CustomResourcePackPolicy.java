package fi.rotclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Built-in dark SkyBlock world packs converted for Minecraft 26.2. These
 * override vanilla {@code minecraft:} block/item textures. They do not touch
 * {@code hypixel_skyblock:} item models, so Legacy SkyBlock Textures can stay
 * on at the same time: remapped swords then pick up the dark vanilla textures.
 */
public final class CustomResourcePackPolicy {
    public static final String MODULE_ID = "qol.custom_resource_pack";
    public static final String OVERWORLD_SETTING = "qol.custom_resource_pack.overworld";
    public static final String CRIMSON_SETTING = "qol.custom_resource_pack.crimson";
    public static final String END_SETTING = "qol.custom_resource_pack.end";
    public static final String GAMEPLAY_FONT_SETTING = "qol.custom_resource_pack.gameplay_font";

    public static final String PACK_OVERWORLD = "rotclient:dark_overworld";
    public static final String PACK_CRIMSON = "rotclient:dark_crimson";
    public static final String PACK_END = "rotclient:dark_end";
    public static final String PACK_GAMEPLAY_FONT = "rotclient:gameplay_font";

    public static final List<String> ALL_PACK_IDS =
            List.of(PACK_OVERWORLD, PACK_CRIMSON, PACK_END, PACK_GAMEPLAY_FONT);

    private CustomResourcePackPolicy() {
    }

    public static List<String> enabledPackIds(
            boolean moduleOn,
            boolean overworld,
            boolean crimson,
            boolean end) {
        return enabledPackIds(moduleOn, overworld, crimson, end, false);
    }

    public static List<String> enabledPackIds(
            boolean moduleOn,
            boolean overworld,
            boolean crimson,
            boolean end,
            boolean gameplayFont) {
        if (!moduleOn) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        if (overworld) {
            ids.add(PACK_OVERWORLD);
        }
        if (crimson) {
            ids.add(PACK_CRIMSON);
        }
        if (end) {
            ids.add(PACK_END);
        }
        if (gameplayFont) {
            ids.add(PACK_GAMEPLAY_FONT);
        }
        return List.copyOf(ids);
    }

    /**
     * Rebuilds the selected-pack list so our packs sit at the end (highest
     * optional priority) without disturbing unrelated user packs.
     */
    public static List<String> applySelection(
            Collection<String> currentSelected,
            Collection<String> desiredOurPacks) {
        List<String> next = new ArrayList<>();
        if (currentSelected != null) {
            for (String id : currentSelected) {
                if (id != null && !isOurPack(id)) {
                    next.add(id);
                }
            }
        }
        if (desiredOurPacks != null) {
            for (String id : desiredOurPacks) {
                if (id != null && !id.isBlank() && !next.contains(id)) {
                    next.add(id);
                }
            }
        }
        return next;
    }

    public static boolean isOurPack(String packId) {
        if (packId == null || packId.isBlank()) {
            return false;
        }
        String normalized = packId.trim();
        if (ALL_PACK_IDS.contains(normalized)) {
            return true;
        }
        for (String id : ALL_PACK_IDS) {
            String path = id.substring(id.indexOf(':') + 1);
            if (normalized.equals(path)
                    || normalized.endsWith("/" + path)
                    || normalized.endsWith(":" + path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean selectionMatches(
            Collection<String> currentSelected,
            Collection<String> desiredOurPacks) {
        List<String> currentOurs = new ArrayList<>();
        if (currentSelected != null) {
            for (String id : currentSelected) {
                if (isOurPack(id)) {
                    currentOurs.add(canonicalId(id));
                }
            }
        }
        List<String> desired = new ArrayList<>();
        if (desiredOurPacks != null) {
            for (String id : desiredOurPacks) {
                desired.add(canonicalId(id));
            }
        }
        return currentOurs.equals(desired);
    }

    public static String canonicalId(String packId) {
        if (packId == null) {
            return "";
        }
        for (String id : ALL_PACK_IDS) {
            String path = id.substring(id.indexOf(':') + 1);
            if (packId.equals(id)
                    || packId.equals(path)
                    || packId.endsWith("/" + path)
                    || packId.endsWith(":" + path)) {
                return id;
            }
        }
        return packId;
    }

    public static boolean independentOfLegacyTextures(
            boolean customPackOn,
            boolean legacyTexturesOn) {
        return customPackOn || legacyTexturesOn || (!customPackOn && !legacyTexturesOn);
    }
}
