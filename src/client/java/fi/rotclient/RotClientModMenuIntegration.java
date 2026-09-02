package fi.rotclient;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional Mod Menu integration. Loaded only when Mod Menu is present.
 * Opens the Rot Client dashboard from the Mod Menu configure button.
 */
public final class RotClientModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            TrackerConfig config = RotClientClient.trackerConfig();
            RotClientHud hud = RotClientClient.hud();
            if (config == null || hud == null) {
                return parent;
            }
            return new MiningUiScreen(config, hud, parent);
        };
    }
}
