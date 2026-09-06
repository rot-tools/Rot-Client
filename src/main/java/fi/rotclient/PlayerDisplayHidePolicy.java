package fi.rotclient;

/**
 * Player Display hide/filter switches. Child hide toggles persist in config
 * so the dashboard can remember them, but they must not apply while the
 * parent module is off. Enabling a stat HUD still auto-hides that fragment
 * on the Hypixel action bar so the two copies do not stack.
 */
public final class PlayerDisplayHidePolicy {
    public record ActionHides(
            boolean health,
            boolean defense,
            boolean mana,
            boolean overflow,
            boolean speed,
            boolean vitality,
            boolean location) {
        static final ActionHides NONE =
                new ActionHides(false, false, false, false, false, false, false);

        public boolean any() {
            return health
                    || defense
                    || mana
                    || overflow
                    || speed
                    || vitality
                    || location;
        }
    }

    private PlayerDisplayHidePolicy() {
    }

    public static ActionHides from(QolUtilityConfig qol) {
        if (qol == null || !qol.isModuleEnabled("qol.player_display")) {
            return ActionHides.NONE;
        }
        return new ActionHides(
                qol.playerDisplayHideActionHealth || qol.playerDisplayHealthHud,
                qol.playerDisplayHideActionDefense || qol.playerDisplayDefenseHud,
                qol.playerDisplayHideActionMana || qol.playerDisplayManaHud,
                qol.playerDisplayHideActionOverflow || qol.playerDisplayOverflowManaHud,
                qol.playerDisplayHideActionSpeed || qol.playerDisplaySpeedHud,
                qol.playerDisplayHideActionVitality || qol.playerDisplayVitalityHud,
                qol.playerDisplayHideActionLocation);
    }
}
