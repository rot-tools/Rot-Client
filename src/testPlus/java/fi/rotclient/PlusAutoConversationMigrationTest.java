package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusAutoConversationMigrationTest {
    @Test
    void oldDialogueChoicesRemainUsableAndDefaultsSurviveReset() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.addProperty("autoConversationEnabled", true);
            qol.addProperty("autoConversationMulti", false);
            qol.addProperty("autoConversationGreen", false);
            qol.addProperty("autoConversationDelayTicks", 9);
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            AutoConversationSettings settings = AutoConversationSettings.from(restored.qolUtilities);
            assertTrue(settings.enabled());
            assertFalse(settings.multi());
            assertFalse(settings.green());
            assertEquals(9, settings.delayTicks());
            assertFalse(restored.qolUtilities.readBoolean("qol.auto_conversation.multi"));
            assertEquals(9.0, restored.qolUtilities.readNumber("qol.auto_conversation.delay"));

            restored.qolUtilities.writeBoolean("qol.auto_conversation.green", true);
            restored.qolUtilities.writeNumber("qol.auto_conversation.delay", 90);
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertTrue(AutoConversationSettings.from(saved.qolUtilities).green());
            assertEquals(40, AutoConversationSettings.from(saved.qolUtilities).delayTicks());
            assertFalse(AutoConversationSettings.from(saved.qolUtilities).multi());

            RotClientProfileSettings profile = RotClientProfileSettings.defaults();
            profile.qolUtilities = saved.qolUtilities;
            assertFalse(AutoConversationSettings.from(profile.copy().qolUtilities).multi());
            saved.qolUtilities.resetModuleToDefaults("qol.auto_conversation");
            AutoConversationSettings defaults = AutoConversationSettings.from(saved.qolUtilities);
            assertFalse(defaults.enabled());
            assertTrue(defaults.multi());
            assertTrue(defaults.green());
            assertEquals(AutoConversationPolicy.DEFAULT_DELAY_TICKS, defaults.delayTicks());
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
