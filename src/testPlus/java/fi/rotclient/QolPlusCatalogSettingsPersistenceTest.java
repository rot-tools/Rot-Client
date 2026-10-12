package fi.rotclient;

/** Runs the same configuration contract with the Plus catalog/provider installed. */
class QolPlusCatalogSettingsPersistenceTest extends QolCatalogSettingsPersistenceTest {
    @org.junit.jupiter.api.Test
    void headerSearchRebuildsAfterThePlusProviderIsInstalled() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        try {
            QolFlavorSupport.install(QolFlavorExtension.NONE);
            org.junit.jupiter.api.Assertions.assertNull(RotClientSettingsIndex.findById("qol.auto_clicker"));
            QolFlavorSupport.install(new RotClientPlusExtension());
            org.junit.jupiter.api.Assertions.assertNotNull(RotClientSettingsIndex.findById("qol.auto_clicker"));
            org.junit.jupiter.api.Assertions.assertTrue(RotClientSettingsIndex.search("Auto Clicker").stream()
                    .anyMatch(entry -> entry.id().equals("qol.auto_clicker")));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
