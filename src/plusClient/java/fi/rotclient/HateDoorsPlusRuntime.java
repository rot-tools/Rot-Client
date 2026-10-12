package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I Hate Doors: client-side stained-glass rewrite of wither/blood/entrance
 * door blocks. Positions are scanned in {@link DungeonRuntime}; a Level mixin
 * returns glass from {@code getBlockState} so the rewrite is not ESP-only.
 */
public final class HateDoorsPlusRuntime {
    private static final Map<Long, EmberDungeonPolicy.GlassTint> DOORS = new ConcurrentHashMap<>();
    private static final Map<Long, EmberDungeonPolicy.GlassTint> DOOR_MARKS = new ConcurrentHashMap<>();
    private static final Map<Long, EmberDungeonPolicy.GlassTint> DIORITE = new ConcurrentHashMap<>();
    private static final Map<EmberDungeonPolicy.GlassTint, BlockState> GLASS = new ConcurrentHashMap<>();
    private static final DungeonBlockScanPolicy doorScan = new DungeonBlockScanPolicy();
    private static final DungeonBlockScanPolicy pillarScan = new DungeonBlockScanPolicy();
    private static int pillarIndex;
    private static final List<DungeonF7Policy.Pillar> PILLARS = DungeonF7Policy.dioritePillars();

    record DoorMark(BlockPos pos, EmberDungeonPolicy.GlassTint tint) { }

    static List<DoorMark> observedDoors() {
        return DOOR_MARKS.entrySet().stream().map(entry -> new DoorMark(BlockPos.of(entry.getKey()), entry.getValue())).toList();
    }

    private HateDoorsPlusRuntime() {
    }

    public static void clear() {
        DOORS.clear();
        DOOR_MARKS.clear();
        DIORITE.clear();
        doorScan.clear();
        pillarScan.clear();
        pillarIndex = 0;
    }

    public static void remember(BlockPos pos, EmberDungeonPolicy.GlassTint tint) {
        if (pos == null || tint == null) {
            return;
        }
        DOORS.put(pos.asLong(), tint);
    }

    public static void forget(BlockPos pos) {
        if (pos != null) {
            DOORS.remove(pos.asLong());
            DOOR_MARKS.remove(pos.asLong());
        }
    }

    public static BlockState rewrite(BlockPos pos, BlockState original) {
        if (pos == null || original == null) {
            return null;
        }
        QolSkyblockExtras extras = extras();
        if (extras == null) {
            return null;
        }
        if (!DIORITE.isEmpty() && extras.dungeonF7Enabled && extras.dungeonF7HideDiorite) {
            EmberDungeonPolicy.GlassTint diorite = DIORITE.get(pos.asLong());
            if (diorite != null) {
                if (original.isAir() || original.is(Blocks.BARRIER)) {
                    DIORITE.remove(pos.asLong());
                } else {
                    String id = blockId(original);
                    if (!DungeonPolicy.isF7Diorite(id)
                            && !id.equals(EmberDungeonPolicy.glassBlockId(diorite))) {
                        DIORITE.remove(pos.asLong());
                        return null;
                    }
                    BlockState glass = glassState(diorite);
                    if (glass != original) {
                        return glass;
                    }
                }
            }
        }
        if (DOORS.isEmpty() || !extras.dungeonEspEnabled || !extras.dungeonEspHateDoors) {
            return null;
        }
        if (original.isAir() || original.is(Blocks.BARRIER)) {
            DOORS.remove(pos.asLong());
            return null;
        }
        EmberDungeonPolicy.GlassTint tint = DOORS.get(pos.asLong());
        if (tint == null) {
            return null;
        }
        // Never keep replacing a door after its true server block or child setting changes.
        var current = EmberDungeonPolicy.hateDoorTint(blockId(original),
                extras.dungeonEspHateWither, extras.dungeonEspHateBlood, extras.dungeonEspHateEntrance,
                extras.dungeonEspHateWitherGlass, extras.dungeonEspHateBloodGlass, extras.dungeonEspHateEntranceGlass);
        if (current.isEmpty()) { forget(pos); return null; }
        BlockState glass = glassState(current.get());
        return glass == original ? null : glass;
    }

