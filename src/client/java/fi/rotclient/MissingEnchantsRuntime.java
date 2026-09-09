package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Missing-enchantment projection. The actual tooltip is
 * extended through Fabric's tooltip callback; this legacy render hook remains
 * as a no-op so old mixin wiring cannot draw a duplicate floating panel.
 */
public final class MissingEnchantsRuntime {
    private static String pinnedIdentity = "";

    private MissingEnchantsRuntime() {
    }

    public static void noteCtrlClick(ItemStack stack, boolean controlDown, int button) {
        if (!controlDown || button != 0 || stack == null || stack.isEmpty()) {
            return;
        }
        String identity = AutoClickerItemIdentity.identify(stack);
        if (identity.isBlank()) {
            return;
        }
        pinnedIdentity = identity.equals(pinnedIdentity) ? "" : identity;
    }

    public static void afterTooltip(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Slot hovered) {
        // Rendered through append(...).
    }

    public static void append(ItemStack stack, List<Component> lines) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        if (qol == null || !qol.missingEnchantsEnabled || stack == null || stack.isEmpty()
                || lines == null || client == null || client.getWindow() == null) {
            return;
        }
        boolean unbound = qol.missingEnchantsKeybind == null || qol.missingEnchantsKeybind.isBlank();
        boolean held = QolKeybindNames.isBoundDown(
                client.getWindow().handle(), qol.missingEnchantsKeybind);
        boolean ctrl = QolKeybindNames.isKeyDown(client.getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                || QolKeybindNames.isKeyDown(client.getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean pinned = !pinnedIdentity.isBlank()
                && pinnedIdentity.equals(AutoClickerItemIdentity.identify(stack));
        if (!MissingEnchantsPolicy.shouldShow(true, unbound, held || ctrl || pinned)) {
            return;
        }
        List<String> lore = InventoryChromeRuntime.loreLines(stack);
        String hover = stack.getHoverName() == null ? "" : stack.getHoverName().getString();
        String type = MissingEnchantsPolicy.itemType(
                lore, hover, AutoClickerItemIdentity.skyBlockId(stack));
        MissingEnchantsPolicy.TooltipPlan plan = MissingEnchantsPolicy.plan(
                type,
                nbtEnchantLevels(stack, lore),
                qol.missingEnchantsShowUpgradable,
                qol.missingEnchantsShowConflicting);
        if (plan.missing().isEmpty() && plan.upgrades().isEmpty()) {
            return;
        }

        int insertAt = rarityLineIndex(lines);
        lines.add(insertAt++, Component.empty());
        lines.add(insertAt++, Component.literal("Enchant Check · " + plan.summary())
                .withStyle(ChatFormatting.DARK_AQUA));
        if (!plan.upgrades().isEmpty()) {
            lines.add(insertAt++, Component.literal("Upgradable Enchantments:")
                    .withStyle(ChatFormatting.AQUA));
            for (String upgrade : plan.upgrades()) {
                lines.add(insertAt++, Component.literal("  " + upgrade)
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        if (!plan.missing().isEmpty()) {
            lines.add(insertAt++, Component.literal("Missing Enchantments:")
                    .withStyle(ChatFormatting.RED));
            for (String enchant : plan.missing()) {
                lines.add(insertAt++, Component.literal("  " + enchant)
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    static Map<String, Integer> nbtEnchantLevels(ItemStack stack, List<String> lore) {
        LinkedHashMap<String, Integer> levels = new LinkedHashMap<>();
        if (stack == null || stack.isEmpty()) {
            return levels;
        }
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        if (tag == null) {
            return levels;
        }
        collectEnchantLevels(tag.getCompound("enchantments").orElse(null), levels);
        tag.getCompound("ExtraAttributes").ifPresent(extra ->
                collectEnchantLevels(extra.getCompound("enchantments").orElse(null), levels));
        if (lore != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "([A-Za-z][A-Za-z' -]{1,}?)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X|\\d{1,2})\\b");
            for (String line : lore) {
                java.util.regex.Matcher matcher = pattern.matcher(
                        ChatTextPolicy.stripFormatting(line));
                while (matcher.find()) {
                    levels.putIfAbsent(
                            MissingEnchantsPolicy.normalizeEnchant(matcher.group(1)),
                            MissingEnchantsPolicy.levelFromToken(matcher.group(2)));
                }
            }
        }
        return Map.copyOf(levels);
    }

    private static void collectEnchantLevels(
            CompoundTag enchantments,
            Map<String, Integer> levels) {
        if (enchantments == null) {
            return;
        }
        for (String key : enchantments.keySet()) {
            if (key != null && !key.isBlank()) {
                levels.put(
                        MissingEnchantsPolicy.normalizeEnchant(key),
                        enchantments.getInt(key).orElse(0));
            }
        }
    }

    private static int rarityLineIndex(List<Component> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String text = ChatTextPolicy.stripFormatting(lines.get(i).getString())
                    .toUpperCase(java.util.Locale.ROOT);
            if (text.matches(".*\\b(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)\\b.*")) {
                return i;
            }
        }
        return lines.size();
    }
}
