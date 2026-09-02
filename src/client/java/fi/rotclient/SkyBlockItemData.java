package fi.rotclient;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Optional;

/**
 * SkyBlock ExtraAttributes + dye color snapshot for tooltip modules.
 */
final class SkyBlockItemData {
    private SkyBlockItemData() {
    }

    static InfoTooltipsPolicy.Snapshot infoSnapshot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new InfoTooltipsPolicy.Snapshot("", 0, 0, 0L, false, false, "");
        }
        CompoundTag extra = extraAttributes(stack);
        String id = AutoClickerItemIdentity.skyBlockId(stack);
        int boost = intValue(extra, "baseStatBoostPercentage");
        if (boost <= 0) {
            boost = intValue(extra, "qualityBoost");
        }
        int tier = intValue(extra, "item_tier");
        long created = longValue(extra, "timestamp");
        boolean museumKnown = extra != null && extra.contains("donated_museum");
        boolean donated = byteValue(extra, "donated_museum") != 0;
        String hex = "";
        DyedItemColor dye = stack.get(DataComponents.DYED_COLOR);
        if (dye != null) {
            hex = InfoTooltipsPolicy.normalizeHex(dye.rgb());
        }
        return new InfoTooltipsPolicy.Snapshot(id, boost, tier, created, donated, museumKnown, hex);
    }

    static String uuid(ItemStack stack) {
        return stringValue(extraAttributes(stack), "uuid");
    }

    static String marketId(ItemStack stack) {
        String id = AutoClickerItemIdentity.skyBlockId(stack);
        if (!id.isBlank()) {
            return id;
        }
        CompoundTag extra = extraAttributes(stack);
        if (extra == null) {
            return "";
        }
        Optional<CompoundTag> enchantments = extra.getCompound("enchantments");
        if (enchantments.isEmpty()) {
            return "";
        }
        for (String key : enchantments.get().keySet()) {
            if (key != null && !key.isBlank()) {
                return "ENCHANTMENT_" + key.toUpperCase();
            }
        }
        return "";
    }

    static CompoundTag extraAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        if (tag == null) {
            return null;
        }
        return tag.getCompound("ExtraAttributes").orElse(tag);
    }

    static int dungeonStars(ItemStack stack) {
        CompoundTag extra = extraAttributes(stack);
        return SkyblockFlavorPolicy.dungeonStars(
                intValue(extra, "dungeon_item_level"),
                intValue(extra, "upgrade_level"));
    }

    static int petCandyUsed(ItemStack stack) {
        CompoundTag extra = extraAttributes(stack);
        if (extra == null) {
            return -1;
        }
        if (extra.contains("candyUsed")) {
            return intValue(extra, "candyUsed");
        }
        return SkyblockFlavorPolicy.parseCandyUsed(stringValue(extra, "petInfo"));
    }

    private static int intValue(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.contains(key)) {
            return 0;
        }
        return tag.getInt(key).orElse(0);
    }

    private static long longValue(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.contains(key)) {
            return 0L;
        }
        return tag.getLong(key).orElse(0L);
    }

    private static byte byteValue(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.contains(key)) {
            return 0;
        }
        return tag.getByte(key).orElse((byte) 0);
    }

    private static String stringValue(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.contains(key)) {
            return "";
        }
        return tag.getString(key).orElse("");
    }
}
