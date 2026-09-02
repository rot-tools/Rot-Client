package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Local chest GUI for {@link TermSimPolicy}. Clicks stay client-side.
 */
public final class TermSimScreen extends ContainerScreen {
    private final SimpleContainer container;
    private final int chestSize;

    private TermSimScreen(
            ChestMenu menu,
            Inventory inventory,
            Component title,
            SimpleContainer container,
            int chestSize) {
        super(menu, inventory, title);
        this.container = container;
        this.chestSize = chestSize;
    }

    static TermSimScreen create(TermSimPolicy.Layout layout) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        int size = layout.size();
        int rows = Math.max(1, size / 9);
        SimpleContainer container = new SimpleContainer(size);
        MenuType<?> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        ChestMenu menu = new ChestMenu(type, 0, player.getInventory(), container, rows);
        TermSimScreen screen = new TermSimScreen(
                menu, player.getInventory(), Component.literal(layout.title()), container, size);
        screen.apply(layout);
        return screen;
    }

    int chestSize() {
        return chestSize;
    }

    void apply(TermSimPolicy.Layout layout) {
        if (layout == null) {
            return;
        }
        for (int i = 0; i < chestSize; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
        for (TermSimPolicy.Slot slot : layout.slots()) {
            if (slot.index() < 0 || slot.index() >= chestSize) {
                continue;
            }
            container.setItem(slot.index(), stackFor(slot));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        TermSimRuntime.onContainerTick(this);
    }

    static void playClick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        }
    }

    private static ItemStack stackFor(TermSimPolicy.Slot slot) {
        Item item = resolve(slot.itemId());
        ItemStack stack = new ItemStack(item, Math.max(1, slot.count()));
        if (slot.name() != null && !slot.name().isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(slot.name()));
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(""));
        }
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, slot.enchanted());
        return stack;
    }

    private static Item resolve(String itemId) {
        Identifier id = Identifier.tryParse("minecraft:" + (itemId == null ? "" : itemId));
        Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        return item == null ? Items.BARRIER : item;
    }
}
