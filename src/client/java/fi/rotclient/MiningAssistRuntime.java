package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crystal Hollows metal-detector chests, wishing-compass intersection, and
 * Fossil Excavator heatmap. Policy stays in {@link MiningAssistPolicy}.
 */
public final class MiningAssistRuntime {
    private static MiningAssistPolicy.Vec3 compassA;
    private static MiningAssistPolicy.Vec3 compassDirA;
    private static MiningAssistPolicy.Vec3 compassHit;
    private static boolean compassWasUsing;
    private static Double treasureMeters;
    private static final List<MiningAssistPolicy.BlockPos> detectorHits = new ArrayList<>();
    private static BlockPos miningPos;
    private static String miningBlockId = "";

    private MiningAssistRuntime() {
    }

    static void clear() {
        compassA = null;
        compassDirA = null;
        compassHit = null;
        compassWasUsing = false;
        treasureMeters = null;
        detectorHits.clear();
        miningPos = null;
        miningBlockId = "";
    }

    public static void startMining(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client == null || client.level == null) {
            return;
        }
        miningPos = pos.immutable();
        miningBlockId = blockId(client.level.getBlockState(pos));
    }

    public static void stopMining() {
        miningPos = null;
        miningBlockId = "";
    }

    public static boolean shouldIgnoreUpdate(BlockPos pos, BlockState incoming) {
        if (pos == null || incoming == null || miningPos == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.miningHelpersEnabled) {
            return false;
        }
        return MiningAssistPolicy.shouldIgnoreMiningUpdate(
                extras.miningHelpersBreakReset,
                extras.miningHelpersGemstoneDesync,
                pos.equals(miningPos),
                miningBlockId,
                blockId(incoming));
    }

    private static String blockId(BlockState state) {
        if (state == null) {
            return "";
        }
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.toString();
    }

    static void observeOverlay(String text) {
        MiningAssistPolicy.parseTreasureMeters(text).ifPresent(meters -> treasureMeters = meters);
    }

    static void observeChat(String text) {
        if (MiningAssistPolicy.isDetectorFoundChat(text)) {
            detectorHits.clear();
            treasureMeters = null;
        }
        if (MiningAssistPolicy.isCompassFail(text)) {
            compassA = null;
            compassDirA = null;
            compassHit = null;
        }
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras extras = extras();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        if (!extras.miningHelpersEnabled) {
            detectorHits.clear();
            return;
        }
        if (extras.miningHelpersDetectorSolver && treasureMeters != null) {
            refreshDetectorHits(client, treasureMeters);
        } else {
            detectorHits.clear();
        }
        if (extras.miningHelpersWishingCompass) {
            tickCompass(client.player);
        }
    }

    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.miningHelpersEnabled) {
            return;
        }
        if (extras.miningHelpersDetectorSolver) {
            for (MiningAssistPolicy.BlockPos pos : detectorHits) {
                AABB box = new AABB(
                        pos.x(), pos.y(), pos.z(),
                        pos.x() + 1, pos.y() + 1, pos.z() + 1);
                Gizmos.cuboid(box, GizmoStyle.stroke(0xFFFFAA00, 2.0F)).setAlwaysOnTop();
            }
        }
        if (extras.miningHelpersWishingCompass && compassHit != null) {
            AABB box = new AABB(
                    compassHit.x() - 0.4D,
                    compassHit.y() - 0.4D,
                    compassHit.z() - 0.4D,
                    compassHit.x() + 0.4D,
                    compassHit.y() + 0.4D,
                    compassHit.z() + 0.4D);
            Gizmos.cuboid(box, GizmoStyle.stroke(0xFF55FFFF, 2.5F)).setAlwaysOnTop();
        }
    }

    static int fossilHighlight(AbstractContainerScreen<?> screen, int slotIndex, ItemStack stack) {
        QolSkyblockExtras extras = extras();
        if (!extras.miningHelpersEnabled || !extras.miningHelpersFossilExcavator) {
            return 0;
        }
        if (screen == null || screen.getTitle() == null) {
            return 0;
        }
        if (!MiningLeftoverPolicy.isFossilExcavatorScreen(screen.getTitle().getString())) {
            return 0;
        }
        if (slotIndex < 0 || slotIndex >= MiningAssistPolicy.FOSSIL_SLOTS) {
            return 0;
        }
        MiningAssistPolicy.FossilTile[] board = readFossilBoard(screen);
        String percent = fossilPercent(screen);
        double[] heat = MiningAssistPolicy.fossilHeatmap(board, percent);
        int best = MiningAssistPolicy.bestFossilSlot(heat);
        if (best == slotIndex) {
            return 0xAA22C55E;
        }
        if (heat[slotIndex] > 0.0D) {
            return 0x6622C55E;
        }
        return 0;
    }

    private static void refreshDetectorHits(Minecraft client, double meters) {
        int previous = detectorHits.size();
        detectorHits.clear();
        LocalPlayer player = client.player;
        MiningAssistPolicy.BlockPos center = null;
        AABB search = player.getBoundingBox().inflate(80.0D, 40.0D, 80.0D);
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
            Optional<String> keeper = MiningAssistPolicy.parseKeeper(stand.getName().getString());
            if (keeper.isEmpty()) {
                continue;
            }
            center = MiningAssistPolicy.minesCenter(
                    keeper.get(),
                    stand.blockPosition().getX(),
                    stand.blockPosition().getY(),
                    stand.blockPosition().getZ()).orElse(null);
            if (center != null) {
                break;
            }
        }
        if (center == null) {
            return;
        }
        List<MiningAssistPolicy.BlockPos> hits = MiningAssistPolicy.filterByDistance(
                MiningAssistPolicy.knownChests(center),
                player.getX(),
                player.getY(),
                player.getZ(),
                meters);
        if (MiningAssistPolicy.shouldRenderDetector(hits)) {
            detectorHits.addAll(hits);
        }
        SkyBlockUtilityRuntime.onDetectorHits(previous, detectorHits.size());
    }

    private static void tickCompass(LocalPlayer player) {
        boolean using = player.isUsingItem()
                && "WISHING_COMPASS".equalsIgnoreCase(
                        AutoClickerItemIdentity.skyBlockId(player.getUseItem()));
        if (using && !compassWasUsing) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            MiningAssistPolicy.Vec3 start = new MiningAssistPolicy.Vec3(eye.x, eye.y, eye.z);
            MiningAssistPolicy.Vec3 dir = new MiningAssistPolicy.Vec3(look.x, look.y, look.z);
            if (compassA == null) {
                compassA = start;
                compassDirA = dir;
            } else {
                compassHit = MiningAssistPolicy.intersectCompassLines(
                        compassA, compassDirA, start, dir).orElse(null);
                compassA = start;
                compassDirA = dir;
            }
        }
        compassWasUsing = using;
    }

    private static MiningAssistPolicy.FossilTile[] readFossilBoard(AbstractContainerScreen<?> screen) {
        MiningAssistPolicy.FossilTile[] board =
                new MiningAssistPolicy.FossilTile[MiningAssistPolicy.FOSSIL_SLOTS];
        for (int i = 0; i < board.length; i++) {
            board[i] = MiningAssistPolicy.FossilTile.UNKNOWN;
        }
        List<Slot> slots = screen.getMenu().slots;
        int limit = Math.min(MiningAssistPolicy.FOSSIL_SLOTS, slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
            String id = stack == null || stack.isEmpty()
                    ? ""
                    : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            board[i] = MiningAssistPolicy.tileFromItem(id);
        }
        return board;
    }

    private static String fossilPercent(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || slot.getItem().isEmpty()) {
                continue;
            }
            for (String line : InventoryChromeRuntime.loreLines(slot.getItem())) {
                Optional<Double> pct = MiningAssistPolicy.parseFossilProgress(line);
                if (pct.isPresent()) {
                    return String.format(java.util.Locale.ROOT, "%.1f", pct.get());
                }
            }
        }
        return "";
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
