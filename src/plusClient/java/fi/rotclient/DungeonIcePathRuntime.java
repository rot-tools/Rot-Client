package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

import static fi.rotclient.DungeonRuntime.*;

/** Plus-only world scan and solution path for the Ice Path puzzle. */
final class DungeonIcePathRuntime {
    private DungeonIcePathRuntime() {
    }

    static void scan(Minecraft client) {
        if (!hashedRoomIs(client, "Ice Path") || client.level == null || client.player == null) {
            return;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return;
        }
        DungeonPuzzleBoardPolicy.BlockProbe probe = relativeBlockProbe(client, rotation);
        boolean[][] blocked = new boolean[DungeonIcePathPolicy.GRID][DungeonIcePathPolicy.GRID];
        for (int z = 0; z < DungeonIcePathPolicy.GRID; z++) {
            for (int x = 0; x < DungeonIcePathPolicy.GRID; x++) {
                DungeonPuzzleBoardPolicy.RelPos rel = DungeonIcePathPolicy.relPos(x, z);
                blocked[z][x] = !DungeonPuzzleBoardPolicy.isAir(probe.idAt(rel.x(), rel.y(), rel.z()));
            }
        }
        DungeonRoomDataPolicy.IntVec center = DungeonRoomDataPolicy.fromComp(15, 67, 15, rotation);
        AABB box = new AABB(
                center.x() - 10, 67, center.z() - 10,
                center.x() + 11, 68, center.z() + 11);
        Silverfish fish = null;
        for (Silverfish candidate : client.level.getEntitiesOfClass(Silverfish.class, box)) {
            fish = candidate;
            break;
        }
        if (fish == null) {
            return;
        }
        BlockPos fishPos = fish.blockPosition();
        DungeonRoomDataPolicy.IntVec rel = DungeonRoomDataPolicy.toComp(
                fishPos.getX(), fishPos.getY(), fishPos.getZ(), rotation);
        Optional<DungeonIcePathPolicy.GridPos> start =
                DungeonIcePathPolicy.gridFromRel(rel.x(), rel.z());
        if (start.isEmpty()) {
            return;
        }
        List<DungeonIcePathPolicy.GridPos> path = DungeonIcePathPolicy.expandPath(
                DungeonIcePathPolicy.solve(blocked, start.get().x(), start.get().z()));
        boolean first = true;
        for (DungeonIcePathPolicy.GridPos cell : path) {
            if (cell.equals(start.get())) {
                continue;
            }
            DungeonPuzzleBoardPolicy.RelPos worldRel = DungeonIcePathPolicy.relPos(cell.x(), cell.z());
            DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(worldRel, rotation);
            addPuzzleMark(
                    blockBox(world.x(), world.y(), world.z()),
                    first ? 0xFF22C55E : 0xFF38BDF8);
            first = false;
        }
    }
}
