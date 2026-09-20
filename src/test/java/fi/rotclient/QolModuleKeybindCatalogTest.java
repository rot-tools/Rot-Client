package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class QolModuleKeybindCatalogTest {
    @Test
    void everyToggleKeybindRowHasSomethingThatReadsIt() {
        List<String> orphaned = new ArrayList<>();
        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                if (setting.type() != QolUtilityCatalog.SettingType.KEYBIND
                        || !setting.description().startsWith("Toggle this module")) {
                    continue;
                }
                assertEquals(QolModuleKeybindCatalog.keybindSettingId(module.id()), setting.id());
                if (!QolModuleKeybindCatalog.TOGGLE_MODULE_IDS.contains(module.id())
                        && !QolModuleKeybindCatalog.HANDLED_ELSEWHERE.contains(module.id())) {
                    orphaned.add(module.id());
                }
            }
        }
        assertTrue(orphaned.isEmpty(), "Toggle keybind rows nothing reads: " + orphaned);
    }

    @Test
    void listedToggleModulesExistAndTheirKeyCanBeRead() {
        QolUtilityConfig config = new QolUtilityConfig();
        for (String moduleId : QolModuleKeybindCatalog.TOGGLE_MODULE_IDS) {
            assertNotNull(QolUtilityCatalog.findById(moduleId), moduleId);
            assertNotNull(
                    config.readKeybind(QolModuleKeybindCatalog.keybindSettingId(moduleId)),
                    moduleId);
        }
    }

    @Test
    void togglingAListedModuleFlipsItsEnabledState() {
        QolUtilityConfig config = new QolUtilityConfig();
        for (String moduleId : QolModuleKeybindCatalog.TOGGLE_MODULE_IDS) {
            boolean before = config.isModuleEnabled(moduleId);
            config.setModuleEnabled(moduleId, !before);
            assertEquals(!before, config.isModuleEnabled(moduleId), moduleId);
        }
    }
}
