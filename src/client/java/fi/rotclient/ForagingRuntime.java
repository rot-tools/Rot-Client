package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Galatea / Park / Torrhus foraging HUD, solvers, audio mutes, and optional
 * local-test cheat assists. Reads the four catalog parents through
 * {@link QolUtilityConfig} the same way the dashboard writes them.
 */
public final class ForagingRuntime {
    private static final ForagingGiftTracker GIFTS = new ForagingGiftTracker();
    private static ForagingPolicy.Island island = ForagingPolicy.Island.NONE;
    private static ForagingPolicy.TreeProgress progress;
    private static int tabSweep;
    private static int clusterWood;
    private static int maxWood;
    private static boolean beaconReady;
    private static ForagingPolicy.BeaconHint beaconHint;
    private static int templePending;
    private static String titleText = "";
    private static int titleTicks;
    private static int cheatDelay;
    private static int lastTargetPane = -1;
    private static int ticksSincePaneMove;
    private static int targetSpeed;
    private static ForagingPolicy.BeaconPitch currentPitch;
    private static boolean hotfOpen;
    private static int shardTotal;
    private static final Set<Integer> lassoAlerted = new HashSet<>();

    private ForagingRuntime() {
    }

    static void clear() {
        GIFTS.resetSession();
        island = ForagingPolicy.Island.NONE;
        progress = null;
        tabSweep = 0;
        clusterWood = 0;
        maxWood = 0;
        beaconReady = false;
        beaconHint = null;
        templePending = 0;
        titleText = "";
        titleTicks = 0;
        cheatDelay = 0;
        lastTargetPane = -1;
        ticksSincePaneMove = 0;
        targetSpeed = 0;
        currentPitch = null;
        hotfOpen = false;
        shardTotal = 0;
        lassoAlerted.clear();
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        if (qol == null) {
            return false;
        }
        ForagingSettings settings = ForagingSettings.from(qol);
        return settings.trees || settings.helpers || settings.cheats;
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        if (!hudVisible(qol)) {
            return List.of();
        }
        QolSkyblockExtras extras = qol.extras();
        List<String> lines = new ArrayList<>();
        if (toggle(extras, "qol.foraging_trees", "qol.foraging_trees.progress_hud", true)
                && ForagingPolicy.showTreeProgress(
                        true,
                        island,
                        toggle(extras, "qol.foraging_trees", "qol.foraging_trees.only_axe", false),
                        holdingAxe(Minecraft.getInstance()),
                        progress)) {
            String line = ForagingPolicy.compactProgressLine(progress);
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        if (toggle(extras, "qol.foraging_trees", "qol.foraging_trees.gift_hud", true)) {
            String gift = GIFTS.hudLine();
            if (!gift.isBlank()) {
                lines.add("Gift " + gift);
            }
        }
        if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.sweep_hud", true)
                && ForagingPolicy.showSweepHud(true, island)) {
            String sweep = ForagingPolicy.sweepHudLine(tabSweep, clusterWood, maxWood);
            if (!sweep.isBlank()) {
                lines.add(sweep);
            }
        }
        if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.beacon_hints", true)
                && ForagingPolicy.customTrees(island)) {
            if (beaconHint != null && beaconHint.pending()) {
                lines.add(beaconHint.hudLine());
            } else if (beaconReady) {
                lines.add("Beacon AVAILABLE");
            }
        }
        if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.temple_solver", true)) {
            String temple = ForagingPolicy.templeHudLine(templePending);
            if (!temple.isBlank()) {
                lines.add(temple);
            }
        }
        if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.frog_mask", true)
                && Minecraft.getInstance().player != null) {
            ItemStack helmet = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD);
            boolean mask = ForagingPolicy.isFrogMask(
                    AutoClickerItemIdentity.skyBlockId(helmet),
                    helmet.getHoverName().getString());
            if (ForagingPolicy.showFrogMaskHud(true, island, mask)) {
                lines.add("Frog Mask");
            }
        }
        if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.lasso_hud", true)
                && Minecraft.getInstance().player != null) {
            ItemStack hand = Minecraft.getInstance().player.getMainHandItem();
            boolean lasso = ForagingPolicy.isLasso(
                    AutoClickerItemIdentity.skyBlockId(hand),
                    hand.getHoverName().getString());
            if (ForagingPolicy.showLassoHud(true, island, lasso)) {
                lines.add("Lasso");
            }
        }
        if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.shard_tracker", true)) {
            String shards = ForagingPolicy.shardHudLine(shardTotal);
            if (!shards.isBlank()) {
                lines.add(shards);
            }
        }
        return lines;
    }

    static String overlayTitle() {
        return titleTicks > 0 ? titleText : "";
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        if (titleTicks > 0) {
            titleTicks--;
        }
        if (cheatDelay > 0) {
            cheatDelay--;
        }
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        if (!trees(extras) && !audio(extras) && !helpers(extras) && !cheats(extras)) {
            return;
        }
        island = detectIsland();
        List<String> tab = CommissionDisplayRuntime.tabLines(client);
        refreshTab(tab);
        scanProgress(client, extras);
        if (helpers(extras)) {
            refreshSweep(client, extras);
            refreshBeaconMenu(client, extras);
            String title = screenTitle(client);
            boolean hotfNow = ForagingPolicy.isHotfTitle(title)
                    && toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.hotf_hint", true);
            if (hotfNow && !hotfOpen) {
                flash("Heart of the Forest", false, client);
            }
            hotfOpen = hotfNow;
            maybeLassoAlert(client, extras);
        }
        if (cheats(extras) && cheatDelay <= 0) {
            maybeCheatBeacon(client, extras);
            maybeCheatChopOrToss(client, extras);
        }
    }

    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        String text = message.getString();
        if (trees(extras)) {
            GIFTS.ingest(text);
            if (ForagingPolicy.isTreeFelledChat(text)
                    && toggle(extras, "qol.foraging_trees", "qol.foraging_trees.fell_title", true)) {
                flash("Tree felled", true, Minecraft.getInstance());
            }
        }
        if (helpers(extras)) {
            int sweep = ForagingPolicy.parseSweepDetails(text);
            if (sweep > 0) {
                tabSweep = sweep;
            }
            if (ForagingPolicy.isHoneyhiveLootChat(text) || ForagingPolicy.isQueenBeeChat(text)) {
                flash(ForagingPolicy.isQueenBeeChat(text) ? "Queen Bee" : "Honeyhive", true, Minecraft.getInstance());
            }
            if (toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.shard_tracker", true)
                    && !ForagingPolicy.parseShardGain(text).isEmpty()) {
                shardTotal++;
            }
        }
    }

    static boolean allowGameMessage(Component message) {
        if (message == null) {
            return true;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!toggle(extras, "qol.foraging_trees", "qol.foraging_trees.hide_unmineable", true)) {
            return true;
        }
        return !ForagingPolicy.hideUnmineableChat(true, island, message.getString());
    }

    public static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (helpers(extras)) {
            renderCinderbats(client, extras);
        }
        if (!helpers(extras) || island == ForagingPolicy.Island.NONE) {
            return;
        }
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        int min = seaLumiesMin(extras);
        boolean highlight = toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.highlights", true);
        boolean temple = toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.temple_solver", true);
        templePending = 0;
        int radius = 24;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = client.level.getBlockState(pos);
                    String id = blockId(state);
                    if (highlight && ForagingPolicy.highlightLushlilac(island, id)) {
                        box(pos, 0xFF55FF55);
                    }
                    if (highlight && ForagingPolicy.highlightVeilshroom(island, id)) {
                        box(pos, 0xFFFF5555);
                    }
                    int pickles = pickleCount(state);
                    if (highlight && ForagingPolicy.highlightSeaLumies(island, id, pickles, min)) {
                        box(pos, 0xFF55FFFF);
                    }
                    int honey = honeyLevel(state);
                    boolean fence = client.level.getBlockState(pos.above()).getBlock().toString()
                            .toLowerCase(Locale.ROOT)
                            .contains("birch_fence");
                    if (highlight && ForagingPolicy.highlightHoneyhive(island, id, honey, fence)) {
                        box(pos, 0xFFFFAA00);
                    }
                }
            }
        }
        if (temple && island == ForagingPolicy.Island.GALATEA) {
            renderForestTemple(client);
        }
        if (temple && island == ForagingPolicy.Island.TORRHUS) {
            renderDesertTempleOrder(client);
        }
        if (highlight && toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.moonglade_beacon", true)
                && island == ForagingPolicy.Island.GALATEA) {
            box(new BlockPos(
                    ForagingPolicy.MOONGLADE_BEACON_X,
                    ForagingPolicy.MOONGLADE_BEACON_Y,
                    ForagingPolicy.MOONGLADE_BEACON_Z), 0xFFCC66FF);
        }
        if (highlight && toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.park_tutorial", true)
                && island == ForagingPolicy.Island.PARK) {
            for (ForagingPolicy.TutorialStep step : ForagingPolicy.PARK_TUTORIAL) {
                box(BlockPos.containing(step.x(), step.y(), step.z()), 0xFFFFFF55);
            }
        }
        if (highlight && toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.hunting_esp", true)
                && ForagingPolicy.customTrees(island)) {
            AABB search = player.getBoundingBox().inflate(48.0D);
            for (Entity entity : client.level.getEntities(player, search)) {
                String name = entity.getCustomName() != null
                        ? entity.getCustomName().getString()
                        : entity.getName().getString();
                ForagingPolicy.HuntGlow glow = ForagingPolicy.huntGlow(
                        island, name, entity.getType().toString());
                if (glow == ForagingPolicy.HuntGlow.CINDERBAT) {
                    continue;
                }
                int color = ForagingPolicy.huntGlowColor(glow);
                if (color != 0) {
                    boxEntity(entity, color);
                }
            }
        }
    }

    private static void renderCinderbats(Minecraft client, QolSkyblockExtras extras) {
        if (!toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.cinderbat", true)
                || client.player == null
                || client.level == null) {
            return;
        }
        AABB search = client.player.getBoundingBox().inflate(48.0D);
        int color = ForagingPolicy.huntGlowColor(ForagingPolicy.HuntGlow.CINDERBAT);
        for (Entity entity : client.level.getEntities(client.player, search)) {
            String name = entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getName().getString();
            if (ForagingPolicy.cinderbatName(name)) {
                boxEntity(entity, color);
            }
        }
    }

    private static void maybeLassoAlert(Minecraft client, QolSkyblockExtras extras) {
        if (!toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.lasso_alert", false)
                || client.player == null
                || client.level == null) {
            lassoAlerted.clear();
            return;
        }
        ItemStack hand = client.player.getMainHandItem();
        boolean holding = ForagingPolicy.isLasso(
                AutoClickerItemIdentity.skyBlockId(hand),
                hand.getHoverName().getString());
        if (!holding) {
            lassoAlerted.clear();
            return;
        }
        AABB search = client.player.getBoundingBox().inflate(16.0D);
        boolean nearby = false;
        for (Entity entity : client.level.getEntities(client.player, search)) {
            String name = entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getName().getString();
            ForagingPolicy.HuntGlow glow = ForagingPolicy.huntGlow(
                    island, name, entity.getType().toString());
            if (glow == ForagingPolicy.HuntGlow.NONE && !ForagingPolicy.cinderbatName(name)) {
                continue;
            }
            nearby = true;
            if (lassoAlerted.add(entity.getId())) {
                client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.9F, 0.7F);
            }
        }
        if (!ForagingPolicy.lassoAlert(true, holding, nearby)) {
            return;
        }
    }

    public static boolean shouldSuppressEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!toggle(extras, "qol.foraging_trees", "qol.foraging_trees.hide_bits", true)) {
            return false;
        }
        if (!(entity instanceof Display.BlockDisplay display)) {
            return false;
        }
        return ForagingPolicy.shouldHideTreeBits(true, island, true, blockId(display.getBlockState()));
    }

    public static boolean shouldHideParticle(ParticleOptions options, double x, double y, double z) {
        return false;
    }

    public static boolean shouldMuteSound(String soundId, float volume) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!audio(extras) || soundId == null) {
            return false;
        }
        return ForagingPolicy.shouldMuteSound(
                toggle(extras, "qol.foraging_audio", "qol.foraging_audio.mute_phantom", true),
                toggle(extras, "qol.foraging_audio", "qol.foraging_audio.mute_tree_break", true),
                toggle(extras, "qol.foraging_audio", "qol.foraging_audio.mute_break_galatea", true),
                toggle(extras, "qol.foraging_audio", "qol.foraging_audio.mute_fusion", true),
                toggle(extras, "qol.foraging_audio", "qol.foraging_audio.mute_stereo", false),
                island,
                nearbyMusicPants(),
                soundId,
                volume);
    }

    public static void observeSound(String soundId, float pitch, float volume) {
        if (soundId == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!helpers(extras) || !toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.beacon_hints", true)) {
            return;
        }
        if (!ForagingPolicy.isBeaconTuneTitle(screenTitle(Minecraft.getInstance()))) {
            return;
        }
        if (!soundId.toLowerCase(Locale.ROOT).contains("note_block.bass")
                && !soundId.toLowerCase(Locale.ROOT).contains("note.bass")) {
            return;
        }
        ForagingPolicy.BeaconPitch heard = ForagingPolicy.classifyPitch(pitch);
        if (currentPitch == null || heard == null) {
            return;
        }
        int clicks = ForagingPolicy.pitchClicks(currentPitch, heard);
        beaconHint = mergeHint(beaconHint, 0, 0, clicks);
    }

    private static void refreshTab(List<String> tab) {
        beaconReady = false;
        for (String line : tab) {
            int sweep = ForagingPolicy.parseTabSweep(line);
            if (sweep > 0) {
                tabSweep = sweep;
            }
            if (ForagingPolicy.isBeaconReady(line)) {
                beaconReady = true;
            }
            ForagingPolicy.Island fromTab = ForagingPolicy.islandFromArea(line);
            island = ForagingPolicy.higherRank(island, fromTab);
        }
    }

    private static void scanProgress(Minecraft client, QolSkyblockExtras extras) {
        if (!trees(extras)) {
            progress = null;
            return;
        }
        ForagingPolicy.TreeProgress best = null;
        AABB search = client.player.getBoundingBox().inflate(48.0D);
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
            String name = stand.getCustomName() != null
                    ? stand.getCustomName().getString()
                    : stand.getName().getString();
            ForagingPolicy.TreeProgress parsed = ForagingPolicy.parseTreeProgress(name);
            if (parsed == null) {
                continue;
            }
            if (best == null || parsed.percent() > best.percent()) {
                best = parsed;
            }
        }
        progress = best;
        if (best != null) {
            island = ForagingPolicy.higherRank(island, ForagingPolicy.islandFromTreeType(best.type()));
        }
    }

    private static void refreshSweep(Minecraft client, QolSkyblockExtras extras) {
        if (!toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.sweep_hud", true)) {
            return;
        }
        LocalPlayer player = client.player;
        String axeId = AutoClickerItemIdentity.skyBlockId(player.getMainHandItem());
        boolean thrown = ForagingPolicy.isThrowableAxe(axeId);
        HitResult hit = client.hitResult;
        clusterWood = 0;
        maxWood = 0;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos look = ((BlockHitResult) hit).getBlockPos();
        BlockState looked = client.level.getBlockState(look);
        String lookId = blockId(looked);
        island = ForagingPolicy.inferIslandFromLog(island, lookId);
        if (!ForagingPolicy.showSweepHud(true, island) || !ForagingPolicy.isChopLog(island, lookId)) {
            return;
        }
        maxWood = ForagingPolicy.maxWood(Math.max(1, tabSweep), ForagingPolicy.toughness(lookId), thrown);
        Set<ForagingPolicy.BlockKey> logs = new HashSet<>();
        int r = 6;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = look.offset(dx, dy, dz);
                    String id = blockId(client.level.getBlockState(pos));
                    if (ForagingPolicy.isChopLog(island, id)
                            && ForagingPolicy.sameChopFamily(island, lookId, id)) {
                        logs.add(new ForagingPolicy.BlockKey(pos.getX(), pos.getY(), pos.getZ()));
                    }
                }
            }
        }
        clusterWood = ForagingPolicy.countSweepCluster(
                logs,
                new ForagingPolicy.BlockKey(look.getX(), look.getY(), look.getZ()),
                maxWood);
    }

    private static void refreshBeaconMenu(Minecraft client, QolSkyblockExtras extras) {
        if (!toggle(extras, "qol.foraging_helpers", "qol.foraging_helpers.beacon_hints", true)) {
            lastTargetPane = -1;
            return;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?> container)
                || !ForagingPolicy.isBeaconTuneTitle(screen.getTitle().getString())) {
            lastTargetPane = -1;
            ticksSincePaneMove = 0;
            return;
        }
        String title = screen.getTitle().getString();
        int colorSlot = ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_COLOR_SLOT);
        int speedSlot = ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_SPEED_SLOT);
        int pitchSlot = ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_PITCH_SLOT);
        String dye = itemPath(stackIn(container, colorSlot));
        String targetGlass = firstPane(container, ForagingPolicy.BEACON_TARGET_SLOT_START, ForagingPolicy.BEACON_TARGET_SLOT_END);
        int colorClicks = ForagingPolicy.colorClicks(dye, targetGlass);
        int currentSpeed = 0;
        for (String lore : InventoryChromeRuntime.loreLines(stackIn(container, speedSlot))) {
            int parsed = ForagingPolicy.parseCurrentSpeed(lore);
            if (parsed > 0) {
                currentSpeed = parsed;
            }
        }
        currentPitch = null;
        for (String lore : InventoryChromeRuntime.loreLines(stackIn(container, pitchSlot))) {
            ForagingPolicy.BeaconPitch parsed = ForagingPolicy.parsePitchName(lore);
            if (parsed != null) {
                currentPitch = parsed;
            }
        }
        int targetPane = firstPaneSlot(container, ForagingPolicy.BEACON_TARGET_SLOT_START, ForagingPolicy.BEACON_TARGET_SLOT_END);
        if (targetPane >= 0 && lastTargetPane >= 0 && targetPane != lastTargetPane) {
            int guessed = ForagingPolicy.speedFromMoveTicks(ticksSincePaneMove);
            if (guessed > 0) {
                targetSpeed = guessed;
            }
            ticksSincePaneMove = 0;
        } else if (targetPane >= 0) {
            ticksSincePaneMove++;
        }
        lastTargetPane = targetPane;
        int speedClicks = ForagingPolicy.speedClicks(currentSpeed, targetSpeed);
        int pitchClicks = beaconHint == null ? 0 : beaconHint.pitchClicks();
        beaconHint = new ForagingPolicy.BeaconHint(colorClicks, speedClicks, pitchClicks);
    }

    private static void maybeCheatBeacon(Minecraft client, QolSkyblockExtras extras) {
        if (!toggle(extras, "qol.foraging_cheats", "qol.foraging_cheats.auto_beacon", false)) {
            return;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?> container) || client.gameMode == null) {
            return;
        }
        String title = screen.getTitle().getString();
        ForagingPolicy.AutoBeaconClick click = ForagingPolicy.nextBeaconClick(
                true, true, title, beaconHint);
        if (click == null) {
            return;
        }
        if (click.slot() < 0 || click.slot() >= container.getMenu().slots.size()) {
            return;
        }
        client.gameMode.handleContainerInput(
                container.getMenu().containerId,
                click.slot(),
                click.rightClick() ? 1 : 0,
                ContainerInput.PICKUP,
                client.player);
        beaconHint = ForagingPolicy.applyPress(beaconHint, click.slot(), click.rightClick(), title);
        cheatDelay = cheatDelay(extras);
    }

    private static void maybeCheatChopOrToss(Minecraft client, QolSkyblockExtras extras) {
        if (client.gui != null && client.gui.screen() != null) {
            return;
        }
        LocalPlayer player = client.player;
        String axeId = AutoClickerItemIdentity.skyBlockId(player.getMainHandItem());
        boolean holdingAxe = ForagingPolicy.isForagingAxe(axeId);
        HitResult hit = client.hitResult;
        boolean lookingLog = false;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            String lookId = blockId(client.level.getBlockState(((BlockHitResult) hit).getBlockPos()));
            island = ForagingPolicy.inferIslandFromLog(island, lookId);
            lookingLog = ForagingPolicy.isChopLog(island, lookId);
        }
        int minCluster = number(extras, "qol.foraging_cheats.min_cluster", 5);
        if (ForagingPolicy.shouldAxeToss(
                cheats(extras),
                toggle(extras, "qol.foraging_cheats", "qol.foraging_cheats.axe_toss", false),
                ForagingPolicy.isThrowableAxe(axeId),
                clusterWood,
                minCluster,
                true)) {
            AutoClickerRuntime.pulseUse(client);
            cheatDelay = cheatDelay(extras);
            return;
        }
        if (ForagingPolicy.shouldAutoChop(
                cheats(extras),
                toggle(extras, "qol.foraging_cheats", "qol.foraging_cheats.auto_chop", false),
                island,
                holdingAxe,
                lookingLog)) {
            AutoClickerRuntime.pulseAttack(client);
            cheatDelay = cheatDelay(extras);
        }
    }

    private static void renderForestTemple(Minecraft client) {
        List<ForagingPolicy.Cardinal> walls = new ArrayList<>();
        for (int x = ForagingPolicy.FOREST_TEMPLE_WALL_X_MAX; x >= ForagingPolicy.FOREST_TEMPLE_WALL_X_MIN; x--) {
            for (int y = ForagingPolicy.FOREST_TEMPLE_WALL_Y_MAX; y >= ForagingPolicy.FOREST_TEMPLE_WALL_Y_MIN; y--) {
                BlockPos pos = new BlockPos(x, y, ForagingPolicy.FOREST_TEMPLE_WALL_Z);
                BlockState state = client.level.getBlockState(pos);
                if (!blockId(state).contains("orange_glazed_terracotta")) {
                    continue;
                }
                ForagingPolicy.Cardinal facing = facingOf(state);
                if (facing != null) {
                    walls.add(facing);
                }
            }
        }
        int pending = 0;
        for (int i = 0; i < walls.size(); i++) {
            ForagingPolicy.BlockKey key = ForagingPolicy.forestTempleFloorBlock(i);
            BlockPos floor = new BlockPos(key.x(), key.y(), key.z());
            BlockState state = client.level.getBlockState(floor);
            ForagingPolicy.Cardinal floorFacing = facingOf(state);
            int turns = ForagingPolicy.forestTempleTurns(walls.get(i), floorFacing);
            if (turns != 0) {
                pending++;
                box(floor, 0xFF5555FF);
            }
        }
        templePending = pending;
    }

    private static void renderDesertTempleOrder(Minecraft client) {
        Map<String, Integer> counts = new HashMap<>();
        BlockPos centre = new BlockPos(-612, 42, 227);
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> queue = new ArrayList<>();
        queue.add(centre);
        visited.add(centre);
        for (int i = 0; i < queue.size(); i++) {
            BlockPos current = queue.get(i);
            BlockState state = client.level.getBlockState(current);
            String id = blockId(state);
            if (!id.contains("stained_glass") || id.contains("pane")) {
                continue;
            }
            String color = id.replace("_stained_glass", "").replace("minecraft:", "");
            counts.merge(color.toUpperCase(Locale.ROOT), 1, Integer::sum);
            for (Direction dir : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockPos next = current.relative(dir);
                if (visited.add(next) && client.level.getBlockState(next).toString().contains("stained_glass")) {
                    queue.add(next);
                }
            }
        }
        if (!counts.isEmpty()) {
            templePending = ForagingPolicy.desertTempleButtonOrder(counts).size();
        }
    }

    private static ForagingPolicy.Island detectIsland() {
        return ForagingPolicy.islandFromTexts(
                SkyBlockSidebar.text(),
                SkyBlockAreaDetector.detect().displayName());
    }

    private static boolean holdingAxe(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        return ForagingPolicy.isForagingAxe(
                AutoClickerItemIdentity.skyBlockId(client.player.getMainHandItem()));
    }

    private static boolean nearbyMusicPants() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return false;
        }
        AABB box = client.player.getBoundingBox().inflate(8.0D);
        for (Player player : client.level.getEntitiesOfClass(Player.class, box)) {
            ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
            String id = AutoClickerItemIdentity.skyBlockId(legs).toUpperCase(Locale.ROOT);
            String name = legs.getHoverName().getString().toUpperCase(Locale.ROOT);
            if (id.contains("STEREO") || name.contains("STEREO")) {
                return true;
            }
        }
        return false;
    }

    private static void flash(String text, boolean sound, Minecraft client) {
        titleText = text == null ? "" : text;
        titleTicks = 40;
        if (sound && client != null && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.2F);
        }
    }

    private static void box(BlockPos pos, int color) {
        AABB aabb = new AABB(pos).inflate(0.02D);
        Gizmos.cuboid(aabb, GizmoStyle.stroke(color, 1.5F)).setAlwaysOnTop();
    }

    private static void boxEntity(Entity entity, int color) {
        Gizmos.cuboid(entity.getBoundingBox().inflate(0.08D), GizmoStyle.stroke(color, 1.5F)).setAlwaysOnTop();
    }

    private static String blockId(BlockState state) {
        if (state == null) {
            return "";
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == null ? "" : id.toString();
    }

    private static String itemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    private static ItemStack stackIn(AbstractContainerScreen<?> screen, int slot) {
        List<Slot> slots = screen.getMenu().slots;
        if (slot < 0 || slot >= slots.size()) {
            return ItemStack.EMPTY;
        }
        return slots.get(slot).getItem();
    }

    private static String firstPane(AbstractContainerScreen<?> screen, int start, int end) {
        int slot = firstPaneSlot(screen, start, end);
        return slot < 0 ? "" : itemPath(stackIn(screen, slot));
    }

    private static int firstPaneSlot(AbstractContainerScreen<?> screen, int start, int end) {
        for (int i = start; i <= end; i++) {
            String path = itemPath(stackIn(screen, i));
            if (ForagingPolicy.cycleIndex(path, ForagingPolicy.BEACON_GLASS_CYCLE) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private static ForagingPolicy.Cardinal facingOf(BlockState state) {
        if (state == null || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return null;
        }
        Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return ForagingPolicy.cardinalFromFacing(dir.getSerializedName());
    }

    private static int pickleCount(BlockState state) {
        if (state == null || !state.hasProperty(BlockStateProperties.PICKLES)) {
            return 0;
        }
        return state.getValue(BlockStateProperties.PICKLES);
    }

    private static int honeyLevel(BlockState state) {
        if (state == null || !state.hasProperty(BlockStateProperties.LEVEL_HONEY)) {
            return 0;
        }
        return state.getValue(BlockStateProperties.LEVEL_HONEY);
    }

    private static ForagingPolicy.BeaconHint mergeHint(
            ForagingPolicy.BeaconHint current, int color, int speed, int pitch) {
        if (current == null) {
            return new ForagingPolicy.BeaconHint(color, speed, pitch);
        }
        return new ForagingPolicy.BeaconHint(
                color != 0 ? color : current.colorClicks(),
                speed != 0 ? speed : current.speedClicks(),
                pitch != 0 ? pitch : current.pitchClicks());
    }

    private static String screenTitle(Minecraft client) {
        if (client == null || client.gui == null || client.gui.screen() == null) {
            return "";
        }
        Component title = client.gui.screen().getTitle();
        return title == null ? "" : title.getString();
    }

    private static boolean trees(QolSkyblockExtras extras) {
        return qol().isModuleEnabled("qol.foraging_trees");
    }

    private static boolean audio(QolSkyblockExtras extras) {
        return qol().isModuleEnabled("qol.foraging_audio");
    }

    private static boolean helpers(QolSkyblockExtras extras) {
        return qol().isModuleEnabled("qol.foraging_helpers");
    }

    private static boolean cheats(QolSkyblockExtras extras) {
        return qol().isModuleEnabled("qol.foraging_cheats");
    }

    private static boolean toggle(QolSkyblockExtras extras, String module, String setting, boolean fallback) {
        QolUtilityConfig config = qol();
        if (!config.isModuleEnabled(module)) {
            return false;
        }
        Boolean value = config.readBoolean(setting);
        return value != null ? value : fallback;
    }

    private static int number(QolSkyblockExtras extras, String setting, int fallback) {
        Double value = qol().readNumber(setting);
        return value == null ? fallback : (int) Math.round(value);
    }

    private static QolUtilityConfig qol() {
        return RotClientClient.qolConfigPublic();
    }

    private static int cheatDelay(QolSkyblockExtras extras) {
        return Math.max(1, number(extras, "qol.foraging_cheats.click_delay", 3));
    }

    private static int seaLumiesMin(QolSkyblockExtras extras) {
        return Math.max(1, Math.min(4, number(extras, "qol.foraging_helpers.sea_lumies_min", 3)));
    }
}
