package fi.rotclient;

/**
 * Persisted per-HUD look: background, text color, and scale.
 */
public final class HudStyleState {
    public boolean showBackground = true;
    public int backgroundColor = HudStylePolicy.DEFAULT_BG;
    public int textColor = HudStylePolicy.DEFAULT_TEXT;
    public float scale = HudStylePolicy.DEFAULT_SCALE;
}
