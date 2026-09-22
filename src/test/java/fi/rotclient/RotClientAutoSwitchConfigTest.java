package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RotClientAutoSwitchConfigTest {

    private static final Gson GSON = new Gson();

    private static RotClientProfileConfig configWith(String... names) {
        RotClientProfileConfig config = RotClientProfileConfig.defaults();
        for (String name : names) {
            config.profiles.add(RotClientProfile.create(name));
        }
        config.normalize();
        return config;
    }

    @Test
    void isOffByDefaultAndNotifiesWhenTurnedOn() {
        RotClientAutoSwitchConfig rules = new RotClientAutoSwitchConfig();

        assertFalse(rules.enabled);
        assertTrue(rules.notify);
        assertTrue(rules.rules.isEmpty());
        assertEquals("", rules.fallbackProfileId);
    }

    @Test
    void ruleBeatsFallbackAndFallbackCoversEverythingElse() {
        RotClientAutoSwitchConfig rules = new RotClientAutoSwitchConfig();
        rules.enabled = true;
        rules.setRule(AutoProfileContext.DUNGEONS, "dungeon-id");
        rules.fallbackProfileId = "general-id";

        assertEquals("dungeon-id",
                rules.targetFor(AutoProfileContext.DUNGEONS));
        assertEquals("general-id",
                rules.targetFor(AutoProfileContext.GLACITE));
        assertEquals("general-id",
                rules.targetFor(AutoProfileContext.OTHER));
    }

    @Test
    void noRuleAndNoFallbackMeansLeaveTheProfileAlone() {
        RotClientAutoSwitchConfig rules = new RotClientAutoSwitchConfig();
        rules.enabled = true;

        assertEquals("", rules.targetFor(AutoProfileContext.KUUDRA));
        assertEquals("", rules.switchTargetFor(AutoProfileContext.KUUDRA, "a"));
    }

    @Test
    void switchTargetRespectsEnabledAndAlreadyActive() {
        RotClientAutoSwitchConfig rules = new RotClientAutoSwitchConfig();
        rules.setRule(AutoProfileContext.DUNGEONS, "dungeon-id");

        assertEquals("",
                rules.switchTargetFor(AutoProfileContext.DUNGEONS, "other"),
                "disabled must never switch");

        rules.enabled = true;

        assertEquals("dungeon-id",
                rules.switchTargetFor(AutoProfileContext.DUNGEONS, "other"));
        assertEquals("",
                rules.switchTargetFor(AutoProfileContext.DUNGEONS, "dungeon-id"),
                "already active must not re-switch");
        assertEquals("dungeon-id",
                rules.switchTargetFor(AutoProfileContext.DUNGEONS, ""),
                "no active profile still switches");
        assertEquals("",
                rules.switchTargetFor(null, "other"));
    }

    @Test
    void blankProfileIdClearsARule() {
        RotClientAutoSwitchConfig rules = new RotClientAutoSwitchConfig();

        rules.setRule(AutoProfileContext.GARDEN, "p");
        assertEquals("p", rules.ruleFor(AutoProfileContext.GARDEN));

        rules.setRule(AutoProfileContext.GARDEN, "  ");
        assertEquals("", rules.ruleFor(AutoProfileContext.GARDEN));
        assertTrue(rules.rules.isEmpty());
    }

    @Test
    void cyclingWalksNotSetThenEachProfileThenBackToNotSet() {
        List<String> ids = List.of("a", "b", "c");

        assertEquals("a", RotClientAutoSwitchConfig.nextProfileId(ids, ""));
        assertEquals("a", RotClientAutoSwitchConfig.nextProfileId(ids, null));
        assertEquals("b", RotClientAutoSwitchConfig.nextProfileId(ids, "a"));
        assertEquals("c", RotClientAutoSwitchConfig.nextProfileId(ids, "b"));
        assertEquals("", RotClientAutoSwitchConfig.nextProfileId(ids, "c"));
        assertEquals("a", RotClientAutoSwitchConfig.nextProfileId(ids, "gone"));
        assertEquals("", RotClientAutoSwitchConfig.nextProfileId(List.of(), "a"));
        assertEquals("", RotClientAutoSwitchConfig.nextProfileId(null, "a"));
    }

    @Test
    void normalizeDropsRulesForDeletedProfilesButKeepsUnknownContexts() {
        RotClientProfileConfig config = configWith("Mining", "Dungeon");
        String mining = config.profiles.get(0).id;
        String dungeon = config.profiles.get(1).id;

        config.autoSwitch.setRule(AutoProfileContext.DWARVEN_MINES, mining);
        config.autoSwitch.setRule(AutoProfileContext.DUNGEONS, dungeon);
        config.autoSwitch.rules.put("future_context", mining);
        config.autoSwitch.fallbackProfileId = dungeon;

        config.profiles.remove(1);
        config.normalize();

        assertEquals(mining,
                config.autoSwitch.ruleFor(AutoProfileContext.DWARVEN_MINES));
        assertEquals("",
                config.autoSwitch.ruleFor(AutoProfileContext.DUNGEONS));
        assertEquals("", config.autoSwitch.fallbackProfileId);
        assertEquals(mining, config.autoSwitch.rules.get("future_context"),
                "a newer build's context id must survive");
    }

    @Test
    void deletingEveryProfileClearsAllRules() {
        RotClientProfileConfig config = configWith("Only");
        config.autoSwitch.setRule(
                AutoProfileContext.GARDEN, config.profiles.get(0).id);
        config.autoSwitch.fallbackProfileId = config.profiles.get(0).id;

        config.profiles.clear();
        config.normalize();

        assertTrue(config.autoSwitch.rules.isEmpty());
        assertEquals("", config.autoSwitch.fallbackProfileId);
    }

    @Test
    void oldProfileFilesWithoutTheSectionLoadWithDefaults() {
        String legacy = "{\"schemaVersion\":1,\"activeProfileId\":\"\","
                + "\"profiles\":[]}";

        RotClientProfileConfig loaded =
                GSON.fromJson(legacy, RotClientProfileConfig.class);
        loaded.normalize();

        assertNotNull(loaded.autoSwitch);
        assertFalse(loaded.autoSwitch.enabled);
        assertTrue(loaded.autoSwitch.notify);
    }

    @Test
    void damagedSectionIsRepairedInsteadOfCrashing() {
        String damaged = "{\"schemaVersion\":1,\"profiles\":[],"
                + "\"autoSwitch\":{\"enabled\":true,\"fallbackProfileId\":null,"
                + "\"rules\":null}}";

        RotClientProfileConfig loaded =
                GSON.fromJson(damaged, RotClientProfileConfig.class);
        loaded.normalize();

        assertTrue(loaded.autoSwitch.enabled);
        assertEquals("", loaded.autoSwitch.fallbackProfileId);
        assertNotNull(loaded.autoSwitch.rules);
    }

    @Test
    void roundTripsThroughJson() {
        RotClientProfileConfig config = configWith("Mining", "Dungeon");
        config.autoSwitch.enabled = true;
        config.autoSwitch.notify = false;
        config.autoSwitch.setRule(
                AutoProfileContext.DUNGEONS, config.profiles.get(1).id);
        config.autoSwitch.fallbackProfileId = config.profiles.get(0).id;

        RotClientProfileConfig loaded = GSON.fromJson(
                GSON.toJson(config), RotClientProfileConfig.class);
        loaded.normalize();

        assertTrue(loaded.autoSwitch.enabled);
        assertFalse(loaded.autoSwitch.notify);
        assertEquals(config.profiles.get(1).id,
                loaded.autoSwitch.ruleFor(AutoProfileContext.DUNGEONS));
        assertEquals(config.profiles.get(0).id,
                loaded.autoSwitch.fallbackProfileId);
    }

    @Test
    void copyIsIndependent() {
        RotClientAutoSwitchConfig original = new RotClientAutoSwitchConfig();
        original.setRule(AutoProfileContext.KUUDRA, "k");

        RotClientAutoSwitchConfig copy = original.copy();
        copy.setRule(AutoProfileContext.KUUDRA, "");
        copy.enabled = true;

        assertEquals("k", original.ruleFor(AutoProfileContext.KUUDRA));
        assertFalse(original.enabled);
    }
}
