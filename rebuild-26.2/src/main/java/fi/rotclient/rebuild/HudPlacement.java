package fi.rotclient.rebuild;

/** Pixel offsets relative to Minecraft's live anchor and a multiplier around that anchor. */
record HudPlacement(double x, double y, double scale) {
    static final HudPlacement DEFAULT = new HudPlacement(0, 0, 1);
    static final double MIN_SCALE = 0.5;
    static final double MAX_SCALE = 2;

    HudPlacement {
        x = InterfaceSettings.clamp(x, -4096, 4096, 0);
        y = InterfaceSettings.clamp(y, -4096, 4096, 0);
        scale = InterfaceSettings.clamp(scale, MIN_SCALE, MAX_SCALE, 1);
    }

    HudPlacement move(double dx, double dy) {
        return new HudPlacement(x + dx, y + dy, scale);
    }

    HudPlacement resize(double factor) {
        return new HudPlacement(x, y, Math.round((scale + factor) * 100) / 100d);
    }
}
