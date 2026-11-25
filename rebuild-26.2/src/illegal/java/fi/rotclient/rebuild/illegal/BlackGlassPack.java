package fi.rotclient.rebuild.illegal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlackGlassPack {
    private static final Logger LOGGER = LoggerFactory.getLogger("rotclient-illegal");
    private static final String PACK_ID = "rotclient_rebuild:msc_black_glass";
    private static final String NAMESPACE = "minecraft";
    private static final String STANDALONE = "MSC-Black-Glass-26.2.zip";
    private static final String BUNDLED = "/assets/rotclient_rebuild/black_glass/source.zip";
    private static final int MAX_ASSET_BYTES = 256 * 1024;
    private static final String TEXTURES = "textures/block/";
    private static final String CTM = "optifine/ctm/rotclient_glass/";
    private static final byte[] METADATA = """
            {"pack":{"description":"MSC Black Glass textures · integrated by Rot-Tools","min_format":[88,0],"max_format":[88,0]}}
            """.getBytes(StandardCharsets.UTF_8);
    private static final Map<String, BlackGlassSettings.Color> ALLOWED_ASSETS = allowedAssets();

    private BlackGlassPack() {}

    public static void addToPackList(BiConsumer<String, Function<String, Pack>> packs) {
        packs.accept(PACK_ID, BlackGlassPack::create);
    }

    private static Pack create(String id) {
        var location = new PackLocationInfo(id, Component.literal("Rot Client MSC Black Glass"),
                PackSource.BUILT_IN, Optional.empty());
        return Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
            @Override public PackResources openPrimary(PackLocationInfo info) {
                return new Resources(info, Map.of());
            }

            @Override public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                var colors = IllegalEdition.blackGlassSettings().activeColors();
                if (colors.isEmpty()) return openPrimary(info);
                try (var source = openSource()) {
                    if (source == null) throw new IOException("MSC Black Glass ZIP is missing");
                    return new Resources(info, readAssets(source, colors));
                } catch (IOException error) {
                    LOGGER.warn("Could not load the selected MSC Black Glass textures", error);
                    return openPrimary(info);
                }
            }
        }, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, true));
    }

    static boolean apply(Minecraft client, IllegalSettings config, BlackGlassSettings selection) {
        if (!selection.activeColors().isEmpty()) {
            try (var source = openSource()) {
                if (source == null || readAssets(source, selection.activeColors()).isEmpty()) return false;
            } catch (IOException error) {
                LOGGER.warn("Could not read the MSC Black Glass source pack", error);
                return false;
            }
        }

        boolean changed = !config.blackGlass.activeColors().equals(selection.activeColors());
        config.blackGlass = selection;
        config.save();
        var repository = client.getResourcePackRepository();
        if (repository.removePack("file/" + STANDALONE)) {
            client.options.updateResourcePacks(repository);
        } else if (changed) {
            client.reloadResourcePacks();
        }
        return true;
    }

    private static InputStream openSource() throws IOException {
        var bundled = BlackGlassPack.class.getResourceAsStream(BUNDLED);
        if (bundled != null) return bundled;
        var local = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(STANDALONE);
        return Files.isRegularFile(local) ? Files.newInputStream(local) : null;
    }

    static Map<Identifier, byte[]> readAssets(InputStream source, Set<BlackGlassSettings.Color> colors) throws IOException {
        var assets = new HashMap<Identifier, byte[]>();
        try (var zip = new ZipInputStream(source)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String path = entry.getName();
                var color = ALLOWED_ASSETS.get(path);
                if (color == null || !colors.contains(color) || entry.isDirectory()) continue;
                byte[] bytes = zip.readNBytes(MAX_ASSET_BYTES + 1);
                if (bytes.length > MAX_ASSET_BYTES) throw new IOException("Oversized MSC Black Glass asset");
                assets.put(Identifier.fromNamespaceAndPath(NAMESPACE, path.substring("assets/minecraft/".length())), bytes);
            }
        }
        return Map.copyOf(assets);
    }

    private static Map<String, BlackGlassSettings.Color> allowedAssets() {
        var paths = new HashMap<String, BlackGlassSettings.Color>();
        for (var color : BlackGlassSettings.Color.values()) {
            String prefix = "assets/minecraft/";
            String block = color.blockPath;
            paths.put(prefix + TEXTURES + block + ".png", color);
            if (color != BlackGlassSettings.Color.TINTED) {
                paths.put(prefix + TEXTURES + block + "_pane_top.png", color);
                paths.put(prefix + CTM + block + "/0.png", color);
                paths.put(prefix + CTM + block + "/black.properties", color);
            }
        }
        return Map.copyOf(paths);
    }

    private static final class Resources extends AbstractPackResources {
        private final Map<Identifier, byte[]> assets;

        Resources(PackLocationInfo location, Map<Identifier, byte[]> assets) {
            super(location);
            this.assets = assets;
        }

        @Override public IoSupplier<InputStream> getRootResource(String... path) {
            return path.length == 1 && PACK_META.equals(path[0]) ? () -> new ByteArrayInputStream(METADATA) : null;
        }

        @Override public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
            byte[] data = type == PackType.CLIENT_RESOURCES ? assets.get(id) : null;
            return data == null ? null : () -> new ByteArrayInputStream(data);
        }

        @Override public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
            if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(namespace)) return;
            String prefix = path.isEmpty() || path.endsWith("/") ? path : path + "/";
            assets.forEach((id, data) -> {
                if (id.getPath().startsWith(prefix)) output.accept(id, () -> new ByteArrayInputStream(data));
            });
        }

        @Override public Set<String> getNamespaces(PackType type) {
            return type == PackType.CLIENT_RESOURCES && !assets.isEmpty() ? Set.of(NAMESPACE) : Set.of();
        }

        @Override public void close() {}
    }
}
