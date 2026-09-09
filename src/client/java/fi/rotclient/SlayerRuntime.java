package fi.rotclient;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.authlib.properties.Property;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minecraft bridge for the shared Slayer session engine. */
public final class SlayerRuntime {
    private static final SlayerSessionEngine ENGINE = new SlayerSessionEngine();
    private static final SlayerMechanicsPolicy.CocoonTimer COCOON_TIMER =
            new SlayerMechanicsPolicy.CocoonTimer();
    private static final SlayerMechanicsPolicy.DaggerSwapState DAGGER_SWAP =
            new SlayerMechanicsPolicy.DaggerSwapState();
    private static final SlayerMechanicsPolicy.SoulcryState SOULCRY =
            new SlayerMechanicsPolicy.SoulcryState();
    private static final SlayerMechanicsPolicy.SoulcryAbilityGate SOULCRY_ABILITY =
            new SlayerMechanicsPolicy.SoulcryAbilityGate();
    private static final SlayerMechanicsPolicy.VengeanceTimer VENGEANCE =
            new SlayerMechanicsPolicy.VengeanceTimer();
    private static final SlayerProgressPolicy.ThresholdState PROGRESS_THRESHOLD =
            new SlayerProgressPolicy.ThresholdState();
    private static SlayerMechanicsPolicy.AttunementDisplay lastAttunement;
    private static SlayerProgressPolicy.Progress latestProgress;
    private static boolean featuresWereEnabled;
    private static int scanCooldown;
    private static int daggerUseCooldown;
    private static int lastDaggerTargetEntityId = Integer.MIN_VALUE;
    /** A carry boss only activates the local fade after the player has attacked it. */
    private static int lastAttackedCarryBossEntityId = Integer.MIN_VALUE;
    private static SlayerDropScalePolicy.Window dropScaleWindow;
    private static final Map<Integer, String> SEEN_VENGEANCE_DAMAGE = new HashMap<>();
    private static final List<FightMarker> FIGHT_MARKERS = new ArrayList<>();
    private static final Set<Integer> SEEN_BEACON_STANDS = new HashSet<>();
    private static final Map<Long, Long> SITTING_BEACONS = new HashMap<>();
    private static final Map<Integer, List<Vec3>> FLYING_BEACON_PATHS = new HashMap<>();
    private static final List<Vec3> YANG_GLYPH_BEAMS = new ArrayList<>();
    private static final double MIN_MARKER_SIZE = 0.65D;
    private static boolean lastOwnedHeldBeacon;
    private static long yangGlyphThrownUntilMillis;
    private static boolean yangGlyphClaimed;
    private static Vec3 yangGlyphThrowOrigin;
    private static SlayerFightPolicy.VoidgloomPhase voidgloomPhase =
            SlayerFightPolicy.VoidgloomPhase.UNKNOWN;
    private static int voidgloomHits = -1;
    private static String voidgloomHealth = "";
    private static long laserEndsAtMillis;
    private static long lastLaserStartMillis;
    private static double sittingBeaconRemaining;
    private static int autoStartTicks = -1;
    private static String autoStartCommand = "";
    private static long lastBeaconWarningMillis;
    private static long lastBoomMillis;
    private static boolean revenantBoomActive;
    private static SlayerFightPolicy.TarantulaPhase tarantulaPhase =
            SlayerFightPolicy.TarantulaPhase.UNKNOWN;
    private static String tarantulaEggHits = "";
    private static boolean tarantulaInvincible;
    private static long lastHatchlingsMillis;
    private static Vec3 lastHatchlingsPos;
    private static long lastHowlMillis;
    private static long lastTwinclawsMillis;
    private static long lastSteakMillis;
    private static long lastManiaMillis;
    private static long twinClawsReadyAtMillis;
    private static boolean vampireTwinclawsActive;
    private static boolean vampireManiaActive;
    private static boolean vampireSteakReady;
    private static double vampireManiaRemaining;
    private static SlayerFightPolicy.InfernoPhase infernoPhase =
            SlayerFightPolicy.InfernoPhase.UNKNOWN;
    private static int firePillarSeconds = -1;
    private static int firePillarHits = -1;
    private static double infernoMaxHealth;
    private static double lastInfernoHealth;
    private static int ownedInfernoTier;
    private static long lastFirePitsMillis;
    private static long lastFirePillarWarnMillis;
    private static long lastQuestWarningMillis;
    private static long lastTimeMessageAtMillis;
    private static long lastMinibossAlertAtMillis;
    private static SlayerSessionEngine.Snapshot overlaySnapshot;
    private static SlayerFightPolicy.QuestRef latestQuest;
    private static List<String> ownedNametagLines = List.of();

    private static long fadeSnapshotTick = Long.MIN_VALUE;
    private static SlayerSessionEngine.Snapshot fadeSnapshot;
    private static long scanOwnedAt;
    private static Vec3[] scanOwnedPositions;
    private static List<Vec3> scanForeignVoidgloomPositions;
    private static int scanOwnedVoidgloomEntityId = Integer.MIN_VALUE;

    private record FightMarker(
            AABB box,
            Vec3 target,
            int color,
            boolean line,
            String label,
            int lineColor,
            float lineWidth,
            int entityId,
            double inflate) {
    }
    private static final Pattern MAGIC_FIND = Pattern.compile(
            "(?i).*(?:RARE DROP!|VERY RARE DROP!|CRAZY RARE DROP!|INSANE DROP!).*\\(\\+(?<mf>\\d+)% .*Magic Find\\).*");
    private static SlayerRngCatalog.Entry selectedRngDrop;
    private static long lastRngStoredXp = -1L;
    /** Null until a verified rare-drop line supplied the Magic Find value. */
    private static Integer lastMagicFind;
    private static long lastRngEmptyWarningMillis;
    private static String pendingTradePlayer = "";
    private static final Map<String, Double> CARRY_PAYMENTS_M = new HashMap<>();
    private static final List<DeathMark> RECENT_DEATHS = new ArrayList<>();
    private static final List<Vec3> ICHOR_BEAMS = new ArrayList<>();
    private static long gummyExpiresAtMillis;
    private static long lastGummyWarnMillis;
    private static boolean gummyNeeded;

    private record DeathMark(double x, double y, double z, long atMillis) {
    }

    private SlayerRuntime() {
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras settings = settings();
        boolean enabled = anyFeatureEnabled(settings);
        if (!enabled || client == null || client.level == null || client.player == null) {
            if (featuresWereEnabled) {
                ENGINE.resetWorld();
                COCOON_TIMER.reset();
                resetDaggerSwap();
                resetAdditionalMechanics();
            }
            resetProgress();
            featuresWereEnabled = enabled;
            scanCooldown = 0;
            return;
        }
        featuresWereEnabled = true;
        syncSidebarQuest(settings);
        overlaySnapshot = ENGINE.viewSnapshot();
        if (!settings.slayerCocoonAlertEnabled) {
            COCOON_TIMER.reset();
        }
        if (!settings.slayerProgressEnabled) {
            resetProgress();
        }
        if (QolFlavorSupport.isPlus() && settings.slayerDaggerSwapEnabled) {
            tickDaggerSwap(client);
        } else {
            resetDaggerSwap();
        }
        tickSoulcry(client, settings);
        tickAutoStart(client, settings);
        tickVoidgloomLaser(client, settings);
        tickGummyWarning(client, settings);
        if (settings.slayerVengeanceEnabled) {
            VENGEANCE.tick();
        } else {
            VENGEANCE.reset();
        }
        if (scanCooldown-- > 0) {
            reapRemoved(client.level);
            return;
        }
        scanCooldown = 2;
        scanEntities(client, settings);
        reapRemoved(client.level);
    }

    static void onChat(Component message) {
        ClientBoundaryGuard.run("SLAYER_CHAT", () -> observeChat(message));
    }

    private static void observeChat(Component message) {
        if (message == null || !anyFeatureEnabled(settings())) {
            return;
        }
        QolSkyblockExtras settings = settings();
        long now = System.currentTimeMillis();
        if (settings.slayerCocoonAlertEnabled
                && COCOON_TIMER.observe(message.getString(), now)) {
            alertCocoon(Minecraft.getInstance(), settings);
        }
        String line = message.getString();
        if (SlayerMinibossAlertPolicy.shouldAnnounce(
                settings.slayerMinibossAlertEnabled,
                SlayerMinibossAlertPolicy.isSpawnChat(line),
                now,
                lastMinibossAlertAtMillis)) {
            lastMinibossAlertAtMillis = now;
            alertMiniboss(
                    Minecraft.getInstance(),
                    settings,
                    SlayerMinibossAlertPolicy.spawnFromChat(line).orElse(null));
        }
        SlayerMechanicsPolicy.abilityCooldownTicks(line).ifPresent(SOULCRY_ABILITY::observeRemaining);
        if (settings.slayerInfernoEnabled
                && settings.slayerInfernoGummyWarning
                && SlayerPolishPolicy.isGummyConsumeChat(line)) {
            gummyExpiresAtMillis = now + SlayerPolishPolicy.GUMMY_DURATION_MILLIS;
        }
        SlayerPolicy.QuestSignal questSignal = ENGINE.onChat(line, now, false);
        if (questSignal != SlayerPolicy.QuestSignal.NONE) {
            resetProgress();
        }
        if (questSignal == SlayerPolicy.QuestSignal.COMPLETED) {
            if (settings.slayerTimeMessagesEnabled
                    && settings.slayerTimeMessagesQuestComplete) {
                long duration = ENGINE.snapshot(now).lastKillDurationMillis();
                if (duration > 0L && Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(RotClientChat.message(
                            settings.slayerTimeMessagesCompact
                                    ? "Quest complete · " + duration(duration)
                                    : "Slayer quest complete in " + duration(duration) + "."));
                }
            }
            scheduleAutoStart(settings);
        }
        if (settings.slayerTarantulaEnabled
                && localQuestFamily(SlayerPolicy.SlayerType.TARANTULA)
                && SlayerFightPolicy.isHatchlingsChat(line)) {
            lastHatchlingsMillis = now;
            tarantulaInvincible = true;
            if (settings.slayerTarantulaInvincibleText) {
                showFightTitle(Minecraft.getInstance(), "Kill hatchlings!");
            }
        }
        if (settings.slayerDropsEnabled) {
            SlayerPolicy.dropObservation(line).ifPresent(drop ->
                    SlayerRngCatalog.resolve(drop.displayName()).ifPresent(entry -> {
                        boolean selected = SlayerDropScalePolicy.selected(
                                settings.slayerDropFilter, entry.skyBlockId());
                        ENGINE.observeDrop(entry.display(), selected, now);
                        if (selected) {
                            announcePricedDrop(entry, settings);
                        }
                    }));
            observeRng(line, settings);
        }
        if (settings.slayerCarryEnabled) {
            observeTrade(line, settings);
        }
    }

    /**
     * Called by the pre-display message gate.  It intentionally recognizes
     * only the exact, formatting-stripped Stored XP status line; all other
     * game chat remains untouched.
     */
    static boolean shouldHideRngMeterChat(Component message) {
        return ClientBoundaryGuard.call("SLAYER_RNG_CHAT_HIDE", () -> {
            QolSkyblockExtras settings = settings();
            if (message == null || !settings.slayerDropsEnabled) {
                return false;
            }
            String line = message.getString().replaceAll("§.", "").trim();
            if (line.startsWith("[Rot Client]")) {
                return false;
            }
            activateStoredRngDrop(currentRngFamily());
            return SlayerRngMeterPolicy.shouldHideChat(
                    settings.slayerDropsRngHideChat,
                    SlayerRngMeterPolicy.chatStoredXp(line).isPresent(),
                    selectedRngDrop != null && selectedRngDrop.requiredXp() > 0L);
        }, false);
    }

    static boolean shouldHideInfernoChat(Component message) {
        return ClientBoundaryGuard.call("SLAYER_INFERNO_CHAT_HIDE", () -> {
            QolSkyblockExtras settings = settings();
            if (message == null || !settings.slayerInfernoEnabled || !settings.slayerInfernoHideChat) {
                return false;
            }
            return SlayerFightPolicy.isWrongAttunementChat(message.getString());
        }, false);
    }

    static void onEntityEvent(Entity entity, byte eventId) {
        if (entity == null || eventId != 3 || !anyFeatureEnabled(settings())) {
            return;
        }
        QolSkyblockExtras settings = settings();
        if (settings.slayerHighlightsEnabled && settings.slayerHighlightsHideSpawnParticles) {
            RECENT_DEATHS.add(new DeathMark(
                    entity.getX(), entity.getY(), entity.getZ(), System.currentTimeMillis()));
        }
        handleDeath(
                ENGINE.onEntityDeath(entity.getId(), System.currentTimeMillis()),
                entity.getX(), entity.getY(), entity.getZ());
    }

    static void onWorldChanged() {
        ENGINE.resetWorld();
        COCOON_TIMER.reset();
        resetDaggerSwap();
        resetAdditionalMechanics();
        scanCooldown = 0;
        pendingTradePlayer = "";
        CARRY_PAYMENTS_M.clear();
        lastMagicFind = null;
        lastRngEmptyWarningMillis = 0L;
        lastMinibossAlertAtMillis = 0L;
        fadeSnapshot = null;
        fadeSnapshotTick = Long.MIN_VALUE;
        scanOwnedPositions = null;
        scanForeignVoidgloomPositions = null;
        scanOwnedVoidgloomEntityId = Integer.MIN_VALUE;
        lastAttackedCarryBossEntityId = Integer.MIN_VALUE;
        latestQuest = null;
        ownedNametagLines = List.of();
        overlaySnapshot = null;
        lastTimeMessageAtMillis = 0L;
        resetProgress();
    }

