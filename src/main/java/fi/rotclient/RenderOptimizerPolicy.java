package fi.rotclient;

/**
 * Pure render-suppression decisions for Render Optimizer. Fail-open when
 * classification is uncertain.
 */
public final class RenderOptimizerPolicy {
    public enum EntityKind {
        FALLING_BLOCK,
        LIGHTNING,
        EXPERIENCE_ORB,
        ARMOR_STAND,
        LIVING_DYING,
        SKYBLOCK_ARCHER_PASSIVE,
        SKYBLOCK_HEALER_FAIRY,
        SKYBLOCK_SOUL_WEAVER,
        SKYBLOCK_TENTACLE_HEAD,
        OTHER
    }

    public enum ParticleKind {
        EXPLOSION,
        POOF,
        FIREWORK,
        DUST,
        OTHER
    }

    private RenderOptimizerPolicy() {
    }

    public static boolean shouldSuppressEntity(
            boolean moduleEnabled,
            boolean hideFallingBlocks,
            boolean hideLightning,
            boolean hideExperienceOrbs,
            boolean hideDeathAnimation,
            boolean hideArmorStands,
            boolean hideArcherPassive,
            boolean hideHealerFairy,
            boolean hideSoulWeaver,
            boolean hideTentacleHead,
            EntityKind kind,
            boolean armorStandSafeToHide,
            boolean skyblockMatchConfident) {
        if (!moduleEnabled || kind == null || kind == EntityKind.OTHER) {
            return false;
        }
        return switch (kind) {
            case FALLING_BLOCK -> hideFallingBlocks;
            case LIGHTNING -> hideLightning;
            case EXPERIENCE_ORB -> hideExperienceOrbs;
            case LIVING_DYING -> hideDeathAnimation;
            case ARMOR_STAND -> hideArmorStands && armorStandSafeToHide;
            case SKYBLOCK_ARCHER_PASSIVE ->
                    hideArcherPassive && skyblockMatchConfident;
            case SKYBLOCK_HEALER_FAIRY ->
                    hideHealerFairy && skyblockMatchConfident;
            case SKYBLOCK_SOUL_WEAVER ->
                    hideSoulWeaver && skyblockMatchConfident;
            case SKYBLOCK_TENTACLE_HEAD ->
                    hideTentacleHead && skyblockMatchConfident;
            case OTHER -> false;
        };
    }

    /**
     * Safe armor-stand subset: unnamed, non-marker, empty equipment.
     * Named / equipped / marker stands fail open (keep rendering).
     */
    public static boolean isSafeArmorStandHideTarget(
            boolean hasCustomName,
            boolean marker,
            boolean hasAnyEquipment) {
        if (hasCustomName || marker || hasAnyEquipment) {
            return false;
        }
        return true;
    }

    /**
     * Confident SkyBlock visual name match. Blank / unrelated → not confident.
     */
    public static boolean matchesSkyblockVisual(
            EntityKind kind,
            String customNamePlain) {
        if (customNamePlain == null || customNamePlain.isBlank()) {
            return false;
        }
        String name = customNamePlain.toLowerCase(java.util.Locale.ROOT);
        return switch (kind) {
            case SKYBLOCK_ARCHER_PASSIVE ->
                    name.contains("archer") && name.contains("passive");
            case SKYBLOCK_HEALER_FAIRY ->
                    name.contains("fairy") || name.contains("healer fairy");
            case SKYBLOCK_SOUL_WEAVER -> name.contains("soul weaver");
            case SKYBLOCK_TENTACLE_HEAD ->
                    name.contains("tentacle") && name.contains("head");
            default -> false;
        };
    }

    public static boolean shouldSuppressParticle(
            boolean moduleEnabled,
            boolean hideExplosionParticles,
            ParticleKind kind) {
        if (!moduleEnabled || kind == null || kind == ParticleKind.OTHER) {
            return false;
        }
        return kind == ParticleKind.EXPLOSION && hideExplosionParticles;
    }

    public static boolean shouldSuppressExtraParticle(
            boolean moduleEnabled,
            boolean hideDeadPoof,
            boolean hideMageBeam,
            boolean hideIceSpray,
            boolean hidePowderCoating,
            ParticleKind kind,
            boolean inDungeon) {
        if (!moduleEnabled || kind == null || kind == ParticleKind.OTHER) {
            return false;
        }
        return switch (kind) {
            case POOF -> hideDeadPoof || hideIceSpray;
            case FIREWORK -> hideMageBeam && inDungeon;
            case DUST -> hidePowderCoating;
            default -> false;
        };
    }

    public static boolean shouldHideHud(
            boolean moduleEnabled,
            boolean toggle) {
        return moduleEnabled && toggle;
    }

    public static boolean shouldHideVignette(
            boolean moduleEnabled,
            String mode,
            boolean danger) {
        if (!moduleEnabled) {
            return false;
        }
        String normalized = QolSkyblockExtras.normalizeVignette(mode);
        return switch (normalized) {
            case QolSkyblockExtras.VIGNETTE_BOTH -> true;
            case QolSkyblockExtras.VIGNETTE_AMBIENT -> !danger;
            case QolSkyblockExtras.VIGNETTE_DANGER -> danger;
            default -> false;
        };
    }

    public static boolean shouldHideEmptyTooltip(
            boolean moduleEnabled,
            boolean hideEmptyTooltips,
            String hoverName,
            String screenTitle) {
        if (!moduleEnabled || !hideEmptyTooltips) {
            return false;
        }
        String title = screenTitle == null ? "" : screenTitle;
        if (title.startsWith("Ultrasequencer (")) {
            return false;
        }
        return hoverName == null || hoverName.trim().isEmpty();
    }

    public static boolean shouldSuppressFireOverlay(
            boolean moduleEnabled,
            boolean hideFireOverlay) {
        return moduleEnabled && hideFireOverlay;
    }
}
