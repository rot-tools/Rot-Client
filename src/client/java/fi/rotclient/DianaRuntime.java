package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime Diana suite for the Serveri. Particle packet
 * count/speed/offset must be forwarded from a packet mixin; ParticleEngine
 * is only a fallback for the lava trail.
 */
public final class DianaRuntime {
    private static final List<DianaPolicy.Point> TRAIL = new ArrayList<>();
    private static final Map<String, LiveBurrow> BURROWS = new LinkedHashMap<>();
    private static final Map<Integer, LiveMob> MOBS = new LinkedHashMap<>();
    private static Map<String, Integer> drops = new LinkedHashMap<>();
    private static DianaPolicy.GuessResult guess;
    private static DianaPolicy.WarpPoint suggestedWarp;
    private static DianaPolicy.DugChat chain;
    private static long coins;
    private static int inquisitors;
    private static long lastTrailAt;
    private static long lastSpadeUseAt;
    private static long lastGriffinWarnAt;
    private static long lastPartyShareAt;
    private static long lastChatAt;
    private static String lastChat = "";
    private static String titleText = "";
    private static int titleTicks;
    private static boolean griffinWasCorrect;
    private static boolean useWasDown;
    private static String lastSharedKey = "";
    private static int nametagScanTicks;

    private record LiveBurrow(
            DianaPolicy.BlockGuess block,
            DianaPolicy.BurrowType type,
            DianaPolicy.BurrowSample sample,
            long lastSeenMs) {
    }

    private record LiveMob(DianaPolicy.RareMob mob, int entityId, long seenMs) {
    }

    private DianaRuntime() {
    }

    static void clear() {
        TRAIL.clear();
        BURROWS.clear();
        MOBS.clear();
        drops = new LinkedHashMap<>();
        guess = null;
        suggestedWarp = null;
        chain = null;
        coins = 0;
        inquisitors = 0;
        lastTrailAt = 0;
        lastSpadeUseAt = 0;
        lastGriffinWarnAt = 0;
        lastPartyShareAt = 0;
        lastChatAt = 0;
        lastChat = "";
        titleText = "";
        titleTicks = 0;
        griffinWasCorrect = false;
        useWasDown = false;
        lastSharedKey = "";
        nametagScanTicks = 0;
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        Flags flags = Flags.from(qol);
        return flags.burrows || (flags.profit && flags.profitHud);
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        Flags flags = Flags.from(qol);
        if (!flags.burrows && !(flags.profit && flags.profitHud)) {
            return List.of();
        }
        return DianaPolicy.hudLines(
                flags.burrows,
                flags.guess ? (guess == null ? null : guess.block()) : null,
                flags.share && suggestedWarp != null ? suggestedWarp : null,
                chain,
                flags.profit && flags.profitHud,
                drops,
                coins,
                inquisitors);
    }

    static String overlayTitle() {
        return titleTicks > 0 ? titleText : "";
    }

    static boolean allowGameMessage(Component message) {
        if (message == null) {
            return true;
        }
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        String text = message.getString();
        long now = System.currentTimeMillis();
        boolean hide = DianaPolicy.shouldHideDuplicate(
                flags.burrows && flags.fixChat, text, lastChat, now - lastChatAt);
        return !hide;
    }

    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        String text = message.getString();
        long now = System.currentTimeMillis();
        lastChat = text;
        lastChatAt = now;
        Minecraft client = Minecraft.getInstance();

        if (flags.burrows) {
            DianaPolicy.parseDug(text).ifPresent(dug -> {
                chain = dug.max() > 0 ? dug : chain;
                removeNearestBurrow(client, 8.0);
                TRAIL.clear();
                griffinWasCorrect = false;
            });
            if (DianaPolicy.isDefenderChat(text)) {
                griffinWasCorrect = false;
            }
        }

