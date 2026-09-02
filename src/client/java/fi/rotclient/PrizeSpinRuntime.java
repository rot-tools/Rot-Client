package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay-only prize reel for dungeon reward chests and Vanguard chat dumps.
 * Does not click slots, buy chests, or send packets.
 */
public final class PrizeSpinRuntime {
    private static String chestKey = "";
    private static long chestSpinStartMs;
    private static List<String> chestReel = List.of();
    private static String chestType = "";
    private static long lastChestClickMs;

    private static boolean collectingVanguard;
    private static final List<String> vanguardItems = new ArrayList<>();
    private static long slotSpinStartMs;
    private static List<String> slotReel = List.of();
    private static long lastSlotClickMs;

    private PrizeSpinRuntime() {
    }

    static void tick(Minecraft client) {
        if (client == null || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            chestKey = "";
            chestSpinStartMs = 0L;
            chestReel = List.of();
            chestType = "";
        }
        if (slotSpinStartMs > 0L
                && System.currentTimeMillis() - slotSpinStartMs > PrizeSpinPolicy.SPIN_MILLIS + 8_000L) {
            slotSpinStartMs = 0L;
            slotReel = List.of();
        }
    }

    static void onChat(String text) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled || !qol.chatSlotMachine) {
            collectingVanguard = false;
            vanguardItems.clear();
            return;
        }
        PrizeSpinPolicy.ChatKind kind = PrizeSpinPolicy.classifyVanguardLine(collectingVanguard, text);
        if (kind == PrizeSpinPolicy.ChatKind.START) {
            collectingVanguard = true;
            vanguardItems.clear();
            return;
        }
        if (!collectingVanguard) {
            return;
        }
        if (kind == PrizeSpinPolicy.ChatKind.ITEM) {
            String name = PrizeSpinPolicy.vanguardItemName(text);
            if (!name.isBlank()) {
                vanguardItems.add(name);
            }
            return;
        }
        if (kind == PrizeSpinPolicy.ChatKind.END) {
            collectingVanguard = false;
            if (!vanguardItems.isEmpty()) {
                slotReel = PrizeSpinPolicy.spinReel(List.copyOf(vanguardItems), 0, PrizeSpinPolicy.REEL_LENGTH);
                slotSpinStartMs = System.currentTimeMillis();
                lastSlotClickMs = 0L;
            }
            vanguardItems.clear();
        }
    }

    public static void observe(AbstractContainerScreen<?> screen) {
        if (!chestEnabled() || screen == null) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        var type = PrizeSpinPolicy.rewardChestType(title);
        if (type.isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack != null && !stack.isEmpty()) {
                names.add(stack.getHoverName().getString());
            }
        }
        List<String> loot = PrizeSpinPolicy.lootNames(names);
        if (loot.isEmpty()) {
            return;
        }
        String key = type.get() + "|" + String.join("\n", loot);
        if (key.equals(chestKey)) {
            return;
        }
        chestKey = key;
        chestType = type.get();
        chestReel = PrizeSpinPolicy.spinReel(loot, 0, PrizeSpinPolicy.REEL_LENGTH);
        chestSpinStartMs = System.currentTimeMillis();
        lastChestClickMs = 0L;
    }

    public static boolean swallowClicks(AbstractContainerScreen<?> screen) {
        if (!chestEnabled() || screen == null || chestSpinStartMs <= 0L) {
            return false;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (PrizeSpinPolicy.rewardChestType(title).isEmpty()) {
            return false;
        }
        return System.currentTimeMillis() - chestSpinStartMs < PrizeSpinPolicy.SPIN_MILLIS;
    }

    public static void renderChest(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics) {
        if (!chestEnabled() || screen == null || graphics == null || chestReel.isEmpty()) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (PrizeSpinPolicy.rewardChestType(title).isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double progress = progress(now, chestSpinStartMs);
        clickTick(client, now, progress < 1.0D, true);
        drawReel(graphics, client.font, chestReel, progress, chestType + " Chest", true);
    }

    public static void renderHud(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || graphics == null || slotReel.isEmpty() || slotSpinStartMs <= 0L) {
            return;
        }
        if (client.gui != null && client.gui.screen() instanceof AbstractContainerScreen<?>) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled || !qol.chatSlotMachine) {
            return;
        }
        long now = System.currentTimeMillis();
        double progress = progress(now, slotSpinStartMs);
        clickTick(client, now, progress < 1.0D, false);
        drawReel(graphics, client.font, slotReel, progress, "Vanguard", false);
        if (progress >= 1.0D && now - slotSpinStartMs > PrizeSpinPolicy.SPIN_MILLIS + 6_000L) {
            slotSpinStartMs = 0L;
            slotReel = List.of();
        }
    }

    private static boolean chestEnabled() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return extras.dungeonMenusEnabled && extras.dungeonMenusChestSpin;
    }

    private static double progress(long now, long start) {
        if (start <= 0L) {
            return 1.0D;
        }
        return Math.min(1.0D, (now - start) / (double) PrizeSpinPolicy.SPIN_MILLIS);
    }

    private static void clickTick(Minecraft client, long now, boolean spinning, boolean chest) {
        if (!spinning || client.player == null) {
            return;
        }
        long last = chest ? lastChestClickMs : lastSlotClickMs;
        if (now - last < 70L) {
            return;
        }
        if (chest) {
            lastChestClickMs = now;
        } else {
            lastSlotClickMs = now;
        }
        client.player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.35F, 1.6F);
    }

    private static void drawReel(
            GuiGraphicsExtractor graphics,
            Font font,
            List<String> reel,
            double progress,
            String heading,
            boolean dimWorld) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        if (dimWorld) {
            graphics.fill(0, 0, width, height, 0xAA070B14);
        }
        int focused = PrizeSpinPolicy.focusedIndex(progress, reel.size());
        int boxW = 118;
        int boxH = 36;
        int gap = 8;
        int visible = 5;
        int rowY = height / 2 - boxH / 2;
        int totalW = visible * boxW + (visible - 1) * gap;
        int rowX = (width - totalW) / 2;
        RotClientUiDraw.text(
                graphics,
                font,
                heading,
                (width - font.width(heading)) / 2,
                rowY - 28,
                0xFFFFE082,
                true);
        for (int i = 0; i < visible; i++) {
            int reelIndex = focused - 2 + i;
            int x = rowX + i * (boxW + gap);
            boolean center = i == 2;
            graphics.fill(x, rowY, x + boxW, rowY + boxH, center ? 0xFF1E3A5F : 0xFF111827);
            graphics.fill(x, rowY, x + boxW, rowY + 2, center ? 0xFFFACC15 : 0xFF334155);
            if (reelIndex < 0 || reelIndex >= reel.size()) {
                continue;
            }
            String label = reel.get(reelIndex);
            if (font.width(label) > boxW - 10) {
                while (label.length() > 3 && font.width(label + "…") > boxW - 10) {
                    label = label.substring(0, label.length() - 1);
                }
                label = label + "…";
            }
            RotClientUiDraw.text(
                    graphics,
                    font,
                    label,
                    x + 6,
                    rowY + 12,
                    center ? 0xFFFFFFFF : 0xFF94A3B8,
                    false);
        }
        if (progress >= 1.0D && focused >= 0 && focused < reel.size()) {
            String winner = reel.get(focused);
            RotClientUiDraw.text(
                    graphics,
                    font,
                    winner,
                    (width - font.width(winner)) / 2,
                    rowY + boxH + 16,
                    0xFF4ADE80,
                    true);
        }
    }
}
