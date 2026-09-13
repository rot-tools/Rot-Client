package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MapArtOverrideWiringTest {
    @Test
    void plusMixinsRegisterPaintingAndItemDisplayHooks() throws Exception {
        String json = Files.readString(Path.of(
                "src/plusClient/resources/rotclient.plus.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("PaintingRendererFoxMixin"));
        assertTrue(json.contains("ItemDisplayRendererFoxMixin"));
        assertTrue(json.contains("ItemDisplayEntityRenderStateFoxMixin"));
        assertTrue(json.contains("DisplayItemDisplayAccessor"));
        assertTrue(json.contains("MapRendererMapArtMixin"));
        String shared = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertFalse(shared.contains("PaintingRendererFoxMixin"));
        assertFalse(shared.contains("ItemDisplayRendererFoxMixin"));
    }

    @Test
    void runtimeReplacesPaintingsAndItemDisplaysLocally() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/MapArtOverrideRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("renderPainting"));
        assertTrue(runtime.contains("renderItemDisplay"));
        assertTrue(runtime.contains("configureItemDisplay"));
        assertTrue(runtime.contains("entitySolidZOffsetForward"));
        assertTrue(runtime.contains("ItemDisplayEntityRenderState"));
        String painting = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/PaintingRendererFoxMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(painting.contains("method = \"submit("));
        String itemDisplay = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ItemDisplayRendererFoxMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(itemDisplay.contains("method = \"submitInner\""));
        assertTrue(itemDisplay.contains("DisplayRenderer.ItemDisplayRenderer"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/plusClient/resources/assets/rotclient/textures/map-art.jpg")));
    }

    @Test
    void catalogDescribesPaintingsAndItemDisplays() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.map_art_override");
        assertTrue(module != null);
        assertTrue(module.searchAliases().contains("painting"));
        assertTrue(module.searchAliases().contains("fox"));
        assertTrue(module.description().toLowerCase().contains("painting"));
        assertTrue(module.description().toLowerCase().contains("item-display"));
    }
}
