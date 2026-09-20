package fi.rotclient;

/**
 * Shape of the standard HUD panel (Pet HUD, Mining HUD): rounded corners, a
 * soft offset shadow and a thin border. The fill colour is not fixed here; it
 * comes from the HUD Layout style ({@link HudStyleState#backgroundColor}).
 */
final class HudCardStyle {
    static final int RADIUS = 5;
    static final int SHADOW_OFFSET_X = 2;
    static final int SHADOW_OFFSET_Y = 3;
    static final int SHADOW_ALPHA = 0x50;
    /** Lighter shadow while the HUD editor is open. */
    static final int EDITOR_SHADOW_ALPHA = 0x38;
    static final int BORDER_ALPHA = 0xA8;
    /** Track behind progress bars (Pet HUD XP bar, Mining auto-pause bar). */
    static final int BAR_TRACK = 0x66333333;

    private HudCardStyle() {
    }
}
