package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Blood-camp speak / move / spawned timings and HUD. */
final class DungeonWatcherRuntime {
    private static long bloodDoorAt;
    private static long speakAt;
    private static long moveAt;
    private static long spawnedAt;
    private static boolean moved;
    private static DungeonWatcherPolicy.Speed lastSpeed = DungeonWatcherPolicy.Speed.NONE;

    private DungeonWatcherRuntime() {
    }

    static void tick(Minecraft client) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.watcherEnabled || client == null || client.player == null || client.level == null) {
            return;
        }
        if (bloodDoorAt <= 0L || moved || speakAt <= 0L) {
            return;
        }
        AABB search = client.player.getBoundingBox().inflate(48.0D);
        for (LivingEntity living : client.level.getEntitiesOfClass(LivingEntity.class, search)) {
            String name = living.getName() == null ? "" : living.getName().getString();
            if (living instanceof ArmorStand) {
                name = living.getCustomName() == null ? name : living.getCustomName().getString();
            }
            if (!DungeonWatcherPolicy.watcherHologram(name) && !EmberDungeonPolicy.isWatcherHologram(name)) {
                continue;
            }
            if (living.getDeltaMovement().lengthSqr() > 0.002D || living.tickCount > 40) {
                markMove(client, athen);
                return;
            }
        }
    }

    static void onChat(String raw, Minecraft client) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.watcherEnabled || raw == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (DungeonWatcherPolicy.bloodDoor(raw)) {
            reset();
            bloodDoorAt = now;
            return;
        }
        if (DungeonWatcherPolicy.speakChat(raw)) {
            speakAt = now;
            long speakMs = bloodDoorAt > 0L ? now - bloodDoorAt : 0L;
            lastSpeed = DungeonWatcherPolicy.speedForSpeak(speakMs);
            if (athen.watcherSpeak) {
                title(client, DungeonWatcherPolicy.alertText(
                        lastSpeed,
                        athen.watcherTextFast,
                        athen.watcherTextNormal,
                        athen.watcherTextSlow,
                        athen.watcherTextVerySlow));
            }
            if (athen.watcherBreakdown && client != null && client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        DungeonWatcherPolicy.breakdownLine("speak", speakMs, athen.watcherShowTicks)));
            }
            return;
        }
        if (DungeonWatcherPolicy.allSpawnedChat(raw)) {
            spawnedAt = now;
            long total = bloodDoorAt > 0L ? now - bloodDoorAt : 0L;
            if (athen.watcherSpawnedAll) {
                title(client, "§cWatcher done");
            }
            if (athen.watcherBreakdown && client != null && client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        DungeonWatcherPolicy.breakdownLine("spawned", total, athen.watcherShowTicks)));
            }
        }
    }

    static List<String> hudLines(boolean editorOpen) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.watcherEnabled || !athen.watcherBloodTimers) {
            return editorOpen ? List.of("Blood timers", "(hidden)") : List.of();
        }
        return new DungeonWatcherPolicy.Timers(
                speakAt > 0L && bloodDoorAt > 0L ? speakAt - bloodDoorAt : 0L,
                moveAt > 0L && bloodDoorAt > 0L ? moveAt - bloodDoorAt : 0L,
                spawnedAt > 0L && bloodDoorAt > 0L ? spawnedAt - bloodDoorAt : 0L,
                spawnedAt > 0L).hudLines(athen.watcherShowTicks);
    }

    private static void markMove(Minecraft client, DungeonAthenSettings athen) {
        moved = true;
        moveAt = System.currentTimeMillis();
        if (athen.watcherMove) {
            title(client, "§eWatcher move");
        }
        if (athen.watcherBreakdown && client.player != null) {
            long moveMs = bloodDoorAt > 0L ? moveAt - bloodDoorAt : 0L;
            client.player.sendSystemMessage(Component.literal(
                    DungeonWatcherPolicy.breakdownLine("move", moveMs, athen.watcherShowTicks)));
        }
    }

    private static void title(Minecraft client, String text) {
        if (client == null || client.gui == null || text == null || text.isBlank()) {
            return;
        }
        client.gui.hud.setTimes(5, 25, 8);
        client.gui.hud.setTitle(Component.literal(text));
    }

    private static void reset() {
        speakAt = 0L;
        moveAt = 0L;
        spawnedAt = 0L;
        moved = false;
        lastSpeed = DungeonWatcherPolicy.Speed.NONE;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
