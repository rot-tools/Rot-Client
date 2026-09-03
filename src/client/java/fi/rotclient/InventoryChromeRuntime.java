package fi.rotclient;

import fi.rotclient.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Caches Hypixel equipment / equipped pet from their chest GUIs and paints
 * the survival-inventory equipment bars, movable pet slot, and skill levels.
 */
public final class InventoryChromeRuntime {
    private static final ItemStack[] EQUIPMENT = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private static ItemStack equippedPet = ItemStack.EMPTY;
    private static PetHudPolicy.Snapshot petHud = null;
    private static boolean petKnownEmpty;
    private static boolean petCachedFromGui;
    private static int tickCounter;
    private static boolean draggingPet;
    private static boolean consumeNextRelease;
    private static int petGrabX;
    private static int petGrabY;
    private static boolean colorEditorOpen;
    private static InventoryOverlayPolicy.ChromeColorRole selectedColorRole =
            InventoryOverlayPolicy.ChromeColorRole.INV_PANEL;
    private static int draggingColorChannel = -1;

    private InventoryChromeRuntime() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null) {
            clear();
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (qol.playerDisplayEnabled || qol.petHudEnabled) {
            if (++tickCounter % 2 != 0) {
                return;
            }
        } else if (++tickCounter % 10 != 0) {
            return;
        }
        observeLiveHudSources(client);
    }

    public static void clear() {
        for (int i = 0; i < EQUIPMENT.length; i++) {
            EQUIPMENT[i] = ItemStack.EMPTY;
        }
        equippedPet = ItemStack.EMPTY;
        petHud = null;
        petKnownEmpty = false;
        petCachedFromGui = false;
        tickCounter = 0;
        draggingPet = false;
        consumeNextRelease = false;
        petGrabX = 0;
        petGrabY = 0;
        colorEditorOpen = false;
        draggingColorChannel = -1;
    }

    public static ItemStack equipment(int index) {
        if (index < 0 || index >= EQUIPMENT.length) {
            return ItemStack.EMPTY;
        }
        return EQUIPMENT[index];
    }

    public static ItemStack equippedPet() {
        return equippedPet;
    }

    public static PetHudPolicy.Snapshot petHudSnapshot() {
        return petHud;
    }

    public static boolean hasEquippedPet() {
        return !equippedPet.isEmpty() && petHud != null && !petKnownEmpty;
    }

    public static boolean shouldHideRecipeBook(Screen screen) {
        if (!(screen instanceof InventoryScreen)) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return (qol.inventoryOverlayEnabled && qol.inventoryOverlayHideRecipeBook)
                || (qol.renderOptimizerEnabled && qol.extras().hideRecipeBook);
    }

    public static boolean shouldHideStatusEffects() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.inventoryOverlayEnabled && qol.inventoryOverlayHideStatusEffects;
    }

    public static void afterContainerContents(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY) {
        if (screen == null || graphics == null) {
            return;
        }
        snapshot(screen);
        SlayerRuntime.observeContainer(screen);
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        graphics.nextStratum();
        renderColorEditor(screen, graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        if (!(screen instanceof InventoryScreen)) {
            return;
        }
        if (qol.inventoryOverlayEnabled && qol.inventoryOverlayEquipment) {
            renderEquipmentColumn(graphics, leftPos, topPos);
        }
        if (qol.inventoryOverlayEnabled && qol.inventoryOverlayPetSlot) {
            renderPetSlot(graphics, leftPos, topPos, qol);
        }
    }

    /**
     * Draw after the whole container (including slots) so skill numbers sit
     * on top of the item textures.
     */
    public static void afterForeground(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos) {
        if (screen == null || graphics == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (qol.skillLevelsEnabled && InventoryOverlayPolicy.isSkillsMenu(titleOf(screen))) {
            graphics.nextStratum();
            graphics.nextStratum();
            graphics.nextStratum();
            renderSkillLevels(screen, graphics, leftPos, topPos, qol);
        }
    }

    public static boolean shouldHideOffhandSlot(Screen screen, Slot slot) {
        if (!(screen instanceof InventoryScreen) || slot == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.inventoryOverlayEnabled
                && qol.inventoryOverlayEquipment
                && InventoryOverlayPolicy.isVanillaOffhandSlot(slot.x, slot.y);
    }

    public static boolean handleInventoryClick(
            AbstractContainerScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY,
            int button,
            boolean controlDown,
            boolean shiftDown) {
        if (screen == null || button != 0) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        if (handleColorEditorClick(screen, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            consumeNextRelease = true;
            return true;
        }
        if (!qol.inventoryOverlayEnabled || !(screen instanceof InventoryScreen)) {
            return false;
        }
        if (qol.inventoryOverlayPetSlot
                && InventoryOverlayPolicy.hitPetSlot(
                        leftPos,
                        topPos,
                        qol.inventoryOverlayPetOffsetX,
                        qol.inventoryOverlayPetOffsetY,
                        mouseX,
                        mouseY)) {
            if (controlDown) {
                petGrabX = InventoryOverlayPolicy.petGrabX(
                        leftPos, mouseX, qol.inventoryOverlayPetOffsetX);
                petGrabY = InventoryOverlayPolicy.petGrabY(
                        topPos, mouseY, qol.inventoryOverlayPetOffsetY);
                draggingPet = true;
                consumeNextRelease = true;
                return true;
            }
            client.player.connection.sendCommand(InventoryOverlayPolicy.OPEN_PETS_COMMAND);
            consumeNextRelease = true;
            return true;
        }
        if (qol.inventoryOverlayEquipment) {
            int index = InventoryOverlayPolicy.hitEquipmentIndex(leftPos, topPos, mouseX, mouseY);
            if (index >= 0) {
                ItemStack equipped = EQUIPMENT[index];
                client.player.connection.sendCommand(
                        equipped.isEmpty()
                                ? InventoryOverlayPolicy.OPEN_STATS_COMMAND
                                : InventoryOverlayPolicy.OPEN_WARDROBE_COMMAND);
                consumeNextRelease = true;
                return true;
            }
        }
        return false;
    }

    public static boolean handleInventoryDrag(
            AbstractContainerScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY,
            int button) {
        if (draggingColorChannel >= 0 && button == 0) {
            applyColorSlider(screen, leftPos, topPos, imageWidth, imageHeight, mouseX);
            return true;
        }
        if (!draggingPet || button != 0 || !(screen instanceof InventoryScreen)) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        InventoryOverlayPolicy.PetDragOffset next = InventoryOverlayPolicy.offsetFromDrag(
                leftPos, topPos, mouseX, mouseY, petGrabX, petGrabY);
        qol.inventoryOverlayPetOffsetX = next.x();
        qol.inventoryOverlayPetOffsetY = next.y();
        return true;
    }

    public static boolean handleInventoryRelease(int button) {
        if (button != 0) {
            return false;
        }
        if (draggingColorChannel >= 0) {
            draggingColorChannel = -1;
            consumeNextRelease = false;
            TrackerStore.save(RotClientClient.trackerConfig());
            return true;
        }
        if (draggingPet) {
            draggingPet = false;
            consumeNextRelease = false;
            TrackerStore.save(RotClientClient.trackerConfig());
            return true;
        }
        if (consumeNextRelease) {
            consumeNextRelease = false;
            return true;
        }
        return false;
    }

    public static void afterContainerTooltip(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int imageWidth,
            int mouseX,
            int mouseY) {
        if (screen == null || graphics == null || !(screen instanceof InventoryScreen)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client == null ? null : client.font;
        if (font == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (InventoryOverlayPolicy.hitWrench(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            ItemStack hint = new ItemStack(Items.IRON_AXE);
            hint.set(DataComponents.CUSTOM_NAME, Component.literal("Inventory colors"));
            graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
            return;
        }
        if (qol.inventoryOverlayEnabled && qol.inventoryOverlayPetSlot
                && InventoryOverlayPolicy.hitPetSlot(
                        leftPos,
                        topPos,
                        qol.inventoryOverlayPetOffsetX,
                        qol.inventoryOverlayPetOffsetY,
                        mouseX,
                        mouseY)) {
            if (hasEquippedPet()) {
                graphics.setTooltipForNextFrame(font, equippedPet, mouseX, mouseY);
            } else {
                ItemStack hint = new ItemStack(Items.PLAYER_HEAD);
                hint.set(
                        DataComponents.CUSTOM_NAME,
                        Component.literal(InventoryOverlayPolicy.OPEN_PETS_LABEL));
                graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
            }
            return;
        }
        if (qol.inventoryOverlayEnabled && qol.inventoryOverlayEquipment) {
            int index = InventoryOverlayPolicy.hitEquipmentIndex(leftPos, topPos, mouseX, mouseY);
            if (index >= 0) {
                ItemStack equipped = EQUIPMENT[index];
                if (!equipped.isEmpty()) {
                    graphics.setTooltipForNextFrame(font, equipped, mouseX, mouseY);
                } else {
                    ItemStack hint = new ItemStack(Items.PAPER);
                    hint.set(
                            DataComponents.CUSTOM_NAME,
                            Component.literal(InventoryOverlayPolicy.OPEN_STATS_LABEL));
                    graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
                }
            }
        }
    }

    private static void snapshot(AbstractContainerScreen<?> screen) {
        String title = titleOf(screen);
        List<Slot> slots = screen.getMenu().slots;
        if (InventoryOverlayPolicy.isEquipmentMenu(title)) {
            snapshotEquipment(slots);
            snapshotStatsMenuPet(slots);
        }
        if (InventoryOverlayPolicy.isEquipmentSetsMenu(title)) {
            snapshotEquipmentSets(slots);
        }
        if (MenuKeybindPolicy.parsePetsTitle(title) != null) {
            ItemStack pet = findEquippedPet(slots);
            if (pet.isEmpty()) {
                equippedPet = ItemStack.EMPTY;
                petHud = null;
                petKnownEmpty = true;
                petCachedFromGui = false;
            } else {
                equippedPet = pet.copy();
                petHud = PetHudPolicy.parse(pet.getHoverName().getString(), loreLines(pet))
                        .orElse(new PetHudPolicy.Snapshot(-1, pet.getHoverName().getString(), ""));
                petKnownEmpty = false;
                petCachedFromGui = true;
            }
        }
    }

    private static void snapshotEquipment(List<Slot> slots) {
        for (int i = 0; i < EQUIPMENT.length; i++) {
            EQUIPMENT[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < InventoryOverlayPolicy.EQUIPMENT_CHEST_SLOTS.length; i++) {
            int index = InventoryOverlayPolicy.EQUIPMENT_CHEST_SLOTS[i];
            ItemStack stack = stackIn(slots, index);
            if (stack.isEmpty() || InventoryOverlayPolicy.isPlaceholder(
                    stack.getHoverName().getString(), itemPath(stack))) {
                continue;
            }
            EQUIPMENT[i] = stack.copy();
        }
        int containerEnd = Math.max(0, slots.size() - 36);
        for (int i = 0; i < containerEnd; i++) {
            ItemStack stack = stackIn(slots, i);
            if (stack.isEmpty() || InventoryOverlayPolicy.isPlaceholder(
                    stack.getHoverName().getString(), itemPath(stack))) {
                continue;
            }
            int kind = InventoryOverlayPolicy.classifyEquipmentIndex(
                    stack.getHoverName().getString(), loreLines(stack));
            if (kind >= 0) {
                EQUIPMENT[kind] = stack.copy();
            }
        }
    }

    private static void snapshotEquipmentSets(List<Slot> slots) {
        int column = -1;
        int containerEnd = Math.max(0, slots.size() - 36);
        for (int i = 36; i < Math.min(45, slots.size()); i++) {
            ItemStack stack = stackIn(slots, i);
            var found = InventoryOverlayPolicy.equipmentSetsColumn(i, itemPath(stack));
            if (found.isPresent()) {
                column = found.getAsInt();
                break;
            }
        }
        if (column < 0) {
            return;
        }
        for (int i = 0; i < EQUIPMENT.length; i++) {
            EQUIPMENT[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < containerEnd; i++) {
            if (!InventoryOverlayPolicy.isEquipmentSetsPieceSlot(i, column)) {
                continue;
            }
            ItemStack stack = stackIn(slots, i);
            if (stack.isEmpty() || InventoryOverlayPolicy.isPlaceholder(
                    stack.getHoverName().getString(), itemPath(stack))) {
                continue;
            }
            int kind = InventoryOverlayPolicy.classifyEquipmentIndex(
                    stack.getHoverName().getString(), loreLines(stack));
            EQUIPMENT[kind >= 0 ? kind : i / 9] = stack.copy();
        }
    }

    private static void snapshotStatsMenuPet(List<Slot> slots) {
        ItemStack pet = stackIn(slots, InventoryOverlayPolicy.STATS_MENU_PET_SLOT);
        if (pet.isEmpty() || InventoryOverlayPolicy.isPlaceholder(
                pet.getHoverName().getString(), itemPath(pet))) {
            return;
        }
        equippedPet = pet.copy();
        petHud = PetHudPolicy.parse(pet.getHoverName().getString(), loreLines(pet))
                .orElse(new PetHudPolicy.Snapshot(-1, pet.getHoverName().getString(), ""));
        petKnownEmpty = false;
        petCachedFromGui = true;
    }

    private static void renderEquipmentColumn(
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos) {
        Minecraft client = Minecraft.getInstance();
        Font font = client == null ? null : client.font;
        for (int i = 0; i < EQUIPMENT.length; i++) {
            int x = leftPos + InventoryOverlayPolicy.equipmentSlotX();
            int y = topPos + InventoryOverlayPolicy.equipmentSlotY(i);
            drawSlotWell(graphics, x, y);
            ItemStack stack = EQUIPMENT[i];
            if (!stack.isEmpty()) {
                graphics.item(stack, x + 1, y + 1);
            } else {
                drawClickHint(graphics, font, x, y);
            }
        }
    }

    private static void renderPetSlot(
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            QolUtilityConfig qol) {
        int x = leftPos + InventoryOverlayPolicy.petSlotX(qol.inventoryOverlayPetOffsetX);
        int y = topPos + InventoryOverlayPolicy.petSlotY(qol.inventoryOverlayPetOffsetY);
        drawSlotWell(graphics, x, y);
        if (hasEquippedPet()) {
            graphics.item(equippedPet, x + 1, y + 1);
        } else {
            Minecraft client = Minecraft.getInstance();
            Font font = client == null ? null : client.font;
            drawClickHint(graphics, font, x, y);
        }
    }

    private static void renderSkillLevels(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            QolUtilityConfig qol) {
        Minecraft client = Minecraft.getInstance();
        Font font = client == null ? null : client.font;
        if (font == null) {
            return;
        }
        List<Slot> slots = screen.getMenu().slots;
        int containerEnd = Math.max(0, slots.size() - 36);
        for (int i = 0; i < containerEnd; i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            Optional<SkillLevelOverlayPolicy.Overlay> overlay =
                    SkillLevelOverlayPolicy.parse(
                            stack.getHoverName().getString(),
                            loreLines(stack),
                            stack.getCount());
            if (overlay.isEmpty()) {
                continue;
            }
            String label = Integer.toString(overlay.get().level());
            int color = SkillLevelOverlayPolicy.colorFor(
                    overlay.get().max(),
                    qol.skillLevelsColor,
                    qol.skillLevelsMaxColor);
            if (slot.x < -1000 || slot.y < -1000) {
                continue;
            }
            int textW = font.width(label);
            int x = leftPos + SkillLevelOverlayPolicy.labelX(slot.x, textW);
            int y = topPos + SkillLevelOverlayPolicy.labelY(slot.y);
            RotClientUiDraw.text(graphics, font, label, x, y, color, true);
        }
    }

    private static void drawSlotWell(GuiGraphicsExtractor graphics, int x, int y) {
        int size = InventoryOverlayPolicy.SLOT_SIZE;
        graphics.fill(x, y, x + size, y + size, InventoryOverlayPolicy.DEFAULT_SLOT_BORDER);
        graphics.fill(
                x + 1,
                y + 1,
                x + size - 1,
                y + size - 1,
                InventoryOverlayPolicy.DEFAULT_SLOT_WELL);
    }

    private static void drawClickHint(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        if (font == null) {
            graphics.fill(x + 8, y + 4, x + 10, y + 14, 0xFF88CCFF);
            graphics.fill(x + 4, y + 8, x + 14, y + 10, 0xFF88CCFF);
            return;
        }
        int plusW = font.width("+");
        RotClientUiDraw.text(graphics, font, "+", x + (18 - plusW) / 2, y + 5, 0xFF88CCFF, true);
    }

    private static ItemStack findEquippedPet(List<Slot> slots) {
        int end = Math.min(54, slots.size());
        for (int i = 0; i < end; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (PetHudPolicy.loreMeansEquipped(loreLines(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stackIn(List<Slot> slots, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        return slots.get(index).getItem();
    }

    private static String titleOf(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }

    private static String itemPath(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    static List<String> loreLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        List<String> lines = new ArrayList<>();
        for (Component line : lore.lines()) {
            lines.add(line.getString());
        }
        for (Component line : lore.styledLines()) {
            String text = line.getString();
            if (!lines.contains(text)) {
                lines.add(text);
            }
        }
        return lines;
    }

    private static void observeLiveHudSources(Minecraft client) {
        StringBuilder tabText = new StringBuilder();
        if (client.gui != null && client.gui.hud != null) {
            PlayerTabOverlayAccessor tab =
                    (PlayerTabOverlayAccessor) client.gui.hud.getTabList();
            Component header = tab.rotclient$getHeader();
            Component footer = tab.rotclient$getFooter();
            if (header != null) {
                appendLine(tabText, header.getString());
            }
            if (footer != null) {
                appendLine(tabText, footer.getString());
            }
        }
        if (client.level != null) {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null) {
                appendLine(tabText, sidebar.getDisplayName().getString());
                for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
                    Component line = PlayerTeam.formatNameForTeam(
                            scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
                    appendLine(tabText, line.getString());
                }
            }
        }
        String blob = tabText.toString();
        if (!blob.isBlank()) {
            RotClientClient.observeHudSourceStats(blob);
            applyTabPet(PetHudPolicy.parseTabText(blob));
        }
    }

    private static void applyTabPet(Optional<PetHudPolicy.Snapshot> tabPet) {
        if (tabPet.isEmpty()) {
            return;
        }
        PetHudPolicy.Snapshot snapshot = tabPet.get();
        if (petCachedFromGui && petHud != null) {
            petHud = new PetHudPolicy.Snapshot(
                    snapshot.level() >= 0 ? snapshot.level() : petHud.level(),
                    snapshot.name().isEmpty() ? petHud.name() : snapshot.name(),
                    petHud.heldItem());
            return;
        }
        petHud = petHud == null
                ? snapshot
                : new PetHudPolicy.Snapshot(
                        snapshot.level() >= 0 ? snapshot.level() : petHud.level(),
                        snapshot.name().isEmpty() ? petHud.name() : snapshot.name(),
                        petHud.heldItem());
        if (equippedPet.isEmpty()) {
            equippedPet = new ItemStack(Items.PLAYER_HEAD);
        }
        petKnownEmpty = false;
    }

    private static void appendLine(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(text);
    }

    public static void extractInventoryBackground(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight) {
        if (!(screen instanceof InventoryScreen) || graphics == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        drawChromeFills(
                graphics,
                InventoryOverlayPolicy.ChromeRegion.PANEL,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                paintChrome(qol.inventoryChromePanel, InventoryOverlayPolicy.ChromeColorRole.INV_PANEL));
        drawChromeFills(
                graphics,
                InventoryOverlayPolicy.ChromeRegion.HEADER,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                paintChrome(qol.inventoryChromeHeader, InventoryOverlayPolicy.ChromeColorRole.INV_HEADER));
        drawChromeFills(
                graphics,
                InventoryOverlayPolicy.ChromeRegion.MAIN,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                paintChrome(qol.inventoryChromeMain, InventoryOverlayPolicy.ChromeColorRole.INV_MAIN));
        drawChromeFills(
                graphics,
                InventoryOverlayPolicy.ChromeRegion.HOTBAR,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                paintChrome(qol.inventoryChromeHotbar, InventoryOverlayPolicy.ChromeColorRole.INV_HOTBAR));
        boolean hideOffhand = qol.inventoryOverlayEnabled && qol.inventoryOverlayEquipment;
        for (InventoryOverlayPolicy.Rect slot : InventoryOverlayPolicy.survivalSlotRects(
                leftPos, topPos, !hideOffhand)) {
            drawSlotWell(graphics, slot.x(), slot.y());
        }
        InventoryOverlayPolicy.Rect border = InventoryOverlayPolicy.chromeRegion(
                InventoryOverlayPolicy.ChromeRegion.BORDER, leftPos, topPos, imageWidth, imageHeight);
        int color = paintChrome(qol.inventoryChromeBorder, InventoryOverlayPolicy.ChromeColorRole.INV_BORDER);
        if (((color >>> 24) & 0xFF) == 0) {
            return;
        }
        graphics.fill(border.x(), border.y(), border.x() + border.width(), border.y() + 1, color);
        graphics.fill(border.x(), border.y() + border.height() - 1,
                border.x() + border.width(), border.y() + border.height(), color);
        graphics.fill(border.x(), border.y(), border.x() + 1, border.y() + border.height(), color);
        graphics.fill(border.x() + border.width() - 1, border.y(),
                border.x() + border.width(), border.y() + border.height(), color);
    }

    public static void renderColorEditor(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY) {
        InventoryOverlayPolicy.Rect wrench = wrenchHost(screen, leftPos, topPos, imageWidth);
        if (wrench == null || graphics == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client == null ? null : client.font;
        boolean hover = wrench.contains(mouseX, mouseY);
        graphics.fill(wrench.x(), wrench.y(), wrench.x() + wrench.width(), wrench.y() + wrench.height(),
                hover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON);
        RotClientTheme.drawOutline(graphics, wrench.x(), wrench.y(), wrench.width(), wrench.height(),
                hover ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER);
        drawPaintBucketIcon(graphics, wrench.x(), wrench.y(), wrench.width());
        if (font != null && hover && !colorEditorOpen) {
            RotClientUiDraw.text(graphics, font, "Edit colors", mouseX + 12, mouseY - 10, 0xFFFFFFFF, true);
        }
        if (!colorEditorOpen || font == null) {
            return;
        }
        InventoryOverlayPolicy.Rect editor = InventoryOverlayPolicy.editorRect(
                wrench.x(), wrench.y(), screen.width, screen.height);
        graphics.fill(editor.x(), editor.y(), editor.x() + editor.width(), editor.y() + editor.height(),
                0xF0111118);
        graphics.fill(editor.x(), editor.y(), editor.x() + editor.width(), editor.y() + 1,
                RotClientTheme.BORDER_BRIGHT);
        RotClientUiDraw.text(graphics, font, "Inventory colors", editor.x() + 8, editor.y() + 6, RotClientTheme.TEXT, false);
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        InventoryOverlayPolicy.ChromeColorRole[] roles = InventoryOverlayPolicy.ChromeColorRole.values();
        for (int i = 0; i < roles.length; i++) {
            InventoryOverlayPolicy.ChromeColorRole role = roles[i];
            InventoryOverlayPolicy.Rect row = InventoryOverlayPolicy.editorRowRect(editor, i);
            boolean selected = role == selectedColorRole;
            if (selected) {
                graphics.fill(row.x(), row.y(), row.x() + row.width(), row.y() + row.height(),
                        RotClientTheme.SELECTED_ROW);
            }
            RotClientUiDraw.text(graphics, font, role.label(), row.x() + 2, row.y() + 4,
                    selected ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED, false);
            InventoryOverlayPolicy.Rect reset = InventoryOverlayPolicy.editorResetRect(row);
            boolean resetHover = reset.contains(mouseX, mouseY);
            graphics.fill(reset.x(), reset.y(), reset.x() + reset.width(), reset.y() + reset.height(),
                    resetHover ? RotClientTheme.BUTTON_HOVER : RotClientTheme.BUTTON);
            RotClientUiDraw.text(graphics, font, "Reset", reset.x() + 3, reset.y() + 2,
                    resetHover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED, false);
            InventoryOverlayPolicy.Rect swatch = InventoryOverlayPolicy.editorSwatchRect(row);
            int color = readChromeColor(qol, role);
            int shown = ((color >>> 24) & 0xFF) == 0 ? 0xFF22222C : color;
            graphics.fill(swatch.x(), swatch.y(), swatch.x() + swatch.width(), swatch.y() + swatch.height(), shown);
        }
        int color = readChromeColor(qol, selectedColorRole);
        String[] labels = {"R", "G", "B", "A"};
        for (int channel = 0; channel < 4; channel++) {
            InventoryOverlayPolicy.Rect slider = InventoryOverlayPolicy.editorSliderRect(editor, channel);
            String channelLabel = channel == 3 ? "Op" : labels[channel];
            RotClientUiDraw.text(graphics, font, channelLabel, editor.x() + 8, slider.y() + 1, RotClientTheme.TEXT_MUTED, false);
            graphics.fill(slider.x(), slider.y(), slider.x() + slider.width(), slider.y() + slider.height(),
                    0xFF1A1A22);
            int value = InventoryOverlayPolicy.channelValue(color, channel);
            int knob = slider.x() + Math.round((slider.width() - 4) * (value / 255f));
            graphics.fill(slider.x(), slider.y(), knob + 4, slider.y() + slider.height(), 0xFF54A0FF);
        }
        int alpha = InventoryOverlayPolicy.channelValue(color, 3);
        String footer = alpha == 0
                ? "Opacity 0 hides this layer · drag R/G/B to paint"
                : selectedColorRole.hint();
        RotClientUiDraw.text(graphics, font, footer,
                editor.x() + 8, editor.y() + editor.height() - 26, RotClientTheme.TEXT_MUTED, false);
        String hex = String.format("#%08X", color);
        RotClientUiDraw.text(graphics, font, hex,
                editor.x() + 8, editor.y() + editor.height() - 14, RotClientTheme.TEXT_DIM, false);
    }

    private static boolean handleColorEditorClick(
            AbstractContainerScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY) {
        InventoryOverlayPolicy.Rect wrench = wrenchHost(screen, leftPos, topPos, imageWidth);
        if (wrench == null) {
            return false;
        }
        if (wrench.contains(mouseX, mouseY)) {
            colorEditorOpen = !colorEditorOpen;
            draggingColorChannel = -1;
            return true;
        }
        if (!colorEditorOpen) {
            return false;
        }
        InventoryOverlayPolicy.Rect editor = InventoryOverlayPolicy.editorRect(
                wrench.x(), wrench.y(), screen.width, screen.height);
        if (!editor.contains(mouseX, mouseY)) {
            return false;
        }
        InventoryOverlayPolicy.ChromeColorRole[] roles = InventoryOverlayPolicy.ChromeColorRole.values();
        for (int i = 0; i < roles.length; i++) {
            InventoryOverlayPolicy.Rect row = InventoryOverlayPolicy.editorRowRect(editor, i);
            if (InventoryOverlayPolicy.editorResetRect(row).contains(mouseX, mouseY)) {
                selectedColorRole = roles[i];
                writeChromeColor(
                        RotClientClient.qolConfigPublic(),
                        roles[i],
                        InventoryOverlayPolicy.defaultChromeColor(roles[i]));
                TrackerStore.save(RotClientClient.trackerConfig());
                return true;
            }
            if (row.contains(mouseX, mouseY)) {
                selectedColorRole = roles[i];
                return true;
            }
        }
        for (int channel = 0; channel < 4; channel++) {
            InventoryOverlayPolicy.Rect slider = InventoryOverlayPolicy.editorSliderRect(editor, channel);
            if (slider.contains(mouseX, mouseY)) {
                draggingColorChannel = channel;
                applyColorSlider(screen, leftPos, topPos, imageWidth, imageHeight, mouseX);
                return true;
            }
        }
        return true;
    }

    private static void applyColorSlider(
            AbstractContainerScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX) {
        if (draggingColorChannel < 0) {
            return;
        }
        InventoryOverlayPolicy.Rect wrench = wrenchHost(screen, leftPos, topPos, imageWidth);
        if (wrench == null) {
            return;
        }
        InventoryOverlayPolicy.Rect slider = InventoryOverlayPolicy.editorSliderRect(
                InventoryOverlayPolicy.editorRect(wrench.x(), wrench.y(), screen.width, screen.height),
                draggingColorChannel);
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        int next = InventoryOverlayPolicy.withChannelEnsuringVisible(
                readChromeColor(qol, selectedColorRole),
                draggingColorChannel,
                InventoryOverlayPolicy.sliderValue(slider, mouseX));
        writeChromeColor(qol, selectedColorRole, next);
    }

    private static InventoryOverlayPolicy.Rect wrenchHost(
            AbstractContainerScreen<?> screen,
            int leftPos,
            int topPos,
            int imageWidth) {
        if (screen != null
                && StorageOverlayRuntime.shouldReplaceVanilla(screen)
                && StorageOverlayRuntime.lastLayout() != null) {
            StorageOverlayPolicy.OverlayLayout layout = StorageOverlayRuntime.lastLayout();
            return InventoryOverlayPolicy.wrenchRect(
                    layout.playerX(), layout.playerY(), StorageOverlayPolicy.PLAYER_WIDTH);
        }
        if (screen instanceof InventoryScreen) {
            return InventoryOverlayPolicy.wrenchRect(leftPos, topPos, imageWidth);
        }
        return null;
    }

    private static int readChromeColor(QolUtilityConfig qol, InventoryOverlayPolicy.ChromeColorRole role) {
        Integer color = qol.readColor(role.settingId());
        return color == null ? 0 : color;
    }

    private static void writeChromeColor(
            QolUtilityConfig qol, InventoryOverlayPolicy.ChromeColorRole role, int argb) {
        qol.writeColor(role.settingId(), argb);
    }

    private static void drawChromeFills(
            GuiGraphicsExtractor graphics,
            InventoryOverlayPolicy.ChromeRegion region,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int argb) {
        for (InventoryOverlayPolicy.Rect rect : InventoryOverlayPolicy.chromeFillRects(
                region, leftPos, topPos, imageWidth, imageHeight)) {
            drawFill(graphics, rect, argb);
        }
    }

    private static void drawFill(GuiGraphicsExtractor graphics, InventoryOverlayPolicy.Rect rect, int argb) {
        if (graphics == null || rect == null || ((argb >>> 24) & 0xFF) == 0) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), argb);
    }

    private static int paintChrome(int stored, InventoryOverlayPolicy.ChromeColorRole role) {
        if (((stored >>> 24) & 0xFF) != 0) {
            return stored;
        }
        return InventoryOverlayPolicy.defaultChromeColor(role);
    }

    /** Spilling paint bucket for the Inventory Colors control. */
    private static void drawPaintBucketIcon(
            GuiGraphicsExtractor graphics, int x, int y, int size) {
        if (graphics == null || size < 8) {
            return;
        }
        int paint = RotClientTheme.BORDER_BRIGHT;
        int metal = RotClientTheme.TEXT;
        int handleX = x + 3;
        graphics.fill(handleX, y + 1, handleX + 2, y + 4, metal);
        graphics.fill(x + 2, y + 3, x + size - 3, y + 5, RotClientTheme.TEXT_DIM);
        graphics.fill(x + 3, y + 5, x + size - 4, y + size - 3, metal);
        graphics.fill(x + 4, y + 6, x + size - 5, y + size - 4, paint);
        graphics.fill(x + size - 5, y + size - 5, x + size - 2, y + size - 2, paint);
        graphics.fill(x + size - 4, y + size - 3, x + size - 1, y + size, paint);
        graphics.fill(x + 2, y + size - 2, x + 6, y + size, paint);
    }
}
