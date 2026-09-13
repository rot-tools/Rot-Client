package fi.rotclient;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;

/**
 * Local-only map presentation. This never changes map data, packets, items,
 * or the server-visible item-frame layout.
 */
public final class MapArtOverrideRuntime {
    private static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath("rotclient", "dynamic/map_art_override");
    private static final Identifier EMBEDDED_IMAGE =
            Identifier.fromNamespaceAndPath("rotclient", "textures/map-art.jpg");
    private static final int SEARCH_RADIUS = 16;

    private static String loadedPath = "";
    private static long loadedModified = Long.MIN_VALUE;
    private static boolean unavailable;

    private MapArtOverrideRuntime() {
    }

    public static boolean render(
            MapRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        if (!ensureTexture() || !(state instanceof MapArtRenderStateAccess access)) {
            return false;
        }
        float u0 = access.rotclient$mapArtU0();
        float v0 = access.rotclient$mapArtV0();
        float u1 = access.rotclient$mapArtU1();
        float v1 = access.rotclient$mapArtV1();
        collector.submitCustomGeometry(poseStack, RenderTypes.text(TEXTURE_ID), (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(u0, v1).setLight(lightCoords);
            buffer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(u1, v1).setLight(lightCoords);
            buffer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(u1, v0).setLight(lightCoords);
            buffer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(u0, v0).setLight(lightCoords);
        });
        return true;
    }

