package fi.rotclient;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * SkyBlock-style held-item identity for auto-clicker whitelists. Prefers NBT
 * {@code uuid} then {@code id}, then falls back to the item hover name.
 */
public final class AutoClickerItemIdentity {
    private AutoClickerItemIdentity() {
    }

    public static String identify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Optional<String> fromTag = readTagString(stack, "uuid");
        if (fromTag.isPresent() && !fromTag.get().isBlank()) {
            return fromTag.get().trim();
        }
        fromTag = readTagString(stack, "id");
        if (fromTag.isPresent() && !fromTag.get().isBlank()) {
            return fromTag.get().trim();
        }
        String name = stack.getHoverName().getString();
        return name == null ? "" : name.trim();
    }

    /**
     * SkyBlock item {@code id} only. Never uses uuid or hover name, so
     * Terminator / Dungeon Breaker gates rely on an exact NBT id check.
     */
    public static String skyBlockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return readTagString(stack, "id").orElse("");
    }

    public static String skyBlockIdFromCustomData(CustomData custom) {
        return readTagString(custom, "id").orElse("");
    }

    /**
     * SkyBlock item {@code id} plus hover name. Never uses uuid, so Terminator
     * / Dungeon Breaker gates still match 1:1 Hypixel NBT.
     */
    public static String kindText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String id = readTagString(stack, "id").orElse("");
        String hover = stack.getHoverName() == null
                ? ""
                : stack.getHoverName().getString();
        return (id + " " + (hover == null ? "" : hover)).trim();
    }

    private static Optional<String> readTagString(ItemStack stack, String key) {
        return readTagString(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY), key);
    }

    private static Optional<String> readTagString(CustomData custom, String key) {
        if (custom == null) {
            return Optional.empty();
        }
        CompoundTag tag = custom.copyTag();
        if (tag == null) {
            return Optional.empty();
        }
        Optional<String> direct = stringFrom(tag, key);
        if (direct.isPresent()) {
            return direct;
        }
        return tag.getCompound("ExtraAttributes").flatMap(extra -> stringFrom(extra, key));
    }

    private static Optional<String> stringFrom(CompoundTag tag, String key) {
        if (tag == null || key == null || key.isBlank() || !tag.contains(key)) {
            return Optional.empty();
        }
        String value = tag.getString(key).orElse("");
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public static boolean isEtherwarpItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        boolean ethermerge = booleanFrom(tag, "ethermerge");
        if (!ethermerge) {
            ethermerge = tag.getCompound("ExtraAttributes")
                    .map(extra -> booleanFrom(extra, "ethermerge"))
                    .orElse(false);
        }
        return EtherwarpHelperPolicy.isEtherwarpItem(ethermerge, skyBlockId(stack));
    }

    private static boolean booleanFrom(CompoundTag tag, String key) {
        if (tag == null || key == null || key.isBlank() || !tag.contains(key)) {
            return false;
        }
        if (tag.getBoolean(key).orElse(false)) {
            return true;
        }
        return tag.getInt(key).orElse(0) != 0;
    }

    public static String normalizeForWhitelist(String identity) {
        if (identity == null || identity.isBlank()) {
            return "";
        }
        return identity.trim();
    }
}
