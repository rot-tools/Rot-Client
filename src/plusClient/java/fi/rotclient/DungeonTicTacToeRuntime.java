package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

import static fi.rotclient.DungeonRuntime.*;

/** Plus-only live board scan for the Tic Tac Toe dungeon puzzle. */
final class DungeonTicTacToeRuntime {
    private DungeonTicTacToeRuntime() {
    }

    static void scan(Minecraft client) {
        if (!hashedRoomIs(client, "Tic Tac Toe") || client.level == null || client.player == null) {
            return;
        }
        int cx = DungeonRoomDataPolicy.roomCenter((int) Math.floor(client.player.getX()));
        int cz = DungeonRoomDataPolicy.roomCenter((int) Math.floor(client.player.getZ()));
        AABB box = new AABB(cx - 9, 65, cz - 9, cx + 9, 73, cz + 9);
        List<ItemFrame> frames = new ArrayList<>();
        for (ItemFrame frame : client.level.getEntitiesOfClass(ItemFrame.class, box)) {
            if (frame.getItem().is(Items.FILLED_MAP) && frame.getItem().has(DataComponents.MAP_ID)) {
                frames.add(frame);
            }
        }
        if (frames.size() == 8 || frames.size() % 2 == 0) {
            return;
        }
        char[] board = new char[9];
        DungeonTicTacToePolicy.BoardPos leftmost = null;
        char facing = 'X';
        int sign = 1;
        for (ItemFrame frame : frames) {
            MapId mapId = frame.getItem().get(DataComponents.MAP_ID);
            if (mapId == null) {
                continue;
            }
            MapItemSavedData mapData = client.level.getMapData(mapId);
            if (mapData == null || mapData.colors == null || mapData.colors.length <= 8256) {
                continue;
            }
            Direction direction = frame.getDirection();
            sign = direction == Direction.SOUTH || direction == Direction.WEST ? -1 : 1;
            BlockPos framePos = frame.blockPosition();
            boolean alongX = Math.abs(frame.getX() % 0.5D) < 1.0E-6D;
            facing = alongX ? 'X' : 'Z';
            int row = 0;
            for (int i = 2; i >= 0; i--) {
                int realI = i * sign;
                BlockPos candidate = alongX
                        ? framePos.offset(realI, 0, 0)
                        : framePos.offset(0, 0, realI);
                String id = blockId(client, candidate);
                if (id.contains("stone_button") || DungeonPuzzleBoardPolicy.isAir(fullBlockId(client, candidate))) {
                    leftmost = new DungeonTicTacToePolicy.BoardPos(
                            candidate.getX(), candidate.getY(), candidate.getZ());
                    row = i;
                    break;
                }
            }
            int column = 72 - (int) frame.getY();
            if (column < 0 || column > 2) {
                continue;
            }
            char mark = DungeonTicTacToePolicy.markFromMapColor(mapData.colors[8256]);
            if (mark != DungeonTicTacToePolicy.EMPTY) {
                board[DungeonTicTacToePolicy.boardIndex(column, row)] = mark;
            }
        }
        if (leftmost == null) {
            return;
        }
        for (int index : DungeonTicTacToePolicy.findBestMoves(
                board, DungeonTicTacToePolicy.PLAYER, DungeonTicTacToePolicy.OPPONENT)) {
            DungeonTicTacToePolicy.BoardPos pos =
                    DungeonTicTacToePolicy.indexToPos(index, leftmost, facing, sign);
            addPuzzleMark(blockBox(pos.x(), pos.y(), pos.z()), 0xFF22C55E);
        }
    }
}
