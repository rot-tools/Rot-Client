package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fi.rotclient.mixin.PlayerTabOverlayAccessor;
import net.fabricmc.loader.api.FabricLoader;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Caches Hypixel equipment / equipped pet from their chest GUIs and paints
 * the survival-inventory equipment bars, movable pet slot, and skill levels.
 * Observed stacks are stored locally like Storage Overlay page contents so
 * they still appear after a restart.
 */
public final class  InventoryChromeRuntime {
    private static final ItemStack[] EQUIPMENT = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private static ItemStack equippedPet = ItemStack.EMPTY;
    private static PetHudPolicy.Snapshot petHud = null;
    private static boolean petKnownEmpty;
    private static boolean petCachedFromGui;
    private static final long PET_GUI_AUTHORITY_MS =
            2_000L;

    private static long petGuiAuthoritativeUntilMs;
    private static int tickCounter;
    private static boolean draggingPet;
    private static boolean consumeNextRelease;
    private static int petGrabX;
    private static int petGrabY;
    private static boolean colorEditorOpen;
    private static InventoryOverlayPolicy.ChromeColorRole selectedColorRole =
            InventoryOverlayPolicy.ChromeColorRole.INV_PANEL;
    private static int draggingColorChannel = -1;
    private static String hoveredValueTip;
    private static boolean cacheLoaded;
    private static boolean codecReady;
    private static boolean diskHadItems;
    private static String lastFingerprint = "";
    private static final Gson CACHE_GSON = new Gson();
    private static final AtomicBoolean CACHE_DIRTY = new AtomicBoolean();
    private static final AtomicBoolean CACHE_SAVE_SCHEDULED = new AtomicBoolean();
    private static final ScheduledExecutorService CACHE_IO = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "RotClient-InventoryChromeCache");
        thread.setDaemon(true);
        return thread;
    });
    private static final long CACHE_SAVE_DELAY_MS = 150L;

    private InventoryChromeRuntime() {
    }

    public static void tick(Minecraft client) {
        loadCache();
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
        tickCounter = 0;
        draggingPet = false;
        consumeNextRelease = false;
        petGrabX = 0;
        petGrabY = 0;
        colorEditorOpen = false;
        draggingColorChannel = -1;
        petGuiAuthoritativeUntilMs = 0L;
    }

    public static void flushForShutdown() {
        if (!CACHE_DIRTY.get()) {
            return;
        }
        try {
            AtomicFileWriter.writeAtomically(cachePath(), buildCacheJson());
            CACHE_DIRTY.set(false);
        } catch (Exception ignored) {
        }
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

    static void noteEquippedPet(
            ItemStack pet) {

        if (pet == null
                || pet.isEmpty()) {

            return;
        }

        equippedPet =
                pet.copy();

        petHud =
                PetHudPolicy
                        .parse(
                                pet.getHoverName()
                                        .getString(),
                                loreLines(pet))
                        .orElse(
                                new PetHudPolicy.Snapshot(
                                        -1,
                                        MenuKeybindPolicy
                                                .stripGuiText(
                                                        pet.getHoverName()
                                                                .getString()),
                                        ""));

        petKnownEmpty = false;
        petCachedFromGui = true;
        /*
         * Tab-list pet information can remain stale briefly after switching.
         * During this period the Pets-menu result is the authoritative source.
         */
        petGuiAuthoritativeUntilMs =
                System.currentTimeMillis()
                        + PET_GUI_AUTHORITY_MS;

        persistObserved();
    }

    public static PetHudPolicy.Snapshot petHudSnapshot() {
        return petHud;
    }

    public static boolean hasEquippedPet() {
        return !equippedPet.isEmpty() && petHud != null && !petKnownEmpty;
    }

    static boolean skyblockInventoryUi() {
        return InventoryOverlayPolicy.showSkyblockInventoryUi(SkyBlockAreaDetector.isInSkyblock());
    }

    private static boolean inventoryOverlayVisible(QolUtilityConfig qol) {
        return qol != null && qol.inventoryOverlayEnabled && skyblockInventoryUi();
    }

    private static boolean inventoryOverlayVisible() {
        return inventoryOverlayVisible(RotClientClient.qolConfigPublic());
    }

    public static boolean shouldHideRecipeBook(Screen screen) {
        if (!(screen instanceof InventoryScreen)) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return (inventoryOverlayVisible(qol) && qol.inventoryOverlayHideRecipeBook)
                || (qol.renderOptimizerEnabled && qol.extras().hideRecipeBook);
    }

    public static boolean shouldHideStatusEffects() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return InventoryOverlayPolicy.hideInventoryStatusEffects(
                qol != null && qol.inventoryOverlayEnabled,
                qol != null && qol.inventoryOverlayHideStatusEffects);
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
        loadCache();
        snapshot(screen);
        SlayerRuntime.observeContainer(screen);
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        hoveredValueTip = null;
        graphics.nextStratum();
        if (screen instanceof InventoryScreen && inventoryOverlayVisible(qol)) {
            if (qol.inventoryOverlayEquipment) {
                renderEquipmentColumn(graphics, leftPos, topPos);
            }
            if (qol.inventoryOverlayPetSlot) {
                renderPetSlot(graphics, leftPos, topPos, qol);
            }
            renderValueMark(
                    screen,
                    graphics,
                    leftPos,
                    topPos,
                    qol.inventoryOverlayPetOffsetX,
                    qol.inventoryOverlayPetOffsetY,
                    mouseX,
                    mouseY);
            renderDashboardButton(
                    graphics,
                    leftPos,
                    topPos,
                    qol.inventoryOverlayPetOffsetX,
                    qol.inventoryOverlayPetOffsetY,
                    mouseX,
                    mouseY);
        }
        renderColorEditor(screen, graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
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
        if (qol.skillLevelsEnabled
                && skyblockInventoryUi()
                && InventoryOverlayPolicy.isSkillsMenu(titleOf(screen))) {
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
        return inventoryOverlayVisible(qol)
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
        if (inventoryOverlayVisible(qol)
                && screen instanceof InventoryScreen
                && InventoryOverlayPolicy.hitDashboardButton(
                        leftPos,
                        topPos,
                        qol.inventoryOverlayPetOffsetX,
                        qol.inventoryOverlayPetOffsetY,
                        mouseX,
                        mouseY)) {
            colorEditorOpen = false;
            draggingColorChannel = -1;
            RotClientClient.openClickGui();
            consumeNextRelease = true;
            return true;
        }
        if (handleColorEditorClick(screen, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            consumeNextRelease = true;
            return true;
        }
        if (!inventoryOverlayVisible(qol) || !(screen instanceof InventoryScreen)) {
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
        if (!inventoryOverlayVisible(qol)) {
            return;
        }
        if (InventoryOverlayPolicy.hitWrench(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            ItemStack hint = new ItemStack(Items.IRON_AXE);
            hint.set(DataComponents.CUSTOM_NAME, Component.literal("Inventory colors"));
            graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
            return;
        }
        if (colorEditorOpen) {
            InventoryOverlayPolicy.Rect wrench = wrenchHost(screen, leftPos, topPos, imageWidth);
            if (wrench != null) {
                InventoryOverlayPolicy.Rect editor = InventoryOverlayPolicy.editorRect(
                        wrench.x(), wrench.y(), screen.width, screen.height);
                if (InventoryOverlayPolicy.editorCloseRect(editor).contains(mouseX, mouseY)) {
                    ItemStack hint = new ItemStack(Items.BARRIER);
                    hint.set(DataComponents.CUSTOM_NAME, Component.literal("Close"));
                    graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
                    return;
                }
            }
        }
        if (inventoryOverlayVisible(qol)
                && InventoryOverlayPolicy.hitDashboardButton(
                        leftPos,
                        topPos,
                        qol.inventoryOverlayPetOffsetX,
                        qol.inventoryOverlayPetOffsetY,
                        mouseX,
                        mouseY)) {
            ItemStack hint = new ItemStack(Items.REDSTONE);
            hint.set(DataComponents.CUSTOM_NAME, Component.literal("Rot dashboard"));
            graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
            return;
        }
        if (inventoryOverlayVisible(qol) && qol.inventoryOverlayPetSlot
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
        if (inventoryOverlayVisible(qol) && qol.inventoryOverlayEquipment) {
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
        if (MenuKeybindPolicy.parsePetsTitle(title) != null
                && !InventoryOverlayPolicy.shouldKeepExistingCache(
                        hasEquippedPet() || !equippedPet.isEmpty() || petHud != null,
                        containerLooksUnpopulated(slots))) {
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
        persistObserved();
    }

    private static void snapshotEquipment(List<Slot> slots) {
        ItemStack[] next = new ItemStack[] {
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        for (int i = 0; i < InventoryOverlayPolicy.EQUIPMENT_CHEST_SLOTS.length; i++) {
            int index = InventoryOverlayPolicy.EQUIPMENT_CHEST_SLOTS[i];
            ItemStack stack = stackIn(slots, index);
            if (stack.isEmpty() || InventoryOverlayPolicy.isPlaceholder(
                    stack.getHoverName().getString(), itemPath(stack))) {
                continue;
            }
            next[i] = stack.copy();
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
                next[kind] = stack.copy();
            }
        }
        if (InventoryOverlayPolicy.shouldKeepExistingCache(
                hasAnyEquipment(), allEmpty(next))) {
            return;
        }
        System.arraycopy(next, 0, EQUIPMENT, 0, EQUIPMENT.length);
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
        ItemStack[] next = new ItemStack[] {
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
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
            next[kind >= 0 ? kind : Math.min(i / 9, next.length - 1)] = stack.copy();
        }
        if (InventoryOverlayPolicy.shouldKeepExistingCache(
                hasAnyEquipment(), allEmpty(next))) {
            return;
        }
        System.arraycopy(next, 0, EQUIPMENT, 0, EQUIPMENT.length);
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
            int textW = RotClientFonts.vanillaWidth(font, label);
            if (qol.skillLevelsBackground) {
                SkillLevelOverlayPolicy.LabelBox box =
                        SkillLevelOverlayPolicy.labelBackground(slot.x, slot.y, textW);
                graphics.fill(
                        leftPos + box.left(),
                        topPos + box.top(),
                        leftPos + box.right(),
                        topPos + box.bottom(),
                        SkillLevelOverlayPolicy.LABEL_BACKGROUND);
            }
            int x = leftPos + SkillLevelOverlayPolicy.labelX(slot.x, textW);
            int y = topPos + SkillLevelOverlayPolicy.labelY(slot.y);
            RotClientUiDraw.vanillaText(graphics, font, label, x, y, color, true);
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

    private static void applyTabPet(
            Optional<PetHudPolicy.Snapshot> tabPet) {

        if (tabPet.isEmpty()) {
            return;
        }

        /*
         * A freshly-confirmed Pets-menu selection is more reliable than the
         * tab list, which can still report the previous pet for a short time.
         */
        if (System.currentTimeMillis()
                < petGuiAuthoritativeUntilMs) {

            return;
        }
        PetHudPolicy.Snapshot snapshot = tabPet.get();
        if (petCachedFromGui && petHud != null) {
            petHud = new PetHudPolicy.Snapshot(
                    snapshot.level() >= 0 ? snapshot.level() : petHud.level(),
                    snapshot.name().isEmpty() ? petHud.name() : snapshot.name(),
                    petHud.heldItem());
            persistObserved();
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
        persistObserved();
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
        if (!(screen instanceof InventoryScreen) || graphics == null || !inventoryOverlayVisible()) {
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
        boolean hideOffhand = qol.inventoryOverlayEquipment;
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
        InventoryOverlayPolicy.Rect close = InventoryOverlayPolicy.editorCloseRect(editor);
        boolean closeHover = close.contains(mouseX, mouseY);
        graphics.fill(close.x(), close.y(), close.x() + close.width(), close.y() + close.height(),
                closeHover ? RotClientTheme.WARNING : RotClientTheme.BUTTON);
        RotClientTheme.drawOutline(graphics, close.x(), close.y(), close.width(), close.height(),
                closeHover ? RotClientTheme.BORDER_BRIGHT : RotClientTheme.BORDER);
        RotClientUiDraw.text(
                graphics,
                font,
                "×",
                close.x() + 1,
                close.y() + 1,
                closeHover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                false);
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
        boolean inClose = InventoryOverlayPolicy.editorCloseRect(editor).contains(mouseX, mouseY);
        if (InventoryOverlayPolicy.dismissColorEditor(
                true, false, editor.contains(mouseX, mouseY), inClose)) {
            colorEditorOpen = false;
            draggingColorChannel = -1;
            return true;
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
        if (screen instanceof InventoryScreen && inventoryOverlayVisible()) {
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

    private static void renderDashboardButton(
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int petOffsetX,
            int petOffsetY,
            int mouseX,
            int mouseY) {
        if (graphics == null) {
            return;
        }
        InventoryOverlayPolicy.Rect button =
                InventoryOverlayPolicy.dashboardButtonRect(leftPos, topPos, petOffsetX, petOffsetY);
        boolean hover = button.contains(mouseX, mouseY);
        drawSlotWell(graphics, button.x(), button.y());
        if (hover) {
            RotClientTheme.drawOutline(
                    graphics,
                    button.x(),
                    button.y(),
                    button.width(),
                    button.height(),
                    RotClientTheme.BORDER_BRIGHT);
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client == null ? null : client.font;
        if (font != null) {
            drawCenteredGlyph(
                    graphics,
                    font,
                    "R",
                    button.x(),
                    button.y(),
                    button.width(),
                    hover ? RotClientTheme.TEXT : RotClientTheme.BORDER_BRIGHT);
        } else {
            drawRotLetterIcon(graphics, button.x(), button.y(), button.width());
        }
    }

    /** Fallback pixel-art R when the font is not ready. */
    private static void drawRotLetterIcon(
            GuiGraphicsExtractor graphics, int x, int y, int size) {
        if (graphics == null || size < 8) {
            return;
        }
        int ink = RotClientTheme.BORDER_BRIGHT;
        int left = x + Math.max(3, size * 3 / 12);
        int top = y + Math.max(2, size * 2 / 12);
        int bottom = y + size - Math.max(2, size * 2 / 12);
        int mid = y + size / 2;
        int stem = Math.max(2, size * 2 / 12);
        graphics.fill(left, top, left + stem, bottom, ink);
        graphics.fill(left, top, x + size - Math.max(3, size * 3 / 12), top + stem, ink);
        graphics.fill(
                x + size - Math.max(5, size * 5 / 12),
                top + stem,
                x + size - Math.max(3, size * 3 / 12),
                mid,
                ink);
        graphics.fill(left, mid - 1, x + size - Math.max(4, size / 3), mid + 1, ink);
        graphics.fill(left + stem, mid, left + stem * 2, mid + stem, ink);
        graphics.fill(left + stem * 2, mid + 1, left + stem * 3, bottom, ink);
    }

    private static void renderValueMark(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int petOffsetX,
            int petOffsetY,
            int mouseX,
            int mouseY) {
        if (graphics == null) {
            return;
        }
        InventoryOverlayPolicy.Rect mark =
                InventoryOverlayPolicy.valueMarkRect(leftPos, topPos, petOffsetX, petOffsetY);
        boolean hover = mark.contains(mouseX, mouseY);
        drawSlotWell(graphics, mark.x(), mark.y());
        if (hover) {
            RotClientTheme.drawOutline(
                    graphics,
                    mark.x(),
                    mark.y(),
                    mark.width(),
                    mark.height(),
                    RotClientTheme.BORDER_BRIGHT);
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client == null ? null : client.font;
        if (font != null) {
            drawCenteredGlyph(
                    graphics,
                    font,
                    "S",
                    mark.x(),
                    mark.y(),
                    mark.width(),
                    hover ? RotClientTheme.TEXT : RotClientTheme.WARNING);
        }
        if (hover) {
            hoveredValueTip = InventoryValuePolicy.tooltip(inventoryMarketValue(screen));
        }
    }

    private static void drawCenteredGlyph(
            GuiGraphicsExtractor graphics,
            Font font,
            String glyph,
            int x,
            int y,
            int size,
            int color) {
        int width = RotClientFonts.width(font, glyph);
        int textX = x + Math.max(0, (size - width) / 2);
        int textY = y + Math.max(0, (size - 9) / 2);
        RotClientUiDraw.text(graphics, font, glyph, textX, textY, color, true);
    }

    public static void applyValueTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hoveredValueTip == null || hoveredValueTip.isBlank() || graphics == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }
        graphics.setTooltipForNextFrame(
                client.font, Component.literal(hoveredValueTip), mouseX, mouseY);
    }

    private static double inventoryMarketValue(AbstractContainerScreen<?> screen) {
        List<StorageOverlayPolicy.MarketLine> lines = new ArrayList<>();
        Map<String, Double> units = new HashMap<>();
        SkyBlockMarketQuoteService.Quotes quotes = SkyBlockMarketQuoteService.current();
        BazaarPriceService.MarketPrices bazaar = RotClientClient.currentMarketPrices();
        if (screen != null && screen.getMenu() != null) {
            for (Slot slot : screen.getMenu().slots) {
                if (!InventoryValuePolicy.includeSurvivalSlot(slot.x, slot.y)) {
                    continue;
                }
                addValuedStack(lines, units, quotes, bazaar, slot.getItem());
            }
        }
        for (ItemStack stack : EQUIPMENT) {
            addValuedStack(lines, units, quotes, bazaar, stack);
        }
        if (hasEquippedPet()) {
            addValuedStack(lines, units, quotes, bazaar, equippedPet);
        }
        return StorageOverlayPolicy.instantSellTotal(lines, units);
    }

    private static void addValuedStack(
            List<StorageOverlayPolicy.MarketLine> lines,
            Map<String, Double> units,
            SkyBlockMarketQuoteService.Quotes quotes,
            BazaarPriceService.MarketPrices bazaar,
            ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        List<String> candidates = InventoryValuePolicy.marketIdCandidates(
                SkyBlockItemData.marketId(stack),
                SkyBlockItemData.petInfo(stack),
                stack.getHoverName().getString());
        String priced = "";
        double unit = 0.0D;
        for (String id : candidates) {
            PriceTooltipsPolicy.Quote quote = quotes.quote(id);
            unit = StorageOverlayPolicy.marketUnitValue(
                    quote.lowestBin(), quote.bazaarBuy(), quote.bazaarSell());
            if (!(unit > 0.0D) && bazaar != null) {
                BazaarPriceService.ProductPrice product = bazaar.forProduct(id);
                if (product == null && id.startsWith("STARRED_")) {
                    product = bazaar.forProduct(id.substring("STARRED_".length()));
                }
                if (product != null) {
                    unit = product.instantSellPrice();
                }
            }
            if (unit > 0.0D && Double.isFinite(unit)) {
                priced = id;
                break;
            }
        }
        if (priced.isBlank()) {
            return;
        }
        lines.add(new StorageOverlayPolicy.MarketLine(priced, Math.max(1, stack.getCount())));
        units.putIfAbsent(priced, unit);
    }

    private static boolean hasAnyEquipment() {
        return !allEmpty(EQUIPMENT);
    }

    private static boolean allEmpty(ItemStack[] stacks) {
        if (stacks == null) {
            return true;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean containerLooksUnpopulated(List<Slot> slots) {
        int end = Math.max(0, slots.size() - 36);
        for (int i = 0; i < end; i++) {
            ItemStack stack = stackIn(slots, i);
            if (!stack.isEmpty()
                    && !InventoryOverlayPolicy.isPlaceholder(
                            stack.getHoverName().getString(), itemPath(stack))) {
                return false;
            }
        }
        return true;
    }

    private static Path cachePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(InventoryOverlayPolicy.CHROME_CACHE_FILE);
    }

    public static void loadCache() {
        Path path = cachePath();
        boolean fileExists = Files.exists(path);
        boolean memoryEmpty = chromeMemoryEmpty();
        Minecraft client = Minecraft.getInstance();
        boolean levelReady = client != null && client.level != null;
        if (!InventoryOverlayPolicy.shouldReloadChromeCache(
                cacheLoaded, memoryEmpty, fileExists, codecReady, levelReady)) {
            return;
        }
        cacheLoaded = true;
        codecReady = levelReady;
        if (!fileExists) {
            lastFingerprint = fingerprint();
            return;
        }
        try {
            JsonObject root = CACHE_GSON.fromJson(Files.readString(path), JsonObject.class);
            if (root == null) {
                lastFingerprint = fingerprint();
                return;
            }
            if (root.has("equipment") && root.get("equipment").isJsonArray()) {
                JsonArray items = root.getAsJsonArray("equipment");
                for (int i = 0; i < EQUIPMENT.length; i++) {
                    EQUIPMENT[i] = i < items.size()
                            ? StorageOverlayRuntime.stackFromCache(items.get(i))
                            : ItemStack.EMPTY;
                }
            }
            if (root.has("pet")) {
                equippedPet = StorageOverlayRuntime.stackFromCache(root.get("pet"));
            }
            petKnownEmpty = root.has("petKnownEmpty") && root.get("petKnownEmpty").getAsBoolean();
            petCachedFromGui = root.has("petCachedFromGui")
                    && root.get("petCachedFromGui").getAsBoolean();
            petHud = null;
            if (root.has("petHud") && root.get("petHud").isJsonObject()) {
                JsonObject hud = root.getAsJsonObject("petHud");
                String name = hud.has("name") ? hud.get("name").getAsString() : "";
                if (name != null && !name.isBlank()) {
                    int level = hud.has("level") ? hud.get("level").getAsInt() : -1;
                    String held = hud.has("heldItem") ? hud.get("heldItem").getAsString() : "";
                    petHud = new PetHudPolicy.Snapshot(level, name, held == null ? "" : held);
                }
            }
            if (petKnownEmpty) {
                equippedPet = ItemStack.EMPTY;
                petHud = null;
                petCachedFromGui = false;
            }
        } catch (Exception ignored) {
        }
        if (!petKnownEmpty
                && petHud == null
                && equippedPet != null
                && !equippedPet.isEmpty()) {
            petHud = PetHudPolicy.parse(
                            equippedPet.getHoverName().getString(),
                            loreLines(equippedPet))
                    .orElse(new PetHudPolicy.Snapshot(
                            -1, equippedPet.getHoverName().getString(), ""));
        }
        if (!petKnownEmpty
                && petHud != null
                && (equippedPet == null || equippedPet.isEmpty())) {
            equippedPet = new ItemStack(Items.PLAYER_HEAD);
        }
        diskHadItems = diskHadItems || hasAnyEquipment() || !equippedPet.isEmpty() || petHud != null;
        lastFingerprint = fingerprint();
    }

    private static void persistObserved() {
        String next = fingerprint();
        if (next.equals(lastFingerprint)) {
            return;
        }
        boolean empty = chromeMemoryEmpty();
        if (InventoryOverlayPolicy.shouldSkipEmptyChromeSave(empty, diskHadItems)) {
            lastFingerprint = next;
            return;
        }
        lastFingerprint = next;
        if (!empty) {
            diskHadItems = true;
        }
        scheduleSave();
    }

    private static boolean chromeMemoryEmpty() {
        return !hasAnyEquipment()
                && equippedPet.isEmpty()
                && petHud == null
                && !petKnownEmpty;
    }

    private static void scheduleSave() {
        CACHE_DIRTY.set(true);
        if (!CACHE_SAVE_SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        CACHE_IO.schedule(
                InventoryChromeRuntime::flushScheduledSave,
                CACHE_SAVE_DELAY_MS,
                TimeUnit.MILLISECONDS);
    }

    private static void flushScheduledSave() {
        CACHE_SAVE_SCHEDULED.set(false);
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(InventoryChromeRuntime::flushCacheToDisk);
        } else {
            flushCacheToDisk();
        }
    }

    private static void flushCacheToDisk() {
        if (!CACHE_DIRTY.get()) {
            return;
        }
        String json;
        try {
            json = buildCacheJson();
            CACHE_DIRTY.set(false);
        } catch (Exception ignored) {
            return;
        }
        CACHE_IO.execute(() -> {
            try {
                AtomicFileWriter.writeAtomically(cachePath(), json);
            } catch (Exception ignored) {
            }
        });
        if (CACHE_DIRTY.get() && CACHE_SAVE_SCHEDULED.compareAndSet(false, true)) {
            CACHE_IO.schedule(
                    InventoryChromeRuntime::flushScheduledSave,
                    CACHE_SAVE_DELAY_MS,
                    TimeUnit.MILLISECONDS);
        }
    }

    private static String buildCacheJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", InventoryOverlayPolicy.CHROME_CACHE_SCHEMA);
        JsonArray equipment = new JsonArray();
        for (ItemStack stack : EQUIPMENT) {
            equipment.add(StorageOverlayRuntime.stackToCache(stack));
        }
        root.add("equipment", equipment);
        root.add("pet", StorageOverlayRuntime.stackToCache(equippedPet));
        root.addProperty("petKnownEmpty", petKnownEmpty);
        root.addProperty("petCachedFromGui", petCachedFromGui);
        if (petHud != null && petHud.name() != null && !petHud.name().isBlank()) {
            JsonObject hud = new JsonObject();
            hud.addProperty("level", petHud.level());
            hud.addProperty("name", petHud.name());
            hud.addProperty("heldItem", petHud.heldItem() == null ? "" : petHud.heldItem());
            root.add("petHud", hud);
        }
        return CACHE_GSON.toJson(root);
    }

    private static String fingerprint() {
        StringBuilder builder = new StringBuilder();
        for (ItemStack stack : EQUIPMENT) {
            builder.append(stackFingerprint(stack)).append(';');
        }
        builder.append('|').append(stackFingerprint(equippedPet));
        builder.append('|').append(petKnownEmpty);
        builder.append('|').append(petCachedFromGui);
        if (petHud != null) {
            builder.append('|').append(petHud.level());
            builder.append('|').append(petHud.name());
            builder.append('|').append(petHud.heldItem());
        }
        return builder.toString();
    }

    private static String stackFingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                + '#'
                + stack.getCount()
                + '#'
                + stack.getHoverName().getString();
    }
}
