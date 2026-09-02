package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class DungeonMapPolicyTest {
    @Test
    void entranceRunCalibratesRoomSize() {
        byte[] colors = new byte[128 * 128];
        int start = 22 + 22 * 128;
        for (int i = 0; i < 16; i++) {
            colors[start + i] = DungeonMapPolicy.COLOR_ENTRANCE;
        }
        DungeonMapPolicy.Calibration cal = DungeonMapPolicy.calibrate(colors, 128);
        assertTrue(cal.ok());
        assertEquals(16, cal.roomSize());
        assertEquals(20, cal.roomGap());
    }

    @Test
    void scanFindsRoomsDoorsPuzzlesAndBlood() {
        byte[] colors = new byte[128 * 128];
        int startX = 22;
        int startZ = 22;
        int size = 16;
        int gap = 20;
        paintRoom(colors, startX, startZ, size, DungeonMapPolicy.COLOR_ENTRANCE);
        paintRoom(colors, startX + gap, startZ, size, DungeonMapPolicy.COLOR_NORMAL);
        paintRoom(colors, startX + gap * 2, startZ, size, DungeonMapPolicy.COLOR_PUZZLE);
        paintRoom(colors, startX, startZ + gap, size, DungeonMapPolicy.COLOR_BLOOD);
        int half = size / 2;
        int connection = size + 2;
        colors[(startZ + half) * 128 + (startX + connection)] = DungeonMapPolicy.COLOR_NORMAL;
        colors[(startZ + connection) * 128 + (startX + half)] = DungeonMapPolicy.COLOR_WITHER_DOOR;
        colors[(startZ + half) * 128 + (startX + gap + half)] = DungeonMapPolicy.COLOR_WHITE_CHECK;

        DungeonMapPolicy.Board board = DungeonMapPolicy.scan(colors, 128, List.of(
                new DungeonMapPolicy.PlayerIcon(30, 30, 90.0F, false)), 0.0D, 0.0D);
        assertTrue(board.rooms().size() >= 4);
        assertTrue(board.summary().contains("puzzle"));
        assertTrue(board.summary().contains("blood"));
        assertFalse(board.doors().isEmpty());
        boolean sawPuzzle = board.rooms().stream().anyMatch(room ->
                room.type() == DungeonMapPolicy.RoomType.PUZZLE);
        boolean sawCheck = board.rooms().stream().anyMatch(room ->
                room.checkmark() == DungeonMapPolicy.Checkmark.WHITE);
        assertTrue(sawPuzzle);
        assertTrue(sawCheck);
        DungeonPuzzlePolicy.MapPreview preview = DungeonMapPolicy.render(colors, 128, board, true, true);
        assertTrue(preview.width() >= 48);
        assertTrue(preview.summary().contains("rooms"));
    }

    @Test
    void hypixelPaletteClassifiesDoorBytes() {
        assertEquals(DungeonMapPolicy.RoomType.ENTRANCE, DungeonMapPolicy.roomType((byte) 30));
        assertEquals(DungeonMapPolicy.RoomType.BLOOD, DungeonMapPolicy.roomType((byte) 18));
        assertEquals(DungeonMapPolicy.RoomType.PUZZLE, DungeonMapPolicy.roomType((byte) 66));
        assertEquals(DungeonMapPolicy.RoomType.FAIRY, DungeonMapPolicy.roomType((byte) 82));
        assertEquals(DungeonMapPolicy.DoorType.WITHER, DungeonMapPolicy.doorType((byte) 119));
        assertEquals(DungeonMapPolicy.DoorType.OPENED_WITHER, DungeonMapPolicy.doorType((byte) 51));
        assertEquals(DungeonMapPolicy.DoorType.ENTRANCE, DungeonMapPolicy.doorType((byte) 30));
        assertEquals(0xFF7C3AED, DungeonMapPolicy.doorArgb(DungeonMapPolicy.DoorType.WITHER));
        assertEquals(0xFFC4A574, DungeonMapPolicy.argb(DungeonMapPolicy.RoomType.NORMAL));
        assertEquals(0xFFA855F7, DungeonMapPolicy.argb(DungeonMapPolicy.RoomType.PUZZLE));
    }

    private static void paintRoom(byte[] colors, int x, int z, int size, byte color) {
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                colors[(z + dz) * 128 + (x + dx)] = color;
            }
        }
    }
}
