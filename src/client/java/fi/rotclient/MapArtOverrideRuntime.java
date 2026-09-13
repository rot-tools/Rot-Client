package fi.rotclient;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import fi.rotclient.mixin.DisplayItemDisplayAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3fc;

/**
 * Local-only map, painting, and item-display presentation. This never changes
 * map data, packets, items, or the server-visible entity layout.
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
        int light = MapArtOverridePolicy.packedLight(lightCoords, 0);
        collector.submitCustomGeometry(poseStack, RenderTypes.text(TEXTURE_ID), (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(u0, v1).setLight(light);
            buffer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(u1, v1).setLight(light);
            buffer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(u1, v0).setLight(light);
            buffer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(u0, v0).setLight(light);
        });
        return true;
    }

    /** Render the local image on a server painting without changing its entity data. */
    public static boolean renderPainting(
            PaintingRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (!ensureTexture()
                || state == null
                || state.direction == null
                || !MapArtOverridePolicy.shouldReplacePainting(true, state.direction.getAxis().isHorizontal())) {
            return false;
        }
        float width;
        float height;
        if (state.variant != null) {
            width = state.variant.width();
            height = state.variant.height();
        } else {
            width = Math.max(1.0F, state.boundingBoxWidth);
            height = Math.max(1.0F, state.boundingBoxHeight);
        }
        if (width < 1.0F || height < 1.0F) {
            return false;
        }
        int preferred = 0;
        if (state.lightCoordsPerBlock != null && state.lightCoordsPerBlock.length > 0) {
            preferred = state.lightCoordsPerBlock[state.lightCoordsPerBlock.length / 2];
        }
        int light = MapArtOverridePolicy.packedLight(preferred, state.lightCoords);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.direction.get2DDataValue() * 90.0F));
        float left = -width / 2.0F;
        float right = width / 2.0F;
        float bottom = -height / 2.0F;
        float top = height / 2.0F;
        float z = -0.03125F;
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entitySolidZOffsetForward(TEXTURE_ID),
                (pose, buffer) -> {
                    foxVertex(pose, buffer, right, bottom, 1.0F, 1.0F, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, left, bottom, 0.0F, 1.0F, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, left, top, 0.0F, 0.0F, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, right, top, 1.0F, 0.0F, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, left, bottom, 0.0F, 1.0F, z, light, 0, 0, 1);
                    foxVertex(pose, buffer, right, bottom, 1.0F, 1.0F, z, light, 0, 0, 1);
                    foxVertex(pose, buffer, right, top, 1.0F, 0.0F, z, light, 0, 0, 1);
                    foxVertex(pose, buffer, left, top, 0.0F, 0.0F, z, light, 0, 0, 1);
                });
        poseStack.popPose();
        return true;
    }

    public static void configureItemDisplay(
            Display.ItemDisplay display,
            ItemDisplayEntityRenderState state,
            float tickProgress) {
        if (!(state instanceof FoxItemDisplayAccess access) || display == null) {
            return;
        }
        access.rotclient$setFoxUv(0.0F, 0.0F, 1.0F, 1.0F);
        access.rotclient$setFoxReplace(false);
        if (!isEnabled()) {
            return;
        }
        boolean replace = MapArtOverridePolicy.shouldReplaceItemDisplay(
                true,
                isMapOrPaintingItem(itemStack(display)),
                isFixedBillboard(display),
                scale(display, tickProgress, 0),
                scale(display, tickProgress, 1),
                scale(display, tickProgress, 2),
                display.getBbWidth(),
                display.getBbHeight());
        access.rotclient$setFoxReplace(replace);
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (!replace
                || config == null
                || !config.extras().mapArtStretchFrames
                || display.getXRot() < -20.0F
                || display.getXRot() > 20.0F) {
            return;
        }
        try {
            Panel panel = findItemDisplayPanel(display);
            if (panel != null) {
                access.rotclient$setFoxUv(panel.u0(), panel.v0(), panel.u1(), panel.v1());
            }
        } catch (RuntimeException ignored) {
            // A stale entity during a chunk transition must leave this display as a single image.
        }
    }

    public static boolean renderItemDisplay(
            ItemDisplayEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        if (!ensureTexture()
                || !(state instanceof FoxItemDisplayAccess access)
                || !access.rotclient$foxReplace()) {
            return false;
        }
        float u0 = access.rotclient$foxU0();
        float v0 = access.rotclient$foxV0();
        float u1 = access.rotclient$foxU1();
        float v1 = access.rotclient$foxV1();
        int light = MapArtOverridePolicy.packedLight(lightCoords, state.lightCoords);
        float z = 0.03125F;
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entitySolidZOffsetForward(TEXTURE_ID),
                (pose, buffer) -> {
                    foxVertex(pose, buffer, 0.5F, -0.5F, u1, v1, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, -0.5F, -0.5F, u0, v1, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, -0.5F, 0.5F, u0, v0, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, 0.5F, 0.5F, u1, v0, z, light, 0, 0, -1);
                    foxVertex(pose, buffer, -0.5F, -0.5F, u0, v1, z, light, 0, 0, 1);
                    foxVertex(pose, buffer, 0.5F, -0.5F, u1, v1, z, light, 0, 0, 1);
                    foxVertex(pose, buffer, 0.5F, 0.5F, u1, v0, z, light, 0, 0, 1);
                    foxVertex(pose, buffer, -0.5F, 0.5F, u0, v0, z, light, 0, 0, 1);
                });
        return true;
    }

    private static void foxVertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float x,
            float y,
            float u,
            float v,
            float z,
            int light,
            int nx,
            int ny,
            int nz) {
        buffer.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
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

    private static ItemStack itemStack(Display.ItemDisplay display) {
        try {
            ItemStack stack = ((DisplayItemDisplayAccessor) display).rotclient$getItemStack();
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (ClassCastException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean isMapOrPaintingItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.FILLED_MAP)
                || stack.is(Items.MAP)
                || stack.is(Items.PAINTING)
                || stack.has(DataComponents.MAP_ID));
    }

    private static boolean isFixedBillboard(Display.ItemDisplay display) {
        Display.RenderState renderState = display.renderState();
        return renderState != null
                && renderState.billboardConstraints() == Display.BillboardConstraints.FIXED;
    }

    private static float scale(Display.ItemDisplay display, float tickProgress, int axis) {
        Display.RenderState renderState = display.renderState();
        if (renderState == null) {
            return 1.0F;
        }
        Transformation transformation = renderState.transformation().get(tickProgress);
        if (transformation == null) {
            return 1.0F;
        }
        Vector3fc scale = transformation.scale();
        if (axis == 0) {
            return scale.x();
        }
        if (axis == 1) {
            return scale.y();
        }
        return scale.z();
    }

    private static boolean matchesItemDisplayPanel(Display.ItemDisplay origin, Display.ItemDisplay candidate) {
        if (candidate == origin) {
            return true;
        }
        if (Math.abs(candidate.getXRot()) > 20.0F) {
            return false;
        }
        Direction originFacing = Direction.fromYRot(origin.getYRot());
        Direction candidateFacing = Direction.fromYRot(candidate.getYRot());
        if (originFacing != candidateFacing || originFacing.getAxis().isVertical()) {
            return false;
        }
        return MapArtOverridePolicy.shouldReplaceItemDisplay(
                true,
                isMapOrPaintingItem(itemStack(candidate)),
                isFixedBillboard(candidate),
                scale(candidate, 1.0F, 0),
                scale(candidate, 1.0F, 1),
                scale(candidate, 1.0F, 2),
                candidate.getBbWidth(),
                candidate.getBbHeight());
    }

    private static Panel findItemDisplayPanel(Display.ItemDisplay origin) {
        Direction direction = Direction.fromYRot(origin.getYRot());
        if (direction.getAxis().isVertical()) {
            return null;
        }
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        BlockPos originPos = origin.blockPosition();
        positions.add(originPos);
        pending.add(originPos);

        Map<BlockPos, Display.ItemDisplay> displays = new HashMap<>();
        for (Display.ItemDisplay candidate : origin.level().getEntitiesOfClass(
                Display.ItemDisplay.class, origin.getBoundingBox().inflate(SEARCH_RADIUS),
                candidate -> matchesItemDisplayPanel(origin, candidate))) {
            displays.put(candidate.blockPosition(), candidate);
        }
        displays.put(originPos, origin);
        while (!pending.isEmpty()) {
            BlockPos here = pending.removeFirst();
            for (BlockPos next : neighbours(here, direction)) {
                if (displays.containsKey(next) && positions.add(next)) {
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
        if (!MapArtOverridePolicy.isFilledRectangle(positions.size(), width, height)) {
            return null;
        }
        int currentColumn = column(originPos, direction) - minColumn;
        int currentRow = maxY - originPos.getY();
        float[] uv = MapArtOverridePolicy.tileUv(currentColumn, currentRow, width, height);
        return new Panel(uv[0], uv[1], uv[2], uv[3]);
    }

    private static Panel findPanel(ItemFrame origin) {
        Direction direction = origin.getDirection();
        int rotation = origin.getRotation();
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        BlockPos originPos = origin.blockPosition();
        positions.add(originPos);
        pending.add(originPos);

        Map<BlockPos, ItemFrame> frames = new HashMap<>();
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
        if (!MapArtOverridePolicy.isFilledRectangle(positions.size(), width, height)) {
            return null;
        }
        int currentColumn = column(originPos, direction) - minColumn;
        int currentRow = maxY - originPos.getY();
        float[] uv = MapArtOverridePolicy.tileUv(currentColumn, currentRow, width, height);
        return new Panel(uv[0], uv[1], uv[2], uv[3]);
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
