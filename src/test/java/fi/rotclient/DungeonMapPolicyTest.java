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
        assertEquals(2, cal.startX());
        assertEquals(2, cal.startZ());
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
        assertEquals(0xFF101010, DungeonMapPolicy.doorArgb(DungeonMapPolicy.DoorType.WITHER));
        assertEquals(0xFF794600, DungeonMapPolicy.argb(DungeonMapPolicy.RoomType.NORMAL));
        assertEquals(0xFF7B007B, DungeonMapPolicy.argb(DungeonMapPolicy.RoomType.PUZZLE));
        assertEquals(DungeonMapPolicy.RoomType.UNDISCOVERED, DungeonMapPolicy.roomType((byte) 85));
        assertEquals(DungeonMapPolicy.Checkmark.QUESTION, DungeonMapPolicy.checkmark((byte) 85, (byte) 85));
        assertEquals(0xFF7B007B, DungeonMapPolicy.argb(
                DungeonMapPolicy.RoomType.PUZZLE, DungeonMapPolicy.Checkmark.QUESTION));
        assertEquals(0xFF414141, DungeonMapPolicy.argb(DungeonMapPolicy.RoomType.UNDISCOVERED));
        assertEquals(DungeonMapPolicy.RoomType.PUZZLE, DungeonMapPolicy.fromRoomData("puzzle"));
        assertEquals(DungeonMapPolicy.RoomType.MINIBOSS, DungeonMapPolicy.fromRoomData("yellow"));
        assertEquals(DungeonMapPolicy.RoomType.TRAP, DungeonMapPolicy.fromRoomData("trap"));
        assertEquals(0, DungeonMapPolicy.floorNumber("E"));
        assertEquals(0, DungeonMapPolicy.floorNumber("Entrance"));
        assertEquals(7, DungeonMapPolicy.floorNumber("F7"));
        assertEquals(5, DungeonMapPolicy.floorNumber("M5"));
        assertEquals(List.of("Gold", "Mine"), DungeonMapPolicy.nameLines("Gold Mine"));
        assertEquals(-200, DungeonMapPolicy.roomWorldOrigin(0));
        assertEquals(-185, DungeonMapPolicy.roomWorldCenter(0));
        assertEquals(-168, DungeonMapPolicy.roomWorldOrigin(1));
        List<DungeonMapPolicy.WorldDoor> worldDoors = DungeonMapPolicy.worldDoors(new DungeonMapPolicy.Board(
                DungeonMapPolicy.Calibration.none(),
                List.of(),
                List.of(
                        new DungeonMapPolicy.DoorTile(0, 0, false, DungeonMapPolicy.DoorType.WITHER),
                        new DungeonMapPolicy.DoorTile(0, 0, true, DungeonMapPolicy.DoorType.BLOOD),
                        new DungeonMapPolicy.DoorTile(1, 1, true, DungeonMapPolicy.DoorType.NORMAL)),
                List.of(),
                ""));
        assertEquals(2, worldDoors.size());
        DungeonMapPolicy.WorldDoor wither = worldDoors.getFirst();
        assertEquals(DungeonMapPolicy.DoorType.WITHER, wither.type());
        assertEquals(-186, wither.minX());
        assertEquals(-169, wither.minZ());
        DungeonMapPolicy.WorldDoor blood = worldDoors.get(1);
        assertEquals(DungeonMapPolicy.DoorType.BLOOD, blood.type());
        assertEquals(-169, blood.minX());
        assertEquals(-186, blood.minZ());
    }

    @Test
    void uniqueRoomsMergeConnectedTilesAndKeepDooredNeighborsApart() {
        DungeonMapPolicy.Calibration cal = new DungeonMapPolicy.Calibration(true, 22, 22, 16, 20, 0.625D);
        DungeonMapPolicy.RoomTile a = tile(0, 0, DungeonMapPolicy.RoomType.NORMAL);
        DungeonMapPolicy.RoomTile b = tile(1, 0, DungeonMapPolicy.RoomType.NORMAL);
        DungeonMapPolicy.RoomTile c = tile(2, 0, DungeonMapPolicy.RoomType.NORMAL);
        DungeonMapPolicy.RoomTile puzzle = tile(0, 1, DungeonMapPolicy.RoomType.PUZZLE);
        DungeonMapPolicy.Board merged = new DungeonMapPolicy.Board(
                cal, List.of(a, b), List.of(), List.of(), "");
        assertEquals(1, DungeonMapPolicy.uniqueRooms(merged).size());
        DungeonMapPolicy.Board split = new DungeonMapPolicy.Board(
                cal,
                List.of(b, c),
                List.of(new DungeonMapPolicy.DoorTile(1, 0, true, DungeonMapPolicy.DoorType.NORMAL)),
                List.of(),
                "");
        assertEquals(2, DungeonMapPolicy.uniqueRooms(split).size());
        DungeonMapPolicy.Board mixed = new DungeonMapPolicy.Board(
                cal, List.of(a, puzzle), List.of(), List.of(), "");
        assertEquals(2, DungeonMapPolicy.uniqueRooms(mixed).size());
        java.util.Map<String, DungeonMapPolicy.RoomIdentity> names = java.util.Map.of(
                "0,0", new DungeonMapPolicy.RoomIdentity("Gold", 1),
                "1,0", new DungeonMapPolicy.RoomIdentity("Gold", 1));
        DungeonMapPolicy.Schematic schematic = DungeonMapPolicy.schematic(
                merged, names, java.util.Map.of("Gold", 1), true, true, false, 0.6D);
        assertTrue(schematic.present());
        assertEquals(1, schematic.rooms().size());
        assertEquals("Gold", schematic.rooms().getFirst().name());
        assertEquals("1/1", schematic.rooms().getFirst().secretsFound()
                + "/" + schematic.rooms().getFirst().secretsTotal());
        assertTrue(schematic.rooms().getFirst().secretsComplete());
        assertEquals(DungeonMapPolicy.HUD_MAP, schematic.width());
        assertEquals(DungeonMapPolicy.HUD_MAP, schematic.height());
        assertEquals("Trap", DungeonMapPolicy.fallbackName(DungeonMapPolicy.RoomType.TRAP));
        assertEquals(24, DungeonMapPolicy.clampHudCell(24));
        assertEquals(12, DungeonMapPolicy.clampHudCell(4));
        assertEquals(40, DungeonMapPolicy.clampHudCell(99));
        assertEquals(0, DungeonMapPolicy.tileFromWorld(-200));
        assertEquals(1, DungeonMapPolicy.tileFromWorld(-168));
    }

    @Test
    void scanKeepsRoomsWestOfEntranceAndCropsTheHud() {
        byte[] colors = new byte[128 * 128];
        int size = 16;
        paintRoom(colors, 22, 22, size, DungeonMapPolicy.COLOR_ENTRANCE);
        paintRoom(colors, 2, 22, size, DungeonMapPolicy.COLOR_NORMAL);
        DungeonMapPolicy.Board board = DungeonMapPolicy.scan(colors, 128, List.of(), null, null);
        assertTrue(board.rooms().stream().anyMatch(room ->
                room.type() == DungeonMapPolicy.RoomType.ENTRANCE && room.tileX() == 1));
        assertTrue(board.rooms().stream().anyMatch(room ->
                room.type() == DungeonMapPolicy.RoomType.NORMAL && room.tileX() == 0));
        DungeonMapPolicy.Schematic schematic = DungeonMapPolicy.schematic(
                board, java.util.Map.of(), java.util.Map.of(), true, false, false, 0.6D);
        assertEquals(2, schematic.rooms().size());
        assertEquals(DungeonMapPolicy.HUD_MAP, schematic.width());
        int minX = schematic.rooms().stream()
                .flatMap(room -> room.rects().stream())
                .mapToInt(DungeonMapPolicy.HudRect::x)
                .min()
                .orElse(-1);
        assertEquals(2, minX);
    }

    @Test
    void playerMarkerUsesStartZAndCropsToOccupiedTiles() {
        DungeonMapPolicy.Calibration cal =
                new DungeonMapPolicy.Calibration(true, 2, 10, 16, 20, 0.625D);
        DungeonMapPolicy.RoomTile room = new DungeonMapPolicy.RoomTile(
                2, 3, DungeonMapPolicy.RoomType.NORMAL, DungeonMapPolicy.Checkmark.NONE, 42, 70);
        DungeonMapPolicy.Board board = new DungeonMapPolicy.Board(
                cal,
                List.of(room),
                List.of(),
                List.of(new DungeonMapPolicy.PlayerIcon(42, 70, 90.0F, true, "mage", "You")),
                "");
        DungeonMapPolicy.Schematic schematic = DungeonMapPolicy.schematic(
                board, java.util.Map.of(), java.util.Map.of(), false, true, false, 0.6D);
        assertEquals(DungeonMapPolicy.HUD_MAP, schematic.width());
        assertEquals(1, schematic.players().size());
        assertEquals(42, schematic.players().getFirst().x());
        assertEquals(70, schematic.players().getFirst().y());
        assertEquals(90.0F, schematic.players().getFirst().yaw());
        assertTrue(schematic.players().getFirst().self());
    }

    @Test
    void mapModeNormalizesLegacyCheaterNames() {
        assertEquals(DungeonMapPolicy.MAP_MODE_EXPLORED, DungeonMapPolicy.normalizeMapMode(""));
        assertEquals(DungeonMapPolicy.MAP_MODE_EXPLORED, DungeonMapPolicy.normalizeMapMode("Explored"));
        assertEquals(DungeonMapPolicy.MAP_MODE_REVEAL, DungeonMapPolicy.normalizeMapMode("Cheater"));
        assertEquals(DungeonMapPolicy.MAP_MODE_REVEAL, DungeonMapPolicy.normalizeMapMode("Reveal Hidden"));
        assertTrue(DungeonMapPolicy.revealsHidden("Hidden"));
        assertFalse(DungeonMapPolicy.revealsHidden("Explored"));
        assertTrue(DungeonMapPolicy.samePlayerName("Steve", "steve"));
        assertFalse(DungeonMapPolicy.samePlayerName("", "steve"));
    }

    @Test
    void unnamedMapDecorationsBindToRosterSkippingSelfFrame() {
        java.util.Map<String, DungeonPolicy.DungeonClass> roster = java.util.Map.of(
                "You", DungeonPolicy.DungeonClass.MAGE,
                "Arch", DungeonPolicy.DungeonClass.ARCHER,
                "Tank", DungeonPolicy.DungeonClass.TANK);
        List<DungeonMapPolicy.PlayerIcon> icons = DungeonMapPolicy.assignTeammateIcons(
                List.of(
                        new DungeonMapPolicy.MapDecorationHint(10, 10, 90.0F, true, ""),
                        new DungeonMapPolicy.MapDecorationHint(40, 50, 180.0F, false, ""),
                        new DungeonMapPolicy.MapDecorationHint(70, 20, 0.0F, false, "")),
                List.of("You", "Arch", "Tank"),
                "You",
                roster,
                true,
                true);
        assertEquals(2, icons.size());
        assertEquals("Arch", icons.getFirst().name());
        assertEquals("archer", icons.getFirst().classKey());
        assertEquals(40, icons.getFirst().mapX());
        assertEquals("Tank", icons.get(1).name());
        assertEquals("tank", icons.get(1).classKey());
        List<DungeonMapPolicy.PlayerIcon> named = DungeonMapPolicy.assignTeammateIcons(
                List.of(new DungeonMapPolicy.MapDecorationHint(12, 12, 0.0F, false, "Tank")),
                List.of("Arch", "Tank"),
                "You",
                roster,
                true,
                false);
        assertEquals(1, named.size());
        assertEquals("Tank", named.getFirst().name());
        assertEquals("tank", named.getFirst().classKey());
    }

    @Test
    void greenPuzzleRoomsCountAsComplete() {
        DungeonMapPolicy.RoomTile open = new DungeonMapPolicy.RoomTile(
                0, 0, DungeonMapPolicy.RoomType.PUZZLE, DungeonMapPolicy.Checkmark.WHITE, 22, 22);
        DungeonMapPolicy.RoomTile done = new DungeonMapPolicy.RoomTile(
                0, 0, DungeonMapPolicy.RoomType.PUZZLE, DungeonMapPolicy.Checkmark.GREEN, 22, 22);
        DungeonMapPolicy.Board board = new DungeonMapPolicy.Board(
                new DungeonMapPolicy.Calibration(true, 22, 22, 16, 20, 0.625D),
                List.of(done), List.of(), List.of(), "");
        assertTrue(DungeonMapPolicy.puzzlesComplete(DungeonMapPolicy.uniqueRooms(board)));
        DungeonMapPolicy.Board unfinished = new DungeonMapPolicy.Board(
                board.calibration(), List.of(open), List.of(), List.of(), "");
        assertFalse(DungeonMapPolicy.puzzlesComplete(DungeonMapPolicy.uniqueRooms(unfinished)));
    }

    @Test
    void revealHiddenPaintsHashedRoomsBehindDoors() {
        DungeonMapPolicy.Calibration cal = new DungeonMapPolicy.Calibration(true, 22, 22, 16, 20, 0.625D);
        DungeonMapPolicy.RoomTile entrance = new DungeonMapPolicy.RoomTile(
                0, 0, DungeonMapPolicy.RoomType.ENTRANCE, DungeonMapPolicy.Checkmark.GREEN, 22, 22);
        DungeonMapPolicy.DoorTile wither = new DungeonMapPolicy.DoorTile(
                0, 0, true, DungeonMapPolicy.DoorType.WITHER);
        DungeonMapPolicy.Board scanned = new DungeonMapPolicy.Board(
                cal, List.of(entrance), List.of(wither), List.of(), "1 rooms");
        DungeonMapPolicy.Board placeholders = DungeonLeftoverPolicy.revealHiddenRooms(scanned);
        java.util.Map<String, DungeonMapPolicy.RoomIdentity> hashed = java.util.Map.of(
                "1,0",
                new DungeonMapPolicy.RoomIdentity(
                        "Tic Tac Toe", 0, DungeonMapPolicy.RoomType.PUZZLE));
        DungeonMapPolicy.Board explored = DungeonMapPolicy.applyHashedRooms(scanned, hashed, false);
        assertEquals(1, explored.rooms().size());
        DungeonMapPolicy.Board revealed = DungeonMapPolicy.applyHashedRooms(placeholders, hashed, true);
        assertEquals(2, revealed.rooms().size());
        DungeonMapPolicy.RoomTile hidden = revealed.rooms().stream()
                .filter(room -> room.tileX() == 1 && room.tileZ() == 0)
                .findFirst()
                .orElseThrow();
        assertEquals(DungeonMapPolicy.RoomType.PUZZLE, hidden.type());
        assertEquals(DungeonMapPolicy.Checkmark.QUESTION, hidden.checkmark());
        DungeonMapPolicy.Schematic schematic = DungeonMapPolicy.schematic(
                revealed, hashed, java.util.Map.of(), true, false, false, 0.6D);
        assertEquals(2, schematic.rooms().size());
        DungeonMapPolicy.HudRoom puzzle = schematic.rooms().stream()
                .filter(room -> "Tic Tac Toe".equals(room.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(0xFF7B007B, puzzle.color());
        assertTrue(DungeonMapPolicy.hiddenOnMap(puzzle.room()));
        java.util.Map<String, DungeonMapPolicy.RoomIdentity> splitNames = java.util.Map.of(
                "0,0", new DungeonMapPolicy.RoomIdentity("Crypt", 1),
                "1,0", new DungeonMapPolicy.RoomIdentity("Raccoon", 4));
        DungeonMapPolicy.Board neighbors = new DungeonMapPolicy.Board(
                cal,
                List.of(
                        tile(0, 0, DungeonMapPolicy.RoomType.NORMAL),
                        tile(1, 0, DungeonMapPolicy.RoomType.NORMAL)),
                List.of(),
                List.of(),
                "");
        assertEquals(1, DungeonMapPolicy.uniqueRooms(neighbors).size());
        assertEquals(2, DungeonMapPolicy.uniqueRooms(neighbors, splitNames).size());
    }

    @Test
    void currentRoomTilesCoverHashedNameAndWaypointFilter() {
        java.util.Map<String, DungeonMapPolicy.RoomIdentity> hashed = java.util.Map.of(
                "0,0", new DungeonMapPolicy.RoomIdentity("Waterfall", 8),
                "0,1", new DungeonMapPolicy.RoomIdentity("Waterfall", 8),
                "1,0", new DungeonMapPolicy.RoomIdentity("Raccoon", 4));
        List<DungeonRoomDataPolicy.MapTile> waterfall =
                DungeonMapPolicy.currentRoomTiles(-185, -185, hashed);
        assertEquals(2, waterfall.size());
        assertTrue(DungeonMapPolicy.inRoomTiles(-185, -153, waterfall));
        assertFalse(DungeonMapPolicy.inRoomTiles(-153, -185, waterfall));
        assertEquals(1, DungeonMapPolicy.tilesNamed(hashed, "Raccoon").size());
    }

    @Test
    void floorCornersFollowMagicalMapLayout() {
        byte[] colors = new byte[128 * 128];
        int start = 22 + 22 * 128;
        for (int i = 0; i < 16; i++) {
            colors[start + i] = DungeonMapPolicy.COLOR_ENTRANCE;
        }
        DungeonMapPolicy.Calibration entrance = DungeonMapPolicy.calibrate(colors, 128, 0);
        assertEquals(22, entrance.startX());
        assertEquals(22, entrance.startZ());
        DungeonMapPolicy.Calibration floorOne = DungeonMapPolicy.calibrate(colors, 128, 1);
        assertEquals(22, floorOne.startX());
        assertEquals(11, floorOne.startZ());
        DungeonMapPolicy.Calibration floorTwo = DungeonMapPolicy.calibrate(colors, 128, 2);
        assertEquals(11, floorTwo.startX());
        assertEquals(11, floorTwo.startZ());
        DungeonMapPolicy.Calibration later = DungeonMapPolicy.calibrate(colors, 128, 7);
        assertEquals(2, later.startX());
        assertEquals(2, later.startZ());
    }

    private static DungeonMapPolicy.RoomTile tile(int x, int z, DungeonMapPolicy.RoomType type) {
        return new DungeonMapPolicy.RoomTile(x, z, type, DungeonMapPolicy.Checkmark.NONE, 22 + x * 20, 22 + z * 20);
    }

    private static void paintRoom(byte[] colors, int x, int z, int size, byte color) {
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                colors[(z + dz) * 128 + (x + dx)] = color;
            }
        }
    }
}