        DianaPolicy.parseSpawn(text).ifPresent(mob -> {
            if (mob == DianaPolicy.RareMob.INQUISITOR) {
                inquisitors++;
            }
            if (flags.mobs && mob.rareHighlight()) {
                flash(mob.display(), true, client);
            }
        });

        if (flags.profit) {
            DianaPolicy.parseDrop(text).ifPresent(drop -> {
                if ("Coins".equals(drop.name())) {
                    coins += drop.amount();
                } else {
                    drops = DianaPolicy.applyDrop(drops, drop);
                }
            });
        }

        if (flags.share) {
            DianaPolicy.parseSharedCoords(text).ifPresent(block -> {
                BURROWS.put(
                        key(block),
                        new LiveBurrow(block, DianaPolicy.BurrowType.GUESS,
                                new DianaPolicy.BurrowSample(true, true, DianaPolicy.BurrowType.GUESS),
                                now));
            });
        }
    }

    static void tick(Minecraft client) {
        if (titleTicks > 0) {
            titleTicks--;
        }
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        if (!flags.burrows && !flags.mobs && !flags.profit && !flags.share) {
            return;
        }
        LocalPlayer player = client.player;
        long now = System.currentTimeMillis();
        boolean holding = holdingSpade(player);
        boolean useDown = client.options != null && client.options.keyUse.isDown();
        if (holding && useDown && !useWasDown) {
            onSpadeUse(now);
        }
        useWasDown = useDown;
        List<String> scoreboard = scoreboardLines(client);
        if (!DianaPolicy.isDoingDiana(scoreboard, holding)) {
            TRAIL.clear();
            BURROWS.clear();
            MOBS.clear();
            guess = null;
            suggestedWarp = null;
            chain = null;
            return;
        }
        DianaPolicy.parseScoreboardBurrows(scoreboard).ifPresent(parsed -> chain = parsed);

        if (flags.mobs && flags.griffinWarn) {
            boolean griffin = hasGriffinPet();
            if (griffin) {
                griffinWasCorrect = true;
            } else if (DianaPolicy.shouldWarnGriffin(
                    true, true, holding, false, griffinWasCorrect)
                    && now - lastGriffinWarnAt > DianaPolicy.GRIFFIN_WARN_COOLDOWN_MS) {
                lastGriffinWarnAt = now;
                flash("Griffin Pet!", true, client);
            }
        }

        if (++nametagScanTicks >= 4) {
            nametagScanTicks = 0;
            scanNametags(client, flags, now);
        }
        expireBurrows(now);
        if (guess != null && flags.share) {
            DianaPolicy.Point target = new DianaPolicy.Point(
                    guess.block().x() + 0.5,
                    guess.block().y(),
                    guess.block().z() + 0.5);
            DianaPolicy.Point here = new DianaPolicy.Point(
                    player.getX(), player.getY(), player.getZ());
            suggestedWarp = DianaPolicy.nearestWarp(target, here).orElse(null);
            if (DianaPolicy.shouldAutoWarp(true, flags.autoWarp)
                    && suggestedWarp != null
                    && player.connection != null
                    && now - lastSpadeUseAt < 1_500L
                    && now - lastPartyShareAt > 2_000L) {
                lastPartyShareAt = now;
                player.connection.sendCommand(suggestedWarp.command());
            }
        } else {
            suggestedWarp = null;
        }
    }

    /**
     * Packet-accurate particle hook. Parent should call this from a
     * ClientboundLevelParticlesPacket mixin with count, maxSpeed, and xyzDist.
     */
    public static void observeParticlePacket(
            String typeId,
            double x,
            double y,
            double z,
            int count,
            float speed,
            float offsetX,
            float offsetY,
            float offsetZ) {
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        Minecraft client = Minecraft.getInstance();
        if (!flags.burrows
                || client.player == null
                || !DianaPolicy.isDoingDiana(
                        scoreboardLines(client),
                        holdingSpade(client.player))) {
            return;
        }
        DianaPolicy.ParticleKind kind = DianaPolicy.classifyParticle(
                new DianaPolicy.ParticlePacket(typeId, count, speed, offsetX, offsetY, offsetZ));
        if (kind == DianaPolicy.ParticleKind.NONE) {
            return;
        }
        long now = System.currentTimeMillis();
        DianaPolicy.Point loc = new DianaPolicy.Point(x, y, z);
        if (kind == DianaPolicy.ParticleKind.SPADE_TRAIL) {
            lastTrailAt = now;
            if (now - lastSpadeUseAt > DianaPolicy.SPADE_GUESS_WINDOW_MS) {
                return;
            }
            if (TRAIL.isEmpty()) {
                TRAIL.add(loc);
            } else if (DianaPolicy.acceptTrailPoint(TRAIL, loc)) {
                TRAIL.add(loc);
            } else {
                return;
            }
            if (flags.guess) {
                DianaPolicy.guessBurrow(TRAIL).ifPresent(result -> {
                    guess = result;
                    if (client.player != null) {
                        DianaPolicy.Point here = new DianaPolicy.Point(
                                client.player.getX(),
                                client.player.getY(),
                                client.player.getZ());
                        suggestedWarp = DianaPolicy.nearestWarp(
                                result.location(), here).orElse(null);
                    }
                });
            }
            return;
        }
        if (!flags.particles) {
            return;
        }
        DianaPolicy.BlockGuess block = DianaPolicy.burrowBlock(loc);
        String key = key(block);
        LiveBurrow existing = BURROWS.get(key);
        DianaPolicy.BurrowSample sample = DianaPolicy.applyParticle(
                existing == null ? null : existing.sample(), kind);
        DianaPolicy.BurrowType type = sample.type();
        BURROWS.put(key, new LiveBurrow(block, type, sample, now));
    }

    public static void observeParticle(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xs,
            double ys,
            double zs) {
        if (options == null) {
            return;
        }
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        String key = id == null ? "" : id.toString();
        if (!key.toLowerCase().contains("dripping_lava")) {
            return;
        }
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        if (!flags.burrows || !flags.guess) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSpadeUseAt > DianaPolicy.SPADE_GUESS_WINDOW_MS) {
            return;
        }
        // Packet mixin already classifies dripping_lava with real count/speed.
        // Do not invent SPADE_TRAIL from the client particle engine.
    }

    public static void onSpadeUse() {
        onSpadeUse(System.currentTimeMillis());
    }

    private static void onSpadeUse(long now) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !holdingSpade(client.player)) {
            return;
        }
        if (DianaPolicy.shouldCancelBuggedSpadeClick(lastTrailAt, now)) {
            return;
        }
        TRAIL.clear();
        guess = null;
        lastSpadeUseAt = now;
    }

    public static boolean shouldMuteSound(
            String soundName,
            float pitch,
            float volume,
            boolean locationZero) {
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        return DianaPolicy.shouldMuteBuggedSpade(
                flags.burrows && flags.muteSpade,
                doingDiana(Minecraft.getInstance()),
                soundName,
                pitch,
                volume,
                locationZero);
    }

    public static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.level == null) {
            return;
        }
        if (!DianaPolicy.isDoingDiana(
                scoreboardLines(client),
                holdingSpade(player))) {
            return;
        }
        Flags flags = Flags.from(RotClientClient.qolConfigPublic());
        if (flags.burrows && flags.waypoints) {
            if (guess != null && flags.guess) {
                box(guess.block(), flags.guessColor);
                Gizmos.line(
                        player.getEyePosition(),
                        new Vec3(
                                guess.block().x() + 0.5,
                                guess.block().y() + 0.2,
                                guess.block().z() + 0.5),
                        flags.guessColor)
                        .setAlwaysOnTop();
            }
            for (LiveBurrow burrow : BURROWS.values()) {
                if (!burrow.sample().complete() && burrow.type() == DianaPolicy.BurrowType.UNKNOWN) {
                    continue;
                }
                box(burrow.block(), colorFor(burrow.type(), flags));
            }
        }
        if (flags.mobs && flags.rareEsp) {
            for (LiveMob live : MOBS.values()) {
                Entity entity = client.level.getEntity(live.entityId());
                if (entity == null) {
                    continue;
                }
                AABB box = entity.getBoundingBox().inflate(0.2D);
                Gizmos.cuboid(
                        box,
                        GizmoStyle.strokeAndFill(flags.mobColor, 2.0F, withAlpha(flags.mobColor, 0x55)))
                        .setAlwaysOnTop();
            }
        }
    }

    private static void scanNametags(Minecraft client, Flags flags, long now) {
        LocalPlayer player = client.player;
        AABB search = player.getBoundingBox().inflate(48.0D);
        Iterator<Map.Entry<Integer, LiveMob>> it = MOBS.entrySet().iterator();
        while (it.hasNext()) {
            Entity entity = client.level.getEntity(it.next().getKey());
            if (entity == null || !entity.isAlive()) {
                it.remove();
            }
        }
        if (!flags.mobs) {
            return;
        }
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
            String name = stand.getName() == null ? "" : stand.getName().getString();
            DianaPolicy.matchNametag(name).ifPresent(mob -> {
                if (!mob.rareHighlight() && mob != DianaPolicy.RareMob.CHAMPION) {
                    return;
                }
                boolean isNew = !MOBS.containsKey(stand.getId());
                MOBS.put(stand.getId(), new LiveMob(mob, stand.getId(), now));
                if (isNew && mob.rareHighlight()) {
                    flash(mob.display(), true, client);
                    if (DianaPolicy.shouldPartyShare(flags.share, flags.partyShare, mob)
                            && client.player.connection != null
                            && now - lastPartyShareAt > DianaPolicy.PARTY_SHARE_COOLDOWN_MS) {
                        String shareKey = mob.display() + ":" + stand.blockPosition().getX()
                                + ":" + stand.blockPosition().getZ();
                        if (!shareKey.equals(lastSharedKey)) {
                            lastSharedKey = shareKey;
                            lastPartyShareAt = now;
                            client.player.connection.sendCommand(
                                    "pc " + DianaPolicy.partyShareLine(
                                            mob,
                                            stand.blockPosition().getX(),
                                            stand.blockPosition().getY(),
                                            stand.blockPosition().getZ()));
                        }
                    }
                }
            });
        }
    }

    private static void expireBurrows(long now) {
        BURROWS.entrySet().removeIf(entry ->
                DianaPolicy.burrowIsStale(
                        now, entry.getValue().lastSeenMs()));
    }

    private static boolean doingDiana(Minecraft client) {
        return client != null
                && client.player != null
                && DianaPolicy.isDoingDiana(
                        scoreboardLines(client),
                        holdingSpade(client.player));
    }

    private static void removeNearestBurrow(Minecraft client, double maxDist) {
        if (client == null || client.player == null || BURROWS.isEmpty()) {
            return;
        }
        Vec3 here = client.player.position();
        String bestKey = null;
        double best = maxDist;
        for (Map.Entry<String, LiveBurrow> entry : BURROWS.entrySet()) {
            DianaPolicy.BlockGuess block = entry.getValue().block();
            double dist = here.distanceTo(new Vec3(block.x() + 0.5, block.y(), block.z() + 0.5));
            if (dist < best) {
                best = dist;
                bestKey = entry.getKey();
            }
        }
        if (bestKey != null) {
            BURROWS.remove(bestKey);
        }
        guess = null;
    }

    private static boolean holdingSpade(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        String id = AutoClickerItemIdentity.skyBlockId(stack);
        String hover = stack == null || stack.isEmpty()
                ? ""
                : stack.getHoverName().getString();
        return DianaPolicy.isDianaSpade(id, hover);
    }

    private static boolean hasGriffinPet() {
        PetHudPolicy.Snapshot snapshot = InventoryChromeRuntime.petHudSnapshot();
        return snapshot != null && DianaPolicy.isGriffinPet(snapshot.name());
    }

    private static List<String> scoreboardLines(Minecraft client) {
        return CommissionDisplayRuntime.tabLines(client);
    }

    private static void flash(String text, boolean sound, Minecraft client) {
        titleText = text;
        titleTicks = 40;
        if (sound && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.3F);
        }
    }

    private static void box(DianaPolicy.BlockGuess block, int color) {
        AABB box = new AABB(
                block.x(),
                block.y(),
                block.z(),
                block.x() + 1,
                block.y() + 1,
                block.z() + 1);
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(color, 2.0F, withAlpha(color, 0x44)))
                .setAlwaysOnTop();
    }

    private static int colorFor(DianaPolicy.BurrowType type, Flags flags) {
        return switch (type) {
            case START -> flags.startColor;
            case MOB -> flags.mobBurrowColor;
            case TREASURE -> flags.treasureColor;
            default -> flags.guessColor;
        };
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static String key(DianaPolicy.BlockGuess block) {
        return block.x() + " " + block.y() + " " + block.z();
    }

    /**
     * Reads parent extras fields when present so this lane compiles before
     * QolSkyblockExtras is merged. Defaults match catalog (modules off,
     * cheat off, legit sub-toggles on).
     */
    record Flags(
            boolean burrows,
            boolean guess,
            boolean particles,
            boolean waypoints,
            boolean muteSpade,
            boolean fixChat,
            boolean mobs,
            boolean rareEsp,
            boolean griffinWarn,
            boolean profit,
            boolean profitHud,
            boolean share,
            boolean partyShare,
            boolean autoWarp,
            int guessColor,
            int startColor,
            int mobBurrowColor,
            int treasureColor,
            int mobColor) {
        static Flags from(QolUtilityConfig qol) {
            QolSkyblockExtras extras = qol == null ? null : qol.extras();
            return new Flags(
                    bool(extras, "dianaBurrowsEnabled", false),
                    bool(extras, "dianaBurrowsGuess", true),
                    bool(extras, "dianaBurrowsParticles", true),
                    bool(extras, "dianaBurrowsWaypoints", true),
                    bool(extras, "dianaBurrowsMuteSpade", true),
                    bool(extras, "dianaBurrowsFixChat", true),
                    bool(extras, "dianaMobsEnabled", false),
                    bool(extras, "dianaMobsRareEsp", true),
                    bool(extras, "dianaMobsGriffinWarn", true),
                    bool(extras, "dianaProfitEnabled", false),
                    bool(extras, "dianaProfitHud", true),
                    QolFlavorSupport.isPlus() && bool(extras, "dianaShareEnabled", false),
                    QolFlavorSupport.isPlus() && bool(extras, "dianaShareParty", false),
                    QolFlavorSupport.isPlus() && bool(extras, "dianaShareAutoWarp", false),
                    color(extras, "dianaBurrowsGuessColor", 0xFF55FF55),
                    color(extras, "dianaBurrowsStartColor", 0xFF55FFFF),
                    color(extras, "dianaBurrowsMobColor", 0xFFFF5555),
                    color(extras, "dianaBurrowsTreasureColor", 0xFFFFAA00),
                    color(extras, "dianaMobsEspColor", 0xFFFF55FF));
        }

        private static boolean bool(QolSkyblockExtras extras, String name, boolean fallback) {
            if (extras == null) {
                return fallback;
            }
            try {
                Field field = extras.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.getBoolean(extras);
            } catch (ReflectiveOperationException ignored) {
                return fallback;
            }
        }

        private static int color(QolSkyblockExtras extras, String name, int fallback) {
            if (extras == null) {
                return fallback;
            }
            try {
                Field field = extras.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(extras);
            } catch (ReflectiveOperationException ignored) {
                return fallback;
            }
        }
    }
}
