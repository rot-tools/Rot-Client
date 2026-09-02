package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Crystal Hollows structure scan + named waypoints. Chunk work is queued and
 * section palettes skip empty / irrelevant 16³ cubes so the game does not hitch.
 */
final class WorldScannerRuntime {
    private static final Map<String, WorldScannerPolicy.Hit> WAYPOINTS =
            new ConcurrentHashMap<>();
    private static final Map<Long, Integer> FLUID_ESP = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Long> PENDING = new ConcurrentLinkedQueue<>();
    private static final Set<Long> QUEUED = ConcurrentHashMap.newKeySet();
    private static boolean lastEnabled;
    private static boolean lastInHollows;
    private static int fluidRefreshTicks;

    private WorldScannerRuntime() {
    }

    static void clear() {
        WAYPOINTS.clear();
        FLUID_ESP.clear();
        PENDING.clear();
        QUEUED.clear();
        lastEnabled = false;
        lastInHollows = false;
        fluidRefreshTicks = 0;
    }

    static void onChunkLoad(ClientLevel level, LevelChunk chunk) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!WorldScannerPolicy.shouldScan(
                qol.worldScannerEnabled,
                qol.worldScannerOnlyHollows,
                inCrystalHollows())) {
            return;
        }
        enqueue(chunk);
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        boolean enabled = qol.worldScannerEnabled;
        boolean inHollows = inCrystalHollows();
        if (WorldScannerPolicy.shouldRescanLoadedChunks(
                enabled,
                lastEnabled,
                qol.worldScannerOnlyHollows,
                inHollows,
                lastInHollows)) {
            enqueueLoadedChunks(client);
        }
        lastEnabled = enabled;
        lastInHollows = inHollows;
        if (!enabled) {
            if (!WAYPOINTS.isEmpty() || !FLUID_ESP.isEmpty() || !PENDING.isEmpty()) {
                clear();
                lastEnabled = false;
            }
            return;
        }
        if (qol.worldScannerOnlyHollows && !inHollows) {
            if (!WAYPOINTS.isEmpty() || !FLUID_ESP.isEmpty() || !PENDING.isEmpty()) {
                WAYPOINTS.clear();
                FLUID_ESP.clear();
                PENDING.clear();
                QUEUED.clear();
            }
            return;
        }
        drainQueue(client, qol);
        refreshFluidsIfDue(client, qol);
    }

    static void renderGizmos() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.worldScannerEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null) {
            return;
        }
        double range = WorldScannerPolicy.clampEspRange(qol.worldScannerEspRange);
        double rangeSq = range * range;
        Vec3 eye = player.getEyePosition();
        for (WorldScannerPolicy.Hit hit : WAYPOINTS.values()) {
            WorldScannerEspSettings.Target target = targetFor(hit.name(), qol);
            if (target == null || !target.enabled) {
                continue;
            }
            boolean fill = WorldScannerEspSettings.drawFill(target.highlightStyle);
            boolean stroke = WorldScannerEspSettings.drawStroke(target.highlightStyle);
            Vec3 center = new Vec3(hit.x() + 0.5D, hit.y() + 0.5D, hit.z() + 0.5D);
            drawBox(hit.x(), hit.y(), hit.z(), target.colorArgb, true, fill, stroke);
            drawPointer(center, target.colorArgb);
            drawBeam(hit.x(), hit.y(), hit.z(), target.colorArgb);
            if (target.displayName) {
                var name = Gizmos.billboardTextOverBlock(
                        WorldScannerPolicy.formatEspLabel(
                                WorldScannerEspSettings.labelForHit(hit.name()),
                                eye.distanceTo(center)),
                        new BlockPos(hit.x(), hit.y(), hit.z()),
                        WorldScannerPolicy.ESP_LABEL_ROW,
                        target.colorArgb,
                        WorldScannerPolicy.espLabelScale(target.nameScale));
                name.setAlwaysOnTop();
            }
            if (target.tracer) {
                var line = Gizmos.line(eye, center, target.colorArgb, WorldScannerPolicy.ESP_TRACER_WIDTH);
                line.setAlwaysOnTop();
            }
        }
        if (qol.worldScannerLavaEsp || qol.worldScannerWaterEsp) {
            for (Map.Entry<Long, Integer> entry : FLUID_ESP.entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                if (player.distanceToSqr(
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                        > rangeSq) {
                    continue;
                }
                drawBox(pos.getX(), pos.getY(), pos.getZ(), entry.getValue(), false, true, true);
            }
        }
    }

    private static void drainQueue(Minecraft client, QolUtilityConfig qol) {
        if (client == null || client.level == null) {
            return;
        }
        ClientLevel level = client.level;
        int budget = WorldScannerPolicy.SCAN_CHUNKS_PER_TICK;
        while (budget-- > 0) {
            Long key = PENDING.poll();
            if (key == null) {
                return;
            }
            QUEUED.remove(key);
            ChunkPos pos = ChunkPos.unpack(key);
            if (!level.hasChunk(pos.x(), pos.z())) {
                continue;
            }
            scanChunk(level.getChunk(pos.x(), pos.z()), qol);
        }
    }

    private static void enqueueLoadedChunks(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        PENDING.clear();
        QUEUED.clear();
        ClientLevel level = client.level;
        int originX = client.player.chunkPosition().x();
        int originZ = client.player.chunkPosition().z();
        int radius = WorldScannerPolicy.SCAN_RADIUS_CHUNKS;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = originX + dx;
                int cz = originZ + dz;
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                enqueue(level.getChunk(cx, cz));
            }
        }
    }

    private static void enqueue(LevelChunk chunk) {
        if (chunk == null) {
            return;
        }
        long key = chunk.getPos().pack();
        if (QUEUED.add(key)) {
            PENDING.add(key);
        }
    }

    private static void scanChunk(LevelChunk chunk, QolUtilityConfig qol) {
        List<WorldScannerPolicy.StructureDef> structures = activeStructures(qol);
        boolean fairy = qol.worldScannerFairyGrottos && targetEnabled("fairy", qol);
        boolean worm = qol.worldScannerWormFishing && targetEnabled("worm", qol);
        Set<String> triggers = WorldScannerPolicy.triggerBlocks(
                structures, fairy, worm, false);
        if (triggers.isEmpty() && !fairy && !worm) {
            return;
        }
        int originX = chunk.getPos().getMinBlockX();
        int originZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int sectionMinY = chunk.getSectionYFromSectionIndex(sectionIndex) * 16;
            if (sectionMinY + 15 < WorldScannerPolicy.STRUCTURE_Y_MIN
                    || sectionMinY > WorldScannerPolicy.STRUCTURE_Y_MAX) {
                continue;
            }
            if (!section.maybeHas(state ->
                    WorldScannerPolicy.isTriggerBlock(blockId(state), triggers))) {
                continue;
            }
            int maxLocalY = 15;
            for (int ly = 0; ly <= maxLocalY; ly++) {
                int y = sectionMinY + ly;
                if (y < WorldScannerPolicy.STRUCTURE_Y_MIN
                        || y > WorldScannerPolicy.STRUCTURE_Y_MAX) {
                    continue;
                }
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        if (state.isAir()) {
                            continue;
                        }
                        String id = blockId(state);
                        if (!WorldScannerPolicy.isTriggerBlock(id, triggers)) {
                            continue;
                        }
                        int x = originX + lx;
                        int z = originZ + lz;
                        String above = blockId(chunk.getBlockState(cursor.set(x, y + 1, z)));
                        matchTrigger(structures, x, y, z, id, above, chunk, qol, cursor);
                        if (fairy
                                && WorldScannerPolicy.isFairyGrottoBlock(id)
                                && !WorldScannerPolicy.inQuarter(
                                        WorldScannerPolicy.Quarter.NUCLEUS, x, y, z)
                                && !WAYPOINTS.containsKey("Fairy Grotto")) {
                            add("Fairy Grotto", x, y, z, qol);
                        }
                        if (worm
                                && !WAYPOINTS.containsKey("Worm Fishing")
                                && WorldScannerPolicy.isWormFishingLava(x, y, z, id, above)) {
                            add("Worm Fishing", x, y, z, qol);
                        }
                    }
                }
            }
        }
    }

    private static void matchTrigger(
            List<WorldScannerPolicy.StructureDef> structures,
            int x,
            int y,
            int z,
            String triggerId,
            String aboveId,
            LevelChunk chunk,
            QolUtilityConfig qol,
            BlockPos.MutableBlockPos cursor) {
        List<WorldScannerPolicy.StructureDef> matches =
                WorldScannerPolicy.matchingStructures(structures, triggerId);
        for (WorldScannerPolicy.StructureDef structure : matches) {
            if (WAYPOINTS.containsKey(structure.name())) {
                continue;
            }
            if (structure.name().equals("Golden Dragon") && !qol.worldScannerDragonNest) {
                continue;
            }
            if (!targetEnabled(WorldScannerEspSettings.idForHit(structure.name()), qol)) {
                continue;
            }
            if (!WorldScannerPolicy.secondBlockMatches(aboveId, structure.sequence())) {
                continue;
            }
            List<String> column = column(chunk, x, y, z, structure.sequence().size(), cursor);
            WorldScannerPolicy.match(structure, x, y, z, column).ifPresent(hit ->
                    add(hit.name(), hit.x(), hit.y(), hit.z(), qol));
        }
    }

    private static List<WorldScannerPolicy.StructureDef> activeStructures(QolUtilityConfig qol) {
        ArrayList<WorldScannerPolicy.StructureDef> out = new ArrayList<>();
        if (qol.worldScannerCrystals) {
            out.addAll(WorldScannerPolicy.crystalStructures());
        }
        if (qol.worldScannerMobSpots) {
            out.addAll(WorldScannerPolicy.mobSpotStructures());
        } else if (qol.worldScannerDragonNest) {
            for (WorldScannerPolicy.StructureDef structure
                    : WorldScannerPolicy.mobSpotStructures()) {
                if ("Golden Dragon".equals(structure.name())) {
                    out.add(structure);
                }
            }
        }
        return out;
    }

    private static List<String> column(
            LevelChunk chunk,
            int x,
            int y,
            int z,
            int length,
            BlockPos.MutableBlockPos cursor) {
        List<String> out = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            out.add(blockId(chunk.getBlockState(cursor.set(x, y + i, z))));
        }
        return out;
    }

    private static void refreshFluidsIfDue(Minecraft client, QolUtilityConfig qol) {
        if (!qol.worldScannerLavaEsp && !qol.worldScannerWaterEsp) {
            if (!FLUID_ESP.isEmpty()) {
                FLUID_ESP.clear();
            }
            return;
        }
        fluidRefreshTicks++;
        if (fluidRefreshTicks < WorldScannerPolicy.FLUID_REFRESH_TICKS) {
            return;
        }
        fluidRefreshTicks = 0;
        refreshNearbyFluids(client, qol);
    }

    private static void refreshNearbyFluids(Minecraft client, QolUtilityConfig qol) {
        FLUID_ESP.clear();
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        int range = WorldScannerPolicy.clampEspRange(qol.worldScannerEspRange);
        int minX = player.getBlockX() - range;
        int maxX = player.getBlockX() + range;
        int minZ = player.getBlockZ() - range;
        int maxZ = player.getBlockZ() + range;
        int minY = Math.max(WorldScannerPolicy.STRUCTURE_Y_MIN, player.getBlockY() - range);
        int maxY = Math.min(WorldScannerPolicy.STRUCTURE_Y_MAX, player.getBlockY() + range);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int remaining = WorldScannerPolicy.MAX_FLUID_MARKERS;
        int minCx = minX >> 4;
        int maxCx = maxX >> 4;
        int minCz = minZ >> 4;
        int maxCz = maxZ >> 4;
        for (int cx = minCx; cx <= maxCx && remaining > 0; cx++) {
            for (int cz = minCz; cz <= maxCz && remaining > 0; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();
                int originX = chunk.getPos().getMinBlockX();
                int originZ = chunk.getPos().getMinBlockZ();
                for (int sectionIndex = 0; sectionIndex < sections.length && remaining > 0; sectionIndex++) {
                    LevelChunkSection section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir() || !section.hasFluid()) {
                        continue;
                    }
                    int sectionMinY = chunk.getSectionYFromSectionIndex(sectionIndex) * 16;
                    if (sectionMinY + 15 < minY || sectionMinY > maxY) {
                        continue;
                    }
                    for (int ly = 0; ly < 16 && remaining > 0; ly++) {
                        int y = sectionMinY + ly;
                        if (y < minY || y > maxY) {
                            continue;
                        }
                        for (int lx = 0; lx < 16 && remaining > 0; lx++) {
                            int x = originX + lx;
                            if (x < minX || x > maxX) {
                                continue;
                            }
                            for (int lz = 0; lz < 16 && remaining > 0; lz++) {
                                int z = originZ + lz;
                                if (z < minZ || z > maxZ) {
                                    continue;
                                }
                                BlockState state = section.getBlockState(lx, ly, lz);
                                String id = blockId(state);
                                String above = blockId(chunk.getBlockState(cursor.set(x, y + 1, z)));
                                if (qol.worldScannerLavaEsp
                                        && WorldScannerPolicy.isEspSurface(id, above, "lava")) {
                                    FLUID_ESP.put(BlockPos.asLong(x, y, z), 0xFFFF5500);
                                    remaining--;
                                } else if (qol.worldScannerWaterEsp
                                        && WorldScannerPolicy.isEspSurface(id, above, "water")) {
                                    FLUID_ESP.put(BlockPos.asLong(x, y, z), 0xFF3399FF);
                                    remaining--;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void add(
            String name, int x, int y, int z, QolUtilityConfig qol) {
        WorldScannerEspSettings.Target target = targetFor(name, qol);
        int color = target == null ? 0xFFFFFFFF : target.colorArgb;
        if (WAYPOINTS.putIfAbsent(name, new WorldScannerPolicy.Hit(name, x, y, z, color))
                != null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || target == null) {
            return;
        }
        String label = WorldScannerEspSettings.labelForHit(name);
        if (target.chatCoords || qol.worldScannerChatCoords) {
            client.player.sendSystemMessage(Component.literal(
                    "World Scanner: " + label + " at " + x + ", " + y + ", " + z));
        }
        if (target.notify) {
            client.player.sendSystemMessage(Component.literal("World Scanner: found " + label));
        }
    }

    private static boolean targetEnabled(String id, QolUtilityConfig qol) {
        WorldScannerEspSettings.Target target = qol.worldScannerTarget(id);
        return target.enabled && WorldScannerEspSettings.masterAllows(
                id,
                qol.worldScannerCrystals,
                qol.worldScannerMobSpots,
                qol.worldScannerFairyGrottos,
                qol.worldScannerDragonNest,
                qol.worldScannerWormFishing);
    }

    private static WorldScannerEspSettings.Target targetFor(String hitName, QolUtilityConfig qol) {
        return qol.worldScannerTarget(WorldScannerEspSettings.idForHit(hitName));
    }

    private static void drawBox(
            int x, int y, int z, int color, boolean alwaysOnTop, boolean fill, boolean stroke) {
        float width = stroke ? WorldScannerPolicy.ESP_BOX_STROKE : 0.01F;
        int fillColor = fill ? withAlpha(color, 0x88) : 0x00000000;
        GizmoStyle style = GizmoStyle.strokeAndFill(color, width, fillColor);
        var props = Gizmos.cuboid(
                new AABB(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D).inflate(0.08D),
                style);
        if (alwaysOnTop) {
            props.setAlwaysOnTop();
        }
    }

    private static void drawPointer(Vec3 center, int color) {
        var point = Gizmos.point(center, color, 0.22F);
        point.setAlwaysOnTop();
        Vec3 tip = new Vec3(center.x, center.y + 0.55D, center.z);
        Vec3 tail = new Vec3(center.x, center.y + 2.6D, center.z);
        var arrow = Gizmos.arrow(tail, tip, color, 2.4F);
        arrow.setAlwaysOnTop();
    }

    private static void drawBeam(int x, int y, int z, int color) {
        Vec3 bottom = new Vec3(x + 0.5D, y + 2.8D, z + 0.5D);
        Vec3 top = new Vec3(x + 0.5D, y + 80.0D, z + 0.5D);
        var beam = Gizmos.line(bottom, top, color, WorldScannerPolicy.ESP_BEAM_WIDTH);
        beam.setAlwaysOnTop();
    }

    private static boolean inCrystalHollows() {
        SkyBlockArea area = SkyBlockAreaDetector.detect();
        return area == SkyBlockArea.CRYSTAL_HOLLOWS
                || area == SkyBlockArea.CRYSTAL_NUCLEUS;
    }

    private static String blockId(BlockState state) {
        if (state == null || state.isAir()) {
            return "air";
        }
        if (state.is(Blocks.LAVA)) {
            return "lava";
        }
        if (state.is(Blocks.WATER)) {
            return "water";
        }
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "air" : key.getPath();
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
