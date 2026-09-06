package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RingPolicyTest {
    @Test
    void visualEditorFormatRoundTripsOrderedActionsAndDelays() {
        RingPolicy.MacroDef original = new RingPolicy.MacroDef(
                "P",
                "LEFT_SHIFT",
                "",
                List.of(
                        new RingPolicy.MacroMessage("/pets", 0),
                        new RingPolicy.MacroMessage("/warp hub", 20)),
                RingPolicy.MODE_SEND,
                RingPolicy.STRATEGY_ASSERT,
                RingPolicy.ACTIVATION_HOLD,
                6,
                0,
                false,
                true);
        String encoded = RingPolicy.serializeMacroList(List.of(original));
        List<RingPolicy.MacroDef> decoded = RingPolicy.parseMacroList(
                encoded,
                RingPolicy.MODE_SEND,
                RingPolicy.STRATEGY_ASSERT,
                RingPolicy.ACTIVATION_HOLD,
                true);
        assertEquals(List.of(original), decoded);
        assertEquals("", RingPolicy.validationError(decoded.getFirst()));
    }

    @Test
    void policyStaysMinecraftFree() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/fi/rotclient/RingPolicy.java"),
                StandardCharsets.UTF_8);
        assertFalse(source.contains("net.minecraft"));
    }

    @Test
    void skyblockPresetsKeepHypixelArmorAndLoadoutCommands() {
        assertEquals("armor", RingPolicy.ARMOR_COMMAND);
        assertEquals("loadout", RingPolicy.LOADOUT_COMMAND);
        assertEquals("equipment", RingPolicy.EQUIP_COMMAND);
        List<RingPolicy.MacroDef> presets = RingPolicy.skyblockPresets(
                "P", "K", "H", "E", "L", "N", "G", "B",
                RingPolicy.MODE_SEND,
                RingPolicy.STRATEGY_ASSERT,
                RingPolicy.ACTIVATION_HOLD,
                true);
        assertTrue(presets.stream().anyMatch(macro ->
                "/armor".equals(macro.messages().getFirst().text()) && macro.skyblockOnly()));
        assertTrue(presets.stream().anyMatch(macro ->
                "/loadout".equals(macro.messages().getFirst().text()) && macro.skyblockOnly()));
        assertFalse(presets.stream().anyMatch(macro ->
                macro.messages().getFirst().text().contains("wardrobe")));
    }

    @Test
    void parseEqualsAndPipeMacros() {
        RingPolicy.MacroDef pets = RingPolicy.parseMacroLine(
                "P=/pets", "SEND", "ASSERT", "HOLD");
        assertNotNull(pets);
        assertTrue(RingPolicy.sameBind("P", pets.key()));
        assertEquals("/pets", pets.messages().getFirst().text());
        assertEquals(RingPolicy.MODE_SEND, pets.sendMode());

        RingPolicy.MacroDef typed = RingPolicy.parseMacroLine(
                "R=/s hello | TYPE", "SEND", "ASSERT", "HOLD");
        assertNotNull(typed);
        assertEquals("/s hello", typed.messages().getFirst().text());
        assertEquals(RingPolicy.MODE_TYPE, typed.sendMode());

        RingPolicy.MacroDef limited = RingPolicy.parseMacroLine(
                "CTRL+G | /warp dungeon_hub | SEND | VETO | VANILLA",
                "SEND",
                "ASSERT",
                "HOLD");
        assertNotNull(limited);
        assertTrue(RingPolicy.sameBind("G", limited.key()));
        assertTrue(RingPolicy.sameBind("CTRL", limited.limitKey()));
        assertEquals(RingPolicy.STRATEGY_VETO, limited.conflictStrategy());
        assertEquals(RingPolicy.ACTIVATION_VANILLA, limited.activationType());
    }

    @Test
    void customMacroWinsOverPresetOnTheSameKey() {
        List<RingPolicy.MacroDef> custom = RingPolicy.parseMacroList(
                "P | /ah",
                "SEND",
                "ASSERT",
                "HOLD",
                true);
        List<RingPolicy.MacroDef> merged = RingPolicy.mergeMacros(
                custom,
                RingPolicy.skyblockPresets(
                        "P", "", "", "", "", "", "", "",
                        "SEND",
                        "ASSERT",
                        "HOLD",
                        true));
        assertEquals(1, merged.size());
        assertEquals("/ah", merged.getFirst().messages().getFirst().text());
        assertFalse(merged.getFirst().skyblockOnly());
    }

    @Test
    void cycleWalksMessagesAndAltReverses() {
        RingPolicy.Session session = new RingPolicy.Session(new Random(1L));
        session.sync(List.of(RingPolicy.parseMacroLine(
                "C | /ah,,/bz,,/pets | CYCLE | ASSERT | VANILLA | 0 | X",
                "SEND",
                "ASSERT",
                "HOLD")));
        RingPolicy.RateSettings rates = new RingPolicy.RateSettings(false, 4, false);
        assertEquals("/bz", firstText(session.handleKey("C", unused -> false, false, true, rates)));
        assertEquals("/pets", firstText(session.handleKey("C", unused -> false, false, true, rates)));
        assertEquals("/ah", firstText(session.handleKey("C", unused -> false, false, true, rates)));
        assertEquals("/pets", firstText(session.handleKey("X", unused -> false, false, true, rates)));
    }

    @Test
    void sendSchedulesDelayThenFiresNextTick() {
        RingPolicy.Session session = new RingPolicy.Session();
        session.sync(List.of(RingPolicy.parseMacroLine(
                "Z | /one@0,,/two@1 | SEND",
                "SEND",
                "ASSERT",
                "VANILLA")));
        RingPolicy.KeyPressResult press = session.handleKey(
                "Z", unused -> false, false, true, new RingPolicy.RateSettings(false, 4, false));
        assertEquals(List.of("/one"), press.immediate().stream().map(RingPolicy.DueSend::message).toList());
        List<RingPolicy.DueSend> later = session.tick(unused -> false);
        assertEquals(List.of("/two"), later.stream().map(RingPolicy.DueSend::message).toList());
    }

    @Test
    void submitSkipsVanillaConflictsAndVetoCancels() {
        RingPolicy.Session session = new RingPolicy.Session();
        session.sync(List.of(
                RingPolicy.parseMacroLine("J | /pets | SEND | SUBMIT", "SEND", "ASSERT", "HOLD"),
                RingPolicy.parseMacroLine("V | /storage | SEND | VETO", "SEND", "ASSERT", "HOLD")));
        RingPolicy.RateSettings rates = new RingPolicy.RateSettings(false, 4, false);
        RingPolicy.KeyPressResult submit = session.handleKey(
                "J", unused -> false, true, true, rates);
        assertTrue(submit.immediate().isEmpty());
        assertFalse(submit.cancelVanilla());
        RingPolicy.KeyPressResult veto = session.handleKey(
                "V", unused -> false, true, true, rates);
        assertEquals("/storage", firstText(veto));
        assertTrue(veto.cancelVanilla());
    }

    @Test
    void limitKeyBeatsUnboundMacroOnTheSameKey() {
        RingPolicy.Session session = new RingPolicy.Session();
        session.sync(RingPolicy.parseMacroList(
                "G | /free\nCTRL+G | /limited",
                "SEND",
                "ASSERT",
                "VANILLA",
                true));
        RingPolicy.RateSettings rates = new RingPolicy.RateSettings(false, 4, false);
        assertEquals("/limited", firstText(session.handleKey(
                "G", key -> RingPolicy.sameBind(key, "CTRL"), false, true, rates)));
        assertEquals("/free", firstText(session.handleKey(
                "G", unused -> false, false, true, rates)));
    }

    @Test
    void skyblockOnlyPresetsStayQuietOutsideSkyblock() {
        RingPolicy.Session session = new RingPolicy.Session();
        session.sync(RingPolicy.mergeMacros(
                List.of(),
                RingPolicy.skyblockPresets(
                        "P", "", "", "", "", "", "", "",
                        "SEND",
                        "ASSERT",
                        "VANILLA",
                        true)));
        RingPolicy.RateSettings rates = new RingPolicy.RateSettings(false, 4, false);
        assertTrue(session.handleKey("P", unused -> false, false, false, rates).immediate().isEmpty());
        assertEquals("/pets", firstText(session.handleKey("P", unused -> false, false, true, rates)));
    }

    @Test
    void placeholdersReplaceAndFault() {
        RingPolicy.PlaceholderContext ok = new RingPolicy.PlaceholderContext(
                "hi",
                "pets",
                "clip",
                "Steve",
                "Alex",
                10,
                64,
                20,
                11,
                65,
                21,
                1.0D,
                0.0D,
                List.of("Player: hello world"));
        assertEquals(
                "10 64 20",
                RingPolicy.replacePlaceholders("%pos%", ok).text());
        assertEquals(
                "13",
                RingPolicy.replacePlaceholders("%x+3%", ok).text());
        assertEquals(
                "22 64 20",
                RingPolicy.replacePlaceholders("%posF12%", ok).text());
        assertEquals(
                "hello world",
                RingPolicy.replacePlaceholders("%#Player: (.*)%", ok).text());
        assertEquals(0, RingPolicy.replacePlaceholders("/warp %lastcmd%", ok).faults());
        RingPolicy.PlaceholderResult fault = RingPolicy.replacePlaceholders(
                "%clipboard%",
                new RingPolicy.PlaceholderContext(
                        "", "", "", "Steve", "", 0, 0, 0, null, null, null, 0.0D, 0.0D, List.of()));
        assertFalse(fault.ok());
        assertTrue(fault.text().contains("?"));
    }

    @Test
    void ratelimitBlocksAfterCountWhenEnforced() {
        RingPolicy.Session session = new RingPolicy.Session();
        session.sync(List.of(RingPolicy.parseMacroLine(
                "R | /pets | SEND | ASSERT | VANILLA",
                "SEND",
                "ASSERT",
                "HOLD")));
        RingPolicy.RateSettings rates = new RingPolicy.RateSettings(true, 2, false);
        assertFalse(session.handleKey("R", unused -> false, false, true, rates).rateLimited());
        assertFalse(session.handleKey("R", unused -> false, false, true, rates).rateLimited());
        assertTrue(session.handleKey("R", unused -> false, false, true, rates).rateLimited());
    }

    @Test
    void typeAndEditOpenInsteadOfSending() {
        RingPolicy.Session session = new RingPolicy.Session();
        session.sync(List.of(
                RingPolicy.parseMacroLine("T | hello %edit% | TYPE", "SEND", "ASSERT", "VANILLA"),
                RingPolicy.parseMacroLine("Y | /msg %edit% | EDIT", "SEND", "ASSERT", "VANILLA")));
        RingPolicy.RateSettings rates = new RingPolicy.RateSettings(false, 4, false);
        RingPolicy.KeyPressResult type = session.handleKey("T", unused -> false, false, true, rates);
        assertTrue(type.cancelCharTyped());
        assertTrue(type.immediate().getFirst().type());
        RingPolicy.KeyPressResult edit = session.handleKey("Y", unused -> false, false, true, rates);
        assertTrue(edit.immediate().getFirst().edit());
    }

    @Test
    void catalogKeepsOneCommandKeybindsParent() {
        assertNotNull(QolUtilityCatalog.findById("qol.command_keybinds"));
        assertTrue(QolUtilityCatalog.findById("qol.command_keybinds").runtimeReady());
        Set<String> ids = QolUtilityCatalog.modules().stream()
                .map(QolUtilityCatalog.ModuleDef::id)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(131, ids.size());
        assertTrue(QolUtilityCatalog.findById("qol.command_keybinds").settings().stream()
                .anyMatch(setting -> "qol.command_keybinds.macros".equals(setting.id())));
    }

    private static String firstText(RingPolicy.KeyPressResult result) {
        assertFalse(result.immediate().isEmpty(), "expected a send");
        return result.immediate().getFirst().message();
    }
}
