package fi.rotclient;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drives {@link ExperimentSolverPolicy} from container packets and exposes the
 * results to the container screen mixin.
 */
public final class ExperimentSolverRuntime {
    private static final ExperimentSolverPolicy POLICY = new ExperimentSolverPolicy();
    private static Object boundScreen;
    private static ExperimentSolverPolicy.Experiment experiment = ExperimentSolverPolicy.Experiment.NONE;
    private static Map<Integer, ExperimentSolverPolicy.Highlight> highlights = Map.of();

    private ExperimentSolverRuntime() {
    }

    /**
     * Called for every single-slot update the server pushes while a container is open.
     */
    public static void onSlotUpdate(int slotIndex, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (client == null
                || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (!enabled()) {
            return;
        }
        bind(screen);
        if (experiment == ExperimentSolverPolicy.Experiment.NONE || slotIndex < 0) {
            return;
        }
        List<Slot> slots = screen.getMenu().slots;
        if (slotIndex >= slots.size()) {
            return;
        }
        ExperimentSolverPolicy.SlotUpdate update = snapshot(slotIndex, stack);
        POLICY.updatePhase(update);
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        switch (experiment) {
            case CHRONOMATRON -> {
                if (extras.experimentChronomatron) {
                    POLICY.onChronomatronSlot(update);
                }
            }
            case ULTRASEQUENCER -> {
                if (extras.experimentUltrasequencer) {
                    POLICY.onUltrasequencerSlot(update, containerSnapshot(screen));
                }
            }
            case SUPERPAIRS -> {
                if (extras.experimentSuperpairs) {
                    POLICY.onSuperpairsSlot(update);
                }
            }
            default -> {
            }
        }
        highlights = POLICY.highlights(experiment);
    }

    /** Called when the server replaces the whole container contents. */
    public static void onContainerRefresh() {
        Minecraft client = Minecraft.getInstance();
        if (client != null
                && client.gui != null
                && client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            bind(screen);
        }
    }

    public static void onScreenClosed() {
        boundScreen = null;
        experiment = ExperimentSolverPolicy.Experiment.NONE;
        highlights = Map.of();
        POLICY.reset();
    }

    /**
     * Highlight color for a slot, or {@code 0} when the slot is not part of a solution.
     */
    public static int highlightColor(AbstractContainerScreen<?> screen, int slotIndex) {
        if (!enabled() || screen != boundScreen || highlights.isEmpty()) {
            return 0;
        }
        ExperimentSolverPolicy.Highlight highlight = highlights.get(slotIndex);
        if (highlight == null) {
            return 0;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return switch (highlight) {
            case FIRST -> extras.experimentFirstColor;
            case SECOND -> extras.experimentSecondColor;
            case MATCHED -> extras.experimentMatchedColor;
            case MATCH -> extras.experimentMatchColor;
            case POWERUP -> extras.experimentPowerupColor;
        };
    }

    /**
     * Swallows clicks that would break the remembered sequence.
     */
    public static boolean shouldBlockClick(AbstractContainerScreen<?> screen, int slotIndex) {
        if (!enabled() || screen != boundScreen || slotIndex < 0) {
            return false;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!QolFlavorSupport.isPlus() || !extras.experimentBlockWrongClicks) {
            return false;
        }
        if (experiment == ExperimentSolverPolicy.Experiment.CHRONOMATRON && !extras.experimentChronomatron) {
            return false;
        }
        if (experiment == ExperimentSolverPolicy.Experiment.ULTRASEQUENCER && !extras.experimentUltrasequencer) {
            return false;
        }
        return POLICY.shouldBlockClick(experiment, slotIndex);
    }

    /**
     * Advances the remembered sequence after a click the solver allowed through.
     */
    public static void onSlotClicked(AbstractContainerScreen<?> screen, int slotIndex) {
        if (!enabled() || screen != boundScreen || slotIndex < 0) {
            return;
        }
        switch (experiment) {
            case CHRONOMATRON -> POLICY.onChronomatronClick(slotIndex);
            case ULTRASEQUENCER -> POLICY.onUltrasequencerClick(slotIndex);
            default -> {
                return;
            }
        }
        highlights = POLICY.highlights(experiment);
    }

    public static boolean shouldHideTooltip(AbstractContainerScreen<?> screen) {
        if (!enabled() || screen != boundScreen || experiment == ExperimentSolverPolicy.Experiment.NONE) {
            return false;
        }
        return RotClientClient.qolConfigPublic().extras().experimentHideTooltip;
    }

    public static boolean shouldHideWrongSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (!enabled() || screen != boundScreen || slot == null || highlights.isEmpty()) {
            return false;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        boolean hide = switch (experiment) {
            case CHRONOMATRON -> extras.experimentHideWrongChrono;
            case ULTRASEQUENCER -> extras.experimentHideWrongUltra;
            default -> false;
        };
        if (!hide || slot.index < 0 || slot.index >= 54) {
            return false;
        }
        return !highlights.containsKey(slot.index);
    }

    private static boolean enabled() {
        return RotClientClient.qolConfigPublic().extras().experimentSolverEnabled;
    }

    private static void bind(AbstractContainerScreen<?> screen) {
        if (boundScreen == screen) {
            return;
        }
        boundScreen = screen;
        POLICY.reset();
        highlights = Map.of();
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        experiment = ExperimentSolverPolicy.experimentFor(title, onPrivateIsland());
    }

    private static boolean onPrivateIsland() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.experimentPrivateIslandOnly) {
            return true;
        }
        return SkyBlockSidebar.text().contains("Your Island");
    }

    /**
     * Only the menu's own slots; the player inventory rows are skipped so a dye in
     * the hotbar cannot pollute an Ultrasequencer solution.
     */
    private static List<ExperimentSolverPolicy.SlotUpdate> containerSnapshot(AbstractContainerScreen<?> screen) {
        List<ExperimentSolverPolicy.SlotUpdate> out = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        Object playerInventory = client == null || client.player == null
                ? null
                : client.player.getInventory();
        List<Slot> slots = screen.getMenu().slots;
        for (int index = 0; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            if (slot == null || slot.container == playerInventory) {
                continue;
            }
            out.add(snapshot(index, slot.getItem()));
        }
        return out;
    }

    private static ExperimentSolverPolicy.SlotUpdate snapshot(int slotIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ExperimentSolverPolicy.SlotUpdate(slotIndex, "air", "", 0, List.of(), "");
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return new ExperimentSolverPolicy.SlotUpdate(
                slotIndex,
                key == null ? "" : key.getPath(),
                stripFormatting(stack.getHoverName().getString()),
                stack.getCount(),
                InventoryChromeRuntime.loreLines(stack),
                textureId(stack));
    }

    private static String textureId(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile() == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Property property : profile.partialProfile().properties().get("textures")) {
            out.append(property.value());
        }
        return out.toString();
    }

    private static String stripFormatting(String text) {
        return text == null ? "" : text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
}
