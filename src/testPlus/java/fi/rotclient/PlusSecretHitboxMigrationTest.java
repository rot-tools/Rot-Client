package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusSecretHitboxMigrationTest {
    @Test
    void plusRestoresMissingDefaultsButKeepsExplicitOffChoices() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            var module = QolPlusCatalog.extraModules().stream()
                    .filter(entry -> "qol.secret_hitboxes".equals(entry.id()))
                    .findFirst().orElseThrow();
            assertTrue(QolUtilityCatalog.hasCheatTag(module));

            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.addProperty("secretHitboxesEnabled", true);
            qol.addProperty("secretHitboxesChests", false);

            TrackerConfig restored = TrackerStore.fromJson(legacy);
            SecretHitboxesSettings settings = SecretHitboxesSettings.from(restored.qolUtilities);
            assertTrue(settings.enabled());
            assertTrue(settings.lever());
            assertTrue(settings.button());
            assertTrue(settings.skull());
            assertFalse(settings.chests());
            assertFalse(qol.has("secretHitboxesLever"));
            assertFalse(qol.has("secretHitboxesButton"));
            assertFalse(qol.has("secretHitboxesSkull"));

            JsonObject explicitOff = TrackerStore.toJson(new TrackerConfig());
            JsonObject oldQol = explicitOff.getAsJsonObject("qolUtilities");
            oldQol.addProperty("secretHitboxesLever", false);
            oldQol.addProperty("secretHitboxesButton", false);
            oldQol.addProperty("secretHitboxesSkull", false);
            TrackerConfig kept = TrackerStore.fromJson(explicitOff);
            SecretHitboxesSettings off = SecretHitboxesSettings.from(kept.qolUtilities);
            assertFalse(off.lever());
            assertFalse(off.button());
            assertFalse(off.skull());

            kept.qolUtilities.setModuleEnabled("qol.secret_hitboxes", true);
            kept.qolUtilities.writeBoolean("qol.secret_hitboxes.chests", true);
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(kept));
            assertTrue(SecretHitboxesSettings.from(saved.qolUtilities).enabled());
            assertTrue(SecretHitboxesSettings.from(saved.qolUtilities).chests());
            assertFalse(SecretHitboxesSettings.from(saved.qolUtilities).lever());

            RotClientProfileSettings profile = RotClientProfileSettings.defaults();
            profile.qolUtilities = kept.qolUtilities;
            assertFalse(SecretHitboxesSettings.from(profile.copy().qolUtilities).lever());

            JsonObject oldProfiles = com.google.gson.JsonParser.parseString("""
                    {"profiles":[{"id":"one","name":"Old","settings":{
                        "qolUtilities":{"secretHitboxesLever":false}
                    }}]}
                    """).getAsJsonObject();
            QolUnknownFieldPreserver.preserveProfiles(oldProfiles);
            RotClientProfileConfig migratedProfiles = new com.google.gson.Gson()
                    .fromJson(oldProfiles, RotClientProfileConfig.class);
            assertFalse(SecretHitboxesSettings.from(migratedProfiles.profiles.get(0)
                    .settings.qolUtilities).lever());
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
