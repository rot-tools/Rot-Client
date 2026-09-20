package fi.rotclient;

import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Edge-triggered module toggle binds for keybind rows that are
 * otherwise only stored in the config.
 */
public final class QolModuleKeybindRuntime {
    private static final List<String> MODULE_IDS = QolModuleKeybindCatalog.TOGGLE_MODULE_IDS;
    private static final boolean[] WAS_DOWN = new boolean[MODULE_IDS.size()];

    private QolModuleKeybindRuntime() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            reset();
            return;
        }
        if (client.gui != null && client.gui.screen() != null) {
            reset();
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        long window = client.getWindow().handle();
        QolClientFlavorSupport.hooks().plusModuleKeybindTick(client);
        for (int i = 0; i < MODULE_IDS.size(); i++) {
            String moduleId = MODULE_IDS.get(i);
            WAS_DOWN[i] = fire(
                    window,
                    qol.readKeybind(QolModuleKeybindCatalog.keybindSettingId(moduleId)),
                    WAS_DOWN[i],
                    moduleId);
        }
    }

    private static boolean fire(
            long window,
            String keyName,
            boolean wasDown,
            String moduleId) {
        boolean down = QolKeybindNames.isBoundDown(window, keyName);
        if (down && !wasDown) {
            QolUtilityConfig qol = RotClientClient.qolConfigPublic();
            boolean next = !qol.isModuleEnabled(moduleId);
            qol.setModuleEnabled(moduleId, next);
            TrackerStore.save(RotClientClient.trackerConfig());
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(moduleId);
            RotClientClient.notifyQolModuleToggled(
                    module == null ? moduleId : module.name(),
                    next);
        }
        return down;
    }

    private static void reset() {
        java.util.Arrays.fill(WAS_DOWN, false);
    }
}
