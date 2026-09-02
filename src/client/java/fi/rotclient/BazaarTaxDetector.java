package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BazaarTaxDetector {
    private static final Pattern TAX = Pattern.compile("(?i)Current\\s+tax:\\s*([\\d.]+)%");
    private int ticks;

    void tick(Minecraft client) {
        if (++ticks % 20 != 0) return;
        ClientBoundaryGuard.run("BAZAAR_TAX_SCAN", () -> {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                return;
            }

            for (Slot slot : screen.getMenu().slots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;
                ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                for (Component line : lore.lines()) {
                    String plain = line.getString();
                    Matcher taxMatcher = TAX.matcher(plain);
                    if (taxMatcher.find()) {
                        try {
                            double percent = Double.parseDouble(taxMatcher.group(1));
                            if (percent >= 0 && percent <= 10) {
                                RotClientClient.onBazaarTaxDetected(percent);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }

                }
            }
        });
    }
}
