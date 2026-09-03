package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * Hover-box pan: wheel deltas move the already screen-clamped tooltip.
 */
public final class CustomTooltipRuntime {
    private static int horizontal;
    private static int vertical;
    private static String lastIdentity = "";

    private CustomTooltipRuntime() {
    }

    static void clear() {
        horizontal = 0;
        vertical = 0;
        lastIdentity = "";
    }

    public static boolean mouseScrolled(
            double horizontalAmount,
            double verticalAmount,
            ItemStack hovered,
            boolean storageOverlay,
            boolean shiftHeld) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.customTooltipEnabled) {
            return false;
        }
        if (hovered == null || hovered.isEmpty()) {
            clear();
            return false;
        }
        int hDelta = 0;
        int vDelta = 0;
        if (CustomTooltipPolicy.panTooltipHorizontally(
                storageOverlay, shiftHeld, qol.customTooltipHorizontal)) {
            hDelta = (int) Math.round(horizontalAmount != 0.0D ? horizontalAmount : verticalAmount);
            if (hDelta == 0 && verticalAmount != 0.0D) {
                hDelta = verticalAmount > 0.0D ? 1 : -1;
            }
        } else if (qol.customTooltipVertical
                && CustomTooltipPolicy.panTooltipVertically(storageOverlay, shiftHeld)) {
            vDelta = CustomTooltipPolicy.verticalWheelDelta(verticalAmount);
        }
        if (hDelta == 0 && vDelta == 0) {
            return false;
        }
        horizontal = CustomTooltipPolicy.nextOffset(
                horizontal, hDelta, qol.customTooltipHorizontalSpeed,
                qol.customTooltipHorizontal, qol.customTooltipInfinite, 240);
        vertical = CustomTooltipPolicy.nextOffset(
                vertical, vDelta, qol.customTooltipVerticalSpeed,
                qol.customTooltipVertical, qol.customTooltipInfinite, 2000);
        return true;
    }

    public static boolean mouseScrolled(double horizontalAmount, double verticalAmount, ItemStack hovered) {
        return mouseScrolled(horizontalAmount, verticalAmount, hovered, false, false);
    }

    public static boolean shouldStealWheel(boolean overOccupiedSlot) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.customTooltipEnabled) {
            return false;
        }
        if (!qol.customTooltipHorizontal && !qol.customTooltipVertical) {
            return false;
        }
        return overOccupiedSlot || !lastIdentity.isEmpty();
    }

    public static Vector2ic afterScreenClamp(Vector2ic clamped) {
        return afterScreenClamp(clamped, 0, 0, 0, 0);
    }

    public static Vector2ic afterScreenClamp(
            Vector2ic clamped,
            int screenWidth,
            int screenHeight,
            int tooltipWidth,
            int tooltipHeight) {
        if (clamped == null || (horizontal == 0 && vertical == 0)) {
            return clamped;
        }
        if (screenWidth <= 0 || screenHeight <= 0) {
            return new Vector2i(
                    CustomTooltipPolicy.panAfterClamp(clamped.x(), horizontal),
                    CustomTooltipPolicy.panAfterClamp(clamped.y(), vertical));
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        boolean infinite = qol != null && qol.customTooltipInfinite;
        return new Vector2i(
                CustomTooltipPolicy.keepOnScreen(
                        clamped.x(), horizontal, screenWidth, tooltipWidth, infinite),
                CustomTooltipPolicy.keepOnScreen(
                        clamped.y(), vertical, screenHeight, tooltipHeight, infinite));
    }

    public static void beforeTooltip(ItemStack hovered) {
        if (hovered == null || hovered.isEmpty()) {
            clear();
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.customTooltipEnabled) {
            clear();
            return;
        }
        String identity = AutoClickerItemIdentity.identify(hovered);
        if (!identity.equals(lastIdentity)) {
            horizontal = 0;
            vertical = 0;
        }
        lastIdentity = identity;
    }

    public static int shiftedX(int mouseX) {
        return CustomTooltipPolicy.panAfterClamp(mouseX, horizontal);
    }

    public static int shiftedY(int mouseY) {
        return CustomTooltipPolicy.panAfterClamp(mouseY, vertical);
    }

    public static boolean onlyName() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        boolean unbound = qol.customTooltipOnlyNameKey == null || qol.customTooltipOnlyNameKey.isBlank();
        boolean held = client != null
                && client.getWindow() != null
                && QolKeybindNames.isBoundDown(
                        client.getWindow().handle(), qol.customTooltipOnlyNameKey);
        return CustomTooltipPolicy.showOnlyName(qol.customTooltipEnabled, unbound, held);
    }
}
