package fi.rotclient.rebuild.illegal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlackGlassPackTest {
    @Test
    void includesSelectedBlockAndPaneWithMatchingCtmRuleOnly() throws IOException {
        var source = zip("textures/block/red_stained_glass.png",
                "textures/block/red_stained_glass_pane_top.png",
                "optifine/ctm/rotclient_glass/red_stained_glass/black.properties",
                "optifine/ctm/rotclient_glass/red_stained_glass/0.png",
                "textures/block/blue_stained_glass.png",
                "optifine/ctm/rotclient_glass/blue_stained_glass/black.properties",
                "textures/block/dirt.png");
        var result = BlackGlassPack.readAssets(source, Set.of(BlackGlassSettings.Color.RED));
        assertEquals(Set.of(
                Identifier.parse("minecraft:textures/block/red_stained_glass.png"),
                Identifier.parse("minecraft:textures/block/red_stained_glass_pane_top.png"),
                Identifier.parse("minecraft:optifine/ctm/rotclient_glass/red_stained_glass/black.properties"),
                Identifier.parse("minecraft:optifine/ctm/rotclient_glass/red_stained_glass/0.png")), result.keySet());
    }

    @Test
    void clearAndTintedAreIndependentAndOffKeepsTheDraft() throws IOException {
        var settings = new BlackGlassSettings(false, Set.of(BlackGlassSettings.Color.TINTED));
        assertEquals(Set.of(BlackGlassSettings.Color.TINTED), settings.colors());
        assertTrue(BlackGlassPack.readAssets(zip("textures/block/tinted_glass.png"), settings.activeColors()).isEmpty());
        var result = BlackGlassPack.readAssets(zip("textures/block/glass.png", "textures/block/glass_pane_top.png",
                        "textures/block/tinted_glass.png", "textures/block/tinted_glass_pane_top.png"),
                settings.colors());
        assertEquals(Set.of(Identifier.parse("minecraft:textures/block/tinted_glass.png")), result.keySet());
    }

    @Test
    void registersValidMinecraft262ResourcePackMetadata() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BlackGlassPack.addToPackList((id, makePack) -> {
            var pack = makePack.apply(id);
            assertNotNull(pack);
            assertTrue(pack.getCompatibility().isCompatible());
            assertEquals("Rot Client MSC Black Glass", pack.getTitle().getString());
        });
    }

    private static ByteArrayInputStream zip(String... paths) throws IOException {
        var content = new ByteArrayOutputStream();
        try (var stream = new ZipOutputStream(content)) {
            for (var path : paths) {
                stream.putNextEntry(new ZipEntry("assets/minecraft/" + path));
                stream.write(path.getBytes(StandardCharsets.UTF_8));
                stream.closeEntry();
            }
        }
        return new ByteArrayInputStream(content.toByteArray());
    }
}
