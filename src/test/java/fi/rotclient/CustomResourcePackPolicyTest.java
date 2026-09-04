package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CustomResourcePackPolicyTest {
    @Test
    void moduleOffRemovesEveryBuiltinPack() {
        assertTrue(CustomResourcePackPolicy.enabledPackIds(false, true, true, true).isEmpty());
    }

    @Test
    void childTogglesSelectOverlayOrderOverworldThenCrimsonThenEnd() {
        assertEquals(
                List.of(
                        CustomResourcePackPolicy.PACK_OVERWORLD,
                        CustomResourcePackPolicy.PACK_CRIMSON,
                        CustomResourcePackPolicy.PACK_END),
                CustomResourcePackPolicy.enabledPackIds(true, true, true, true));
        assertEquals(
                List.of(CustomResourcePackPolicy.PACK_END),
                CustomResourcePackPolicy.enabledPackIds(true, false, false, true));
        assertEquals(
                List.of(CustomResourcePackPolicy.PACK_GAMEPLAY_FONT),
                CustomResourcePackPolicy.enabledPackIds(
                        true, false, false, false, true));
    }

    @Test
    void applySelectionKeepsForeignPacksAndPutsOursLast() {
        List<String> next = CustomResourcePackPolicy.applySelection(
                List.of("vanilla", "file/Hypixel.zip", CustomResourcePackPolicy.PACK_END),
                List.of(
                        CustomResourcePackPolicy.PACK_OVERWORLD,
                        CustomResourcePackPolicy.PACK_CRIMSON));
        assertEquals(
                List.of(
                        "vanilla",
                        "file/Hypixel.zip",
                        CustomResourcePackPolicy.PACK_OVERWORLD,
                        CustomResourcePackPolicy.PACK_CRIMSON),
                next);
    }

    @Test
    void legacyItemRemapStaysIndependentOfTheDarkPackToggle() {
        assertTrue(LegacyTexturesPolicy.shouldReplace(true, true, "HYPERION"));
        assertTrue(LegacyTexturesPolicy.shouldReplace(
                true, true, "HYPERION", "hypixel_skyblock"));
        assertFalse(LegacyTexturesPolicy.shouldReplace(
                true, true, "HYPERION", "minecraft"));
        assertEquals(
                List.of(CustomResourcePackPolicy.PACK_OVERWORLD),
                CustomResourcePackPolicy.enabledPackIds(true, true, false, false));
        assertTrue(CustomResourcePackPolicy.selectionMatches(
                List.of("vanilla", CustomResourcePackPolicy.PACK_OVERWORLD),
                List.of(CustomResourcePackPolicy.PACK_OVERWORLD)));
    }

    @Test
    void convertedPacksAreMinecraft26FormatWithoutOptifine() throws Exception {
        Path root = Path.of("src/client/resources/resourcepacks");
        assertTrue(Files.isRegularFile(root.resolve("dark_overworld/pack.mcmeta")));
        assertTrue(Files.isRegularFile(root.resolve("dark_crimson/pack.mcmeta")));
        assertTrue(Files.isRegularFile(root.resolve("dark_end/pack.mcmeta")));
        String overworldMeta = Files.readString(root.resolve("dark_overworld/pack.mcmeta"));
        assertTrue(overworldMeta.contains("\"min_format\""));
        assertTrue(overworldMeta.contains("88"));
        String sword = Files.readString(root.resolve(
                "dark_overworld/assets/minecraft/models/item/iron_sword.json"));
        assertTrue(sword.contains("minecraft:item/handheld"));
        assertFalse(sword.contains("builtin/generated"));
        assertTrue(Files.isRegularFile(root.resolve(
                "dark_end/assets/minecraft/textures/block/end_stone.png")));
        assertTrue(Files.isRegularFile(root.resolve(
                "dark_crimson/assets/minecraft/textures/block/netherrack.png")));
        assertFalse(Files.exists(root.resolve("dark_overworld/assets/minecraft/optifine")));
        assertFalse(Files.exists(root.resolve("dark_end/assets/mcpatcher")));
        Path fontPack = root.resolve("gameplay_font");
        assertTrue(Files.isRegularFile(fontPack.resolve("pack.mcmeta")));
        assertTrue(Files.readString(
                        fontPack.resolve("assets/minecraft/font/default.json"))
                .contains("rotclient:ui.ttf"));
        assertTrue(Files.readString(fontPack.resolve("OFL-ATTRIBUTION.txt"))
                .contains("SIL Open Font License"));
        assertTrue(Files.readString(fontPack.resolve("OFL.txt"))
                .contains("SIL OPEN FONT LICENSE Version 1.1"));
        String overworldCredit = Files.readString(root.resolve("dark_overworld/ATTRIBUTION.txt"));
        assertTrue(overworldCredit.contains("Rot-authored"));
        assertTrue(overworldCredit.contains("Minecraft EULA"));
        assertTrue(overworldMeta.contains("Rot Client dark overworld"));
        assertFalse(overworldMeta.toLowerCase().contains("original"));
        assertFalse(overworldMeta.contains("Miska"));
        String crimsonMeta = Files.readString(root.resolve("dark_crimson/pack.mcmeta"));
        assertTrue(crimsonMeta.contains("Rot Client dark Crimson"));
        assertFalse(crimsonMeta.contains("AxE"));
        String endMeta = Files.readString(root.resolve("dark_end/pack.mcmeta"));
        assertTrue(endMeta.contains("Rot Client dark End"));
        assertFalse(endMeta.contains("fiyr"));
    }
}