    /**
     * Receives the unfiltered central action-bar stream. It deliberately does
     * not depend on the optional action-bar filtering mixin.
     */
    static void onActionBar(Component message) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerProgressEnabled) {
            resetProgress();
            return;
        }
        if (message == null) {
            return;
        }
        boolean questVisible = ENGINE.questState() == SlayerSessionEngine.QuestState.ACTIVE
                || latestQuest != null;
        SlayerProgressPolicy.parse(message.getString(), questVisible).ifPresent(progress ->
                applyProgress(settings, progress, questVisible));
    }

    static void onAttack(Entity entity) {
        QolSkyblockExtras settings = settings();
        Minecraft client = Minecraft.getInstance();
        rememberAttackedCarryBoss(client, entity, settings);
        warnWrongQuest(client, entity, settings);
        if (QolFlavorSupport.isPlus()
                && settings.slayerAutoSoulcryEnabled
                && settings.slayerAutoSoulcryAttackBased) {
            tryAttackSoulcry(client, entity, settings);
        }
        if (!QolFlavorSupport.isPlus() || !settings.slayerDaggerSwapEnabled || entity == null
                || client == null || client.level == null) {
            return;
        }
        // The dagger swap reads attunement holograms on whatever was
        // attacked. Requiring DEMON classification dropped swaps when the
        // hologram box missed Quazii/Typhoeus tags.
        String attunementLine = attachedAttunementLine(client.level, entity);
        if (attunementLine == null) {
            return;
        }
        if (lastDaggerTargetEntityId != entity.getId()) {
            DAGGER_SWAP.reset();
            lastDaggerTargetEntityId = entity.getId();
        }
        int variance = SlayerMechanicsPolicy.clampVariance(
                settings.slayerDaggerSwapVariance);
        int sampled = variance <= 0
                ? 0
                : ThreadLocalRandom.current().nextInt(variance + 1);
        DAGGER_SWAP.observe(
                attunementLine,
                settings.slayerDaggerSwapDelay,
                sampled);
    }

    static SlayerSessionEngine.Snapshot snapshot() {
        return ENGINE.snapshot(System.currentTimeMillis());
    }

    /**
     * Local render-only decision used by the LivingEntity renderer mixin.
     * No entity state, packets, hitboxes, or server data are modified.
     */
    public static boolean shouldFadeForActiveBoss(Entity target) {
        QolSkyblockExtras settings = settings();
        if (target == null || !settings.slayerActiveBossTransparencyEnabled) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null
                || !(target instanceof LivingEntity)) {
            return false;
        }
        SlayerSessionEngine.Snapshot snapshot = fadeSnapshot(client);
        boolean targetIsTrackedBoss = snapshot.activeBosses().stream()
                .anyMatch(active -> active.entityId() == target.getId());
        boolean verifiedFightActive = hasOwnedBoss(snapshot) || hasAttackedCarryBoss(snapshot);
        boolean targetIsLocalPlayer = target.getId() == client.player.getId();
        boolean targetIsOtherPlayer = target instanceof net.minecraft.world.entity.player.Player;
        return SlayerTransparencyPolicy.shouldFade(
                new SlayerTransparencyPolicy.Options(
                        true,
                        settings.slayerActiveBossTransparencyStrength,
                        settings.slayerActiveBossTransparencyPlayers),
                verifiedFightActive,
                targetIsTrackedBoss,
                targetIsLocalPlayer,
                targetIsOtherPlayer);
    }

    /**
     * Supplies an optional local model tint for the shared Slayer fade mixin.
     * Active-boss transparency takes priority over irrelevant-mob fading.
     */
    public static Integer slayerFadeTintFor(Entity target) {
        if (shouldFadeForActiveBoss(target)) {
            return activeBossFadeTint();
        }
        return shouldFadeIrrelevantMob(target) ? irrelevantMobFadeTint() : null;
    }

    /** Opacity multiplier used only after {@link #shouldFadeForActiveBoss(Entity)} returned true. */
    public static int activeBossFadeTint() {
        int opacity = SlayerTransparencyPolicy.opacityPercent(new SlayerTransparencyPolicy.Options(
                true,
                settings().slayerActiveBossTransparencyStrength,
                settings().slayerActiveBossTransparencyPlayers));
        return tintForOpacity(opacity);
    }

    private static boolean shouldFadeIrrelevantMob(Entity target) {
        QolSkyblockExtras settings = settings();
        Minecraft client = Minecraft.getInstance();
        if (!settings.slayerIrrelevantMobsEnabled || target == null
                || client == null || client.player == null || !(target instanceof LivingEntity)) {
            return false;
        }
        SlayerSessionEngine.Snapshot snapshot = fadeSnapshot(client);
        SlayerPolicy.SlayerType activeType = activeFadeType(snapshot);
        boolean tracked = snapshot.activeBosses().stream()
                .anyMatch(active -> active.entityId() == target.getId());
        return SlayerIrrelevantMobsPolicy.shouldFade(
                new SlayerIrrelevantMobsPolicy.Options(
                        true, settings.slayerIrrelevantMobsStrength),
                activeType,
                tracked,
                target instanceof net.minecraft.world.entity.player.Player,
                mobKind(target));
    }

    private static int irrelevantMobFadeTint() {
        int opacity = SlayerIrrelevantMobsPolicy.opacityPercent(
                new SlayerIrrelevantMobsPolicy.Options(
                        true, settings().slayerIrrelevantMobsStrength));
        return tintForOpacity(opacity);
    }

    private static int tintForOpacity(int opacityPercent) {
        int alpha = (int) Math.round(255.0D * opacityPercent / 100.0D);
        return (alpha << 24) | 0x00FFFFFF;
    }

    static SlayerSessionEngine engine() {
        return ENGINE;
    }

    static boolean completeCarry(String player) {
        if (!ENGINE.completeCarry(player, System.currentTimeMillis())) return false;
        SlayerSessionEngine.Carry carry = ENGINE.carries().stream()
                .filter(c -> c.player().equalsIgnoreCase(player)).findFirst().orElse(null);
        if (carry != null) recordCarryProgress(carry, settings(), Minecraft.getInstance());
        return true;
    }

    static List<String> displayLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerDisplayEnabled) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        SlayerSessionEngine.Snapshot snapshot = ENGINE.snapshot(now);
        SlayerSessionEngine.ActiveBoss boss = primaryOwnedBoss(snapshot);
        List<String> lines = new ArrayList<>();
        lines.add("SLAYER");
        List<String> nametags = SlayerPolicy.nametagHudLines(ownedNametagLines);
        if (boss == null) {
            if (!nametags.isEmpty()) {
                lines.addAll(nametags);
            } else {
                if (latestQuest != null) {
                    lines.add(latestQuest.type().displayName()
                            + SlayerFightPolicy.romanLabel(latestQuest.tier()));
                }
                lines.add(snapshot.questState() == SlayerSessionEngine.QuestState.ACTIVE
                        || latestQuest != null
                        ? "Quest active"
                        : "No active Slayer boss");
            }
            if (settings.slayerDisplayKillTime && snapshot.lastKillDurationMillis() > 0L) {
                lines.add("Last kill " + duration(snapshot.lastKillDurationMillis()));
            }
            return lines;
        }
        SlayerPolicy.EntityDescriptor descriptor = boss.descriptor();
        if (!nametags.isEmpty()) {
            lines.addAll(nametags);
        } else {
            int tier = descriptor.tier();
            if (tier <= 0 && latestQuest != null && latestQuest.type() == descriptor.type()) {
                tier = latestQuest.tier();
            }
            lines.add(descriptor.displayName() + tierSuffix(tier));
            if (!descriptor.owner().isBlank()) {
                lines.add((boss.owned() ? "Your boss · " : "Owner: ") + descriptor.owner());
            }
            lines.add("Time " + duration(now - boss.spawnedAtMillis()));
        }
        if (descriptor.attunement() != SlayerPolicy.Attunement.UNKNOWN) {
            lines.add("Attunement " + descriptor.attunement().name());
        }
        if (settings.slayerVoidgloomEnabled
                && descriptor.type() == SlayerPolicy.SlayerType.VOIDGLOOM) {
            if (settings.slayerVoidgloomPhaseDisplay || settings.slayerVoidgloomHitsDisplay) {
                String phase = settings.slayerVoidgloomHitsDisplay
                        ? SlayerFightPolicy.phaseLabel(voidgloomPhase, voidgloomHits)
                        : SlayerFightPolicy.phaseLabel(voidgloomPhase);
                if (!phase.isBlank()) {
                    lines.add(phase);
                }
            }
            if (settings.slayerVoidgloomLaserTimer && laserEndsAtMillis > now) {
                lines.add("Laser " + SlayerFightPolicy.countdownLabel(
                        (laserEndsAtMillis - now) / 1000.0D));
            }
            if (settings.slayerVoidgloomLaserHealth
                    && laserEndsAtMillis > now
                    && !voidgloomHealth.isBlank()) {
                lines.add("HP " + voidgloomHealth);
            }
            if (settings.slayerVoidgloomBeaconTimer && sittingBeaconRemaining > 0.0D) {
                lines.add("Yang Glyph " + SlayerFightPolicy.countdownLabel(sittingBeaconRemaining));
            }
        }
        if (settings.slayerRevenantEnabled
                && settings.slayerRevenantBoomDisplay
                && descriptor.type() == SlayerPolicy.SlayerType.REVENANT
                && revenantBoomActive) {
            lines.add("BOOM");
        }
        if (settings.slayerTarantulaEnabled
                && descriptor.type() == SlayerPolicy.SlayerType.TARANTULA) {
            if (settings.slayerTarantulaPhaseDisplay) {
                String phase = SlayerFightPolicy.tarantulaPhaseLabel(tarantulaPhase);
                if (!phase.isBlank()) {
                    lines.add(phase);
                }
            }
            if (settings.slayerTarantulaEggHits && !tarantulaEggHits.isBlank()) {
                lines.add("Egg " + tarantulaEggHits);
            }
            if (settings.slayerTarantulaInvincibleText && tarantulaInvincible) {
                lines.add("Kill hatchlings!");
            }
        }
        if (settings.slayerVampireMarkersEnabled
                && descriptor.type() == SlayerPolicy.SlayerType.VAMPIRE) {
            if (settings.slayerVampireMarkersTwinclaws && vampireTwinclawsActive) {
                lines.add("Twinclaws");
            }
            if (settings.slayerVampireMarkersMania && vampireManiaActive) {
                if (settings.slayerVampireMarkersManiaTimer && vampireManiaRemaining > 0.0D) {
                    lines.add("Mania " + SlayerFightPolicy.countdownLabel(vampireManiaRemaining));
                } else {
                    lines.add("Mania");
                }
            }
            if (settings.slayerVampireMarkersSteakAlert && vampireSteakReady) {
                lines.add("Steak!");
            }
        }
        if (settings.slayerInfernoEnabled
                && descriptor.type() == SlayerPolicy.SlayerType.INFERNO) {
            if (settings.slayerInfernoPhaseDisplay) {
                String phase = SlayerFightPolicy.infernoPhaseLabel(infernoPhase, descriptor.tier());
                if (!phase.isBlank()) {
                    lines.add(phase);
                }
            }
            if (settings.slayerInfernoFirePillar && firePillarSeconds >= 0) {
                String pillar = "Fire Pillar " + firePillarSeconds + "s";
                if (firePillarHits >= 0) {
                    pillar += " " + firePillarHits + " hits";
                }
                lines.add(pillar);
            }
            if (settings.slayerInfernoGummyWarning && gummyNeeded) {
                lines.add("No Polar Bear!");
            }
        }
        return lines;
    }

    static List<String> statsLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerStatsEnabled) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        SlayerSessionEngine.Snapshot snapshot = ENGINE.snapshot(now);
        boolean sessionStarted = snapshot.sessionStartedAtMillis() > 0L;
        List<String> lines = new ArrayList<>();
        lines.add("SLAYER STATS");
        if (settings.slayerStatsBossesKilled) {
            lines.add("Bosses " + snapshot.bossesKilled());
        }
        if (settings.slayerStatsBossesPerHour && sessionStarted) {
            lines.add(String.format(Locale.ROOT, "Bosses/h %.1f", snapshot.bossesPerHour(now)));
        }
        if (settings.slayerStatsAverageKillTime) {
            lines.add("Average " + duration(snapshot.averageKillDurationMillis()));
        }
        if (settings.slayerStatsSessionTime) {
            lines.add(sessionStarted
                    ? "Session " + duration(now - snapshot.sessionStartedAtMillis())
                    : "Session waiting for Slayer activity");
        }
        if (settings.slayerDropsEnabled && settings.slayerDropsBossesSince) {
            lines.add("Since rare drop " + snapshot.bossesSinceLastDrop());
        }
        return lines;
    }

    /** Does not consult the session engine: opening the HUD editor is read-only. */
    static List<String> progressLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerProgressEnabled) {
            return List.of();
        }
        SlayerProgressPolicy.Progress progress = latestProgress;
        if (progress == null) {
            boolean questActive = ENGINE.questState() == SlayerSessionEngine.QuestState.ACTIVE
                    || latestQuest != null;
            return editorOpen || questActive
                    ? List.of("SLAYER PROGRESS", "Waiting for quest XP")
                    : List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("SLAYER PROGRESS");
        lines.add(String.format(
                Locale.ROOT,
                "Combat XP %,d / %,d (%d%%)",
                progress.earnedXp(),
                progress.requiredXp(),
                progress.percent()));
        if (settings.slayerProgressShowRemaining) {
            lines.add(String.format(Locale.ROOT, "Remaining %,d XP", progress.remainingXp()));
        }
        return lines;
    }

    static List<String> rngLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerDropsEnabled || !settings.slayerDropsRngHud) return List.of();
        activateStoredRngDrop(currentRngFamily());
        if (selectedRngDrop == null) {
            return editorOpen ? List.of("RNG METER", "Waiting for selected drop") : List.of();
        }
        String progress = lastRngStoredXp < 0L
                ? "Stored XP pending"
                : String.format(Locale.ROOT, "%,d / %,d XP", lastRngStoredXp, selectedRngDrop.requiredXp());
        if (settings.slayerDropsShowChance && lastRngStoredXp >= 0L && selectedRngDrop.requiredXp() > 0L) {
            if (lastMagicFind == null) {
                progress += " · Magic Find pending";
            } else {
                double chance = SlayerCarryPolicy.rngChancePercent(lastRngStoredXp,
                        selectedRngDrop.requiredXp(), selectedRngDrop.baseChancePercent(), lastMagicFind);
                progress += String.format(Locale.ROOT, " · %.5f%%", chance);
            }
        }
        return List.of("RNG METER", selectedRngDrop.display(), progress);
    }

    static List<String> profitLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        Minecraft client = Minecraft.getInstance();
        boolean inventoryOpen = client != null && client.gui.screen() instanceof AbstractContainerScreen<?>;
        if (!SlayerProfitHudPolicy.shouldRender(
                settings.slayerDropsEnabled,
                settings.slayerDropsProfitHud,
                settings.slayerDropsProfitHideOutsideInventory,
                editorOpen,
                inventoryOpen)) {
            return List.of();
        }
        SlayerSessionEngine.Snapshot snapshot = ENGINE.viewSnapshot();
        SlayerItemProfitPolicy.Projection projection = SlayerItemProfitPolicy.project(
                snapshot.dropCounts(), slayerUnitPrices());
        if (projection.rows().isEmpty() && !editorOpen) return List.of();
        if (projection.rows().isEmpty()) {
            return List.of("SLAYER PROFIT", "No rare drops yet", "Bazaar instant sell · live quotes only");
        }
        String latestLine = snapshot.lastDropName() == null || snapshot.lastDropName().isBlank()
                ? "Latest · none"
                : "Latest · " + snapshot.lastDropName();
        List<String> lines = new ArrayList<>();
        lines.add("SLAYER PROFIT");
        lines.add(String.format(Locale.ROOT, "%,.2f coins", projection.totalValue()));
        if (settings.slayerDropsProfitPerHour) {
            long elapsedMillis = Math.max(1L, System.currentTimeMillis() - snapshot.sessionStartedAtMillis());
            double hourlyValue = projection.totalValue().doubleValue() * 3_600_000.0D / elapsedMillis;
            lines.add(String.format(Locale.ROOT, "%,.2f / hour", hourlyValue));
        }
        if (settings.slayerDropsProfitTable) {
            projection.rows().stream()
                    .limit(settings.slayerDropsProfitItemsShown)
                    .forEach(row -> lines.add(profitRow(row)));
        } else {
            lines.add(profitRow(projection.rows().getFirst()));
        }
        lines.add(latestLine);
        lines.add("Priced " + projection.pricedItems() + " · Unpriced " + projection.unpricedItems());
        lines.add("Bazaar instant sell · live quotes only");
        return List.copyOf(lines);
    }

    private static String profitRow(SlayerItemProfitPolicy.Row row) {
        return row.priced()
                ? row.displayName() + " ×" + row.quantity() + " · "
                + String.format(Locale.ROOT, "%,.2f", row.totalValue())
                : row.displayName() + " ×" + row.quantity() + " · unpriced";
    }

    private static Map<String, BigDecimal> slayerUnitPrices() {
        return ClientBoundaryGuard.call("SLAYER_UNIT_PRICES", () -> {
            MiningSessionBazaarPriceCache.CacheSnapshot cache = MiningSessionBazaarPriceCache.capture();
            if (!cache.isAvailable()) return Map.of();
            Map<String, BigDecimal> prices = new HashMap<>();
            for (SlayerRngCatalog.Entry entry : SlayerRngCatalog.entries()) {
                BazaarPriceService.ProductPrice quote = cache.marketPrices().forProduct(entry.skyBlockId());
                if (quote == null) continue;
                double instantSell = quote.instantSellPrice();
                if (instantSell > 0D && Double.isFinite(instantSell)) {
                    prices.put(entry.skyBlockId(), BigDecimal.valueOf(instantSell));
                }
            }
            return prices;
        }, Map.of());
    }

    private static void announcePricedDrop(
            SlayerRngCatalog.Entry entry,
            QolSkyblockExtras settings) {
        if (entry == null || (!settings.slayerDropsPriceInChat && !settings.slayerDropsPriceTitle)) return;
        BigDecimal price = slayerUnitPrices().get(entry.skyBlockId());
        Minecraft client = Minecraft.getInstance();
        if (price == null || price.signum() <= 0 || client.player == null) return;
        if (settings.slayerDropsPriceTitle
                && price.compareTo(BigDecimal.valueOf(settings.slayerDropsPriceTitleMinimum)) >= 0) {
            client.gui.hud.setTimes(10, 40, 10);
            client.gui.hud.setTitle(Component.literal("§bRare Drop! §f" + entry.display()));
            if (settings.slayerDropsPriceTitleSound) {
                client.player.level().playLocalSound(
                        client.player.getX(), client.player.getY(), client.player.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8F, 1.35F, false);
            }
        }
        if (!settings.slayerDropsPriceInChat) return;
        client.player.sendSystemMessage(RotClientChat.message(String.format(
                Locale.ROOT,
                "Rare drop · %s · Bazaar instant sell %,.2f coins",
                entry.display(), price)));
    }

    static List<String> carryLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerCarryEnabled || !settings.slayerCarryDisplay) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("SLAYER CARRIES");
        for (SlayerSessionEngine.Carry carry : ENGINE.carries()) {
            lines.add(carry.player() + " · " + carry.type().displayName()
                    + tierSuffix(carry.tier()) + " · " + carry.completed() + "/" + carry.total());
        }
        if (lines.size() == 1) {
            lines.add("No active carries");
        }
        return lines;
    }

    static List<String> cocoonLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerCocoonAlertEnabled || !settings.slayerCocoonTimer) {
            return List.of();
        }
        long remaining = COCOON_TIMER.remainingMillis(System.currentTimeMillis());
        if (remaining <= 0L && !editorOpen) {
            return List.of();
        }
        if (remaining <= 0L) {
            remaining = SlayerMechanicsPolicy.COCOON_DURATION_MILLIS;
        }
        return List.of(
                "COCOON TIMER",
                String.format(Locale.ROOT, "Boss releases in %.1fs", remaining / 1_000.0D));
    }

    static List<String> attunementLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerAttunementDisplayEnabled) {
            return List.of();
        }
        SlayerMechanicsPolicy.AttunementDisplay display = lastAttunement;
        if (display == null && !editorOpen) {
            return List.of();
        }
        if (display == null) {
            display = SlayerMechanicsPolicy.attunementDisplay(
                    "AURIC ♨5 00:12", settings.slayerAttunementDisplayCount).orElse(null);
        }
        return display == null ? List.of() : List.of("ATTUNEMENT", display.formatted());
    }

    static List<String> vengeanceLines(boolean editorOpen) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerVengeanceEnabled || (!VENGEANCE.active() && !editorOpen)) {
            return List.of();
        }
        String value = VENGEANCE.active()
                ? VENGEANCE.display(settings.slayerVengeanceUseTicks)
                : (settings.slayerVengeanceUseTicks ? "120" : "6.0s");
        return settings.slayerVengeanceCompact
                ? List.of(value)
                : List.of("VENGEANCE", value);
    }

    public static boolean shouldHideLaser(Entity entity) {
        QolSkyblockExtras settings = settings();
        Minecraft client = Minecraft.getInstance();
        if (!settings.slayerLaserHiderEnabled || !(entity instanceof Guardian)
                || client == null || client.level == null) {
            return false;
        }
        SlayerSessionEngine.Snapshot snapshot = ENGINE.snapshot(System.currentTimeMillis());
        List<SlayerMechanicsPolicy.LaserAnchor> anchors = new ArrayList<>();
        for (SlayerSessionEngine.ActiveBoss active : snapshot.activeBosses()) {
            if (active.descriptor().type() != SlayerPolicy.SlayerType.VOIDGLOOM
                    || active.descriptor().role() != SlayerPolicy.EntityRole.BOSS) {
                continue;
            }
            Entity boss = client.level.getEntity(active.entityId());
            if (!(boss instanceof EnderMan)) {
                continue;
            }
            boolean carry = snapshot.carries().stream().anyMatch(candidate ->
                    !candidate.complete()
                            && candidate.type() == SlayerPolicy.SlayerType.VOIDGLOOM
                            && candidate.player().equalsIgnoreCase(active.descriptor().owner())
                            && (candidate.tier() == 0
                            || candidate.tier() == active.descriptor().tier()));
            anchors.add(new SlayerMechanicsPolicy.LaserAnchor(
                    boss.getX(), boss.getY(), boss.getZ(), active.owned(), carry));
        }
        return SlayerMechanicsPolicy.shouldHideLaser(
                entity.getX(), entity.getY(), entity.getZ(), anchors,
                settings.slayerLaserShowForCarries);
    }

    public static boolean shouldSuppressSound(String soundId) {
        QolSkyblockExtras settings = settings();
        onSound(soundId, settings);
        if (settings.slayerSvenEnabled
                && settings.slayerSvenMuteSounds
                && SlayerFightPolicy.isWolfSound(soundId)
                && inSvenSoundArea()) {
            return true;
        }
        if (settings.slayerTarantulaEnabled
                && settings.slayerTarantulaMuteSounds
                && SlayerFightPolicy.isSpiderSound(soundId)
                && inTarantulaSoundArea()) {
            return true;
        }
        if (settings.slayerVampireMarkersEnabled
                && settings.slayerVampireMarkersMuteSounds
                && (SlayerFightPolicy.isVampireNoise(soundId)
                || SlayerFightPolicy.isKillerSpringSound(soundId))
                && inVampireSoundArea()) {
            return true;
        }
        if (!settings.slayerSoundsEnabled) {
            return false;
        }
        return (settings.slayerSoundsDisableVoidgloom
                && SlayerMechanicsPolicy.isVoidgloomNoise(soundId))
                || (settings.slayerSoundsDisableVampire
                && SlayerFightPolicy.isVampireNoise(soundId));
    }

    public static boolean shouldHideSvenPupHologram(Entity entity) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerSvenEnabled || !settings.slayerSvenHidePupNametags
                || !(entity instanceof ArmorStand)
                || !localQuestFamily(SlayerPolicy.SlayerType.SVEN)) {
            return false;
        }
        return SlayerFightPolicy.isPupName(name(entity));
    }

    private static boolean inSvenSoundArea() {
        long now = System.currentTimeMillis();
        if (hasOwnedBossOfType(SlayerPolicy.SlayerType.SVEN, now)) {
            return true;
        }
        return SlayerFightPolicy.isSvenSoundArea(SkyBlockSidebar.text());
    }

    private static boolean inTarantulaSoundArea() {
        long now = System.currentTimeMillis();
        boolean infernoActive = hasOwnedBossOfType(SlayerPolicy.SlayerType.INFERNO, now)
                || SlayerFightPolicy.isInfernoFightArea(SkyBlockSidebar.text());
        if (infernoActive) {
            return false;
        }
        if (hasOwnedBossOfType(SlayerPolicy.SlayerType.TARANTULA, now)) {
            return true;
        }
        return SlayerFightPolicy.isTarantulaSoundArea(SkyBlockSidebar.text());
    }

    private static boolean inVampireSoundArea() {
        long now = System.currentTimeMillis();
        if (hasOwnedBossOfType(SlayerPolicy.SlayerType.VAMPIRE, now)) {
            return true;
        }
        return SlayerFightPolicy.isVampireSoundArea(SkyBlockSidebar.text());
    }

    public static boolean shouldHideVoidgloomParticles() {
        QolSkyblockExtras settings = settings();
        return settings.slayerVoidgloomEnabled && settings.slayerVoidgloomHideParticles;
    }

    public static boolean shouldHideVoidgloomParticleAt(double x, double y, double z) {
        if (!shouldHideVoidgloomParticles()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return false;
        }
        Vec3 owned = ownedBossPosition(
                client.level, SlayerPolicy.SlayerType.VOIDGLOOM, System.currentTimeMillis());
        if (owned == null) {
            return false;
        }
        return owned.distanceTo(new Vec3(x, y, z)) <= 6.0D;
    }

    public static boolean shouldHideSpawnNametag(Entity entity) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerHighlightsEnabled
                || !settings.slayerHighlightsHideMobNames
                || !(entity instanceof ArmorStand)
                || ENGINE.questState() != SlayerSessionEngine.QuestState.ACTIVE) {
            return false;
        }
        return SlayerPolishPolicy.shouldHideSpawnMobName(name(entity));
    }

    public static boolean shouldHideDamageSplash(Entity entity) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerHighlightsEnabled
                || !settings.slayerHighlightsHideDamageSplash
                || !(entity instanceof ArmorStand)
                || !SlayerPolishPolicy.isDamageSplash(name(entity))) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return false;
        }
        SlayerSessionEngine.Snapshot snapshot = overlaySnapshot != null
                ? overlaySnapshot
                : ENGINE.viewSnapshot();
        for (SlayerSessionEngine.ActiveBoss active : snapshot.activeBosses()) {
            if (!active.owned()) {
                continue;
            }
            Entity boss = client.level.getEntity(active.entityId());
            if (boss != null && boss.distanceTo(entity) <= SlayerPolishPolicy.DAMAGE_SPLASH_RANGE) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldHideSpawnParticleAt(String particleId, double x, double y, double z) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerHighlightsEnabled
                || !settings.slayerHighlightsHideSpawnParticles
                || ENGINE.questState() != SlayerSessionEngine.QuestState.ACTIVE
                || !SlayerPolishPolicy.isSpawnParticle(particleId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        RECENT_DEATHS.removeIf(death -> now - death.atMillis() > SlayerPolishPolicy.SPAWN_PARTICLE_WINDOW_MILLIS);
        for (DeathMark death : RECENT_DEATHS) {
            if (SlayerPolishPolicy.nearRecentDeath(
                    x, y, z, death.x(), death.y(), death.z(), death.atMillis(), now)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldBlockMaddoxClick(AbstractContainerScreen<?> screen, ItemStack stack) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerAutoStartEnabled
                || !settings.slayerAutoStartBlockNotSpawnable
                || screen == null
                || stack == null
                || stack.isEmpty()) {
            return false;
        }
        return SlayerPolishPolicy.shouldBlockMaddoxClick(
                screen.getTitle().getString(),
                InventoryChromeRuntime.loreLines(stack));
    }

    public static boolean shouldHideInfernoParticles() {
        QolSkyblockExtras settings = settings();
        return settings.slayerInfernoEnabled && settings.slayerInfernoHideParticles;
    }

    public static boolean shouldHideInfernoParticleAt(double x, double y, double z) {
        if (!shouldHideInfernoParticles()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return false;
        }
        Vec3 owned = ownedBossPosition(
                client.level, SlayerPolicy.SlayerType.INFERNO, System.currentTimeMillis());
        if (owned == null) {
            return false;
        }
        if (owned.distanceTo(new Vec3(x, y, z)) > 8.0D) {
            return false;
        }
        AABB area = new AABB(x - 3.0D, y - 3.0D, z - 3.0D, x + 3.0D, y + 3.0D, z + 3.0D);
        for (LivingEntity living : client.level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (living == null || !living.isAlive()) {
                continue;
            }
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            if (typeId == null) {
                continue;
            }
            String path = typeId.getPath();
            if ("blaze".equals(path) || "magma_cube".equals(path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldHideInfernoFireball(Entity entity) {
        if (!shouldHideInfernoParticles() || entity == null) {
            return false;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null || !typeId.getPath().contains("fireball")) {
            return false;
        }
        return shouldHideInfernoParticleAt(entity.getX(), entity.getY(), entity.getZ());
    }

    public static float itemDropScale(ItemEntity entity) {
        QolSkyblockExtras settings = settings();
        if (!settings.slayerBigDropsEnabled || entity == null || dropScaleWindow == null
                || !familyEnabled(settings, dropScaleWindow.type())) {
            return 1.0F;
        }
        String id = AutoClickerItemIdentity.skyBlockId(entity.getItem());
        return SlayerDropScalePolicy.shouldScale(
                dropScaleWindow,
                id,
                System.currentTimeMillis(),
                entity.getX(), entity.getY(), entity.getZ(),
                settings.slayerBigDropsRange,
                settings.slayerBigDropsUnscaleSeconds)
                && SlayerDropScalePolicy.selected(settings.slayerDropFilter, id)
                ? (float) SlayerDropScalePolicy.clampScale(settings.slayerBigDropsScale)
                : 1.0F;
    }

    public static void renderGizmos() {
        ClientBoundaryGuard.run("SLAYER_GIZMOS", SlayerRuntime::renderGizmosInternal);
    }

    private static void renderGizmosInternal() {
        QolSkyblockExtras settings = settings();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if ((!settings.slayerHighlightsEnabled
                && !settings.slayerDropsGroundHighlight
                && !settings.slayerDropsGroundLabels
                && !fightOverlaysEnabled(settings))
                || player == null || client.level == null) {
            return;
        }
        SlayerHighlightPolicy.Options highlightOptions = new SlayerHighlightPolicy.Options(
                settings.slayerHighlightsOnlyMine,
                settings.slayerHighlightsBoss,
                settings.slayerHighlightsMiniboss,
                settings.slayerHighlightsDemon,
                settings.slayerHighlightsTargetLines,
                settings.slayerHighlightsTargetLineDistance);
        float partialTick = partialTick(client);
        Vec3 eye = player.getEyePosition(partialTick);
        if (settings.slayerHighlightsEnabled) {
            int drawn = 0;
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(System.currentTimeMillis()).activeBosses()) {
                if (drawn >= 24) {
                    break;
                }
                SlayerPolicy.EntityDescriptor descriptor = active.descriptor();
                SlayerPolicy.EntityRole role = descriptor.role();
                boolean localFamily = localQuestFamily(descriptor.type());
                if (!SlayerHighlightPolicy.shouldHighlight(
                        role, descriptor.type(), active.owned(), localFamily, highlightOptions)) {
                    continue;
                }
                Entity entity = client.level.getEntity(active.entityId());
                if (entity == null) {
                    continue;
                }
                int color = color(settings, role);
                if (settings.slayerInfernoEnabled
                        && settings.slayerInfernoColorByAttunement
                        && descriptor.type() == SlayerPolicy.SlayerType.INFERNO) {
                    color = SlayerFightPolicy.attunementColor(descriptor.attunement());
                }
                float width = (float) width(settings, role);
                AABB box = interpolatedBox(entity, partialTick, 0.05D);
                var props = Gizmos.cuboid(box, GizmoStyle.strokeAndFill(
                        color, width, withAlpha(color, 0x33)));
                if (!settings.slayerHighlightsDepth) {
                    props.setAlwaysOnTop();
                }
                if (SlayerHighlightPolicy.shouldDrawTargetLine(
                        role,
                        descriptor.type(),
                        active.owned(),
                        localFamily,
                        eye.distanceTo(box.getCenter()),
                        highlightOptions)) {
                    var line = Gizmos.line(
                            eye,
                            box.getCenter(),
                            color,
                            (float) settings.slayerHighlightsTargetLineWidth);
                    if (!settings.slayerHighlightsDepth) {
                        line.setAlwaysOnTop();
                    }
                }
                drawn++;
            }
        }
        if (settings.slayerDropsEnabled && (settings.slayerDropsGroundHighlight || settings.slayerDropsGroundLabels)) {
            Map<String, BigDecimal> prices = settings.slayerDropsGroundLabels ? slayerUnitPrices() : Map.of();
            AABB search = player.getBoundingBox().inflate(24.0D);
            for (Entity entity : client.level.getEntities(player, search)) {
                if (!(entity instanceof ItemEntity item)) continue;
                String id = AutoClickerItemIdentity.skyBlockId(item.getItem());
                if (!SlayerItemProfitPolicy.isKnownSlayerDropId(id)) continue;
                if (settings.slayerDropsGroundHighlight) {
                    var props = Gizmos.cuboid(interpolatedBox(item, partialTick, 0.15D),
                            GizmoStyle.strokeAndFill(0xFF51E6A8, 2.0F, 0x3351E6A8));
                    if (!settings.slayerHighlightsDepth) props.setAlwaysOnTop();
                }
                if (settings.slayerDropsGroundLabels) {
                    SlayerRngCatalog.Entry entry = SlayerRngCatalog.byId(id).orElse(null);
                    if (entry == null) continue;
                    BigDecimal price = prices.get(entry.skyBlockId());
                    BigDecimal totalValue = SlayerGroundDropPolicy.totalValue(price, item.getItem().getCount());
                    if (!SlayerGroundDropPolicy.shouldShowLabel(
                            true,
                            price,
                            item.getItem().getCount(),
                            settings.slayerDropsGroundLabelMinimum)) {
                        continue;
                    }
                    String label = entry.display() + " ×" + item.getItem().getCount();
                    label += " · " + String.format(Locale.ROOT, "%,.2f", totalValue) + " coins";
                    var text = Gizmos.billboardTextOverBlock(
                            label,
                            BlockPos.containing(item.position()),
                            0,
                            0xFF51E6A8,
                            0.75F);
                    text.setAlwaysOnTop();
                }
            }
        }
        for (FightMarker marker : FIGHT_MARKERS) {
            AABB box = marker.box();
            Vec3 target = marker.target();
            if (marker.entityId() != Integer.MIN_VALUE) {
                Entity tracked = client.level.getEntity(marker.entityId());
                if (tracked == null) {
                    continue;
                }
                box = interpolatedMarkerBox(tracked, partialTick, marker.inflate());
                target = box.getCenter();
            }
            var props = Gizmos.cuboid(box, GizmoStyle.strokeAndFill(
                    marker.color(), 2.0F, withAlpha(marker.color(), 0x33)));
            props.setAlwaysOnTop();
            if (marker.line()) {
                Gizmos.line(eye, target, marker.lineColor(), marker.lineWidth()).setAlwaysOnTop();
            }
            if (marker.label() != null && !marker.label().isBlank()) {
                var text = Gizmos.billboardTextOverBlock(
                        marker.label(),
                        BlockPos.containing(target),
                        0,
                        marker.color(),
                        0.85F);
                text.setAlwaysOnTop();
            }
        }
        if (settings.slayerVoidgloomEnabled
                && (settings.slayerVoidgloomHighlightBeacon || settings.slayerVoidgloomBeaconTimer)) {
            int glyphColor = settings.slayerVoidgloomBeaconColor;
            double beamTop = SlayerFightPolicy.YANG_GLYPH_BEAM_HEIGHT;
            for (Vec3 pos : YANG_GLYPH_BEAMS) {
                var pillar = Gizmos.cuboid(
                        new AABB(pos.x - 0.22D, pos.y, pos.z - 0.22D,
                                pos.x + 0.22D, pos.y + beamTop, pos.z + 0.22D),
                        GizmoStyle.strokeAndFill(glyphColor, 1.8F, withAlpha(glyphColor, 0x55)));
                pillar.setAlwaysOnTop();
                Gizmos.line(
                        new Vec3(pos.x, pos.y, pos.z),
                        new Vec3(pos.x, pos.y + beamTop, pos.z),
                        glyphColor,
                        4.0F).setAlwaysOnTop();
            }
        }
        if (settings.slayerVampireMarkersEnabled && settings.slayerVampireMarkersIchorBeam) {
            int color = settings.slayerVampireMarkersIchorColor;
            for (Vec3 pos : ICHOR_BEAMS) {
                var beam = Gizmos.cuboid(
                        new AABB(pos.x - 0.15D, pos.y, pos.z - 0.15D,
                                pos.x + 0.15D, pos.y + 24.0D, pos.z + 0.15D),
                        GizmoStyle.strokeAndFill(color, 1.5F, withAlpha(color, 0x33)));
                beam.setAlwaysOnTop();
            }
        }
        if (settings.slayerVampireMarkersEnabled && settings.slayerVampireMarkersEffigies) {
            int color = 0xFFFF2020;
            for (SlayerPolishPolicy.BlockCoord coord : unbrokenEffigies(client)) {
                var box = Gizmos.cuboid(
                        new AABB(coord.x(), coord.y(), coord.z(),
                                coord.x() + 1.0D, coord.y() + 1.0D, coord.z() + 1.0D),
                        GizmoStyle.strokeAndFill(color, 2.0F, withAlpha(color, 0x44)));
                box.setAlwaysOnTop();
                var beam = Gizmos.cuboid(
                        new AABB(coord.x() + 0.4D, coord.y() - 6.0D, coord.z() + 0.4D,
                                coord.x() + 0.6D, coord.y() + 16.0D, coord.z() + 0.6D),
                        GizmoStyle.strokeAndFill(color, 1.5F, withAlpha(color, 0x33)));
                beam.setAlwaysOnTop();
            }
        }
        if (settings.slayerVoidgloomEnabled && settings.slayerVoidgloomBeaconPath) {
            int pathColor = settings.slayerVoidgloomLineColor;
            float pathWidth = SlayerFightPolicy.clampLineWidth(settings.slayerVoidgloomLineWidth);
            for (List<Vec3> path : FLYING_BEACON_PATHS.values()) {
                for (int i = 1; i < path.size(); i++) {
                    Gizmos.line(path.get(i - 1), path.get(i), pathColor, pathWidth).setAlwaysOnTop();
                }
            }
        }
    }

    private static void scanEntities(Minecraft client, QolSkyblockExtras settings) {
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        String localName = player.getGameProfile().name();
        SlayerMechanicsPolicy.AttunementDisplay observedAttunement = null;
        Set<Integer> seenDamageEntities = new HashSet<>();
        AABB search = player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D);
        Map<Integer, List<String>> linesByEntity = new HashMap<>();
        Map<Integer, Entity> hosts = new HashMap<>();
        ownedNametagLines = List.of();
        for (Entity entity : level.getEntities(player, search)) {
            if (settings.slayerVengeanceDamageEnabled && entity instanceof ArmorStand) {
                seenDamageEntities.add(entity.getId());
                reportVengeanceDamage(client, settings, entity);
            }
            if (isNameHologram(entity)) {
                if (!SlayerPolicy.isSlayerHologram(name(entity))) {
                    continue;
                }
                Entity host = nearestSlayerHost(level, entity);
                if (host == null) {
                    continue;
                }
                hosts.put(host.getId(), host);
                linesByEntity.computeIfAbsent(host.getId(), id -> new ArrayList<>()).add(name(entity));
                continue;
            }
            if (isSlayerHost(entity)) {
                hosts.putIfAbsent(entity.getId(), entity);
                for (Entity passenger : entity.getPassengers()) {
                    if (isNameHologram(passenger)) {
                        linesByEntity.computeIfAbsent(entity.getId(), id -> new ArrayList<>())
                                .add(name(passenger));
                    }
                }
            }
        }
        for (Entity entity : level.getEntities(player, search)) {
            if (!isNameHologram(entity)) {
                continue;
            }
            Entity host = nearestSlayerHost(level, entity);
            if (host == null || !hosts.containsKey(host.getId())) {
                continue;
            }
            String line = name(entity);
            if (line.isBlank()) {
                continue;
            }
            List<String> bucket = linesByEntity.computeIfAbsent(host.getId(), id -> new ArrayList<>());
            if (!bucket.contains(line)) {
                bucket.add(line);
            }
        }
        for (Entity entity : hosts.values()) {
            List<String> bucket = linesByEntity.computeIfAbsent(entity.getId(), id -> new ArrayList<>());
            for (int offset : SlayerPolicy.HOLOGRAM_ID_OFFSETS) {
                Entity tag = level.getEntity(entity.getId() + offset);
                if (!isNameHologram(tag)) {
                    continue;
                }
                String line = name(tag);
                if (!line.isBlank() && !bucket.contains(line)) {
                    bucket.add(line);
                }
            }
        }
        long now = System.currentTimeMillis();
        for (Entity entity : hosts.values()) {
            List<String> lines = linesByEntity.getOrDefault(entity.getId(), List.of());
            if (lines.isEmpty()) {
                String self = name(entity);
                if (self.isBlank()) {
                    continue;
                }
                lines = List.of(self);
            }
            SlayerPolicy.EntityDescriptor descriptor = SlayerPolicy.classifyHolograms(lines).orElse(null);
            if (descriptor == null || !SlayerPolicy.shouldTrackLiveEntity(descriptor)) {
                continue;
            }
            SlayerSessionEngine.SpawnResult spawn = ENGINE.observeEntity(
                    entity.getId(), descriptor, localName, now);
            boolean owned = descriptor.owner().equalsIgnoreCase(localName);
            if (owned && descriptor.role() == SlayerPolicy.EntityRole.BOSS) {
                ownedNametagLines = List.copyOf(lines);
            }
            if (owned && descriptor.role() == SlayerPolicy.EntityRole.BOSS
                    && descriptor.type() == SlayerPolicy.SlayerType.INFERNO) {
                for (Entity attached : attachedNameEntities(level, entity)) {
                    String line = name(attached);
                    var parsed = SlayerMechanicsPolicy.attunementDisplay(
                            line, settings.slayerAttunementDisplayCount);
                    if (parsed.isPresent()) {
                        observedAttunement = parsed.get();
                    }
                    if (settings.slayerVengeanceEnabled
                            && SlayerMechanicsPolicy.isVengeanceStartTag(line)
                            && isVengeanceDagger(player.getMainHandItem())
                            && !VENGEANCE.active()) {
                        VENGEANCE.start();
                    }
                }
            }
            if (!spawn.spawned()) {
                continue;
            }
            if (descriptor.role() == SlayerPolicy.EntityRole.MINIBOSS
                    && SlayerMinibossAlertPolicy.shouldAnnounceNearbyHost(
                    spawn.owned(),
                    localQuestFamily(descriptor.type()))) {
                alertMiniboss(client, settings, descriptor);
            }
            if (descriptor.role() == SlayerPolicy.EntityRole.BOSS
                    && settings.slayerCarryEnabled
                    && settings.slayerCarryShowSpawnMessage
                    && ENGINE.carries().stream().anyMatch(carry ->
                    !carry.complete()
                            && carry.player().equalsIgnoreCase(descriptor.owner())
                            && carry.type() == descriptor.type()
                            && (carry.tier() == 0 || carry.tier() == descriptor.tier()))) {
                player.sendSystemMessage(RotClientChat.message(
                        "Carry boss spawned: " + descriptor.owner() + " · "
                                + descriptor.displayName() + tierSuffix(descriptor.tier())));
            }
        }
        SEEN_VENGEANCE_DAMAGE.keySet().retainAll(seenDamageEntities);
        lastAttunement = observedAttunement;
        scanFightMarkers(client, settings);
    }

    private static void reportVengeanceDamage(
            Minecraft client,
            QolSkyblockExtras settings,
            Entity marker) {
        String markerName = name(marker);
        var damage = SlayerMechanicsPolicy.vengeanceDamage(markerName);
        if (damage.isEmpty() || markerName.equals(SEEN_VENGEANCE_DAMAGE.get(marker.getId()))) {
            return;
        }
        boolean nearOwnedInferno = ENGINE.snapshot(System.currentTimeMillis()).activeBosses().stream()
                .filter(active -> active.owned()
                        && active.descriptor().type() == SlayerPolicy.SlayerType.INFERNO)
                .map(active -> client.level.getEntity(active.entityId()))
                .filter(java.util.Objects::nonNull)
                .anyMatch(boss -> marker.distanceTo(boss) <= 5.0F);
        if (!nearOwnedInferno) {
            return;
        }
        SEEN_VENGEANCE_DAMAGE.put(marker.getId(), markerName);
        String value = settings.slayerVengeanceDamageAbbreviate
                ? SlayerMechanicsPolicy.abbreviateDamage(damage.get())
                : String.format(Locale.ROOT, "%,d", damage.get());
        client.player.sendSystemMessage(RotClientChat.message("Vengeance → " + value));
    }

    private static SlayerPolicy.EntityDescriptor descriptor(ClientLevel level, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        Entity typeTag = level.getEntity(entity.getId() + 1);
        Entity timerTag = level.getEntity(entity.getId() + 2);
        Entity ownerTag = level.getEntity(entity.getId() + 3);
        if (isNameHologram(typeTag)) {
            lines.add(name(typeTag));
        }
        if (isNameHologram(timerTag)) {
            lines.add(name(timerTag));
        }
        if (isNameHologram(ownerTag)) {
            lines.add(name(ownerTag));
        }
        for (Entity nearby : attachedNameEntities(level, entity)) {
            String line = name(nearby);
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        String self = name(entity);
        if (!self.isBlank()) {
            lines.add(self);
        }
        return SlayerPolicy.classifyHolograms(lines).orElse(null);
    }

    private static String attachedAttunementLine(ClientLevel level, Entity entity) {
        for (Entity attached : attachedNameEntities(level, entity)) {
            String line = name(attached);
            if (line.contains(":") && line.contains("♨")
                    && SlayerMechanicsPolicy.daggerAttunement(line).isPresent()) {
                return line;
            }
        }
        return null;
    }

    private static List<Entity> attachedNameEntities(ClientLevel level, Entity entity) {
        List<Entity> attached = new ArrayList<>();
        // Hypixel holograms sit above the mob, often a few blocks up and
        // slightly offset. A 0×1×0 inflate missed those stands, so bosses,
        // demons, attunement, vengeance, soulcry, and lasers never classified.
        AABB box = entity.getBoundingBox().inflate(0.9D, 2.8D, 0.9D);
        for (Entity nearby : level.getEntities(entity, box)) {
            if (isNameHologram(nearby)) {
                attached.add(nearby);
            }
        }
        for (Entity passenger : entity.getPassengers()) {
            if (isNameHologram(passenger) && !attached.contains(passenger)) {
                attached.add(passenger);
            }
        }
        attached.sort(Comparator.comparingDouble(entity::distanceToSqr));
        return attached;
    }

    private static boolean isNameHologram(Entity entity) {
        return entity instanceof ArmorStand
                && (entity.hasCustomName() || !name(entity).isBlank());
    }

    private static boolean isSlayerHost(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof ArmorStand)
                && !(entity instanceof LocalPlayer);
    }

    private static boolean hologramBelongsTo(Entity host, Entity hologram) {
        if (host == null || hologram == null) {
            return false;
        }
        if (hologram.getVehicle() == host) {
            return true;
        }
        double dx = host.getX() - hologram.getX();
        double dz = host.getZ() - hologram.getZ();
        return dx * dx + dz * dz <= 1.0D;
    }

    private static Entity nearestSlayerHost(ClientLevel level, Entity hologram) {
        if (hologram.getVehicle() instanceof LivingEntity vehicle && isSlayerHost(vehicle)) {
            return vehicle;
        }
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = hologram.getBoundingBox().inflate(0.9D, 2.8D, 0.9D);
        for (Entity nearby : level.getEntities(hologram, box)) {
            if (!isSlayerHost(nearby) || !hologramBelongsTo(nearby, hologram)) {
                continue;
            }
            double dist = nearby.distanceToSqr(hologram);
            if (dist < bestDist) {
                bestDist = dist;
                best = nearby;
            }
        }
        return best;
    }

    private static void syncSidebarQuest(QolSkyblockExtras settings) {
        Minecraft client = Minecraft.getInstance();
        List<String> sidebar = List.of(SkyBlockSidebar.text().split("\n"));
        List<String> tab = client == null
                ? List.of()
                : CommissionDisplayRuntime.tabLines(client);
        boolean visible = SlayerFightPolicy.sidebarHasSlayerQuest(sidebar)
                || SlayerFightPolicy.sidebarHasSlayerQuest(tab)
                || SlayerProgressPolicy.showsQuest(tab)
                || SlayerProgressPolicy.showsQuest(sidebar);
        long now = System.currentTimeMillis();
        ENGINE.observeQuestVisible(visible, now);
        Optional<SlayerFightPolicy.QuestRef> quest = SlayerFightPolicy.questFromSidebar(sidebar);
        if (quest.isEmpty()) {
            quest = SlayerFightPolicy.questFromSidebar(tab);
        }
        if (quest.isPresent()) {
            latestQuest = quest.get();
            activateStoredRngDrop(latestQuest.type());
        } else if (!visible && ENGINE.questState() != SlayerSessionEngine.QuestState.ACTIVE) {
            latestQuest = null;
        }
        if (!settings.slayerProgressEnabled) {
            return;
        }
        Optional<SlayerProgressPolicy.Progress> progress =
                SlayerProgressPolicy.parseLines(sidebar, visible);
        if (progress.isEmpty()) {
            progress = SlayerProgressPolicy.parseLines(tab, visible);
        }
        progress.ifPresent(value -> applyProgress(settings, value, visible));
    }

    private static void applyProgress(
            QolSkyblockExtras settings,
            SlayerProgressPolicy.Progress incoming,
            boolean questVisible) {
        SlayerProgressPolicy.Progress progress =
                SlayerProgressPolicy.preferFresh(latestProgress, incoming);
        latestProgress = progress;
        if (!settings.slayerProgressBossWarning) {
            PROGRESS_THRESHOLD.reset();
            return;
        }
        if (PROGRESS_THRESHOLD.observe(
                questVisible,
                progress,
                settings.slayerProgressWarningPercent,
                settings.slayerProgressWarningRepeat)) {
            alertBossSpawnSoon(Minecraft.getInstance(), progress);
        }
    }

    private static boolean localQuestFamily(SlayerPolicy.SlayerType type) {
        if (type != null && latestQuest != null && latestQuest.type() == type) {
            return true;
        }
        if (ENGINE.questState() != SlayerSessionEngine.QuestState.ACTIVE) {
            return false;
        }
        SlayerSessionEngine.Snapshot snapshot = overlaySnapshot != null
                ? overlaySnapshot
                : ENGINE.viewSnapshot();
        return snapshot.activeBosses().stream()
                .anyMatch(active -> active.owned() && active.descriptor().type() == type);
    }

    private static Vec3 ownedBossPosition(
            ClientLevel level,
            SlayerPolicy.SlayerType type,
            long now) {
        SlayerSessionEngine.Snapshot snapshot = overlaySnapshot != null
                ? overlaySnapshot
                : ENGINE.snapshot(now);
        for (SlayerSessionEngine.ActiveBoss active : snapshot.activeBosses()) {
            if (!active.owned()
                    || active.descriptor().role() != SlayerPolicy.EntityRole.BOSS
                    || active.descriptor().type() != type) {
                continue;
            }
            Entity entity = level.getEntity(active.entityId());
            if (entity != null) {
                return entity.position();
            }
        }
        return null;
    }

    private static SlayerSessionEngine.ActiveBoss primaryOwnedBoss(
            SlayerSessionEngine.Snapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        List<SlayerSessionEngine.ActiveBoss> owned = snapshot.activeBosses().stream()
                .filter(SlayerSessionEngine.ActiveBoss::owned)
                .filter(active -> active.descriptor().role() == SlayerPolicy.EntityRole.BOSS)
                .toList();
        if (owned.isEmpty()) {
            return null;
        }
        if (latestQuest != null) {
            for (SlayerSessionEngine.ActiveBoss boss : owned) {
                if (boss.descriptor().type() == latestQuest.type()) {
                    return boss;
                }
            }
        }
        return owned.getFirst();
    }

    private static boolean nearOwnedBoss(
            ClientLevel level,
            SlayerPolicy.SlayerType type,
            Entity entity,
            long now) {
        if (level == null || type == null || entity == null) {
            return false;
        }
        Vec3 owned = scanOwnedAt == now && scanOwnedPositions != null
                ? cachedOwnedPosition(type)
                : ownedBossPosition(level, type, now);
        Minecraft client = Minecraft.getInstance();
        double playerDistance = client != null && client.player != null
                ? entity.distanceTo(client.player)
                : Double.NaN;
        return SlayerHighlightPolicy.shouldDrawLocalFightMarker(
                owned != null,
                owned == null ? Double.NaN : entity.position().distanceTo(owned),
                localQuestFamily(type),
                playerDistance);
    }

    private static boolean hasOwnedBossOfType(SlayerPolicy.SlayerType type, long now) {
        SlayerSessionEngine.Snapshot snapshot = overlaySnapshot != null
                ? overlaySnapshot
                : ENGINE.snapshot(now);
        return snapshot.activeBosses().stream().anyMatch(active ->
                active.owned()
                        && active.descriptor().role() == SlayerPolicy.EntityRole.BOSS
                        && active.descriptor().type() == type);
    }

    private static void cacheOwnedBossPositions(ClientLevel level, long now) {
        SlayerPolicy.SlayerType[] types = SlayerPolicy.SlayerType.values();
        scanOwnedPositions = new Vec3[types.length];
        for (SlayerPolicy.SlayerType type : types) {
            scanOwnedPositions[type.ordinal()] = ownedBossPosition(level, type, now);
        }
        scanOwnedAt = now;
    }

    private static void cacheForeignVoidgloomPositions(ClientLevel level, long now) {
        scanOwnedVoidgloomEntityId = ownedVoidgloomEntityId(now);
        List<Vec3> positions = new ArrayList<>();
        if (level != null) {
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (active.owned()
                        || active.descriptor().role() != SlayerPolicy.EntityRole.BOSS
                        || active.descriptor().type() != SlayerPolicy.SlayerType.VOIDGLOOM) {
                    continue;
                }
                Entity entity = level.getEntity(active.entityId());
                if (entity != null) {
                    positions.add(entity.position());
                }
            }
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null) {
                int ownedId = scanOwnedVoidgloomEntityId;
                AABB box = client.player.getBoundingBox().inflate(
                        SlayerHighlightPolicy.YANG_GLYPH_PLAYER_RANGE);
                for (EnderMan enderman : level.getEntitiesOfClass(EnderMan.class, box)) {
                    if (enderman.getId() == ownedId || !holdingBeacon(enderman)) {
                        continue;
                    }
                    positions.add(enderman.position());
                }
            }
        }
        scanForeignVoidgloomPositions = positions;
    }

    private static int ownedVoidgloomEntityId(long now) {
        SlayerSessionEngine.Snapshot snapshot = overlaySnapshot != null
                ? overlaySnapshot
                : ENGINE.snapshot(now);
        for (SlayerSessionEngine.ActiveBoss active : snapshot.activeBosses()) {
            if (active.owned()
                    && active.descriptor().role() == SlayerPolicy.EntityRole.BOSS
                    && active.descriptor().type() == SlayerPolicy.SlayerType.VOIDGLOOM) {
                return active.entityId();
            }
        }
        return Integer.MIN_VALUE;
    }

    private static double nearestForeignVoidgloomDistance(Vec3 at) {
        if (at == null || scanForeignVoidgloomPositions == null || scanForeignVoidgloomPositions.isEmpty()) {
            return Double.NaN;
        }
        double best = Double.NaN;
        for (Vec3 pos : scanForeignVoidgloomPositions) {
            if (pos == null) {
                continue;
            }
            double dist = at.distanceTo(pos);
            if (!Double.isFinite(best) || dist < best) {
                best = dist;
            }
        }
        return best;
    }

    private static Vec3 cachedOwnedPosition(SlayerPolicy.SlayerType type) {
        if (type == null || scanOwnedPositions == null || type.ordinal() >= scanOwnedPositions.length) {
            return null;
        }
        return scanOwnedPositions[type.ordinal()];
    }

    private static SlayerSessionEngine.Snapshot fadeSnapshot(Minecraft client) {
        long tick = client.player != null ? client.player.tickCount : Long.MIN_VALUE;
        if (fadeSnapshot == null || fadeSnapshotTick != tick) {
            fadeSnapshotTick = tick;
            fadeSnapshot = ENGINE.snapshot(System.currentTimeMillis());
        }
        return fadeSnapshot;
    }

    private static float partialTick(Minecraft client) {
        return client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    private static AABB interpolatedBox(Entity entity, float partialTick, double inflate) {
        EntityLerpPolicy.Offset offset = EntityLerpPolicy.renderOffset(
                entity.getX(), entity.getY(), entity.getZ(),
                entity.xo, entity.yo, entity.zo,
                partialTick);
        return entity.getBoundingBox().move(offset.x(), offset.y(), offset.z()).inflate(inflate);
    }

    private static AABB interpolatedMarkerBox(Entity entity, float partialTick, double inflate) {
        AABB box = interpolatedBox(entity, partialTick, inflate);
        if (box.getXsize() >= MIN_MARKER_SIZE
                && box.getYsize() >= MIN_MARKER_SIZE
                && box.getZsize() >= MIN_MARKER_SIZE) {
            return box;
        }
        EntityLerpPolicy.Offset offset = EntityLerpPolicy.renderOffset(
                entity.getX(), entity.getY(), entity.getZ(),
                entity.xo, entity.yo, entity.zo,
                partialTick);
        double half = MIN_MARKER_SIZE / 2.0D;
        double cx = entity.getX() + offset.x();
        double cy = entity.getY() + offset.y() + half;
        double cz = entity.getZ() + offset.z();
        return new AABB(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half);
    }

    private static void reapRemoved(ClientLevel level) {
        for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(System.currentTimeMillis()).activeBosses()) {
            Entity entity = level.getEntity(active.entityId());
            if (entity != null && (entity.isRemoved()
                    || (entity instanceof LivingEntity living && living.isDeadOrDying()))) {
                handleDeath(
                        ENGINE.onEntityDeath(active.entityId(), System.currentTimeMillis()),
                        entity.getX(), entity.getY(), entity.getZ());
            }
        }
        if (lastAttackedCarryBossEntityId != Integer.MIN_VALUE
                && level.getEntity(lastAttackedCarryBossEntityId) == null) {
            lastAttackedCarryBossEntityId = Integer.MIN_VALUE;
        }
    }

    private static void rememberAttackedCarryBoss(
            Minecraft client,
            Entity entity,
            QolSkyblockExtras settings) {
        if (!settings.slayerActiveBossTransparencyEnabled || client == null
                || client.level == null || entity == null) {
            return;
        }
        SlayerPolicy.EntityDescriptor descriptor = descriptor(client.level, entity);
        if (descriptor == null || descriptor.role() != SlayerPolicy.EntityRole.BOSS) {
            return;
        }
        boolean matchingCarry = ENGINE.carries().stream().anyMatch(carry ->
                !carry.complete()
                        && carry.player().equalsIgnoreCase(descriptor.owner())
                        && carry.type() == descriptor.type()
                        && (carry.tier() == 0 || carry.tier() == descriptor.tier()));
        if (matchingCarry) {
            lastAttackedCarryBossEntityId = entity.getId();
        }
    }

    private static boolean hasOwnedBoss(SlayerSessionEngine.Snapshot snapshot) {
        return snapshot.activeBosses().stream().anyMatch(active ->
                active.owned() && active.descriptor().role() == SlayerPolicy.EntityRole.BOSS);
    }

    private static boolean hasAttackedCarryBoss(SlayerSessionEngine.Snapshot snapshot) {
        return snapshot.activeBosses().stream().anyMatch(active ->
                active.entityId() == lastAttackedCarryBossEntityId
                        && active.descriptor().role() == SlayerPolicy.EntityRole.BOSS
                        && ENGINE.carries().stream().anyMatch(carry ->
                        !carry.complete()
                                && carry.player().equalsIgnoreCase(active.descriptor().owner())
                                && carry.type() == active.descriptor().type()
                                && (carry.tier() == 0 || carry.tier() == active.descriptor().tier())));
    }

    private static SlayerPolicy.SlayerType activeFadeType(SlayerSessionEngine.Snapshot snapshot) {
        SlayerSessionEngine.ActiveBoss owned = primaryOwnedBoss(snapshot);
        if (owned != null) {
            return owned.descriptor().type();
        }
        return snapshot.activeBosses().stream()
                .filter(active -> active.entityId() == lastAttackedCarryBossEntityId)
                .filter(active -> active.descriptor().role() == SlayerPolicy.EntityRole.BOSS)
                .filter(active -> ENGINE.carries().stream().anyMatch(carry ->
                        !carry.complete()
                                && carry.player().equalsIgnoreCase(active.descriptor().owner())
                                && carry.type() == active.descriptor().type()
                                && (carry.tier() == 0 || carry.tier() == active.descriptor().tier())))
                .map(active -> active.descriptor().type())
                .findFirst()
                .orElse(null);
    }

    private static SlayerIrrelevantMobsPolicy.MobKind mobKind(Entity entity) {
        if (entity instanceof Zombie) return SlayerIrrelevantMobsPolicy.MobKind.ZOMBIE;
        if (entity instanceof Spider || entity instanceof CaveSpider) {
            return SlayerIrrelevantMobsPolicy.MobKind.SPIDER;
        }
        if (entity instanceof Wolf) return SlayerIrrelevantMobsPolicy.MobKind.WOLF;
        if (entity instanceof EnderMan) return SlayerIrrelevantMobsPolicy.MobKind.ENDERMAN;
        if (entity instanceof Blaze) return SlayerIrrelevantMobsPolicy.MobKind.BLAZE;
        return SlayerIrrelevantMobsPolicy.MobKind.OTHER;
    }

    private static void alertMiniboss(
            Minecraft client,
            QolSkyblockExtras settings,
            SlayerPolicy.EntityDescriptor descriptor) {
        if (client == null || client.player == null) {
            return;
        }
        boolean big = descriptor != null && descriptor.bigMiniboss();
        String configured = big
                ? settings.slayerBigMinibossAlertText
                : settings.slayerMinibossAlertText;
        String text = configured == null || configured.isBlank()
                ? (descriptor == null ? "Miniboss spawned!" : descriptor.displayName() + " spawned!")
                : configured;
        if (settings.slayerMinibossAlertMessage) {
            client.player.sendSystemMessage(RotClientChat.message(text, false, 0));
        }
        if (settings.slayerMinibossAlertTitle) {
            client.gui.hud.setTimes(10, 30, 10);
            client.gui.hud.setTitle(Component.literal("§b" + text));
        }
    }

    private static void alertCocoon(Minecraft client, QolSkyblockExtras settings) {
        if (!settings.slayerCocoonShowAlert || client == null || client.player == null) {
            return;
        }
        String text = SlayerMechanicsPolicy.formatAlertText(
                settings.slayerCocoonAlertMessage);
        client.gui.hud.setTimes(10, 30, 10);
        client.gui.hud.setTitle(Component.literal(text));

        String configured = settings.slayerCocoonAlertSound == null
                ? ""
                : settings.slayerCocoonAlertSound.trim();
        Identifier soundId = Identifier.tryParse(configured.contains(":")
                ? configured
                : "minecraft:" + configured);
        SoundEvent sound = soundId == null
                ? null
                : BuiltInRegistries.SOUND_EVENT.getValue(soundId);
        if (sound == null) {
            sound = SoundEvents.NOTE_BLOCK_PLING.value();
        }
        client.player.level().playLocalSound(
                client.player.getX(), client.player.getY(), client.player.getZ(),
                sound,
                SoundSource.PLAYERS,
                (float) Math.max(0.0D, Math.min(1.0D, settings.slayerCocoonAlertVolume)),
                (float) Math.max(0.0D, Math.min(2.0D, settings.slayerCocoonAlertPitch)),
                false);
    }

    private static void alertBossSpawnSoon(
            Minecraft client,
            SlayerProgressPolicy.Progress progress) {
        if (client == null || client.player == null) {
            return;
        }
        client.gui.hud.setTimes(5, 30, 10);
        client.gui.hud.setTitle(Component.literal("Slayer boss soon!")
                .withStyle(ChatFormatting.YELLOW));
        client.player.level().playLocalSound(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                0.75F,
                1.2F,
                false);
        client.player.sendSystemMessage(RotClientChat.message(String.format(
                Locale.ROOT,
                "Slayer boss soon - %d%% Combat XP",
                progress.percent())));
    }

    private static void tickDaggerSwap(Minecraft client) {
        if (daggerUseCooldown > 0) {
            daggerUseCooldown--;
        }
        DAGGER_SWAP.tick();
        DAGGER_SWAP.ready().ifPresent(attunement -> applyDaggerSwap(client, attunement));
    }

    private static void applyDaggerSwap(
            Minecraft client,
            SlayerMechanicsPolicy.DaggerAttunement attunement) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || player.connection == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (SlayerMechanicsPolicy.supportsDagger(
                AutoClickerItemIdentity.skyBlockId(held), attunement)) {
            if (attunementMode(held) == attunement.mode()) {
                DAGGER_SWAP.complete();
                return;
            }
            if (daggerUseCooldown <= 0) {
                ClickPulseHelper.pulseUse(client);
                daggerUseCooldown = 2;
            }
            return;
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!SlayerMechanicsPolicy.supportsDagger(
                    AutoClickerItemIdentity.skyBlockId(candidate), attunement)) {
                continue;
            }
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            daggerUseCooldown = 1;
            return;
        }
        DAGGER_SWAP.complete();
    }

    private static int attunementMode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        int direct = tag.getInt("td_attune_mode").orElse(-1);
        if (direct >= 0) {
            return direct;
        }
        return tag.getCompound("ExtraAttributes")
                .map(extra -> extra.getInt("td_attune_mode").orElse(-1))
                .orElse(-1);
    }

    private static void resetDaggerSwap() {
        DAGGER_SWAP.reset();
        daggerUseCooldown = 0;
        lastDaggerTargetEntityId = Integer.MIN_VALUE;
    }

    private static void resetAdditionalMechanics() {
        SOULCRY.reset();
        SOULCRY_ABILITY.reset();
        VENGEANCE.reset();
        lastAttunement = null;
        dropScaleWindow = null;
        SEEN_VENGEANCE_DAMAGE.clear();
        FIGHT_MARKERS.clear();
        SEEN_BEACON_STANDS.clear();
        SITTING_BEACONS.clear();
        FLYING_BEACON_PATHS.clear();
        YANG_GLYPH_BEAMS.clear();
        lastOwnedHeldBeacon = false;
        yangGlyphThrownUntilMillis = 0L;
        yangGlyphClaimed = false;
        yangGlyphThrowOrigin = null;
        voidgloomPhase = SlayerFightPolicy.VoidgloomPhase.UNKNOWN;
        voidgloomHits = -1;
        voidgloomHealth = "";
        laserEndsAtMillis = 0L;
        lastLaserStartMillis = 0L;
        sittingBeaconRemaining = 0.0D;
        revenantBoomActive = false;
        tarantulaPhase = SlayerFightPolicy.TarantulaPhase.UNKNOWN;
        tarantulaEggHits = "";
        tarantulaInvincible = false;
        lastHatchlingsMillis = 0L;
        lastHatchlingsPos = null;
        vampireTwinclawsActive = false;
        vampireManiaActive = false;
        vampireSteakReady = false;
        vampireManiaRemaining = 0.0D;
        twinClawsReadyAtMillis = 0L;
        infernoPhase = SlayerFightPolicy.InfernoPhase.UNKNOWN;
        firePillarSeconds = -1;
        firePillarHits = -1;
        infernoMaxHealth = 0.0D;
        lastInfernoHealth = 0.0D;
        ownedInfernoTier = 0;
        lastFirePitsMillis = 0L;
        lastFirePillarWarnMillis = 0L;
        gummyNeeded = false;
        ICHOR_BEAMS.clear();
        RECENT_DEATHS.clear();
        autoStartTicks = -1;
        autoStartCommand = "";
    }

    private static void tickSoulcry(Minecraft client, QolSkyblockExtras settings) {
        SOULCRY_ABILITY.tick();
        if (!QolFlavorSupport.isPlus() || !settings.slayerAutoSoulcryEnabled || !settings.slayerAutoSoulcryTickBased
                || client == null || client.player == null || client.level == null
                || (client.gui != null && client.gui.screen() != null)) {
            SOULCRY.reset();
            return;
        }
        ItemStack held = client.player.getMainHandItem();
        if (!SlayerMechanicsPolicy.isSoulcryKatana(AutoClickerItemIdentity.skyBlockId(held))) {
            SOULCRY.reset();
            return;
        }
        SlayerSessionEngine.ActiveBoss boss = ENGINE.snapshot(System.currentTimeMillis())
                .activeBosses().stream()
                .filter(SlayerSessionEngine.ActiveBoss::owned)
                .filter(candidate -> candidate.descriptor().role() == SlayerPolicy.EntityRole.BOSS)
                .filter(candidate -> candidate.descriptor().type() == SlayerPolicy.SlayerType.VOIDGLOOM)
                .findFirst().orElse(null);
        if (boss == null || (settings.slayerAutoSoulcryCheckHitbox
                && (!(client.hitResult instanceof EntityHitResult hit)
                || hit.getEntity().getId() != boss.entityId()))) {
            SOULCRY.reset();
            return;
        }
        if (settings.slayerAutoSoulcryCheckMana && !hasSoulcryMana(held)) {
            SOULCRY.reset();
            return;
        }
        if (!SOULCRY_ABILITY.ready(heldItemOnCooldown(client, held))) {
            SOULCRY.reset();
            return;
        }
        int min = SlayerMechanicsPolicy.clampSoulcryDelay(settings.slayerAutoSoulcryMinDelay);
        int max = Math.max(min,
                SlayerMechanicsPolicy.clampSoulcryDelay(settings.slayerAutoSoulcryMaxDelay));
        if (SOULCRY.arm(min, max,
                min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1))) {
            return;
        }
        SOULCRY.tick();
        if (SOULCRY.ready()) {
            useHeldItem(client);
            SOULCRY.complete();
            SOULCRY_ABILITY.markUsed();
        }
    }

    private static void tryAttackSoulcry(
            Minecraft client,
            Entity entity,
            QolSkyblockExtras settings) {
        if (client == null || client.player == null || client.level == null
                || entity == null) {
            return;
        }
        SlayerPolicy.EntityDescriptor descriptor = descriptor(client.level, entity);
        if (descriptor == null || descriptor.role() != SlayerPolicy.EntityRole.BOSS
                || descriptor.type() != SlayerPolicy.SlayerType.VOIDGLOOM) {
            return;
        }
        boolean owned = descriptor.owner().equalsIgnoreCase(client.player.getGameProfile().name());
        if (!owned && !settings.slayerAutoSoulcryOtherBosses) {
            return;
        }
        ItemStack held = client.player.getMainHandItem();
        if (!SlayerMechanicsPolicy.isSoulcryKatana(AutoClickerItemIdentity.skyBlockId(held))
                || (settings.slayerAutoSoulcryCheckMana && !hasSoulcryMana(held))) {
            return;
        }
        if (!SOULCRY_ABILITY.ready(heldItemOnCooldown(client, held))) {
            return;
        }
        useHeldItem(client);
        SOULCRY_ABILITY.markUsed();
    }

    private static boolean heldItemOnCooldown(Minecraft client, ItemStack held) {
        if (client == null || client.player == null || held == null || held.isEmpty()) {
            return false;
        }
        return client.player.getCooldowns().isOnCooldown(held);
    }

    private static boolean hasSoulcryMana(ItemStack held) {
        SkyBlockStatBarParser.Stats stats = RotClientClient.qolHud().statsTracker().stats();
        double mana = stats.mana().orElse(-1.0D);
        double overflow = stats.overflowMana().orElse(0.0D);
        if (mana < 0.0D) {
            return false;
        }
        return SlayerMechanicsPolicy.hasSoulcryMana(
                mana, overflow, hasUltimateWise(held));
    }

    private static boolean hasUltimateWise(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag direct = tag.getCompound("enchantments").orElse(null);
        if (direct != null && direct.contains("ultimate_wise")) {
            return true;
        }
        return tag.getCompound("ExtraAttributes")
                .flatMap(extra -> extra.getCompound("enchantments"))
                .map(enchantments -> enchantments.contains("ultimate_wise"))
                .orElse(false);
    }

    private static void useHeldItem(Minecraft client) {
        ClickPulseHelper.pulseUse(client);
    }

    private static boolean isVengeanceDagger(ItemStack stack) {
        String id = AutoClickerItemIdentity.skyBlockId(stack);
        return id.equals("HEARTFIRE_DAGGER")
                || id.equals("BURSTFIRE_DAGGER")
                || id.equals("FIREDUST_DAGGER");
    }

    private static void handleDeath(
            SlayerSessionEngine.DeathResult result,
            double x,
            double y,
            double z) {
        QolSkyblockExtras settings = settings();
        Minecraft client = Minecraft.getInstance();
        if (result != null && result.bossKilled() && result.owned()
                && settings.slayerBigDropsEnabled) {
            dropScaleWindow = new SlayerDropScalePolicy.Window(
                    result.descriptor().type(), System.currentTimeMillis(), x, y, z);
        }
        if (result != null && result.bossKilled()) {
            lastAttunement = null;
            VENGEANCE.reset();
        }
        if (result != null && result.bossKilled() && result.owned()
                && settings.slayerTimeMessagesEnabled) {
            reportTimeMessages(client, settings, result);
        }
        if (result == null || !result.carryAdvanced() || !settings.slayerCarryEnabled) {
            return;
        }
        SlayerSessionEngine.Carry carry = ENGINE.carries().stream()
                .filter(c -> c.player().equalsIgnoreCase(result.completedCarryPlayer()))
                .findFirst().orElse(null);
        if (carry == null) return;
        recordCarryProgress(carry, settings, client);
    }

    private static void reportTimeMessages(
            Minecraft client,
            QolSkyblockExtras settings,
            SlayerSessionEngine.DeathResult result) {
        if (client == null || client.player == null || result.descriptor() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long duration = Math.max(0L, result.durationMillis());
        boolean announce = SlayerTimeMessagePolicy.shouldAnnounce(
                settings.slayerTimeMessagesEnabled,
                true,
                result.bossKilled(),
                result.owned(),
                result.descriptor().role(),
                duration,
                now,
                lastTimeMessageAtMillis,
                result.descriptor());
        if (!announce) {
            return;
        }
        if (settings.slayerPersonalBests == null) {
            settings.slayerPersonalBests = new java.util.LinkedHashMap<>();
        }
        String key = SlayerPolishPolicy.personalBestKey(
                result.descriptor().type(), result.descriptor().tier());
        Long previous = settings.slayerPersonalBests.get(key);
        boolean personalBest = SlayerPolishPolicy.improvedPersonalBest(previous, duration).isPresent();
        if (personalBest) {
            settings.slayerPersonalBests.put(key, duration);
            RotClientClient.save();
        }
        lastTimeMessageAtMillis = now;
        if (settings.slayerTimeMessagesTimeToKill) {
            String message = settings.slayerTimeMessagesCompact
                    ? result.descriptor().displayName() + " · " + duration(duration)
                    : result.descriptor().displayName() + " defeated in " + duration(duration) + '.';
            client.player.sendSystemMessage(RotClientChat.message(message, false, 0));
        }
        if (settings.slayerTimeMessagesPersonalBest && personalBest) {
            client.player.sendSystemMessage(RotClientChat.message(
                    "New personal best · " + duration(duration) + '.', false, 0));
        }
    }

    private static void recordCarryProgress(
            SlayerSessionEngine.Carry carry,
            QolSkyblockExtras settings,
            Minecraft client) {
        String progress = carry.player() + " · " + carry.type().displayName()
                + tierSuffix(carry.tier()) + " · " + carry.completed() + "/" + carry.total();
        if (settings.slayerCarryWebhook
                && (settings.slayerCarryWebhookEach || carry.complete())) {
            SlayerCarryWebhookRuntime.send(settings.slayerCarryWebhookUrl,
                    "Rot Client Slayer carry: " + progress);
        }
        if (carry.complete()) {
            boolean exists = settings.slayerCarryHistory.stream().anyMatch(h ->
                    h.player.equalsIgnoreCase(carry.player())
                            && h.completedAtMillis == carry.lastCompletedAtMillis());
            if (!exists) {
                settings.slayerCarryHistory.add(new SlayerCarryPolicy.HistoryEntry(
                        carry.player(), carry.type(), carry.tier(), carry.total(),
                        carry.lastCompletedAtMillis() - carry.startedAtMillis(),
                        carry.lastCompletedAtMillis(),
                        CARRY_PAYMENTS_M.getOrDefault(
                                carry.player().toLowerCase(Locale.ROOT), 0.0D)));
                if (settings.slayerCarryHistory.size() > 500) {
                    settings.slayerCarryHistory.remove(0);
                }
                RotClientClient.save();
            }
            CARRY_PAYMENTS_M.remove(carry.player().toLowerCase(Locale.ROOT));
            ENGINE.removeCarry(carry.player());
        }
        if (client.player != null && settings.slayerCarryAnnounceParty) {
            client.player.connection.sendCommand("pc " + progress);
        }
    }

    private static void observeTrade(String line, QolSkyblockExtras settings) {
        var player = SlayerCarryPolicy.tradePlayer(line);
        if (player.isPresent()) {
            pendingTradePlayer = player.get();
            return;
        }
        var amount = SlayerCarryPolicy.receivedMillions(line);
        if (amount.isEmpty() || pendingTradePlayer.isBlank()) return;
        String trader = pendingTradePlayer;
        pendingTradePlayer = "";
        List<SlayerCarryPolicy.PriceMatch> matches = SlayerCarryPolicy.infer(
                amount.get(), settings.slayerCarryVoidT3Prices, settings.slayerCarryVoidT4Prices,
                settings.slayerCarryInfernoT2Prices, settings.slayerCarryInfernoT3Prices,
                settings.slayerCarryInfernoT4Prices);
        Minecraft client = Minecraft.getInstance();
        CARRY_PAYMENTS_M.put(trader.toLowerCase(Locale.ROOT), amount.get());
        if (client.player == null || matches.isEmpty()) return;
        String options = matches.stream().map(match -> match.count() + "x "
                + match.type().displayName() + " T" + match.tier()).collect(java.util.stream.Collectors.joining(" or "));
        client.player.sendSystemMessage(RotClientChat.message("Payment from "
                + trader + " matches " + options + ". Add the carry from the manager."));
    }

    private static void observeRng(String line, QolSkyblockExtras settings) {
        String plain = line.replaceAll("§.", "").trim();
        if (settings.slayerDropsDetectAutomatically) {
            SlayerRngMeterPolicy.chatSelection(plain).ifPresent(item -> {
                SlayerPolicy.SlayerType family = SlayerRngMeterPolicy.chatFamily(plain).orElse(null);
                SlayerRngCatalog.resolve(item, family).ifPresent(entry -> applyRngSelection(
                        new SlayerRngMeterPolicy.Selection(entry.display(), -1L, entry.type())));
            });
        }
        Matcher mf = MAGIC_FIND.matcher(plain);
        if (mf.matches()) {
            try { lastMagicFind = Integer.parseInt(mf.group("mf")); } catch (NumberFormatException ignored) {}
        }
        Optional<Long> storedXp = SlayerRngMeterPolicy.chatStoredXp(plain);
        if (storedXp.isEmpty()) {
            return;
        }
        activateStoredRngDrop(currentRngFamily());
        if (SlayerRngMeterPolicy.shouldWarnEmpty(
                settings.slayerDropsRngWarnEmpty, true, rngDropKnown())) {
            warnEmptyRngMeter(Minecraft.getInstance());
            return;
        }
        if (selectedRngDrop == null || selectedRngDrop.requiredXp() <= 0L) {
            return;
        }
        lastRngStoredXp = Math.max(0L, storedXp.get());
        persistRngSelection(selectedRngDrop.type(), selectedRngDrop, lastRngStoredXp);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && settings.slayerDropsShowChance) {
            if (lastMagicFind == null) {
                client.player.sendSystemMessage(RotClientChat.message(String.format(Locale.ROOT,
                        "RNG Meter · %s · %,d/%,d XP · Magic Find pending",
                        selectedRngDrop.display(), lastRngStoredXp, selectedRngDrop.requiredXp())));
            } else {
                double chance = SlayerCarryPolicy.rngChancePercent(lastRngStoredXp, selectedRngDrop.requiredXp(),
                        selectedRngDrop.baseChancePercent(), lastMagicFind);
                client.player.sendSystemMessage(RotClientChat.message(String.format(Locale.ROOT,
                        "RNG Meter · %s · %,d/%,d XP · %.5f%% [✯%d]",
                        selectedRngDrop.display(), lastRngStoredXp, selectedRngDrop.requiredXp(), chance, lastMagicFind)));
            }
        }
    }

    private static void warnEmptyRngMeter(Minecraft client) {
        long now = System.currentTimeMillis();
        if (client == null || client.player == null || now - lastRngEmptyWarningMillis < 10_000L) {
            return;
        }
        lastRngEmptyWarningMillis = now;
        client.player.sendSystemMessage(RotClientChat.message(
                "No selected Slayer RNG Meter drop. Pick a target before the luck runs dry."));
        client.gui.hud.setTimes(5, 30, 10);
        client.gui.hud.setTitle(Component.literal("§cNo RNG Meter drop selected"));
    }

    private static boolean anyFeatureEnabled(QolSkyblockExtras settings) {
        return settings.slayerDisplayEnabled
                || settings.slayerTimeMessagesEnabled
                || settings.slayerProgressEnabled
                || settings.slayerStatsEnabled
                || settings.slayerHighlightsEnabled
                || settings.slayerActiveBossTransparencyEnabled
                || settings.slayerIrrelevantMobsEnabled
                || settings.slayerMinibossAlertEnabled
                || settings.slayerDropsEnabled
                || settings.slayerCarryEnabled
                || settings.slayerCocoonAlertEnabled
                || settings.slayerDaggerSwapEnabled
                || settings.slayerLaserHiderEnabled
                || settings.slayerAttunementDisplayEnabled
                || settings.slayerAutoSoulcryEnabled
                || settings.slayerSoundsEnabled
                || settings.slayerVengeanceEnabled
                || settings.slayerVengeanceDamageEnabled
                || settings.slayerBigDropsEnabled
                || settings.slayerVoidgloomEnabled
                || settings.slayerRevenantEnabled
                || settings.slayerTarantulaEnabled
                || settings.slayerSvenEnabled
                || settings.slayerVampireMarkersEnabled
                || settings.slayerInfernoEnabled
                || settings.slayerQuestWarningEnabled
                || settings.slayerAutoStartEnabled;
    }

    private static boolean familyEnabled(
            QolSkyblockExtras settings,
            SlayerPolicy.SlayerType type) {
        return switch (type) {
            case REVENANT -> settings.slayerBigDropsRevenant;
            case TARANTULA -> settings.slayerBigDropsTarantula;
            case SVEN -> settings.slayerBigDropsSven;
            case VOIDGLOOM -> settings.slayerBigDropsVoidgloom;
            case INFERNO -> settings.slayerBigDropsInferno;
            case VAMPIRE -> settings.slayerBigDropsVampire;
        };
    }

    private static boolean fightOverlaysEnabled(QolSkyblockExtras settings) {
        return settings.slayerVoidgloomEnabled
                || settings.slayerRevenantEnabled
                || settings.slayerTarantulaEnabled
                || settings.slayerSvenEnabled
                || settings.slayerVampireMarkersEnabled
                || settings.slayerInfernoEnabled;
    }

    private static void scanFightMarkers(Minecraft client, QolSkyblockExtras settings) {
        FIGHT_MARKERS.clear();
        voidgloomPhase = SlayerFightPolicy.VoidgloomPhase.UNKNOWN;
        voidgloomHits = -1;
        sittingBeaconRemaining = 0.0D;
        revenantBoomActive = false;
        tarantulaPhase = SlayerFightPolicy.TarantulaPhase.UNKNOWN;
        tarantulaEggHits = "";
        tarantulaInvincible = lastHatchlingsMillis > 0L
                && System.currentTimeMillis() - lastHatchlingsMillis
                < SlayerFightPolicy.HATCHLINGS_INVINCIBLE_MILLIS;
        vampireTwinclawsActive = false;
        vampireManiaActive = false;
        vampireSteakReady = false;
        vampireManiaRemaining = 0.0D;
        infernoPhase = SlayerFightPolicy.InfernoPhase.UNKNOWN;
        firePillarSeconds = -1;
        firePillarHits = -1;
        ownedInfernoTier = 0;
        ICHOR_BEAMS.clear();
        YANG_GLYPH_BEAMS.clear();
        if (!fightOverlaysEnabled(settings) || client.level == null || client.player == null) {
            SITTING_BEACONS.clear();
            FLYING_BEACON_PATHS.clear();
            yangGlyphClaimed = false;
            yangGlyphThrowOrigin = null;
            return;
        }
        ClientLevel level = client.level;
        long now = System.currentTimeMillis();
        cacheOwnedBossPositions(level, now);
        cacheForeignVoidgloomPositions(level, now);
        for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
            if (active.owned() && active.descriptor().type() == SlayerPolicy.SlayerType.INFERNO) {
                ownedInfernoTier = Math.max(ownedInfernoTier, active.descriptor().tier());
            }
        }
        boolean fightingVoidgloom = cachedOwnedPosition(SlayerPolicy.SlayerType.VOIDGLOOM) != null
                || localQuestFamily(SlayerPolicy.SlayerType.VOIDGLOOM)
                || now < yangGlyphThrownUntilMillis;
        Vec3 ownedSven = cachedOwnedPosition(SlayerPolicy.SlayerType.SVEN);
        boolean nearOwnedSven = ownedSven != null
                && client.player.position().distanceTo(ownedSven) <= 24.0D;
        Set<Integer> liveBeacons = new HashSet<>();
        Set<Long> liveSitting = new HashSet<>();
        AABB search = client.player.getBoundingBox().inflate(48.0D, 32.0D, 48.0D);
        for (Entity entity : level.getEntities(client.player, search)) {
            if (entity instanceof ArmorStand stand) {
                observeStandMarker(
                        client, settings, stand, liveBeacons, liveSitting,
                        fightingVoidgloom, now);
            }
            if (entity instanceof Wolf wolf
                    && settings.slayerSvenEnabled
                    && settings.slayerSvenHighlightPups
                    && nearOwnedSven
                    && (SlayerFightPolicy.isPupName(name(wolf)) || wolf.isBaby())) {
                addTrackedMarker(wolf, 0.0D,
                        settings.slayerSvenPupColor,
                        settings.slayerSvenPupLine,
                        settings.slayerSvenWorldLabels ? "Sven Pup" : "");
            }
            if (settings.slayerSvenEnabled
                    && settings.slayerSvenHowlWarning
                    && nearOwnedSven
                    && SlayerFightPolicy.isHowlHologram(name(entity))) {
                long nowHowl = System.currentTimeMillis();
                if (nowHowl - lastHowlMillis > 2_000L) {
                    lastHowlMillis = nowHowl;
                    showFightTitle(client, "Howl");
                }
            }
            if (settings.slayerVoidgloomEnabled
                    && nearOwnedBoss(level, SlayerPolicy.SlayerType.VOIDGLOOM, entity, now)) {
                observeVoidgloomEntity(client, settings, entity, now);
            }
            if (settings.slayerRevenantEnabled
                    && SlayerFightPolicy.isBoomHologram(name(entity))
                    && nearOwnedBoss(level, SlayerPolicy.SlayerType.REVENANT, entity, now)) {
                revenantBoomActive = true;
            }
            if (settings.slayerTarantulaEnabled
                    && nearOwnedBoss(level, SlayerPolicy.SlayerType.TARANTULA, entity, now)) {
                observeTarantulaEntity(entity);
            }
            if (settings.slayerVampireMarkersEnabled
                    && nearOwnedBoss(level, SlayerPolicy.SlayerType.VAMPIRE, entity, now)) {
                observeVampireEntity(client, settings, entity, now);
            }
            if (settings.slayerInfernoEnabled
                    && nearOwnedBoss(level, SlayerPolicy.SlayerType.INFERNO, entity, now)) {
                observeInfernoEntity(client, settings, entity, now);
            }
        }
        adoptBeaconsNearFlyingPaths(level, liveSitting, now);
        SEEN_BEACON_STANDS.retainAll(liveBeacons);
        FLYING_BEACON_PATHS.keySet().retainAll(liveBeacons);
        pruneAndPaintSittingBeacons(level, settings, now, liveSitting);
        if (SITTING_BEACONS.isEmpty() && FLYING_BEACON_PATHS.isEmpty()) {
            yangGlyphClaimed = false;
            if (now >= yangGlyphThrownUntilMillis) {
                yangGlyphThrowOrigin = null;
            }
        }
        if (settings.slayerVoidgloomEnabled && settings.slayerVoidgloomLineToBoss) {
            float bossWidth = SlayerFightPolicy.clampLineWidth(settings.slayerVoidgloomBossLineWidth);
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.VOIDGLOOM) {
                    continue;
                }
                Entity boss = level.getEntity(active.entityId());
                if (boss != null) {
                    addTrackedMarker(boss, 0.05D,
                            0xFF55FFFF, true, "", 0xFF55FFFF, bossWidth);
                }
            }
        }
        if (settings.slayerSvenEnabled && settings.slayerSvenLineToBoss) {
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.SVEN) {
                    continue;
                }
                Entity boss = level.getEntity(active.entityId());
                if (boss != null) {
                    addTrackedMarker(boss, 0.05D,
                            settings.slayerSvenPupColor, true, "");
                }
            }
        }
        if (settings.slayerRevenantEnabled && settings.slayerRevenantLineToBoss) {
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.REVENANT) {
                    continue;
                }
                Entity boss = level.getEntity(active.entityId());
                if (boss != null) {
                    addTrackedMarker(boss, 0.05D,
                            settings.slayerRevenantBoomColor, true, "");
                }
            }
        }
        if (settings.slayerRevenantEnabled
                && settings.slayerRevenantBoomHighlight
                && revenantBoomActive) {
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.REVENANT) {
                    continue;
                }
                Entity boss = level.getEntity(active.entityId());
                if (boss != null) {
                    addTrackedMarker(boss, 0.2D,
                            settings.slayerRevenantBoomColor,
                            false,
                            settings.slayerRevenantWorldLabels ? "BOOM" : "");
                }
            }
        }
        if (settings.slayerTarantulaEnabled) {
            float bossWidth = SlayerFightPolicy.clampLineWidth(settings.slayerTarantulaBossLineWidth);
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.TARANTULA) {
                    continue;
                }
                Entity boss = level.getEntity(active.entityId());
                if (boss == null) {
                    continue;
                }
                if (tarantulaInvincible) {
                    Vec3 pos = boss.position();
                    if (lastHatchlingsPos == null) {
                        lastHatchlingsPos = pos;
                    } else if (pos.distanceTo(lastHatchlingsPos) > 0.35D) {
                        tarantulaInvincible = false;
                        lastHatchlingsMillis = 0L;
                        lastHatchlingsPos = null;
                    }
                }
                if (settings.slayerTarantulaLineToBoss && active.owned()) {
                    addTrackedMarker(boss, 0.05D,
                            settings.slayerTarantulaEggColor, true, "",
                            settings.slayerTarantulaEggColor, bossWidth);
                }
                if (settings.slayerTarantulaHighlightInvincible && tarantulaInvincible) {
                    addTrackedMarker(boss, 0.2D,
                            settings.slayerTarantulaInvincibleColor,
                            false,
                            settings.slayerTarantulaWorldLabels
                                    || settings.slayerTarantulaInvincibleText
                                    ? "Kill hatchlings!" : "");
                }
            }
        }
        if (settings.slayerVampireMarkersEnabled) {
            float bossWidth = SlayerFightPolicy.clampLineWidth(settings.slayerVampireMarkersBossLineWidth);
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.VAMPIRE) {
                    continue;
                }
                Entity boss = level.getEntity(active.entityId());
                if (boss == null) {
                    continue;
                }
                Entity vehicle = boss.getVehicle();
                if (vehicle != null) {
                    double remaining = SlayerFightPolicy.maniaRemainingSeconds(vehicle.tickCount);
                    if (remaining > 0.0D) {
                        vampireManiaActive = true;
                        vampireManiaRemaining = remaining;
                    }
                }
                if (settings.slayerVampireMarkersLineToBoss) {
                    addTrackedMarker(boss, 0.05D,
                            settings.slayerVampireMarkersIchorColor, true, "",
                            settings.slayerVampireMarkersIchorColor, bossWidth);
                }
                if (vampireSteakReady) {
                    addTrackedMarker(boss, 0.2D,
                            settings.slayerVampireMarkersSteakColor,
                            false,
                            settings.slayerVampireMarkersWorldLabels ? "Steak!" : "");
                }
            }
            if (!vampireTwinclawsActive) {
                twinClawsReadyAtMillis = 0L;
            }
        }
        if (settings.slayerInfernoEnabled) {
            for (SlayerSessionEngine.ActiveBoss active : ENGINE.snapshot(now).activeBosses()) {
                if (!active.owned() || active.descriptor().type() != SlayerPolicy.SlayerType.INFERNO) {
                    continue;
                }
                ownedInfernoTier = Math.max(ownedInfernoTier, active.descriptor().tier());
                Entity boss = level.getEntity(active.entityId());
                if (boss == null) {
                    continue;
                }
                int color = settings.slayerInfernoColorByAttunement
                        ? SlayerFightPolicy.attunementColor(active.descriptor().attunement())
                        : settings.slayerInfernoPillarColor;
                if (settings.slayerInfernoLineToBoss || settings.slayerInfernoColorByAttunement) {
                    addTrackedMarker(boss, 0.05D,
                            color,
                            settings.slayerInfernoLineToBoss,
                            "");
                }
            }
            if (!hasOwnedBossOfType(SlayerPolicy.SlayerType.INFERNO, now)) {
                infernoMaxHealth = 0.0D;
                lastInfernoHealth = 0.0D;
            }
        }
    }

    private static void observeVampireEntity(
            Minecraft client,
            QolSkyblockExtras settings,
            Entity entity,
            long now) {
        String hologram = name(entity);
        if (SlayerFightPolicy.isManiaHologram(hologram)) {
            vampireManiaActive = true;
            if (settings.slayerVampireMarkersMania && now - lastManiaMillis > 20_000L) {
                lastManiaMillis = now;
                showFightTitle(client, "Mania");
            }
            if (settings.slayerVampireMarkersWorldLabels) {
                addTrackedMarker(entity, 0.2D,
                        settings.slayerVampireMarkersSteakColor, false, "Mania");
            }
        }
        if (SlayerFightPolicy.isSteakReady(hologram)) {
            vampireSteakReady = true;
            if (settings.slayerVampireMarkersSteakAlert && now - lastSteakMillis > 8_000L) {
                lastSteakMillis = now;
                showFightTitle(client, "Steak!");
            }
        }
        if (settings.slayerVampireMarkersChalice
                && SlayerPolishPolicy.isChaliceHologram(hologram)
                && inVampireSoundArea()) {
            addTrackedMarker(entity, 0.25D,
                    settings.slayerVampireMarkersChaliceColor,
                    false,
                    settings.slayerVampireMarkersWorldLabels ? "Chalice" : "");
        }
    }

    private static void observeInfernoEntity(
            Minecraft client,
            QolSkyblockExtras settings,
            Entity entity,
            long now) {
        String hologram = name(entity);
        Optional<SlayerFightPolicy.HealthReading> health = SlayerFightPolicy.healthReading(hologram);
        if (health.isEmpty()) {
            return;
        }
        String text = hologram.toLowerCase(Locale.ROOT);
        if (!text.contains("inferno")
                && !text.contains("demonlord")
                && !text.contains("quazii")
                && !text.contains("typhoeus")) {
            return;
        }
        double current = health.get().current();
        double max = health.get().max();
        if (infernoMaxHealth <= 0.0D || max > infernoMaxHealth) {
            infernoMaxHealth = max;
        }
        if (current > infernoMaxHealth) {
            infernoMaxHealth = current;
        }
        int tier = ownedInfernoTier > 0 ? ownedInfernoTier : 4;
        infernoPhase = SlayerFightPolicy.infernoPhase(tier, current, infernoMaxHealth);
        if (settings.slayerInfernoFirePits
                && SlayerFightPolicy.crossedFirePits(tier, lastInfernoHealth, current, infernoMaxHealth)
                && now - lastFirePitsMillis > 4_000L) {
            lastFirePitsMillis = now;
            showFightTitle(client, "Fire Pits!");
            if (client.player != null) {
                client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.8F);
            }
        }
        lastInfernoHealth = current;
    }

    private static void observeTarantulaEntity(Entity entity) {
        String hologram = name(entity);
        SlayerFightPolicy.TarantulaPhase phase = SlayerFightPolicy.tarantulaPhase(hologram);
        if (phase != SlayerFightPolicy.TarantulaPhase.UNKNOWN) {
            tarantulaPhase = phase;
        }
        SlayerFightPolicy.eggHitsLabel(hologram).ifPresent(hits -> tarantulaEggHits = hits);
        if (SlayerFightPolicy.isInvincibleHologram(hologram)) {
            tarantulaInvincible = true;
        }
    }

    private static void observeVoidgloomEntity(
            Minecraft client,
            QolSkyblockExtras settings,
            Entity entity,
            long now) {
        Vec3 ownedPos = cachedOwnedPosition(SlayerPolicy.SlayerType.VOIDGLOOM);
        boolean ownedEnderman = entity instanceof EnderMan
                && entity.getId() == scanOwnedVoidgloomEntityId;
        double distOwned = ownedPos == null ? Double.NaN : entity.position().distanceTo(ownedPos);
        boolean onOwnedFight = ownedEnderman
                || (ownedPos != null
                && distOwned <= SlayerHighlightPolicy.LOCAL_FIGHT_MARKER_RANGE
                && SlayerHighlightPolicy.closerToLocalBoss(
                        distOwned, nearestForeignVoidgloomDistance(entity.position())));
        if (!onOwnedFight) {
            return;
        }
        String hologram = name(entity);
        SlayerFightPolicy.VoidgloomPhase phase = SlayerFightPolicy.voidgloomPhase(hologram);
        if (phase != SlayerFightPolicy.VoidgloomPhase.UNKNOWN) {
            voidgloomPhase = phase;
        }
        if (phase == SlayerFightPolicy.VoidgloomPhase.BEACON && !lastOwnedHeldBeacon) {
            noteOwnedYangGlyphThrow(ownedPos != null ? ownedPos : entity.position(), now);
        }
        SlayerFightPolicy.hitsRemaining(hologram).ifPresent(hits -> voidgloomHits = hits);
        SlayerFightPolicy.compactHealth(hologram).ifPresent(health -> voidgloomHealth = health);
        if (!(entity instanceof EnderMan enderman)) {
            return;
        }
        boolean ownedVoidgloom = ownedEnderman;
        if (settings.slayerVoidgloomHighlightHeld && ownedVoidgloom && holdingBeacon(enderman)) {
            addTrackedMarker(enderman, 0.12D,
                    settings.slayerVoidgloomBeaconColor,
                    settings.slayerVoidgloomBeaconLine,
                    settings.slayerVoidgloomWorldLabels ? "Yang Glyph" : "",
                    settings.slayerVoidgloomLineColor,
                    SlayerFightPolicy.clampLineWidth(settings.slayerVoidgloomLineWidth));
        }
        boolean holding = holdingBeacon(enderman);
        if (ownedVoidgloom && lastOwnedHeldBeacon && !holding) {
            yangGlyphClaimed = false;
            yangGlyphThrowOrigin = enderman.position();
            yangGlyphThrownUntilMillis = now + 8_000L;
        }
        if (ownedVoidgloom) {
            lastOwnedHeldBeacon = holding;
        }
        if (ownedVoidgloom
                && (settings.slayerVoidgloomLaserTimer || settings.slayerVoidgloomLaserHealth)
                && laserEndsAtMillis > now) {
            String label = "";
            if (settings.slayerVoidgloomLaserTimer) {
                label = SlayerFightPolicy.countdownLabel((laserEndsAtMillis - now) / 1000.0D);
            }
            if (settings.slayerVoidgloomLaserHealth && !voidgloomHealth.isBlank()) {
                label = label.isBlank() ? voidgloomHealth : label + " · " + voidgloomHealth;
            }
            if (!label.isBlank()) {
                addTrackedMarker(enderman, 0.08D,
                        0xFF55FFFF,
                        false,
                        label);
            }
        }
        if (ownedVoidgloom
                && (settings.slayerVoidgloomLaserTimer || settings.slayerVoidgloomLaserHealth)
                && enderman.getVehicle() instanceof ArmorStand
                && now - lastLaserStartMillis >= SlayerFightPolicy.LASER_REARM_MILLIS) {
            lastLaserStartMillis = now;
            laserEndsAtMillis = now + SlayerFightPolicy.LASER_DURATION_MILLIS;
            voidgloomPhase = SlayerFightPolicy.VoidgloomPhase.LASER;
        }
    }

    private static boolean holdingBeacon(EnderMan enderman) {
        BlockState carried = enderman.getCarriedBlock();
        return carried != null && carried.is(Blocks.BEACON);
    }

    private static void tickVoidgloomLaser(Minecraft client, QolSkyblockExtras settings) {
        if (!settings.slayerVoidgloomEnabled
                || (!settings.slayerVoidgloomLaserTimer && !settings.slayerVoidgloomLaserHealth)) {
            laserEndsAtMillis = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (laserEndsAtMillis > 0L && now >= laserEndsAtMillis) {
            laserEndsAtMillis = 0L;
        }
        if (client != null && client.level != null && laserEndsAtMillis > now) {
            voidgloomPhase = SlayerFightPolicy.VoidgloomPhase.LASER;
        }
    }

    private static void tickGummyWarning(Minecraft client, QolSkyblockExtras settings) {
        gummyNeeded = false;
        if (!settings.slayerInfernoEnabled || !settings.slayerInfernoGummyWarning
                || client == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String sidebar = SkyBlockSidebar.text();
        boolean habanero = false;
        for (EquipmentSlot slot : List.of(
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            CompoundTag extra = SkyBlockItemData.extraAttributes(client.player.getItemBySlot(slot));
            if (extra == null) {
                continue;
            }
            CompoundTag enchants = extra.getCompound("enchantments").orElse(null);
            if (enchants == null) {
                continue;
            }
            for (String key : enchants.keySet()) {
                if (SlayerPolishPolicy.hasHabaneroEnchant(key)) {
                    habanero = true;
                    break;
                }
            }
        }
        boolean gummyActive = SlayerPolishPolicy.isGummyActive(gummyExpiresAtMillis, now, sidebar);
        gummyNeeded = SlayerPolishPolicy.shouldWarnGummy(
                habanero, SlayerPolishPolicy.isSmolderingArea(sidebar), gummyActive);
        if (gummyNeeded && now - lastGummyWarnMillis >= SlayerPolishPolicy.GUMMY_WARN_INTERVAL_MILLIS) {
            lastGummyWarnMillis = now;
            showFightTitle(client, "No Polar Bear!");
        }
    }

    private static List<SlayerPolishPolicy.BlockCoord> unbrokenEffigies(Minecraft client) {
        if (client == null || client.level == null) {
            return List.of();
        }
        String sidebar = SkyBlockSidebar.text();
        if (!SlayerFightPolicy.isVampireSoundArea(sidebar)) {
            return List.of();
        }
        net.minecraft.world.scores.Scoreboard scoreboard = client.level.getScoreboard();
        net.minecraft.world.scores.Objective objective =
                scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR);
        if (objective == null) {
            return List.of();
        }
        for (net.minecraft.world.scores.PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            Component line = net.minecraft.world.scores.PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            if (!SlayerPolishPolicy.isEffigyScoreboardLine(line.getString())) {
                continue;
            }
            List<String> colors = new ArrayList<>();
            line.visit((style, text) -> {
                if (text == null || text.isBlank() || text.toLowerCase(Locale.ROOT).contains("effig")) {
                    return java.util.Optional.empty();
                }
                net.minecraft.network.chat.TextColor color = style.getColor();
                colors.add(color == null ? "" : Integer.toHexString(color.getValue()));
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            return SlayerPolishPolicy.unbrokenEffigies(colors);
        }
        return List.of();
    }

    private static void observeStandMarker(
            Minecraft client,
            QolSkyblockExtras settings,
            ArmorStand stand,
            Set<Integer> liveBeacons,
            Set<Long> liveSitting,
            boolean fightingVoidgloom,
            long now) {
        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        String helmetName = helmet.isEmpty() ? "" : helmet.getHoverName().getString();
        if (nearPowerOrb(client.level, stand)) {
            return;
        }
        var marker = SlayerFightPolicy.markerFromStand(name(stand), helmetName);
        if (marker.isEmpty() && !helmet.isEmpty() && helmet.is(Items.BEACON)) {
            marker = Optional.of(SlayerFightPolicy.Marker.BEACON);
        }
        if (marker.isEmpty() && isNukekubiStand(stand, helmet, fightingVoidgloom)) {
            marker = Optional.of(SlayerFightPolicy.Marker.NUKEKUBI);
        }
        if (marker.isEmpty()) {
            return;
        }
        Optional<SlayerPolicy.SlayerType> family = SlayerFightPolicy.familyForMarker(marker.get());
        if (marker.get() == SlayerFightPolicy.Marker.BEACON) {
            if (!SlayerFightPolicy.isThrownYangGlyphStand(
                    name(stand), helmetName, !helmet.isEmpty() && helmet.is(Items.BEACON))) {
                return;
            }
            if (!nearYangGlyph(client.level, stand, now)) {
                return;
            }
        } else if (family.isPresent()
                && !nearOwnedBoss(client.level, family.get(), stand, now)) {
            return;
        }
        switch (marker.get()) {
            case BEACON -> {
                if (!settings.slayerVoidgloomEnabled) {
                    return;
                }
                liveBeacons.add(stand.getId());
                yangGlyphClaimed = true;
                boolean drawBeacon = settings.slayerVoidgloomHighlightBeacon
                        || settings.slayerVoidgloomBeaconLine
                        || settings.slayerVoidgloomBeaconTimer
                        || settings.slayerVoidgloomBeaconPath
                        || settings.slayerVoidgloomWorldLabels
                        || settings.slayerVoidgloomPhaseDisplay;
                String flyingLabel = settings.slayerVoidgloomWorldLabels ? "Beacon" : "";
                rememberFlyingBeacon(stand);
                boolean sitting = highlightSittingBeacon(
                        client.level, stand, liveSitting, now)
                        || adoptLandedFlyingBeacon(stand, liveSitting, now);
                if (sitting) {
                    FLYING_BEACON_PATHS.remove(stand.getId());
                }
                if (drawBeacon && !sitting) {
                    addTrackedMarker(stand, 0.2D,
                            settings.slayerVoidgloomBeaconColor,
                            settings.slayerVoidgloomBeaconLine,
                            flyingLabel,
                            settings.slayerVoidgloomLineColor,
                            SlayerFightPolicy.clampLineWidth(settings.slayerVoidgloomLineWidth));
                }
                if (settings.slayerVoidgloomBeaconWarning
                        && SEEN_BEACON_STANDS.add(stand.getId())
                        && now - lastBeaconWarningMillis > 1_500L) {
                    lastBeaconWarningMillis = now;
                    showFightTitle(client, "Yang Glyph!");
                    if (settings.slayerVoidgloomBeaconSound && client.player != null) {
                        client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.5F);
                    }
                }
            }
            case NUKEKUBI -> {
                if (!settings.slayerVoidgloomEnabled) {
                    return;
                }
                boolean drawBox = settings.slayerVoidgloomHighlightNukekubi;
                boolean drawLine = settings.slayerVoidgloomNukekubiLine;
                if (drawBox || drawLine) {
                    addTrackedMarker(stand, 0.55D,
                            settings.slayerVoidgloomNukekubiColor,
                            drawLine,
                            settings.slayerVoidgloomWorldLabels ? "Nukekubi Skull" : "",
                            settings.slayerVoidgloomNukekubiColor,
                            SlayerFightPolicy.clampLineWidth(settings.slayerVoidgloomLineWidth));
                }
            }
            case EGG_SAC -> {
                if (!settings.slayerTarantulaEnabled) {
                    return;
                }
                SlayerFightPolicy.eggHitsLabel(name(stand)).ifPresent(hits -> tarantulaEggHits = hits);
                if (settings.slayerTarantulaHighlightEggSacs) {
                    String label = "";
                    if (settings.slayerTarantulaWorldLabels || settings.slayerTarantulaEggHits) {
                        label = SlayerFightPolicy.eggHitsLabel(name(stand)).orElse(
                                settings.slayerTarantulaWorldLabels ? "Egg Sac" : "");
                    }
                    addTrackedMarker(stand, 0.2D,
                            settings.slayerTarantulaEggColor, false, label);
                }
            }
            case INVINCIBLE -> {
                if (!settings.slayerTarantulaEnabled) {
                    return;
                }
                tarantulaInvincible = true;
                if (settings.slayerTarantulaHighlightInvincible) {
                    addTrackedMarker(stand, 0.35D,
                            settings.slayerTarantulaInvincibleColor,
                            false,
                            settings.slayerTarantulaWorldLabels
                                    || settings.slayerTarantulaInvincibleText
                                    ? "Kill hatchlings!" : "");
                }
            }
            case BOOM -> {
                if (!settings.slayerRevenantEnabled) {
                    return;
                }
                revenantBoomActive = true;
                if (settings.slayerRevenantBoomHighlight) {
                    addTrackedMarker(stand, 0.25D,
                            settings.slayerRevenantBoomColor,
                            false,
                            settings.slayerRevenantWorldLabels ? "BOOM" : "");
                }
                if (settings.slayerRevenantBoomDisplay && now - lastBoomMillis > 1_500L) {
                    lastBoomMillis = now;
                    showFightTitle(client, "BOOM");
                    if (settings.slayerRevenantBoomSound && client.player != null) {
                        client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.35F);
                    }
                }
            }
            case PUP -> {
                if (settings.slayerSvenEnabled && settings.slayerSvenHighlightPups) {
                    addTrackedMarker(stand, 0.15D,
                            settings.slayerSvenPupColor,
                            settings.slayerSvenPupLine,
                            settings.slayerSvenWorldLabels ? "Sven Pup" : "");
                }
            }
            case BLOOD_ICHOR -> {
                if (settings.slayerVampireMarkersEnabled && settings.slayerVampireMarkersBloodIchor) {
                    addTrackedMarker(stand, 0.2D,
                            settings.slayerVampireMarkersIchorColor,
                            true,
                            settings.slayerVampireMarkersWorldLabels ? "Blood Ichor" : "");
                    if (settings.slayerVampireMarkersIchorBeam) {
                        ICHOR_BEAMS.add(stand.position());
                    }
                }
            }
            case KILLER_SPRING -> {
                if (settings.slayerVampireMarkersEnabled && settings.slayerVampireMarkersKillerSpring) {
                    addTrackedMarker(stand, 0.2D,
                            settings.slayerVampireMarkersSpringColor,
                            true,
                            settings.slayerVampireMarkersWorldLabels ? "Killer Spring" : "");
                }
            }
            case TWINCLAWS -> {
                if (!settings.slayerVampireMarkersEnabled) {
                    return;
                }
                vampireTwinclawsActive = true;
                if (settings.slayerVampireMarkersWorldLabels) {
                    addTrackedMarker(stand, 0.2D,
                            settings.slayerVampireMarkersIchorColor, false, "Twinclaws");
                }
                if (!settings.slayerVampireMarkersTwinclaws) {
                    return;
                }
                long delay = Math.max(0L, settings.slayerVampireMarkersTwinclawsDelay);
                if (twinClawsReadyAtMillis == 0L) {
                    twinClawsReadyAtMillis = now + delay;
                }
                if (now >= twinClawsReadyAtMillis && lastTwinclawsMillis < twinClawsReadyAtMillis) {
                    lastTwinclawsMillis = now;
                    showFightTitle(client, "Twinclaws");
                }
            }
            case FIRE_PILLAR -> {
                if (!settings.slayerInfernoEnabled) {
                    return;
                }
                firePillarSeconds = SlayerFightPolicy.firePillarSeconds(name(stand)).orElse(-1);
                firePillarHits = SlayerFightPolicy.firePillarHits(name(stand)).orElse(-1);
                if (settings.slayerInfernoFirePillar || settings.slayerInfernoWorldLabels) {
                    String label = settings.slayerInfernoWorldLabels
                            ? (firePillarSeconds >= 0 ? firePillarSeconds + "s Fire Pillar" : "Fire Pillar")
                            : "";
                    addTrackedMarker(stand, 0.35D,
                            settings.slayerInfernoPillarColor,
                            true,
                            label);
                }
                if (settings.slayerInfernoFirePillar
                        && firePillarSeconds >= 0
                        && firePillarSeconds <= 5
                        && now - lastFirePillarWarnMillis > 900L) {
                    lastFirePillarWarnMillis = now;
                    showFightTitle(client, firePillarSeconds + "s " + Math.max(0, firePillarHits) + " hits");
                    if (settings.slayerInfernoFirePillarSound && client.player != null) {
                        client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.7F);
                    }
                }
            }
        }
    }

    private static boolean isNukekubiStand(ArmorStand stand, ItemStack helmet, boolean fightingVoidgloom) {
        if (helmet.isEmpty()) {
            return false;
        }
        if (SlayerFightPolicy.isNukekubiTexture(skullTexture(helmet))) {
            return true;
        }
        return (fightingVoidgloom || localQuestFamily(SlayerPolicy.SlayerType.VOIDGLOOM))
                && stand.isMarker()
                && helmet.is(Items.PLAYER_HEAD);
    }

    private static String skullTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile() == null) {
            return stack.toString();
        }
        StringBuilder out = new StringBuilder();
        for (Property property : profile.partialProfile().properties().get("textures")) {
            out.append(property.value());
        }
        return out.length() == 0 ? stack.toString() : out.toString();
    }

    private static boolean nearPowerOrb(ClientLevel level, Entity stand) {
        if (level == null || stand == null) {
            return false;
        }
        if (SlayerFightPolicy.isPowerOrbHologram(name(stand))) {
            return true;
        }
        AABB box = stand.getBoundingBox().inflate(2.8D, 3.5D, 2.8D);
        for (ArmorStand other : level.getEntitiesOfClass(ArmorStand.class, box)) {
            if (other != stand && SlayerFightPolicy.isPowerOrbHologram(name(other))) {
                return true;
            }
        }
        return false;
    }

    private static boolean highlightSittingBeacon(
            ClientLevel level,
            ArmorStand stand,
            Set<Long> liveSitting,
            long now) {
        boolean found = false;
        BlockPos origin = stand.blockPosition();
        int scan = SlayerFightPolicy.YANG_GLYPH_SIT_SCAN;
        for (int dx = -scan; dx <= scan; dx++) {
            for (int dy = -scan; dy <= scan; dy++) {
                for (int dz = -scan; dz <= scan; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).is(Blocks.BEACON)) {
                        continue;
                    }
                    found = true;
                    adoptSittingBeacon(pos, now, liveSitting);
                }
            }
        }
        return found;
    }

    private static boolean adoptLandedFlyingBeacon(
            ArmorStand stand,
            Set<Long> liveSitting,
            long now) {
        List<Vec3> path = FLYING_BEACON_PATHS.get(stand.getId());
        if (path == null || path.isEmpty()) {
            return false;
        }
        Vec3 last = path.get(path.size() - 1);
        Vec3 first = path.get(0);
        Vec3 here = stand.position();
        if (!SlayerFightPolicy.flyingBeaconLanded(
                first.distanceToSqr(here), last.distanceToSqr(here))) {
            return false;
        }
        adoptSittingBeacon(stand.blockPosition(), now, liveSitting);
        return true;
    }

    private static void adoptBeaconsNearFlyingPaths(
            ClientLevel level,
            Set<Long> liveSitting,
            long now) {
        if (level == null) {
            return;
        }
        int scan = SlayerFightPolicy.YANG_GLYPH_SIT_SCAN;
        for (List<Vec3> path : FLYING_BEACON_PATHS.values()) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            Vec3 last = path.get(path.size() - 1);
            BlockPos origin = BlockPos.containing(last);
            for (int dx = -scan; dx <= scan; dx++) {
                for (int dy = -scan; dy <= scan; dy++) {
                    for (int dz = -scan; dz <= scan; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (level.getBlockState(pos).is(Blocks.BEACON)) {
                            adoptSittingBeacon(pos, now, liveSitting);
                        }
                    }
                }
            }
        }
    }

    private static void adoptSittingBeacon(BlockPos pos, long now, Set<Long> liveSitting) {
        if (pos == null) {
            return;
        }
        long key = pos.asLong();
        if (liveSitting != null) {
            liveSitting.add(key);
        }
        SITTING_BEACONS.putIfAbsent(key, now);
    }

    private static void pruneAndPaintSittingBeacons(
            ClientLevel level,
            QolSkyblockExtras settings,
            long now,
            Set<Long> liveSitting) {
        if (level == null) {
            return;
        }
        SITTING_BEACONS.entrySet().removeIf(entry -> {
            BlockPos pos = BlockPos.of(entry.getKey());
            if (level.getBlockState(pos).is(Blocks.BEACON) || liveSitting.contains(entry.getKey())) {
                return false;
            }
            long age = now - entry.getValue();
            return age > SlayerFightPolicy.SITTING_BEACON_MILLIS + 400L;
        });
        sittingBeaconRemaining = 0.0D;
        boolean draw = settings.slayerVoidgloomEnabled
                && (settings.slayerVoidgloomHighlightBeacon
                || settings.slayerVoidgloomBeaconTimer
                || settings.slayerVoidgloomBeaconLine
                || settings.slayerVoidgloomWorldLabels);
        float lineWidth = SlayerFightPolicy.clampLineWidth(settings.slayerVoidgloomLineWidth);
        for (Map.Entry<Long, Long> entry : SITTING_BEACONS.entrySet()) {
            BlockPos pos = BlockPos.of(entry.getKey());
            boolean beaconBlock = level.getBlockState(pos).is(Blocks.BEACON);
            if (!beaconBlock && !liveSitting.contains(entry.getKey())) {
                continue;
            }
            double remaining = SlayerFightPolicy.remainingSeconds(
                    entry.getValue(), now, SlayerFightPolicy.SITTING_BEACON_MILLIS);
            sittingBeaconRemaining = Math.max(sittingBeaconRemaining, remaining);
            if (!draw) {
                continue;
            }
            String label = "";
            if (settings.slayerVoidgloomBeaconTimer && remaining > 0.0D) {
                label = "Yang Glyph " + SlayerFightPolicy.countdownLabel(remaining);
            } else if (settings.slayerVoidgloomWorldLabels) {
                label = "Yang Glyph";
            }
            addMarker(new AABB(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D),
                    settings.slayerVoidgloomBeaconColor,
                    settings.slayerVoidgloomBeaconLine,
                    label,
                    settings.slayerVoidgloomLineColor,
                    lineWidth);
            YANG_GLYPH_BEAMS.add(Vec3.atCenterOf(pos));
        }
    }

    static void onBlockUpdate(BlockPos pos, BlockState newState) {
        if (pos == null || newState == null) {
            return;
        }
        QolSkyblockExtras extras = settings();
        if (!extras.slayerVoidgloomEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long key = pos.asLong();
        if (!newState.is(Blocks.BEACON)) {
            SITTING_BEACONS.remove(key);
            return;
        }
        Vec3 at = Vec3.atCenterOf(pos);
        if (client.player.distanceToSqr(at) > SlayerHighlightPolicy.YANG_GLYPH_PLAYER_RANGE
                * SlayerHighlightPolicy.YANG_GLYPH_PLAYER_RANGE) {
            return;
        }
        boolean nearPath = nearTrackedYangGlyphPath(pos);
        Vec3 origin = yangGlyphThrowOrigin != null
                ? yangGlyphThrowOrigin
                : cachedOwnedPosition(SlayerPolicy.SlayerType.VOIDGLOOM);
        SlayerHighlightPolicy.YangGlyphTrack probe = new SlayerHighlightPolicy.YangGlyphTrack(
                false,
                yangGlyphClaimed,
                now < yangGlyphThrownUntilMillis,
                origin != null,
                origin == null ? Double.NaN : at.distanceTo(origin),
                nearestForeignVoidgloomDistance(at),
                client.player.position().distanceTo(at));
        if (!SlayerHighlightPolicy.shouldAdoptSittingYangGlyph(nearPath, probe)) {
            return;
        }
        SITTING_BEACONS.putIfAbsent(key, now);
        yangGlyphClaimed = true;
    }

    static void observeContainer(AbstractContainerScreen<?> screen) {
        ClientBoundaryGuard.run("SLAYER_RNG_CONTAINER", () -> observeRngContainer(screen));
    }

    private static void observeRngContainer(AbstractContainerScreen<?> screen) {
        QolSkyblockExtras extras = settings();
        if (!extras.slayerDropsEnabled || screen == null) {
            return;
        }
        String title = screen.getTitle().getString();
        List<SlayerRngMeterPolicy.SlotView> slots = new ArrayList<>();
        List<Slot> menuSlots = screen.getMenu().slots;
        Object playerInventory = Minecraft.getInstance().player == null
                ? null
                : Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < menuSlots.size(); i++) {
            Slot slot = menuSlots.get(i);
            if (slot == null || slot.container == playerInventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            slots.add(new SlayerRngMeterPolicy.SlotView(
                    slot.index,
                    stack.getHoverName().getString(),
                    InventoryChromeRuntime.loreLines(stack)));
        }
        SlayerRngMeterPolicy.fromRngMeterInventory(title, slots).ifPresent(SlayerRuntime::applyRngSelection);
        SlayerRngMeterPolicy.fromSlayerMenu(title, slots).ifPresent(SlayerRuntime::applyRngSelection);
    }

    private static void applyRngSelection(SlayerRngMeterPolicy.Selection selection) {
        if (selection == null) {
            return;
        }
        SlayerPolicy.SlayerType family = selection.family();
        if (!selection.hasItem()) {
            if (family == null || selectedRngDrop == null || selectedRngDrop.type() == family) {
                selectedRngDrop = null;
                lastRngStoredXp = -1L;
            }
            persistRngSelection(family, null, -1L);
            return;
        }
        Optional<SlayerRngCatalog.Entry> resolved =
                SlayerRngCatalog.resolve(selection.itemName(), family);
        if (resolved.isEmpty()) {
            if (family != null) {
                persistRawRngSelection(family, selection.itemName(), selection.storedXp());
            }
            return;
        }
        SlayerRngCatalog.Entry entry = resolved.get();
        selectedRngDrop = entry;
        if (selection.storedXp() >= 0L) {
            lastRngStoredXp = selection.storedXp();
        }
        persistRngSelection(entry.type(), entry, lastRngStoredXp);
    }

    private static SlayerPolicy.SlayerType currentRngFamily() {
        if (latestQuest != null) {
            return latestQuest.type();
        }
        return selectedRngDrop == null ? null : selectedRngDrop.type();
    }

    private static boolean rngDropKnown() {
        activateStoredRngDrop(currentRngFamily());
        if (selectedRngDrop != null) {
            return true;
        }
        SlayerPolicy.SlayerType family = currentRngFamily();
        if (family == null) {
            return false;
        }
        Map<String, String> selected = settings().slayerRngMeterSelectedByFamily;
        if (selected == null) {
            return false;
        }
        String id = selected.get(family.name());
        return id != null && !id.isBlank();
    }

    private static void activateStoredRngDrop(SlayerPolicy.SlayerType family) {
        if (family == null) {
            return;
        }
        if (selectedRngDrop != null && selectedRngDrop.type() == family) {
            return;
        }
        QolSkyblockExtras extras = settings();
        Map<String, String> selected = extras.slayerRngMeterSelectedByFamily;
        String id = selected == null ? null : selected.get(family.name());
        if (id == null || id.isBlank()) {
            if (selectedRngDrop != null && selectedRngDrop.type() != family) {
                selectedRngDrop = null;
                lastRngStoredXp = -1L;
            }
            return;
        }
        Optional<SlayerRngCatalog.Entry> entry = SlayerRngCatalog.byId(id)
                .or(() -> SlayerRngCatalog.resolve(id, family));
        if (entry.isEmpty()) {
            return;
        }
        selectedRngDrop = entry.get();
        Map<String, Long> stored = extras.slayerRngMeterStoredXpByFamily;
        if (stored != null && stored.get(family.name()) != null) {
            lastRngStoredXp = stored.get(family.name());
        }
    }

    private static void persistRngSelection(
            SlayerPolicy.SlayerType family,
            SlayerRngCatalog.Entry entry,
            long storedXp) {
        if (family == null && entry != null) {
            family = entry.type();
        }
        persistRawRngSelection(
                family,
                entry == null ? "" : entry.skyBlockId(),
                storedXp);
    }

    private static void persistRawRngSelection(
            SlayerPolicy.SlayerType family,
            String itemId,
            long storedXp) {
        if (family == null) {
            return;
        }
        QolSkyblockExtras extras = settings();
        if (extras.slayerRngMeterSelectedByFamily == null) {
            extras.slayerRngMeterSelectedByFamily = new LinkedHashMap<>();
        }
        if (extras.slayerRngMeterStoredXpByFamily == null) {
            extras.slayerRngMeterStoredXpByFamily = new LinkedHashMap<>();
        }
        String nextId = itemId == null ? "" : itemId.trim();
        String previousId = extras.slayerRngMeterSelectedByFamily.get(family.name());
        Long previousXp = extras.slayerRngMeterStoredXpByFamily.get(family.name());
        boolean changed = false;
        if (nextId.isBlank()) {
            changed = extras.slayerRngMeterSelectedByFamily.remove(family.name()) != null;
            changed = extras.slayerRngMeterStoredXpByFamily.remove(family.name()) != null || changed;
        } else {
            if (!nextId.equals(previousId)) {
                extras.slayerRngMeterSelectedByFamily.put(family.name(), nextId);
                changed = true;
            }
            if (storedXp >= 0L && !Objects.equals(previousXp, storedXp)) {
                extras.slayerRngMeterStoredXpByFamily.put(family.name(), storedXp);
                changed = true;
            }
        }
        if (changed) {
            RotClientClient.save();
        }
    }

    private static boolean nearYangGlyph(ClientLevel level, Entity entity, long now) {
        if (level == null || entity == null) {
            return false;
        }
        boolean alreadyTracking = FLYING_BEACON_PATHS.containsKey(entity.getId())
                || SEEN_BEACON_STANDS.contains(entity.getId());
        Vec3 origin = yangGlyphThrowOrigin != null
                ? yangGlyphThrowOrigin
                : (scanOwnedAt == now && scanOwnedPositions != null
                ? cachedOwnedPosition(SlayerPolicy.SlayerType.VOIDGLOOM)
                : ownedBossPosition(level, SlayerPolicy.SlayerType.VOIDGLOOM, now));
        Minecraft client = Minecraft.getInstance();
        double playerDistance = client != null && client.player != null
                ? entity.distanceTo(client.player)
                : Double.NaN;
        return SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                alreadyTracking,
                yangGlyphClaimed && !alreadyTracking,
                now < yangGlyphThrownUntilMillis,
                origin != null,
                origin == null ? Double.NaN : entity.position().distanceTo(origin),
                nearestForeignVoidgloomDistance(entity.position()),
                playerDistance));
    }

    private static void noteOwnedYangGlyphThrow(Vec3 origin, long now) {
        if (now >= yangGlyphThrownUntilMillis) {
            yangGlyphClaimed = false;
            yangGlyphThrowOrigin = origin;
        } else if (yangGlyphThrowOrigin == null && origin != null) {
            yangGlyphThrowOrigin = origin;
        }
        yangGlyphThrownUntilMillis = Math.max(yangGlyphThrownUntilMillis, now + 8_000L);
    }

    private static boolean nearTrackedYangGlyphPath(BlockPos pos) {
        if (pos == null || FLYING_BEACON_PATHS.isEmpty()) {
            return false;
        }
        Vec3 target = Vec3.atCenterOf(pos);
        double max = SlayerFightPolicy.YANG_GLYPH_SIT_SCAN + 0.85D;
        for (List<Vec3> path : FLYING_BEACON_PATHS.values()) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.get(path.size() - 1).distanceTo(target) <= max) {
                return true;
            }
        }
        return false;
    }

    private static void rememberFlyingBeacon(ArmorStand stand) {
        int id = stand.getId();
        Vec3 point = stand.getEyePosition();
        List<Vec3> path = FLYING_BEACON_PATHS.computeIfAbsent(id, ignored -> new ArrayList<>());
        if (path.isEmpty() || path.get(path.size() - 1).distanceToSqr(point) >= 0.04D) {
            path.add(point);
            if (path.size() > SlayerFightPolicy.MAX_BEACON_PATH_POINTS) {
                path.remove(0);
            }
        }
    }

    private static void addMarker(AABB box, int color, boolean line) {
        addMarker(box, color, line, "");
    }

    private static void addMarker(AABB box, int color, boolean line, String label) {
        addMarker(box, color, line, label, color, 2.0F);
    }

    private static void addMarker(
            AABB box,
            int color,
            boolean line,
            String label,
            int lineColor,
            float lineWidth) {
        FIGHT_MARKERS.add(new FightMarker(
                box, box.getCenter(), color, line, label, lineColor, lineWidth,
                Integer.MIN_VALUE, 0.0D));
    }

    private static void addTrackedMarker(
            Entity entity,
            double inflate,
            int color,
            boolean line,
            String label) {
        addTrackedMarker(entity, inflate, color, line, label, color, 2.0F);
    }

    private static void addTrackedMarker(
            Entity entity,
            double inflate,
            int color,
            boolean line,
            String label,
            int lineColor,
            float lineWidth) {
        if (entity == null) {
            return;
        }
        AABB box = entity.getBoundingBox().inflate(inflate);
        FIGHT_MARKERS.add(new FightMarker(
                box, box.getCenter(), color, line, label, lineColor, lineWidth,
                entity.getId(), inflate));
    }

    private static void showFightTitle(Minecraft client, String text) {
        if (client == null || client.gui == null) {
            return;
        }
        client.gui.hud.setTimes(5, 20, 8);
        client.gui.hud.setTitle(Component.literal(text).withStyle(ChatFormatting.RED));
    }

    private static void onSound(String soundId, QolSkyblockExtras settings) {
        if (!settings.slayerSvenEnabled || !settings.slayerSvenHowlWarning
                || !SlayerFightPolicy.isHowlSound(soundId)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastHowlMillis < 2_000L) {
            return;
        }
        lastHowlMillis = now;
        showFightTitle(Minecraft.getInstance(), "Howl");
    }

    private static void warnWrongQuest(Minecraft client, Entity entity, QolSkyblockExtras settings) {
        if (!settings.slayerQuestWarningEnabled || entity == null || client == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastQuestWarningMillis < 4_000L) {
            return;
        }
        Optional<SlayerFightPolicy.QuestRef> quest = SlayerFightPolicy.questFromSidebar(
                List.of(SkyBlockSidebar.text().split("\n")));
        Optional<SlayerPolicy.SlayerType> family = SlayerFightPolicy.familyFromMobName(name(entity));
        if (quest.isEmpty() || family.isEmpty()
                || !SlayerFightPolicy.wrongQuest(quest.get().type(), family.get())) {
            return;
        }
        lastQuestWarningMillis = now;
        String text = "Wrong Slayer · " + quest.get().type().displayName();
        if (settings.slayerQuestWarningTitle) {
            showFightTitle(client, text);
        }
        if (settings.slayerQuestWarningChat && client.player != null) {
            client.player.sendSystemMessage(RotClientChat.message(text));
        }
    }

    private static void scheduleAutoStart(QolSkyblockExtras settings) {
        if (!QolFlavorSupport.isPlus() || !settings.slayerAutoStartEnabled) {
            return;
        }
        Optional<SlayerFightPolicy.QuestRef> quest = SlayerFightPolicy.questFromSidebar(
                List.of(SkyBlockSidebar.text().split("\n")));
        if (quest.isEmpty()) {
            return;
        }
        autoStartCommand = SlayerFightPolicy.autoStartCommand(quest.get().type(), quest.get().tier());
        autoStartTicks = SlayerFightPolicy.clampAutoStartDelayTicks(settings.slayerAutoStartDelay);
    }

    private static void tickAutoStart(Minecraft client, QolSkyblockExtras settings) {
        if (!QolFlavorSupport.isPlus() || !settings.slayerAutoStartEnabled || autoStartTicks < 0) {
            autoStartTicks = -1;
            return;
        }
        if (autoStartTicks > 0) {
            autoStartTicks--;
            return;
        }
        String command = autoStartCommand;
        autoStartTicks = -1;
        autoStartCommand = "";
        if (command == null || command.isBlank() || client == null || client.player == null) {
            return;
        }
        client.player.connection.sendCommand(command);
    }

    private static String name(Entity entity) {
        if (entity == null) {
            return "";
        }
        Component custom = entity.getCustomName();
        return custom == null ? "" : custom.getString();
    }

    private static QolSkyblockExtras settings() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private static void resetProgress() {
        latestProgress = null;
        PROGRESS_THRESHOLD.reset();
    }

    private static int color(QolSkyblockExtras settings, SlayerPolicy.EntityRole role) {
        return switch (role) {
            case BOSS -> settings.slayerHighlightsBossColor;
            case MINIBOSS -> settings.slayerHighlightsMinibossColor;
            case DEMON -> settings.slayerHighlightsDemonColor;
        };
    }

    private static double width(QolSkyblockExtras settings, SlayerPolicy.EntityRole role) {
        return switch (role) {
            case BOSS -> settings.slayerHighlightsBossWidth;
            case MINIBOSS -> settings.slayerHighlightsMinibossWidth;
            case DEMON -> settings.slayerHighlightsDemonWidth;
        };
    }

    private static String tierSuffix(int tier) {
        return tier <= 0 ? "" : " " + switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    static String duration(long millis) {
        long safe = Math.max(0L, millis);
        long seconds = safe / 1_000L;
        if (seconds < 60L) {
            return String.format(Locale.ROOT, "%.1fs", safe / 1_000.0D);
        }
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