    /** Render the local image on a server painting without changing its entity data. */
    public static boolean renderPainting(
            PaintingRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (!ensureTexture() || state == null || state.variant == null || state.direction == null) {
            return false;
        }
        int width = state.variant.width();
        int height = state.variant.height();
        if (width < 1 || height < 1) {
            return false;
        }
        int light = state.lightCoordsPerBlock.length == 0
                ? 0
                : state.lightCoordsPerBlock[state.lightCoordsPerBlock.length / 2];
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                180 - state.direction.get2DDataValue() * 90));
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entitySolidZOffsetForward(TEXTURE_ID),
                (pose, buffer) -> {
                    float left = -width / 2.0F;
                    float right = width / 2.0F;
                    float bottom = -height / 2.0F;
                    float top = height / 2.0F;
                    paintingVertex(pose, buffer, right, bottom, 1.0F, 1.0F, light);
                    paintingVertex(pose, buffer, left, bottom, 0.0F, 1.0F, light);
                    paintingVertex(pose, buffer, left, top, 0.0F, 0.0F, light);
                    paintingVertex(pose, buffer, right, top, 1.0F, 0.0F, light);
                });
        poseStack.popPose();
        return true;
    }

    private static void paintingVertex(
            PoseStack.Pose pose,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            float x,
            float y,
            float u,
            float v,
            int light) {
        buffer.addVertex(pose, x, y, -0.03125F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 0, -1);
    }

    /** Attach one image region to an item-frame map state. */
    public static void configureItemFrame(MapRenderState state, ItemFrame frame) {
        if (!(state instanceof MapArtRenderStateAccess access)) {
            return;
        }
        access.rotclient$setMapArtUv(0.0F, 0.0F, 1.0F, 1.0F);
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (!isEnabled()
                || config == null
                || !config.extras().mapArtStretchFrames
                || frame == null
                || frame.getDirection().getAxis().isVertical()) {
            return;
        }
        try {
            Panel panel = findPanel(frame);
            if (panel != null) {
                access.rotclient$setMapArtUv(panel.u0(), panel.v0(), panel.u1(), panel.v1());
            }
        } catch (RuntimeException ignored) {
            // A stale entity during a chunk transition must leave this map as a single image.
        }
    }

    public static void reset() {
        loadedPath = "";
        loadedModified = Long.MIN_VALUE;
        unavailable = false;
    }

    private static boolean isEnabled() {
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        return config != null && config.isModuleEnabled("qol.map_art_override");
    }

    private static boolean ensureTexture() {
        if (!isEnabled()) {
            return false;
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        String configured = config == null ? "" : config.readText("qol.map_art_override.image_path");
        if (configured == null || configured.isBlank()) {
            return loadEmbeddedTexture();
        }
        Path image = resolveImage(configured);
        if (image == null || !Files.isRegularFile(image)) {
            return loadEmbeddedTexture();
        }
        long modified;
        try {
            modified = Files.getLastModifiedTime(image).toMillis();
        } catch (IOException ignored) {
            return false;
        }
        String absolute = image.toAbsolutePath().normalize().toString();
        if (absolute.equals(loadedPath) && modified == loadedModified && !unavailable) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getTextureManager() == null) {
            return false;
        }
        try (InputStream stream = Files.newInputStream(image)) {
            NativeImage pixels = NativeImage.read(stream);
            client.getTextureManager().register(
                    TEXTURE_ID,
                    new DynamicTexture(() -> "Rot Client local map art", pixels));
            loadedPath = absolute;
            loadedModified = modified;
            unavailable = false;
            return true;
        } catch (IOException | RuntimeException ignored) {
            unavailable = true;
            return false;
        }
    }

    private static boolean loadEmbeddedTexture() {
        final String embeddedKey = "embedded:" + EMBEDDED_IMAGE;
        if (embeddedKey.equals(loadedPath) && !unavailable) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getTextureManager() == null) {
            return false;
        }
        try (InputStream stream = client.getResourceManager().open(EMBEDDED_IMAGE)) {
            NativeImage pixels = NativeImage.read(stream);
            client.getTextureManager().register(
                    TEXTURE_ID,
                    new DynamicTexture(() -> "Rot Client bundled map art", pixels));
            loadedPath = embeddedKey;
            loadedModified = Long.MIN_VALUE;
            unavailable = false;
            return true;
        } catch (IOException | RuntimeException ignored) {
            unavailable = true;
            return false;
        }
    }

    private static Path resolveImage(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        try {
            Path raw = Path.of(configured.trim());
            return raw.isAbsolute() ? raw : FabricLoader.getInstance().getConfigDir().resolve(raw).normalize();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Panel findPanel(ItemFrame origin) {
        Direction direction = origin.getDirection();
        int rotation = origin.getRotation();
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        BlockPos originPos = origin.blockPosition();
        positions.add(originPos);
        pending.add(originPos);

        java.util.Map<BlockPos, ItemFrame> frames = new java.util.HashMap<>();
        for (ItemFrame candidate : origin.level().getEntitiesOfClass(
                ItemFrame.class, origin.getBoundingBox().inflate(SEARCH_RADIUS),
                candidate -> candidate.getDirection() == direction
                        && candidate.getRotation() == rotation
                        && candidate.getFramedMapId(candidate.getItem()) != null)) {
            frames.put(candidate.blockPosition(), candidate);
        }
        frames.put(originPos, origin);
        while (!pending.isEmpty()) {
            BlockPos here = pending.removeFirst();
            for (BlockPos next : neighbours(here, direction)) {
                if (frames.containsKey(next) && positions.add(next)) {
                    pending.addLast(next);
                }
            }
        }
        if (positions.size() < 2) {
            return null;
        }
        int minColumn = Integer.MAX_VALUE;
        int maxColumn = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos position : positions) {
            int column = column(position, direction);
            minColumn = Math.min(minColumn, column);
            maxColumn = Math.max(maxColumn, column);
            minY = Math.min(minY, position.getY());
            maxY = Math.max(maxY, position.getY());
        }
        int width = maxColumn - minColumn + 1;
        int height = maxY - minY + 1;
        if (width < 1 || height < 1 || width * height != positions.size()) {
            return null; // Only contiguous rectangles are safe to stretch.
        }
        int currentColumn = column(originPos, direction) - minColumn;
        int currentRow = maxY - originPos.getY();
        return new Panel(
                currentColumn / (float) width,
                currentRow / (float) height,
                (currentColumn + 1.0F) / width,
                (currentRow + 1.0F) / height);
    }

    private static Set<BlockPos> neighbours(BlockPos position, Direction direction) {
        Set<BlockPos> out = new HashSet<>();
        out.add(position.above());
        out.add(position.below());
        if (direction.getAxis() == Direction.Axis.Z) {
            out.add(position.east());
            out.add(position.west());
        } else {
            out.add(position.north());
            out.add(position.south());
        }
        return out;
    }

    private static int column(BlockPos position, Direction direction) {
        return switch (direction) {
            case NORTH -> position.getX();
            case SOUTH -> -position.getX();
            case WEST -> position.getZ();
            case EAST -> -position.getZ();
            default -> position.getX();
        };
    }

    private record Panel(float u0, float v0, float u1, float v1) {
    }
}
