package fi.rotclient;

/**
 * Hide vanilla / Hypixel HUD layers.
 *
 * The HUD Elements Editor exposes each vanilla hide row as a direct switch,
 * so those flags apply independently of the HUD Layout module's master state.
 * Player Display and Render Optimizer compatibility hides still apply as well.
 */
public final class HudLayerHidePolicy {
    public enum Layer {
        HOTBAR,
        HEALTH,
        FOOD,
        ARMOR,
        XP,
        AIR,
        MOUNT,
        SCOREBOARD,
        BOSS,
        ACTION,
        ITEM_NAME,
        EFFECTS,
        TITLES,
        TAB
    }

    public record Flags(
            boolean layoutEnabled,
            boolean hideHotbar,
            boolean hideHealth,
            boolean hideFood,
            boolean hideArmor,
            boolean hideXp,
            boolean hideAir,
            boolean hideMount,
            boolean hideScoreboard,
            boolean hideBoss,
            boolean hideAction,
            boolean hideItemName,
            boolean hideEffects,
            boolean hideTitles,
            boolean hideTab,
            boolean playerDisplayEnabled,
            boolean playerHideHealth,
            boolean playerHideFood,
            boolean playerHideArmor,
            boolean playerHideXp,
            boolean renderOptimizerEnabled,
            boolean optimizerHideBoss,
            boolean optimizerHideArmor,
            boolean optimizerHideFood,
            boolean optimizerHideEffects,
            boolean optimizerHideItemName,
            boolean customBoardEnabled,
            boolean customBoardHideVanilla) {
    }

    public static Flags flags(QolUtilityConfig qol) {
        if (qol == null) {
            return new Flags(
                    false, false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false, false, false, false);
        }
        QolSkyblockExtras extras = qol.extras();
        return new Flags(
                extras.hudLayoutEnabled,
                extras.hudHideHotbar,
                extras.hudHideHealth,
                extras.hudHideFood,
                extras.hudHideArmor,
                extras.hudHideXp,
                extras.hudHideAir,
                extras.hudHideMount,
                extras.hudHideScoreboard,
                extras.hudHideBoss,
                extras.hudHideAction,
                extras.hudHideItemName,
                extras.hudHideEffects,
                extras.hudHideTitles,
                extras.hudHideTab,
                qol.playerDisplayEnabled,
                qol.playerDisplayHideVanillaHealth,
                qol.playerDisplayHideVanillaFood,
                qol.playerDisplayHideVanillaArmor,
                qol.playerDisplayHideVanillaXp,
                qol.renderOptimizerEnabled,
                extras.hideBossBar,
                extras.hideArmorBar,
                extras.hideFoodBar,
                extras.hideEffectDisplay,
                extras.hideSelectedItemName,
                qol.isModuleEnabled(CustomScoreboardPolicy.MODULE_ID)
                        && (SkyBlockAreaDetector.isInSkyblock()
                        || extras.board().showOutsideSkyblock),
                extras.board().hideVanilla);
    }

    private HudLayerHidePolicy() {
    }

    public static boolean shouldHide(Layer layer, Flags flags) {
        if (layer == null || flags == null) {
            return false;
        }
        return switch (layer) {
            case HOTBAR -> flags.hideHotbar();

            case HEALTH -> flags.hideHealth()
                    || VanillaHudHidePolicy.shouldHideLayer(
                            flags.playerDisplayEnabled(),
                            flags.playerHideHealth(),
                            flags.playerHideFood(),
                            flags.playerHideArmor(),
                            flags.playerHideXp(),
                            VanillaHudHidePolicy.Layer.HEALTH);

            case FOOD -> flags.hideFood()
                    || VanillaHudHidePolicy.shouldHideLayer(
                            flags.playerDisplayEnabled(),
                            flags.playerHideHealth(),
                            flags.playerHideFood(),
                            flags.playerHideArmor(),
                            flags.playerHideXp(),
                            VanillaHudHidePolicy.Layer.FOOD)
                    || (flags.renderOptimizerEnabled()
                            && flags.optimizerHideFood());

            case ARMOR -> flags.hideArmor()
                    || VanillaHudHidePolicy.shouldHideLayer(
                            flags.playerDisplayEnabled(),
                            flags.playerHideHealth(),
                            flags.playerHideFood(),
                            flags.playerHideArmor(),
                            flags.playerHideXp(),
                            VanillaHudHidePolicy.Layer.ARMOR)
                    || (flags.renderOptimizerEnabled()
                            && flags.optimizerHideArmor());

            case XP -> flags.hideXp()
                    || VanillaHudHidePolicy.shouldHideLayer(
                            flags.playerDisplayEnabled(),
                            flags.playerHideHealth(),
                            flags.playerHideFood(),
                            flags.playerHideArmor(),
                            flags.playerHideXp(),
                            VanillaHudHidePolicy.Layer.XP);

            case AIR -> flags.hideAir();
            case MOUNT -> flags.hideMount();

            case SCOREBOARD -> flags.hideScoreboard()
                    || (flags.customBoardEnabled()
                            && flags.customBoardHideVanilla());

            case BOSS -> flags.hideBoss()
                    || (flags.renderOptimizerEnabled()
                            && flags.optimizerHideBoss());

            case ACTION -> flags.hideAction();

            case ITEM_NAME -> flags.hideItemName()
                    || (flags.renderOptimizerEnabled()
                            && flags.optimizerHideItemName());

            case EFFECTS -> flags.hideEffects()
                    || (flags.renderOptimizerEnabled()
                            && flags.optimizerHideEffects());

            case TITLES -> flags.hideTitles();
            case TAB -> flags.hideTab();
        };
    }
}
