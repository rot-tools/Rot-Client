package fi.rotclient;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Dungeon-floor carry tracker, HUD, highlight, and webhook. */
final class DungeonCarryRuntime {
    private DungeonCarryRuntime() {
    }

    static void tick(Minecraft client) {
        // Highlight is drawn from renderGizmos.
    }

    static void renderGizmos(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        DungeonAthenSettings athen = extras().athen();
        if (!athen.carryEnabled || !athen.carryHighlight || client.level == null) {
            return;
        }
        DungeonCarryPolicy.Snapshot snapshot = snapshot();
        if (snapshot.active().isEmpty()) {
            return;
        }
        Vec3 eye = client.player.getEyePosition();
        for (Player other : client.level.getEntitiesOfClass(
                Player.class, client.player.getBoundingBox().inflate(48.0D))) {
            if (other == client.player) {
                continue;
            }
            String name = other.getGameProfile().name();
            if (DungeonCarryPolicy.find(snapshot.active(), name).isEmpty()) {
                continue;
            }
            AABB box = other.getBoundingBox();
            var props = net.minecraft.gizmos.Gizmos.cuboid(
                    box,
                    net.minecraft.gizmos.GizmoStyle.stroke(
                            athen.carryPlayerColor, (float) athen.carryLineWidth));
            props.setAlwaysOnTop();
            net.minecraft.gizmos.Gizmos.line(eye, box.getCenter(), athen.carryPlayerColor,
                    (float) athen.carryLineWidth).setAlwaysOnTop();
        }
    }

    static void onChat(String raw) {
        if (raw == null || !extras().athen().carryEnabled) {
            return;
        }
        if (!DungeonPolicy.isDungeonEnd(raw)) {
            return;
        }
        incrementCurrentFloor();
    }

    static List<String> hudLines(boolean editorOpen) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.carryEnabled || !athen.carryDisplay) {
            return editorOpen ? List.of("Dungeon Carry", "(hidden)") : List.of();
        }
        boolean inDungeon = SkyBlockDungeonDetector.confidentlyInDungeon();
        List<String> lines = DungeonCarryPolicy.hudLines(snapshot().active(), athen.carryOnlyDungeons, inDungeon);
        if (lines.isEmpty() && editorOpen) {
            return List.of("Dungeon Carry", "(hidden outside dungeon)");
        }
        return lines;
    }

    static DungeonCarryPolicy.Snapshot snapshot() {
        return DungeonCarryPolicy.readSnapshot(extras().athen().carryState);
    }

    static void persist(DungeonCarryPolicy.Snapshot snapshot) {
        extras().athen().carryState = DungeonCarryPolicy.writeSnapshot(snapshot);
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    static String add(String player, String floor, int total) {
        DungeonCarryPolicy.Snapshot current = snapshot();
        List<DungeonCarryPolicy.TrackedCarry> active =
                DungeonCarryPolicy.add(current.active(), player, floor, total);
        persist(new DungeonCarryPolicy.Snapshot(active, current.history()));
        DungeonCarryPolicy.TrackedCarry carry = DungeonCarryPolicy.find(active, player).orElse(null);
        if (carry == null) {
            return "";
        }
        String start = DungeonCarryPolicy.startMessage(carry);
        DungeonAthenSettings athen = extras().athen();
        Minecraft client = Minecraft.getInstance();
        if (athen.carryShowStart && client.player != null && !start.isBlank()) {
            client.player.sendSystemMessage(Component.literal(start));
        }
        return start;
    }

    static boolean remove(String player) {
        DungeonCarryPolicy.Snapshot current = snapshot();
        List<DungeonCarryPolicy.TrackedCarry> active = DungeonCarryPolicy.remove(current.active(), player);
        if (active.size() == current.active().size()) {
            return false;
        }
        persist(new DungeonCarryPolicy.Snapshot(active, current.history()));
        return true;
    }

    static List<String> listLines() {
        List<String> lines = new ArrayList<>();
        for (DungeonCarryPolicy.TrackedCarry carry : snapshot().active()) {
            if (carry != null) {
                lines.add(carry.hudLine());
            }
        }
        return lines;
    }

    static List<String> historyLines() {
        List<String> lines = new ArrayList<>();
        for (DungeonCarryPolicy.HistoryEntry entry : snapshot().history()) {
            if (entry != null) {
                lines.add(entry.player + " " + entry.floor + " x" + entry.total);
            }
        }
        return lines;
    }

    private static void incrementCurrentFloor() {
        DungeonAthenSettings athen = extras().athen();
        String floor = DungeonCarryPolicy.normalizeFloor(DungeonRuntime.sidebar().floor());
        DungeonCarryPolicy.Snapshot current = snapshot();
        List<DungeonCarryPolicy.TrackedCarry> active = new ArrayList<>(current.active());
        List<DungeonCarryPolicy.HistoryEntry> history = new ArrayList<>(current.history());
        boolean changed = false;
        Minecraft client = Minecraft.getInstance();
        for (DungeonCarryPolicy.TrackedCarry carry : active) {
            if (carry == null || carry.done() || !DungeonCarryPolicy.matchesFloor(carry.floor, floor)) {
                continue;
            }
            carry.completed = Math.min(carry.total, carry.completed + 1);
            changed = true;
            String progress = DungeonCarryPolicy.progressMessage(carry);
            if (athen.carryAnnounceParty && client.player != null && client.player.connection != null) {
                client.player.connection.sendCommand("pc " + progress);
            }
            if (athen.carryWebhook && (athen.carryWebhookEach || carry.done())) {
                sendWebhook(athen, carry.done() ? DungeonCarryPolicy.completeMessage(carry) : progress);
            }
            if (carry.done()) {
                DungeonCarryPolicy.HistoryEntry entry = new DungeonCarryPolicy.HistoryEntry();
                entry.player = carry.player;
                entry.floor = carry.floor;
                entry.total = carry.total;
                entry.completedAtMillis = System.currentTimeMillis();
                entry.durationMillis = Math.max(0L, entry.completedAtMillis - carry.startedAtMillis);
                history.add(entry);
            }
        }
        if (changed) {
            persist(new DungeonCarryPolicy.Snapshot(List.copyOf(active), List.copyOf(history)));
        }
    }

    private static void sendWebhook(DungeonAthenSettings athen, String content) {
        String url = DungeonCarryPolicy.sanitizeWebhookUrl(athen.carryWebhookUrl);
        if (url.isBlank() || content == null || content.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("content", content.substring(0, Math.min(1800, content.length())));
        SlayerCarryWebhookRuntime.send(url, content);
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
