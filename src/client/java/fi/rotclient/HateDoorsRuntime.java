package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I Hate Doors: client-side stained-glass rewrite of wither/blood/entrance
 * door blocks. Positions are scanned in {@link DungeonRuntime}; a Level mixin
 * returns glass from {@code getBlockState} so the rewrite is not ESP-only.
 */
public final class HateDoorsRuntime {
    private static final Map<Long, EmberDungeonPolicy.GlassTint> DOORS = new ConcurrentHashMap<>();
    private static final Map<Long, EmberDungeonPolicy.GlassTint> DIORITE = new ConcurrentHashMap<>();

    private HateDoorsRuntime() {
    }

    public static void clear() {
        DOORS.clear();
        DIORITE.clear();
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
        BlockState glass = glassState(tint);
        return glass == original ? null : glass;
    }

    static void scan(Minecraft client, QolSkyblockExtras extras) {
        if (client == null || client.player == null || client.level == null || extras == null) {
            return;
        }
        if (!extras.dungeonEspEnabled || !extras.dungeonEspHateDoors) {
            DOORS.clear();
        } else {
            BlockPos origin = client.player.blockPosition();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -4; dy <= 8; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        BlockState state = client.level.getBlockState(pos);
                        if (state.isAir() || state.is(Blocks.BARRIER)) {
                            forget(pos);
                            continue;
                        }
                        String id = blockId(state);
                        EmberDungeonPolicy.hateDoorTint(
                                id,
                                extras.dungeonEspHateWither,
                                extras.dungeonEspHateBlood,
                                extras.dungeonEspHateEntrance,
                                extras.dungeonEspHateWitherGlass,
                                extras.dungeonEspHateBloodGlass,
                                extras.dungeonEspHateEntranceGlass)
                                .ifPresent(tint -> remember(pos, tint));
                    }
                }
            }
        }
        scanDiorite(client, extras);
    }

    private static void scanDiorite(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonF7Enabled || !extras.dungeonF7HideDiorite || client.player == null) {
            DIORITE.clear();
            return;
        }
        BlockPos origin = client.player.blockPosition();
        if (!DungeonPolicy.inF7PillarBox(origin.getX(), origin.getY(), origin.getZ())) {
            DIORITE.clear();
            return;
        }
        for (DungeonF7Policy.Pillar pillar : DungeonF7Policy.dioritePillars()) {
            EmberDungeonPolicy.IntVec base = pillar.origin();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 0; dy <= 37; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos pos = new BlockPos(base.x() + dx, base.y() + dy, base.z() + dz);
                        if (origin.distSqr(pos) > 64 * 64) {
                            continue;
                        }
                        BlockState state = client.level.getBlockState(pos);
                        String id = blockId(state);
                        if (DungeonPolicy.isF7Diorite(id)
                                || id.contains(EmberDungeonPolicy.glassBlockId(pillar.tint()).replace("minecraft:", ""))) {
                            DIORITE.put(pos.asLong(), pillar.tint());
                        }
                    }
                }
            }
        }
    }

    static BlockState glassState(EmberDungeonPolicy.GlassTint tint) {
        Identifier location = Identifier.tryParse(EmberDungeonPolicy.glassBlockId(tint));
        var block = location == null ? null : BuiltInRegistries.BLOCK.getValue(location);
        if (block == null || block == Blocks.AIR) {
            return Blocks.GLASS.defaultBlockState();
        }
        return block.defaultBlockState();
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
