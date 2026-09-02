package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Subpixel compositor for integer Minecraft GUI fills. Layout and scissors stay
 * on rounded pixels; the leftover fraction is a pose translate so 60/144 Hz
 * scrolling does not stair-step a whole pixel each frame.
 */
final class RotClientUiMotion {
    private RotClientUiMotion() {
    }

    static float fractionalPixel(double value) {
        if (!Double.isFinite(value)) {
            return 0.0F;
        }
        return (float) (value - Math.round(value));
    }

    static void pushFractionalScroll(GuiGraphicsExtractor graphics, RotClientScrollState scroll) {
        if (graphics == null) {
            return;
        }
        graphics.pose().pushMatrix();
        if (scroll == null) {
            return;
        }
        float frac = (float) (scroll.displayedScroll() - scroll.scrollPixels());
        if (frac != 0.0F) {
            graphics.pose().translate(0.0F, -frac);
        }
    }

    static void pop(GuiGraphicsExtractor graphics) {
        if (graphics != null) {
            graphics.pose().popMatrix();
        }
    }
}
