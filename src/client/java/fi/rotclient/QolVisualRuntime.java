package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Client-side Batch 2 QoL visual controllers. Pure decisions live in policy /
 * predictor helpers; this bridges Minecraft entities to those helpers.
 */
public final class QolVisualRuntime {
    private static EtherwarpPredictor.Target lastSoundTarget;
    private static long lastSoundAtMs;
    private static int tickGeneration;
    private static int cachedFlagGeneration = Integer.MIN_VALUE;
    private static SuppressFlags suppressFlags = SuppressFlags.NONE;

    private QolVisualRuntime() {
    }

    static void beginTick() {
        tickGeneration++;
    }

    public static boolean shouldSuppressEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Player player && DungeonRuntime.shouldHideTeammate(player)) {
            return true;
        }
        SuppressFlags flags = suppressFlags();
        if (!flags.any) {
            return false;
        }
        if (flags.fishing && FishingSuiteRuntime.shouldSuppressEntity(entity)) {
            return true;
        }
        if (flags.foraging && ForagingRuntime.shouldSuppressEntity(entity)) {
            return true;
        }
        if (flags.ghosts && GhostsRuntime.shouldSuppress(entity)) {
            return true;
        }
        if (flags.freecam && QolClientFlavorSupport.hooks().freecamHideLocalBody(entity)) {
            return true;
        }
        if (flags.slayerLaser && SlayerRuntime.shouldHideLaser(entity)) {
            return true;
        }
        if (flags.slayerPups && SlayerRuntime.shouldHideSvenPupHologram(entity)) {
            return true;
        }
        if (flags.slayerInferno && SlayerRuntime.shouldHideInfernoFireball(entity)) {
            return true;
        }
        if (flags.slayerNametags && (SlayerRuntime.shouldHideSpawnNametag(entity)
                || SlayerRuntime.shouldHideDamageSplash(entity))) {
            return true;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (entity instanceof Player player) {
            return flags.hidePlayers && shouldHidePlayer(player, qol);
        }
        if (!qol.renderOptimizerEnabled) {
            return false;
        }

        String name = plainName(entity.getCustomName());
        RenderOptimizerPolicy.EntityKind skyKind = classifySkyblock(name);
        if (skyKind != RenderOptimizerPolicy.EntityKind.OTHER
                && RenderOptimizerPolicy.shouldSuppressEntity(
                true,
                qol.hideFallingBlocks,
                qol.hideLightning,
                qol.hideExperienceOrbs,
                qol.hideDeathAnimation,
                qol.hideArmorStands,
                qol.hideArcherPassive,
                qol.hideHealerFairy,
                qol.hideSoulWeaver,
                qol.hideTentacleHead,
                skyKind,
                false,
                true)) {
            return true;
        }

        RenderOptimizerPolicy.EntityKind kind = classifyEntity(entity);
        boolean armorSafe = false;
        if (kind == RenderOptimizerPolicy.EntityKind.ARMOR_STAND
                && entity instanceof ArmorStand stand) {
            armorSafe = RenderOptimizerPolicy.isSafeArmorStandHideTarget(
                    stand.hasCustomName(),
                    stand.isMarker(),
                    hasEquipment(stand));
        }
        return RenderOptimizerPolicy.shouldSuppressEntity(
                true,
                qol.hideFallingBlocks,
                qol.hideLightning,
                qol.hideExperienceOrbs,
                qol.hideDeathAnimation,
                qol.hideArmorStands,
                qol.hideArcherPassive,
                qol.hideHealerFairy,
                qol.hideSoulWeaver,
                qol.hideTentacleHead,
                kind,
                armorSafe,
                false)
                || shouldSuppressNamedExtras(entity, qol);
    }

    private static SuppressFlags suppressFlags() {
        if (cachedFlagGeneration == tickGeneration) {
            return suppressFlags;
        }
        cachedFlagGeneration = tickGeneration;
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        boolean fishing = (qol.fishingHelperEnabled && qol.fishingHelperHideHookNametag)
                || (extras.fishingCreaturesEnabled && extras.fishingCreaturesHideCommon)
                || (extras.fishingVisualsEnabled
                && (extras.fishingVisualsHideOtherBobbers || extras.fishingVisualsChumHider));
        boolean foraging = extras.foragingTreesEnabled && extras.foragingTreesHideBits;
        boolean ghosts = extras.ghostsEnabled && !extras.ghostsShowGhosts && !extras.ghostsShowPowered;
        boolean freecam = QolClientFlavorSupport.hooks().freecamActive();
        boolean slayerLaser = extras.slayerLaserHiderEnabled;
        boolean slayerPups = extras.slayerSvenEnabled && extras.slayerSvenHidePupNametags;
        boolean slayerInferno = extras.slayerInfernoEnabled && extras.slayerInfernoHideParticles;
        boolean slayerNametags = extras.slayerHighlightsEnabled
                && (extras.slayerHighlightsHideMobNames || extras.slayerHighlightsHideDamageSplash);
        boolean hidePlayers = qol.hidePlayersEnabled;
        boolean optimizer = qol.renderOptimizerEnabled;
        boolean any = fishing || foraging || ghosts || freecam || slayerLaser || slayerPups
                || slayerInferno || slayerNametags || hidePlayers || optimizer;
        suppressFlags = new SuppressFlags(
                any,
                fishing,
                foraging,
                ghosts,
                freecam,
                slayerLaser,
                slayerPups,
                slayerInferno,
                slayerNametags,
                hidePlayers,
                optimizer);
        return suppressFlags;
    }

    private record SuppressFlags(
            boolean any,
            boolean fishing,
            boolean foraging,
            boolean ghosts,
            boolean freecam,
            boolean slayerLaser,
            boolean slayerPups,
            boolean slayerInferno,
            boolean slayerNametags,
            boolean hidePlayers,
            boolean optimizer) {
        static final SuppressFlags NONE = new SuppressFlags(
                false, false, false, false, false, false, false, false, false, false, false);
    }

    public static boolean shouldSuppressParticle(ParticleOptions options) {
        return shouldSuppressParticle(options, Double.NaN, Double.NaN, Double.NaN);
    }

    public static boolean shouldSuppressParticle(ParticleOptions options, double x, double y, double z) {
        if (options == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        RenderOptimizerPolicy.ParticleKind kind = classifyParticle(options);
        if (RenderOptimizerPolicy.shouldSuppressParticle(
                qol.renderOptimizerEnabled,
                qol.hideExplosionParticles,
                kind)) {
            return true;
        }
        Identifier particleId = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (particleId != null
                && SlayerFightPolicy.shouldHideVoidgloomParticle(particleId.toString())
                && (Double.isNaN(x) || SlayerRuntime.shouldHideVoidgloomParticleAt(x, y, z))) {
            return true;
        }
        if (particleId != null
                && SlayerFightPolicy.shouldHideBlazeParticle(particleId.toString())
                && (Double.isNaN(x) || SlayerRuntime.shouldHideInfernoParticleAt(x, y, z))) {
            return true;
        }
        if (particleId != null
                && !Double.isNaN(x)
                && SlayerRuntime.shouldHideSpawnParticleAt(particleId.toString(), x, y, z)) {
            return true;
        }
        if (FishingSuiteRuntime.shouldHideParticle(options, x, y, z)) {
            return true;
        }
        if (particleId != null
                && !Double.isNaN(x)
                && SkyBlockUtilityRuntime.hideImplosion(particleId.toString(), x, y, z)) {
            return true;
        }
        return RenderOptimizerPolicy.shouldSuppressExtraParticle(
                qol.renderOptimizerEnabled,
                extras.hideDeadPoof,
                extras.hideMageBeam,
                extras.hideIceSpray,
                extras.hidePowderCoating,
                kind,
                SkyBlockDungeonDetector.confidentlyInDungeon());
    }

    public static boolean shouldHideFireOverlay() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return RenderOptimizerPolicy.shouldSuppressFireOverlay(
                qol.renderOptimizerEnabled,
                qol.hideFireOverlay);
    }

    public static boolean shouldHideEntityFire() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.renderOptimizerEnabled && qol.extras().hideEntityFire;
    }

    public static boolean shouldHideNausea() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.renderOptimizerEnabled && qol.extras().hideNausea;
    }

    public static boolean shouldHideFog() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.renderOptimizerEnabled && qol.extras().hideFog;
    }

    public static boolean shouldHideClouds() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.renderOptimizerEnabled) {
            return false;
        }
        return SkyBlockUtilityPolicy.shouldHideClouds(
                qol.extras().hideIslandClouds,
                SkyBlockAreaDetector.detect().displayName());
    }

    public static float netherFogFactor() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        if (!qol.renderOptimizerEnabled || client == null || client.player == null) {
            return 1.0F;
        }
        String area = SkyBlockAreaDetector.detect().displayName()
                + " "
                + MiningLeftoverRuntime.scoreboardText();
        boolean nightVision = client.player.hasEffect(MobEffects.NIGHT_VISION);
        return SkyBlockUtilityPolicy.netherFogFactor(
                qol.extras().netherFog,
                SkyBlockUtilityPolicy.isCrimsonIsle(area),
                nightVision,
                qol.extras().netherFogScale);
    }

    public static boolean shouldHideArmor(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.renderOptimizerEnabled) {
            return false;
        }
        Entity entity = client.level.getEntity(entityId);
        if (!(entity instanceof Player)) {
            return false;
        }
        boolean self = entity.getId() == client.player.getId();
        int percent = self ? qol.extras().armorSelf : qol.extras().armorOthers;
        return SkyBlockUtilityPolicy.hideArmor(percent);
    }

    public static boolean shouldHideBreakParticles() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.renderOptimizerEnabled && qol.extras().hideBreakParticles;
    }

    public static boolean shouldHideStuckArrows() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.renderOptimizerEnabled && qol.extras().hideStuckArrows;
    }

    public static boolean shouldHideDiorite(BlockState state) {
        if (state == null || !DungeonRuntime.shouldHideDioriteNow()) {
            return false;
        }
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return DungeonPolicy.isF7Diorite(key == null ? "" : key.getPath());
    }

    public static boolean animationFixEnabled() {
        return RotClientClient.qolConfigPublic().extras().animationFixEnabled;
    }

    public static boolean disconnectFixEnabled() {
        return RotClientClient.qolConfigPublic().extras().disconnectFixEnabled;
    }

    public static boolean itemCountFixEnabled() {
        return RotClientClient.qolConfigPublic().extras().itemCountFixEnabled;
    }

    /**
     * Vanilla model swap for {@code hypixel_skyblock:} items. Null keeps the
     * original model, including heads and every unmapped new-pack item.
     */
    public static Identifier maybeLegacyItemModel(ItemStack stack, Identifier currentModel) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return maybeLegacyItemModel(AutoClickerItemIdentity.skyBlockId(stack), currentModel);
    }

    public static Identifier maybeLegacyItemModel(CustomData custom, Identifier currentModel) {
        return maybeLegacyItemModel(AutoClickerItemIdentity.skyBlockIdFromCustomData(custom), currentModel);
    }

    public static Identifier maybeLegacyItemModel(String skyBlockId, Identifier currentModel) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        String namespace = currentModel == null ? "" : currentModel.getNamespace();
        if (!LegacyTexturesPolicy.shouldReplace(
                extras.legacyTexturesEnabled, extras.legacyTexturesItems, skyBlockId, namespace)) {
            return null;
        }
        String model = LegacyTexturesPolicy.vanillaModel(skyBlockId);
        if (model.isBlank()) {
            return null;
        }
        Identifier next = Identifier.tryParse(model);
        if (next == null || next.equals(currentModel)) {
            return null;
        }
        return next;
    }

    public static boolean hideArmorBar() {
        return HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.ARMOR, HudLayerHidePolicy.flags(RotClientClient.qolConfigPublic()));
    }

    public static boolean hideFoodBar() {
        return HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.FOOD, HudLayerHidePolicy.flags(RotClientClient.qolConfigPublic()));
    }

    public static boolean hideVanillaHealth() {
        return HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.HEALTH, HudLayerHidePolicy.flags(RotClientClient.qolConfigPublic()));
    }

    public static boolean hideVanillaXp() {
        return HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.XP, HudLayerHidePolicy.flags(RotClientClient.qolConfigPublic()));
    }

    public static boolean hideEffectDisplay() {
        return HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.EFFECTS, HudLayerHidePolicy.flags(RotClientClient.qolConfigPublic()));
    }

    public static boolean hideSelectedItemName() {
        return HudLayerHidePolicy.shouldHide(
                HudLayerHidePolicy.Layer.ITEM_NAME, HudLayerHidePolicy.flags(RotClientClient.qolConfigPublic()));
    }

    public static float adjustedEyeHeight(float original) {
        var extras = RotClientClient.qolConfigPublic().extras();
        if (extras.eyeHeightFixEnabled && Math.abs(original - 1.27F) < 0.001F) {
            return 1.54F;
        }
        return original;
    }

    public static boolean instantSneakEnabled() {
        return RotClientClient.qolConfigPublic().extras().instantSneakEnabled;
    }

    public static boolean eyeHeightFixEnabled() {
        return RotClientClient.qolConfigPublic().extras().eyeHeightFixEnabled;
    }

    public static boolean shouldHideEmptyTooltip(String hoverName, String title) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return RenderOptimizerPolicy.shouldHideEmptyTooltip(
                qol.renderOptimizerEnabled,
                qol.extras().hideEmptyTooltips,
                hoverName,
                title);
    }

    public static boolean shouldHideVignette(boolean danger) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return RenderOptimizerPolicy.shouldHideVignette(
                qol.renderOptimizerEnabled,
                qol.extras().vignetteMode,
                danger);
    }

    public static float itemEntityScale() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return ItemScalePolicy.renderScale(extras.itemScaleEnabled, extras.itemScale);
    }

    public static PlayerSizePolicy.Scale localPlayerScale() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return PlayerSizePolicy.resolve(
                qol.playerSizeEnabled,
                qol.playerSizeX,
                qol.playerSizeY,
                qol.playerSizeZ);
    }

    public static void renderEtherwarpGizmos() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.etherwarpEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        if (!EtherwarpPredictor.shouldPreviewWhileAiming(
                player.isShiftKeyDown(),
                player.getMainHandItem().getHoverName().getString())) {
            return;
        }
        EtherwarpPredictor.Vec3d eye = new EtherwarpPredictor.Vec3d(
                player.getEyePosition().x,
                player.getEyePosition().y,
                player.getEyePosition().z);
        var look = player.getViewVector(1.0F);
        EtherwarpPredictor.Vec3d dir = new EtherwarpPredictor.Vec3d(
                look.x, look.y, look.z);
        // Server-position toggle is deferred: no reliable reconciled eye sample.
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                eye,
                Optional.empty(),
                qol.etherwarpUseServerPosition,
                dir,
                EtherwarpPredictor.DEFAULT_RANGE,
                occupancy(client.level));
        if (!EtherwarpPredictor.shouldRenderTarget(
                target, qol.etherwarpShowGuess, qol.etherwarpShowFailed)) {
            return;
        }
        EtherwarpPredictor.Target hit = target.get();
        int color = hit.validity() == EtherwarpPredictor.Validity.VALID
                ? qol.etherwarpColor
                : qol.etherwarpFailColor;
        GizmoStyle style;
        if (EtherwarpPredictor.drawFilled(qol.etherwarpRenderStyle)
                && EtherwarpPredictor.drawOutline(qol.etherwarpRenderStyle)) {
            style = GizmoStyle.strokeAndFill(color, 2.0F, withAlpha(color, 0x44));
        } else if (EtherwarpPredictor.drawFilled(qol.etherwarpRenderStyle)) {
            style = GizmoStyle.strokeAndFill(color, 0.01F, withAlpha(color, 0x66));
        } else {
            style = GizmoStyle.strokeAndFill(color, 2.0F, withAlpha(color, 0x00));
        }
        EtherwarpPredictor.HighlightPlane plane =
                EtherwarpPredictor.highlightBox(hit, qol.etherwarpFullBlock);
        var props = Gizmos.cuboid(
                new AABB(
                        plane.minX(),
                        plane.minY(),
                        plane.minZ(),
                        plane.maxX(),
                        plane.maxY(),
                        plane.maxZ()),
                style);
        if (!EtherwarpPredictor.depthRespectsOcclusion(qol.etherwarpDepth)) {
            props.setAlwaysOnTop();
        }
        maybePlaySound(qol, player, hit);
    }

    public static void renderTrajectoryGizmos() {
        TrajectoryRuntime.renderGizmos();
    }

    public static void renderWorldScannerGizmos() {
        WorldScannerRuntime.renderGizmos();
    }

    public static void renderWaypointGizmos() {
        WaypointRuntime.renderGizmos();
    }

    private static void maybePlaySound(
            QolUtilityConfig qol,
            LocalPlayer player,
            EtherwarpPredictor.Target hit) {
        if (!qol.etherwarpSounds) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastSoundTarget != null
                && lastSoundTarget.blockX() == hit.blockX()
                && lastSoundTarget.blockY() == hit.blockY()
                && lastSoundTarget.blockZ() == hit.blockZ()
                && lastSoundTarget.validity() == hit.validity()) {
            return;
        }
        if (now - lastSoundAtMs < 250L) {
            return;
        }
        lastSoundTarget = hit;
        lastSoundAtMs = now;
        player.level().playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                hit.validity() == EtherwarpPredictor.Validity.VALID
                        ? SoundEvents.NOTE_BLOCK_PLING.value()
                        : SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS,
                0.15F,
                hit.validity() == EtherwarpPredictor.Validity.VALID ? 1.4F : 0.7F,
                false);
    }

    private static boolean shouldHidePlayer(Player player, QolUtilityConfig qol) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer local = client == null ? null : client.player;
        boolean isLocal = local != null && player.getId() == local.getId();
        double distance = local == null ? -1.0D : local.distanceTo(player);
        return HidePlayersPolicy.shouldHideRemotePlayer(
                qol.hidePlayersEnabled,
                qol.hidePlayersOnlyDungeons,
                SkyBlockDungeonDetector.confidentlyInDungeon(),
                qol.hidePlayersHideAll,
                qol.hidePlayersDistance,
                distance,
                isLocal);
    }

    private static RenderOptimizerPolicy.EntityKind classifyEntity(Entity entity) {
        if (entity.getType() == EntityTypes.FALLING_BLOCK) {
            return RenderOptimizerPolicy.EntityKind.FALLING_BLOCK;
        }
        if (entity.getType() == EntityTypes.LIGHTNING_BOLT) {
            return RenderOptimizerPolicy.EntityKind.LIGHTNING;
        }
        if (entity.getType() == EntityTypes.EXPERIENCE_ORB) {
            return RenderOptimizerPolicy.EntityKind.EXPERIENCE_ORB;
        }
        if (entity instanceof ArmorStand) {
            return RenderOptimizerPolicy.EntityKind.ARMOR_STAND;
        }
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return RenderOptimizerPolicy.EntityKind.LIVING_DYING;
        }
        return RenderOptimizerPolicy.EntityKind.OTHER;
    }

    private static boolean shouldSuppressNamedExtras(Entity entity, QolUtilityConfig qol) {
        QolSkyblockExtras extras = qol.extras();
        boolean dungeon = SkyBlockDungeonDetector.confidentlyInDungeon();
        if (extras.hideDeadEntities
                && entity instanceof LivingEntity living
                && !living.isAlive()) {
            return true;
        }
        if (extras.hideGuidedSheep
                && dungeon
                && entity.getType() == EntityTypes.SHEEP
                && entity instanceof LivingEntity sheep
                && Math.abs(sheep.getHealth() - 8.0F) < 0.01F) {
            return true;
        }
        if (extras.hideBonePlating
                && dungeon
                && entity.getType() == EntityTypes.ITEM
                && entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
            ItemStack stack = item.getItem();
            return stack.is(Items.BONE_MEAL)
                    && ChatTextPolicy.stripFormatting(stack.getHoverName().getString())
                    .equalsIgnoreCase("Bone Meal");
        }
        if (extras.hideTreeBits && entity.getType() == EntityTypes.BLOCK_DISPLAY) {
            return true;
        }
        return false;
    }

    private static RenderOptimizerPolicy.ParticleKind classifyParticle(ParticleOptions options) {
        if (options.getType() == ParticleTypes.EXPLOSION
                || options.getType() == ParticleTypes.EXPLOSION_EMITTER
                || options.getType() == ParticleTypes.GUST
                || options.getType() == ParticleTypes.GUST_EMITTER_LARGE) {
            return RenderOptimizerPolicy.ParticleKind.EXPLOSION;
        }
        if (options.getType() == ParticleTypes.POOF) {
            return RenderOptimizerPolicy.ParticleKind.POOF;
        }
        if (options.getType() == ParticleTypes.FIREWORK) {
            return RenderOptimizerPolicy.ParticleKind.FIREWORK;
        }
        if (options.getType() == ParticleTypes.DUST) {
            return RenderOptimizerPolicy.ParticleKind.DUST;
        }
        return RenderOptimizerPolicy.ParticleKind.OTHER;
    }

    private static RenderOptimizerPolicy.EntityKind classifySkyblock(String name) {
        if (RenderOptimizerPolicy.matchesSkyblockVisual(
                RenderOptimizerPolicy.EntityKind.SKYBLOCK_ARCHER_PASSIVE, name)) {
            return RenderOptimizerPolicy.EntityKind.SKYBLOCK_ARCHER_PASSIVE;
        }
        if (RenderOptimizerPolicy.matchesSkyblockVisual(
                RenderOptimizerPolicy.EntityKind.SKYBLOCK_HEALER_FAIRY, name)) {
            return RenderOptimizerPolicy.EntityKind.SKYBLOCK_HEALER_FAIRY;
        }
        if (RenderOptimizerPolicy.matchesSkyblockVisual(
                RenderOptimizerPolicy.EntityKind.SKYBLOCK_SOUL_WEAVER, name)) {
            return RenderOptimizerPolicy.EntityKind.SKYBLOCK_SOUL_WEAVER;
        }
        if (RenderOptimizerPolicy.matchesSkyblockVisual(
                RenderOptimizerPolicy.EntityKind.SKYBLOCK_TENTACLE_HEAD, name)) {
            return RenderOptimizerPolicy.EntityKind.SKYBLOCK_TENTACLE_HEAD;
        }
        return RenderOptimizerPolicy.EntityKind.OTHER;
    }

    private static boolean hasEquipment(ArmorStand stand) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (!stand.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String plainName(Component component) {
        return component == null ? "" : component.getString();
    }

    private static EtherwarpPredictor.BlockOccupancy occupancy(BlockGetter level) {
        return new EtherwarpPredictor.BlockOccupancy() {
            @Override
            public boolean isSolidSurface(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return !state.getCollisionShape(level, pos).isEmpty();
            }

            @Override
            public boolean isStandSpaceClear(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return state.getCollisionShape(level, pos).isEmpty()
                        && level.getFluidState(pos).isEmpty();
            }
        };
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