    static void scan(Minecraft client, QolSkyblockExtras extras) {
        if (client == null || client.player == null || client.level == null || extras == null) {
            clear();
            return;
        }
        if (!SkyBlockDungeonDetector.confidentlyInDungeon()) {
            clear();
            return;
        }
        if (!extras.dungeonEspEnabled || !extras.dungeonEspHateDoors) {
            DOORS.clear();
            DOOR_MARKS.clear();
            doorScan.clear();
        } else {
            BlockPos origin = client.player.blockPosition();
            var slice = doorScan.next(new DungeonBlockScanPolicy.Region(
                    origin.getX() - 8, origin.getY() - 4, origin.getZ() - 8, 17, 13, 17));
            if (slice.started()) DOORS.keySet().removeIf(key -> {
                BlockPos pos = BlockPos.of(key);
                return !slice.region().contains(pos.getX(), pos.getY(), pos.getZ());
            });
            if (slice.started()) DOOR_MARKS.keySet().removeIf(key -> {
                BlockPos pos = BlockPos.of(key);
                return !slice.region().contains(pos.getX(), pos.getY(), pos.getZ());
            });
            for (var cell : slice.cells()) {
                BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
                if (!client.level.hasChunkAt(pos)) { forget(pos); continue; }
                // Read the actual chunk state, not our own Level glass substitution.
                BlockState state = client.level.getChunkAt(pos).getBlockState(pos);
                String id = blockId(state);
                var tint = EmberDungeonPolicy.hateDoorTint(id,
                        extras.dungeonEspHateWither, extras.dungeonEspHateBlood, extras.dungeonEspHateEntrance,
                        extras.dungeonEspHateWitherGlass, extras.dungeonEspHateBloodGlass, extras.dungeonEspHateEntranceGlass);
                if (tint.isPresent()) remember(pos, tint.get()); else forget(pos);
                if (EmberDungeonPolicy.isHateDoorBlock(id,
                        extras.dungeonEspHateWither, extras.dungeonEspHateBlood, extras.dungeonEspHateEntrance)) {
                    // Iron bars remain entrance ESP without turning them into glass.
                    DOOR_MARKS.put(pos.asLong(), tint.orElse(EmberDungeonPolicy.glassTint(extras.dungeonEspHateEntranceGlass)));
                }
            }
        }
        scanDiorite(client, extras);
    }

    private static void scanDiorite(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonF7Enabled || !extras.dungeonF7HideDiorite || client.player == null
                || !DungeonRuntime.sidebar.boss() || DungeonMapPolicy.floorNumber(DungeonRuntime.sidebar.floor()) != 7) {
            DIORITE.clear();
            pillarScan.clear();
            pillarIndex = 0;
            return;
        }
        BlockPos origin = client.player.blockPosition();
        if (!DungeonPolicy.inF7PillarBox(origin.getX(), origin.getY(), origin.getZ())) {
            DIORITE.clear();
            pillarScan.clear();
            pillarIndex = 0;
            return;
        }
        var pillar = PILLARS.get(pillarIndex);
        var base = pillar.origin();
        var slice = pillarScan.next(new DungeonBlockScanPolicy.Region(base.x() - 3, base.y(), base.z() - 3, 7, 38, 7));
        for (var cell : slice.cells()) {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            if (origin.distSqr(pos) > 64 * 64 || !client.level.hasChunkAt(pos)) {
                DIORITE.remove(pos.asLong());
                continue;
            }
            String id = blockId(client.level.getChunkAt(pos).getBlockState(pos));
            if (DungeonPolicy.isF7Diorite(id) || id.equals(EmberDungeonPolicy.glassBlockId(pillar.tint()))) {
                DIORITE.put(pos.asLong(), pillar.tint());
            } else DIORITE.remove(pos.asLong());
        }
        if (slice.complete()) {
            pillarIndex = (pillarIndex + 1) % PILLARS.size();
            pillarScan.clear();
        }
    }

    static BlockState glassState(EmberDungeonPolicy.GlassTint tint) {
        return GLASS.computeIfAbsent(tint, key -> {
            Identifier location = Identifier.tryParse(EmberDungeonPolicy.glassBlockId(key));
            var block = location == null ? null : BuiltInRegistries.BLOCK.getValue(location);
            return block == null || block == Blocks.AIR ? Blocks.GLASS.defaultBlockState() : block.defaultBlockState();
        });
    }

    private static String blockId(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.toString();
    }

    private static QolSkyblockExtras extras() {
        try {
            return RotClientClient.qolConfigPublic().extras();
        } catch (Exception ignored) {
            return null;
        }
    }
}
