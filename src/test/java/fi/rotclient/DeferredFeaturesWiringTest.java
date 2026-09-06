package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DeferredFeaturesWiringTest {
    @Test
    void deferredFeaturesStayOnExistingParents() {
        assertEquals(131, QolUtilityCatalog.modules().size());
        for (String settingId : List.of(
                "qol.render_optimizer.full_text_shadow",
                "qol.custom_resource_pack.gameplay_font",
                "qol.command_keybinds.open_sequence_editor",
                "qol.storage_overlay.open_item_search",
                "qol.storage_overlay.craft_helper",
                "qol.storage_overlay.museum_armor",
                "qol.player_size.player_animals",
                "qol.animation_fix.dyes",
                "qol.animation_fix.skins")) {
            assertTrue(
                    QolUtilityCatalog.modules().stream()
                            .flatMap(module -> module.settings().stream())
                            .anyMatch(setting -> setting.id().equals(settingId)),
                    settingId);
        }
    }

    @Test
    void visualAndTextMixinsAreRegistered() throws Exception {
        String mixins = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"));
        assertTrue(mixins.contains("BakedSheetGlyphShadowMixin"));
        assertTrue(mixins.contains("LivingEntityPlayerAnimalsMixin"));
        assertTrue(mixins.contains("ItemModelResolverAnimationMixin"));
    }

    @Test
    void playerAnimalModelsShareAvatarCompatibleStateType() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LivingEntityPlayerAnimalsMixin.java"));
        assertTrue(source.contains(
                "private static final GenericQuadrupedModel ROTCLIENT_COW"));
        assertTrue(source.contains(
                "private static final GenericQuadrupedModel ROTCLIENT_PIG"));
        assertTrue(source.contains(
                "GenericQuadrupedModel animal = switch"));
    }

    @Test
    void itemDataAndFontAssetsAreBundled() {
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/data/rotclient/items.tsv")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/client/resources/assets/rotclient/font/ui.ttf")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/client/resources/resourcepacks/gameplay_font/OFL-ATTRIBUTION.txt")));
    }
}
