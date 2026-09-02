package fi.rotclient;

/**
 * Public mixin boundary for text appearance settings.
 */
public final class TextAppearanceRuntime {
    private TextAppearanceRuntime() {
    }

    public static boolean fullTextShadowEnabled() {
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (config == null) {
            return false;
        }
        return config.isModuleEnabled("qol.render_optimizer")
                && config.extras().fullTextShadow;
    }
}
