package fi.rotclient;

import fi.rotclient.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client bridge for item search keybinds and museum tooltip evidence. */
public final class ItemToolsRuntime {
    private static boolean keyWasDown;

    private ItemToolsRuntime() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            keyWasDown = false;
            return;
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = config.extras();
        boolean down = config.isModuleEnabled("qol.storage_overlay")
                && extras.storageItemSearch
                && QolKeybindNames.isBoundDown(
                        client.getWindow().handle(), extras.storageItemSearchKeybind);
        if (down && !keyWasDown && !(client.gui.screen() instanceof ItemSearchScreen)) {
            Screen parent = client.gui.screen();
            client.gui.setScreen(new ItemSearchScreen(parent, hoveredItemId(parent)));
        }
        keyWasDown = down;
    }

    public static void appendMuseumTooltip(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty() || lines == null) {
            return;
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = config.extras();
        if (!config.isModuleEnabled("qol.storage_overlay") || !extras.storageMuseumArmor) {
            return;
        }
        RotItemIndex.ItemDef item = RotItemIndex.find(SkyBlockItemData.marketId(stack));
        if (item == null || item.museumSet().isBlank()) {
            return;
        }
        List<String> missing = RotItemIndex.missingMuseumPieces(item.museumSet(), ownedIds());
        lines.add(Component.literal("§5Museum set: §d" + item.museumSet()));
        lines.add(Component.literal(missing.isEmpty()
                ? "§aComplete armor set in inventory"
                : "§eMissing: " + String.join(", ", missing)));
    }

    public static void appendCraftTooltip(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty() || lines == null) {
            return;
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (!config.isModuleEnabled("qol.storage_overlay")
                || !config.extras().storageCraftHelper) {
            return;
        }
        String itemId = SkyBlockItemData.marketId(stack);
        Map<String, Integer> ingredients = RotItemIndex.aggregateIngredients(itemId, 1);
        if (ingredients.isEmpty()) {
            return;
        }
        lines.add(Component.literal("§6Craft helper:"));
        int shown = 0;
        for (Map.Entry<String, Integer> ingredient : ingredients.entrySet()) {
            if (shown++ >= 3) {
                break;
            }
            int owned = ownedCount(ingredient.getKey());
            lines.add(Component.literal(
                    (owned >= ingredient.getValue() ? "§a" : "§c")
                            + ingredient.getKey() + ": "
                            + owned + " / " + ingredient.getValue()));
        }
    }

    static int ownedCount(String itemId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return 0;
        }
        int count = StorageOverlayRuntime.cachedOwnedCount(itemId);
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (itemId.equalsIgnoreCase(SkyBlockItemData.marketId(stack))) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static Set<String> ownedIds() {
        Minecraft client = Minecraft.getInstance();
        Set<String> result = new LinkedHashSet<>();
        if (client == null || client.player == null) {
            return result;
        }
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            String id = SkyBlockItemData.marketId(stack);
            if (!id.isBlank()) {
                result.add(id);
            }
        }
        return result;
    }

    private static String hoveredItemId(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> container
                && container instanceof AbstractContainerScreenAccessor accessor) {
            Slot slot = accessor.rotclient$hoveredSlot();
            if (slot != null && slot.hasItem()) {
                String id = SkyBlockItemData.marketId(slot.getItem());
                if (!id.isBlank()) {
                    return id;
                }
                return slot.getItem().getHoverName().getString();
            }
        }
        return "";
    }
}
