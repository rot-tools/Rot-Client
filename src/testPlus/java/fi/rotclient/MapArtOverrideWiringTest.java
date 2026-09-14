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
        assertTrue(json.contains("BlockDisplayRendererFoxMixin"));
        assertTrue(json.contains("DisplayEntityRenderStateFoxMixin"));
        assertTrue(json.contains("ItemFrameRenderStateFoxMixin"));
        assertTrue(json.contains("DisplayItemDisplayAccessor"));
        assertTrue(json.contains("MapRendererMapArtMixin"));
        assertTrue(json.contains("WrapOperation") || Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/ItemFrameRendererMapArtMixin.java"),
                StandardCharsets.UTF_8).contains("WrapOperation"));
        String shared = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertFalse(shared.contains("PaintingRendererFoxMixin"));
        assertFalse(shared.contains("ItemDisplayRendererFoxMixin"));
        assertFalse(shared.contains("BlockDisplayRendererFoxMixin"));
        assertFalse(shared.contains("DisplayItemDisplayAccessor"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/DisplayItemDisplayAccessor.java")));
        assertFalse(Files.isRegularFile(Path.of(
                "src/client/java/fi/rotclient/mixin/DisplayItemDisplayAccessor.java")));
    }

    @Test
    void runtimeReplacesPaintingsAndItemDisplaysLocally() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/MapArtOverrideRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("renderPainting"));
        assertTrue(runtime.contains("renderItemDisplay"));
        assertTrue(runtime.contains("renderFramedMap"));
        assertTrue(runtime.contains("mapFrameZDegrees"));
        assertTrue(runtime.contains("MAP_QUAD_CENTER"));
        assertTrue(runtime.contains("itemFrameZDegrees"));
        assertTrue(runtime.contains("HUB_MAP_SEARCH_RADIUS"));
        assertTrue(runtime.contains("HUB_MAP_NEIGHBOR_REACH"));
        assertTrue(runtime.contains("tileUvContain"));
        assertTrue(runtime.contains("GlowItemFrame"));
        assertTrue(runtime.contains("isMapWallFrame"));
        assertTrue(runtime.contains("isWallFrameItem"));
        assertTrue(runtime.contains("submitContainedMapQuad"));
        assertFalse(runtime.contains("getRotation() == rotation"));
        assertTrue(runtime.contains("configureBlockDisplay"));
        assertTrue(runtime.contains("textures/map-art.png"));
        assertTrue(runtime.contains("RenderTypes.text"));
        assertTrue(runtime.contains("ItemDisplayEntityRenderState"));
        String painting = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/PaintingRendererFoxMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(painting.contains("method = \"submit("));
        String itemDisplay = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/ItemDisplayRendererFoxMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(itemDisplay.contains("method = \"submitInner\""));
        assertTrue(itemDisplay.contains("DisplayRenderer.ItemDisplayRenderer"));
        String itemFrame = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/ItemFrameRendererMapArtMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(itemFrame.contains("WrapOperation"));
        assertTrue(itemFrame.contains("renderFramedMap"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/plusClient/resources/assets/rotclient/textures/map-art.png")));
        assertFalse(Files.isRegularFile(Path.of(
                "src/plusClient/resources/assets/rotclient/textures/map-art.jpg")));
    }

    @Test
    void catalogDescribesPaintingsAndItemDisplays() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.map_art_override");
        assertTrue(module != null);
        assertTrue(module.searchAliases().contains("painting"));
        assertTrue(module.searchAliases().contains("fox"));
        assertTrue(module.searchAliases().contains("block display"));
        assertTrue(module.description().toLowerCase().contains("painting"));
        assertTrue(module.description().toLowerCase().contains("item-display"));
        assertTrue(module.description().toLowerCase().contains("block-display"));
        assertTrue(module.description().toLowerCase().contains("14x7"));
    }
}
