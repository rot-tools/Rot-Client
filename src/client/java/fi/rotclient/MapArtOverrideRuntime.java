package fi.rotclient;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import fi.rotclient.mixin.DisplayItemDisplayAccessor;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;

/**
 * Local-only map, painting, item-frame, and display presentation. This never
 * changes map data, packets, items, or the server-visible entity layout.
 */
public final class MapArtOverrideRuntime {
    private static final Identifier CUSTOM_TEXTURE_ID =
            Identifier.fromNamespaceAndPath("rotclient", "dynamic/map_art_override");
    private static final Identifier BUNDLED_TEXTURE =
            Identifier.fromNamespaceAndPath("rotclient", "textures/map-art.png");
    private static final String BUNDLED_KEY = "bundled:" + BUNDLED_TEXTURE;
    private static final int SEARCH_RADIUS = MapArtOverridePolicy.HUB_MAP_SEARCH_RADIUS;
    private static final long CUSTOM_RETRY_MS = 2000L;

    private static String loadedPath = "";
    private static long loadedModified = Long.MIN_VALUE;
    private static long nextCustomAttemptAt;
    private static boolean unavailable;
    private static Identifier boundTexture = BUNDLED_TEXTURE;
    private static float imageAspect = MapArtOverridePolicy.FOX_IMAGE_ASPECT;

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
        int light = MapArtOverridePolicy.packedLight(lightCoords, 0);
        submitContainedMapQuad(
                poseStack,
                collector,
                access.rotclient$mapArtU0(),
                access.rotclient$mapArtV0(),
                access.rotclient$mapArtU1(),
                access.rotclient$mapArtV1(),
                light);
        return true;
    }

    /**
     * Item-frame maps. Vanilla already rotated the pose by
     * {@link MapArtOverridePolicy#mapFrameZDegrees(int)}; undo that so tiled UVs
     * stay wall-aligned when Hub frames alternate 0/2 rotation.
     */
    public static boolean renderFramedMap(
            MapRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int frameRotation) {
        if (!ensureTexture() || !(state instanceof MapArtRenderStateAccess access)) {
            return false;
        }
        int light = MapArtOverridePolicy.packedLight(lightCoords, 0);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                -MapArtOverridePolicy.mapFrameZDegrees(frameRotation)));
        submitContainedMapQuad(
                poseStack,
                collector,
                access.rotclient$mapArtU0(),
                access.rotclient$mapArtV0(),
                access.rotclient$mapArtU1(),
                access.rotclient$mapArtV1(),
                light);
        poseStack.popPose();
        return true;
    }

    /** Render the local image on a server painting without changing its entity data. */
    public static boolean renderPainting(
            PaintingRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (!ensureTexture() || state == null) {
            return false;
        }
        Direction direction = state.direction == null ? Direction.SOUTH : state.direction;
        if (!MapArtOverridePolicy.shouldReplacePainting(true, direction.getAxis().isHorizontal())) {
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
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - direction.get2DDataValue() * 90.0F));
        submitTextQuad(
                poseStack,
                collector,
                -width / 2.0F,
                -height / 2.0F,
                width / 2.0F,
                height / 2.0F,
                -0.03125F,
                0.0F,
                0.0F,
                1.0F,
                1.0F,
                light,
                true);
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

    public static void configureBlockDisplay(
            Display.BlockDisplay display,
            BlockDisplayEntityRenderState state,
            float tickProgress) {
        if (!(state instanceof FoxItemDisplayAccess access) || display == null) {
            return;
        }
        access.rotclient$setFoxUv(0.0F, 0.0F, 1.0F, 1.0F);
        access.rotclient$setFoxReplace(false);
        if (!isEnabled()) {
            return;
        }
        access.rotclient$setFoxReplace(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true,
                false,
                isFixedBillboard(display),
                scale(display, tickProgress, 0),
                scale(display, tickProgress, 1),
                scale(display, tickProgress, 2),
                display.getBbWidth(),
                display.getBbHeight()));
    }

    public static boolean renderDisplay(
            DisplayEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        if (!ensureTexture()
                || !(state instanceof FoxItemDisplayAccess access)
                || !access.rotclient$foxReplace()) {
            return false;
        }
        int light = MapArtOverridePolicy.packedLight(lightCoords, state.lightCoords);
        submitContainedTextQuad(
                poseStack,
                collector,
                -0.5F,
                -0.5F,
                0.5F,
                0.5F,
                0.03125F,
                access.rotclient$foxU0(),
                access.rotclient$foxV0(),
                access.rotclient$foxU1(),
                access.rotclient$foxV1(),
                light,
                true);
        return true;
    }

    public static boolean renderItemDisplay(
            ItemDisplayEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        return renderDisplay(state, poseStack, collector, lightCoords);
    }

    public static boolean renderItemFrameItem(
            ItemFrameRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords) {
        if (!ensureTexture()
                || !(state instanceof FoxItemFrameAccess access)
                || !access.rotclient$foxReplaceItem()) {
            return false;
        }
        int light = MapArtOverridePolicy.packedLight(lightCoords, state.lightCoords);
        submitContainedTextQuad(
                poseStack,
                collector,
                -0.5F,
                -0.5F,
                0.5F,
                0.5F,
                0.03125F,
                access.rotclient$foxU0(),
                access.rotclient$foxV0(),
                access.rotclient$foxU1(),
                access.rotclient$foxV1(),
                light,
                true);
        return true;
    }

    /** Attach one image region to an item-frame map or custom wall tile. */
    public static void configureItemFrame(ItemFrameRenderState state, ItemFrame frame) {
        if (state != null && state.mapRenderState instanceof MapArtRenderStateAccess mapAccess) {
            mapAccess.rotclient$setMapArtUv(0.0F, 0.0F, 1.0F, 1.0F);
        }
        if (state instanceof FoxItemFrameAccess frameAccess) {
            frameAccess.rotclient$setFoxUv(0.0F, 0.0F, 1.0F, 1.0F);
            frameAccess.rotclient$setFoxReplaceItem(false);
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (!isEnabled() || config == null || frame == null || state == null) {
            return;
        }
        boolean stretch = config.extras().mapArtStretchFrames
                && frame.getDirection().getAxis().isHorizontal();
        try {
            Panel mapPanel = stretch ? findPanel(frame, true, 2) : null;
            if (mapPanel != null && state.mapRenderState instanceof MapArtRenderStateAccess mapAccess) {
                mapAccess.rotclient$setMapArtUv(
                        mapPanel.u0(), mapPanel.v0(), mapPanel.u1(), mapPanel.v1());
            }
            if (state instanceof FoxItemFrameAccess frameAccess && state.mapId == null) {
                boolean paintingOrMap = isMapOrPaintingItem(frame.getItem());
                Panel wall = mapPanel != null
                        ? mapPanel
                        : (stretch ? findPanel(frame, false, 4) : null);
                if (paintingOrMap || wall != null) {
                    frameAccess.rotclient$setFoxReplaceItem(true);
                    if (wall != null) {
                        frameAccess.rotclient$setFoxUv(wall.u0(), wall.v0(), wall.u1(), wall.v1());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // A stale entity during a chunk transition must leave this frame as a single image.
        }
    }

    public static void reset() {
        loadedPath = "";
        loadedModified = Long.MIN_VALUE;
        nextCustomAttemptAt = 0L;
        unavailable = false;
        boundTexture = BUNDLED_TEXTURE;
        imageAspect = MapArtOverridePolicy.FOX_IMAGE_ASPECT;
    }

    private static void submitContainedMapQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float u0,
            float v0,
            float u1,
            float v1,
            int light) {
        submitMapQuad(poseStack, collector, 0.0F, 128.0F, 128.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, light, 0xFF000000);
        float[] image = MapArtOverridePolicy.containedTileImageQuad(u0, v0, u1, v1);
        if (image == null) {
            return;
        }
        submitMapQuad(
                poseStack,
                collector,
                image[0] * 128.0F,
                image[3] * 128.0F,
                image[2] * 128.0F,
                image[1] * 128.0F,
                image[4],
                image[5],
                image[6],
                image[7],
                light,
                -1);
    }

    private static void submitContainedTextQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            float u0,
            float v0,
            float u1,
            float v1,
            int light,
            boolean doubleSided) {
        submitTextQuad(poseStack, collector, x0, y0, x1, y1, z, 0.0F, 0.0F, 1.0F, 1.0F, light, doubleSided, 0xFF000000);
        float[] image = MapArtOverridePolicy.containedTileImageQuad(u0, v0, u1, v1);
        if (image == null) {
            return;
        }
        float subX0 = x0 + (x1 - x0) * image[0];
        float subX1 = x0 + (x1 - x0) * image[2];
        float subTop = y1 + (y0 - y1) * image[1];
        float subBottom = y1 + (y0 - y1) * image[3];
        submitTextQuad(
                poseStack,
                collector,
                subX0,
                subBottom,
                subX1,
                subTop,
                z,
                image[4],
                image[5],
                image[6],
                image[7],
                light,
                doubleSided,
                -1);
    }

    private static void submitMapQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x0,
            float y0,
            float x1,
            float y1,
            float u0,
            float v0,
            float u1,
            float v1,
            int light,
            int color) {
        collector.submitCustomGeometry(poseStack, RenderTypes.text(boundTexture), (pose, buffer) -> {
            buffer.addVertex(pose, x0, y0, -0.01F).setColor(color).setUv(u0, v1).setLight(light);
            buffer.addVertex(pose, x1, y0, -0.01F).setColor(color).setUv(u1, v1).setLight(light);
            buffer.addVertex(pose, x1, y1, -0.01F).setColor(color).setUv(u1, v0).setLight(light);
            buffer.addVertex(pose, x0, y1, -0.01F).setColor(color).setUv(u0, v0).setLight(light);
        });
    }

    private static void submitTextQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            float u0,
            float v0,
            float u1,
            float v1,
            int light,
            boolean doubleSided) {
        submitTextQuad(poseStack, collector, x0, y0, x1, y1, z, u0, v0, u1, v1, light, doubleSided, -1);
    }

    private static void submitTextQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            float u0,
            float v0,
            float u1,
            float v1,
            int light,
            boolean doubleSided,
            int color) {
        collector.submitCustomGeometry(poseStack, RenderTypes.text(boundTexture), (pose, buffer) -> {
            buffer.addVertex(pose, x1, y0, z).setColor(color).setUv(u1, v1).setLight(light);
            buffer.addVertex(pose, x0, y0, z).setColor(color).setUv(u0, v1).setLight(light);
            buffer.addVertex(pose, x0, y1, z).setColor(color).setUv(u0, v0).setLight(light);
            buffer.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v0).setLight(light);
            if (doubleSided) {
                buffer.addVertex(pose, x0, y0, -z).setColor(color).setUv(u0, v1).setLight(light);
                buffer.addVertex(pose, x1, y0, -z).setColor(color).setUv(u1, v1).setLight(light);
                buffer.addVertex(pose, x1, y1, -z).setColor(color).setUv(u1, v0).setLight(light);
                buffer.addVertex(pose, x0, y1, -z).setColor(color).setUv(u0, v0).setLight(light);
            }
        });
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
            loadedPath = BUNDLED_KEY;
            loadedModified = Long.MIN_VALUE;
            unavailable = false;
            boundTexture = BUNDLED_TEXTURE;
            imageAspect = MapArtOverridePolicy.FOX_IMAGE_ASPECT;
            return true;
        }
        Path image = resolveImage(configured);
        if (image == null || !Files.isRegularFile(image)) {
            loadedPath = BUNDLED_KEY;
            loadedModified = Long.MIN_VALUE;
            unavailable = false;
            boundTexture = BUNDLED_TEXTURE;
            imageAspect = MapArtOverridePolicy.FOX_IMAGE_ASPECT;
            return true;
        }
        long modified;
        try {
            modified = Files.getLastModifiedTime(image).toMillis();
        } catch (IOException ignored) {
            return false;
        }
        String absolute = image.toAbsolutePath().normalize().toString();
        if (absolute.equals(loadedPath) && modified == loadedModified && !unavailable) {
            boundTexture = CUSTOM_TEXTURE_ID;
            return true;
        }
        long now = System.currentTimeMillis();
        if (unavailable && now < nextCustomAttemptAt) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getTextureManager() == null) {
            return false;
        }
        try (InputStream stream = Files.newInputStream(image)) {
            NativeImage pixels = decodeToRgba(stream);
            client.getTextureManager().register(
                    CUSTOM_TEXTURE_ID,
                    new DynamicTexture(() -> "Rot Client local map art", pixels));
            loadedPath = absolute;
            loadedModified = modified;
            unavailable = false;
            boundTexture = CUSTOM_TEXTURE_ID;
            imageAspect = pixels.getHeight() > 0
                    ? pixels.getWidth() / (float) pixels.getHeight()
                    : MapArtOverridePolicy.FOX_IMAGE_ASPECT;
            return true;
        } catch (IOException | RuntimeException ignored) {
            unavailable = true;
            nextCustomAttemptAt = now + CUSTOM_RETRY_MS;
            return false;
        }
    }

    private static NativeImage decodeToRgba(InputStream stream) throws IOException {
        byte[] data = stream.readAllBytes();
        try {
            NativeImage image = NativeImage.read(data);
            if (image.format() == NativeImage.Format.RGBA) {
                return image;
            }
            image.close();
        } catch (IOException | RuntimeException ignored) {
            // Fall through to ImageIO so JPEG / GIF files still become GPU-safe RGBA.
        }
        BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(data));
        if (buffered == null) {
            throw new IOException("Could not decode local Fox image");
        }
        BufferedImage argb = new BufferedImage(
                buffered.getWidth(), buffered.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argb.createGraphics();
        graphics.drawImage(buffered, 0, 0, null);
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        if (!ImageIO.write(argb, "png", png)) {
            throw new IOException("Could not encode local Fox image as PNG");
        }
        return NativeImage.read(png.toByteArray());
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

    private static boolean isFixedBillboard(Display display) {
        Display.RenderState renderState = display.renderState();
        return renderState != null
                && renderState.billboardConstraints() == Display.BillboardConstraints.FIXED;
    }

    private static float scale(Display display, float tickProgress, int axis) {
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

    private static boolean isMapWallFrame(ItemFrame frame) {
        ItemStack stack = frame.getItem();
        boolean glowOrNormal = frame instanceof GlowItemFrame || frame instanceof ItemFrame;
        return glowOrNormal
                && MapArtOverridePolicy.isMapWallItem(
                        stack.is(Items.FILLED_MAP),
                        stack.is(Items.MAP),
                        stack.has(DataComponents.MAP_ID) || frame.getFramedMapId(stack) != null);
    }

    private static AABB searchBox(BlockPos originPos) {
        return new AABB(
                originPos.getX() - SEARCH_RADIUS,
                originPos.getY() - SEARCH_RADIUS,
                originPos.getZ() - SEARCH_RADIUS,
                originPos.getX() + SEARCH_RADIUS + 1,
                originPos.getY() + SEARCH_RADIUS + 1,
                originPos.getZ() + SEARCH_RADIUS + 1);
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
                Display.ItemDisplay.class, searchBox(originPos),
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
        Panel flooded = panelAt(originPos, direction, positions, 2);
        return preferHubPanel(originPos, direction, displays.keySet(), flooded, 2);
    }

    private static Panel findPanel(ItemFrame origin, boolean mapsOnly, int minTiles) {
        Direction direction = origin.getDirection();
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        BlockPos originPos = origin.blockPosition();
        positions.add(originPos);
        pending.add(originPos);

        Map<BlockPos, ItemFrame> frames = new HashMap<>();
        for (ItemFrame candidate : origin.level().getEntitiesOfClass(
                ItemFrame.class, searchBox(originPos),
                candidate -> candidate.getDirection() == direction
                        && !candidate.getItem().isEmpty()
                        && (!mapsOnly || isMapWallFrame(candidate)))) {
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
        Panel flooded = panelAt(originPos, direction, positions, minTiles);
        return preferHubPanel(originPos, direction, frames.keySet(), flooded, minTiles);
    }

    private static Panel preferHubPanel(
            BlockPos originPos,
            Direction direction,
            Set<BlockPos> nearby,
            Panel flooded,
            int minTiles) {
        Panel boxed = panelAt(originPos, direction, nearby, minTiles);
        if (boxed != null && hubSized(nearby, direction)) {
            return boxed;
        }
        return flooded;
    }

    private static boolean hubSized(Set<BlockPos> positions, Direction direction) {
        if (positions.isEmpty()) {
            return false;
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
        return (maxColumn - minColumn + 1) == MapArtOverridePolicy.HUB_MAP_COLUMNS
                && (maxY - minY + 1) == MapArtOverridePolicy.HUB_MAP_ROWS;
    }

    private static Panel panelAt(
            BlockPos originPos, Direction direction, Set<BlockPos> positions, int minTiles) {
        if (positions.size() < minTiles) {
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
        if (!MapArtOverridePolicy.isMapPanelLayout(positions.size(), width, height)
                || positions.size() < minTiles) {
            return null;
        }
        int currentColumn = column(originPos, direction) - minColumn;
        int currentRow = maxY - originPos.getY();
        float[] uv = MapArtOverridePolicy.tileUvContain(
                currentColumn, currentRow, width, height, imageAspect);
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
