package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QolUnknownFieldPreserverTest {
    @Test
    void liteRoundTripRetainsUnknownQolSettingsAsOpaqueJson() {
        JsonObject original = TrackerStore.toJson(new TrackerConfig());
        JsonObject qol = original.getAsJsonObject("qolUtilities");
        JsonObject unknown = JsonParser.parseString(
                "{\"enabled\":true,\"options\":[1,\"keep\"]}").getAsJsonObject();
        qol.add("futureEditionModule", unknown);
        qol.addProperty("inventoryWalkEnabled", true);
        qol.addProperty("inventoryWalkPingMs", 320);
        qol.addProperty("autoConversationEnabled", true);
        qol.addProperty("autoConversationMulti", false);
        qol.addProperty("autoConversationDelayTicks", 9);
        qol.addProperty("trajectoriesEnabled", true);
        qol.addProperty("trajectoriesRange", 47);
        qol.addProperty("worldScannerEnabled", true);
        qol.addProperty("worldScannerOnlyHollows", false);
        qol.addProperty("mobHighlightEnabled", true);
        qol.addProperty("etherwarpDepth", false);
        qol.addProperty("mobHighlightAddKey", "H");
        qol.add("mobHighlightNames", JsonParser.parseString("[\"Goblin\"]"));
        qol.add("worldScannerTargets", JsonParser.parseString(
                "{\"fairy\":{\"enabled\":false,\"custom\":\"keep\"}}").getAsJsonObject());
        qol.addProperty("commissionDisplayTitle", "Known field");
        JsonObject extras = qol.getAsJsonObject("extras");
        extras.addProperty("experimentSolverEnabled", true);
        extras.addProperty("experimentFirstColor", 0x80123456);
        extras.add("futureExtras", JsonParser.parseString("{\"value\":\"keep\"}"));
        JsonObject athen = extras.getAsJsonObject("athen");
        athen.addProperty("termClickEnabled", true);
        athen.addProperty("termClickRadius", 9);
        athen.addProperty("termClickLeftColor", 0xFF123456);

        TrackerConfig loaded = TrackerStore.fromJson(original);
        assertEquals(unknown, loaded.qolUtilities.extensionFields.get("futureEditionModule"));
        assertTrue(loaded.qolUtilities.extensionFields.get("inventoryWalkEnabled").getAsBoolean());
        assertEquals(320, loaded.qolUtilities.extensionFields.get("inventoryWalkPingMs").getAsInt());
        assertTrue(loaded.qolUtilities.extensionFields.get("autoConversationEnabled").getAsBoolean());
        assertFalse(loaded.qolUtilities.extensionFields.get("autoConversationMulti").getAsBoolean());
        assertEquals(9, loaded.qolUtilities.extensionFields.get("autoConversationDelayTicks").getAsInt());
        assertTrue(loaded.qolUtilities.extensionFields.get("trajectoriesEnabled").getAsBoolean());
        assertEquals(47, loaded.qolUtilities.extensionFields.get("trajectoriesRange").getAsInt());
        assertTrue(loaded.qolUtilities.extensionFields.get("worldScannerEnabled").getAsBoolean());
        assertFalse(loaded.qolUtilities.extensionFields.get("worldScannerOnlyHollows").getAsBoolean());
        assertTrue(loaded.qolUtilities.extensionFields.get("mobHighlightEnabled").getAsBoolean());
        assertFalse(loaded.qolUtilities.extensionFields.get("etherwarpDepth").getAsBoolean());
        assertEquals("H", loaded.qolUtilities.extensionFields.get("mobHighlightAddKey").getAsString());
        assertEquals("keep", loaded.qolUtilities.extensionFields
                .getAsJsonObject("worldScannerTargets").getAsJsonObject("fairy")
                .get("custom").getAsString());
        assertFalse(loaded.qolUtilities.extensionFields.has("commissionDisplayTitle"));
        assertTrue(loaded.qolUtilities.extras().extensionFields
                .get("experimentSolverEnabled").getAsBoolean());
        assertEquals(0x80123456, loaded.qolUtilities.extras().extensionFields
                .get("experimentFirstColor").getAsInt());
        assertEquals("keep", loaded.qolUtilities.extras().extensionFields
                .getAsJsonObject("futureExtras").get("value").getAsString());
        assertTrue(loaded.qolUtilities.extras().athen().extensionFields
                .get("termClickEnabled").getAsBoolean());
        assertEquals(9, loaded.qolUtilities.extras().athen().extensionFields
                .get("termClickRadius").getAsInt());
        assertTrue(qol.has("futureEditionModule"));
        assertFalse(qol.getAsJsonObject("extensionFields").has("futureEditionModule"));

        JsonObject saved = TrackerStore.toJson(loaded);
        assertEquals(unknown, saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").get("futureEditionModule"));
        assertEquals(320, saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").get("inventoryWalkPingMs").getAsInt());
        assertEquals(9, saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").get("autoConversationDelayTicks").getAsInt());
        assertEquals(47, saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").get("trajectoriesRange").getAsInt());
        assertEquals("Goblin", saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").getAsJsonArray("mobHighlightNames")
                .get(0).getAsString());
        assertTrue(saved.getAsJsonObject("qolUtilities").getAsJsonObject("extras")
                .getAsJsonObject("extensionFields").get("experimentSolverEnabled")
                .getAsBoolean());
        assertEquals(0xFF123456, saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extras").getAsJsonObject("athen")
                .getAsJsonObject("extensionFields").get("termClickLeftColor").getAsInt());
        assertEquals("keep", saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").getAsJsonObject("worldScannerTargets")
                .getAsJsonObject("fairy").get("custom").getAsString());
        assertEquals(unknown, TrackerStore.fromJson(saved).qolUtilities
                .extensionFields.get("futureEditionModule"));
    }

    @Test
    void savedProfilesAlsoRetainUnknownQolSettings() {
        JsonObject root = JsonParser.parseString("""
                {"profiles":[{"id":"one","name":"A","settings":{
                    "qolUtilities":{"futureEditionModule":{"mode":"keep"}}
                }}]}
                """).getAsJsonObject();
        QolUnknownFieldPreserver.preserveProfiles(root);

        RotClientProfileConfig loaded = new Gson().fromJson(root, RotClientProfileConfig.class);
        JsonObject opaque = loaded.profiles.get(0).settings.qolUtilities.extensionFields;
        assertEquals("keep", opaque.getAsJsonObject("futureEditionModule")
                .get("mode").getAsString());
        assertTrue(new Gson().toJsonTree(loaded).getAsJsonObject()
                .getAsJsonArray("profiles").get(0).getAsJsonObject()
                .getAsJsonObject("settings").getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields").has("futureEditionModule"));
    }
}
