package fi.rotclient;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Optional;

/**
 * Switches settings profiles automatically when the player moves between
 * places the user has given a rule (Dungeons, Dwarven Mines, ...).
 *
 * It only decides when to switch. The switch itself is the ordinary
 * {@link RotClientProfileController#switchTo}, so an automatic switch behaves
 * exactly like clicking the profile: the outgoing profile is saved first and
 * the target is applied live. A manual switch is never reverted; rules apply
 * again the next time the context changes.
 */
final class RotClientAutoProfileSwitcher {
    private final RotClientProfileManager profiles;
    private final RotClientProfileController controller;
    private final AutoProfileSwitchTracker tracker =
            new AutoProfileSwitchTracker();

    RotClientAutoProfileSwitcher(
            RotClientProfileManager profiles,
            RotClientProfileController controller) {
        this.profiles = profiles;
        this.controller = controller;
    }

    /**
     * Called once per sidebar read.
     *
     * @param lines     sidebar title plus rows
     * @param location  location parsed from those lines
     * @param inDungeon dungeon detector's current verdict
     */
    void observeSidebar(
            List<String> lines,
            SkyBlockLocation location,
            boolean inDungeon,
            long nowMillis) {

        RotClientAutoSwitchConfig rules = profiles.autoSwitch();

        if (!rules.enabled) {
            // Re-enabling later must apply to wherever the player is then.
            tracker.reset();
            return;
        }

        Optional<AutoProfileContext> observed =
                AutoProfileContext.classify(lines, location, inDungeon);

        AutoProfileContext stable =
                tracker.observe(observed.orElse(null), nowMillis);

        if (stable == null) {
            return;
        }

        RotClientProfile active = profiles.activeProfile();

        String targetId =
                rules.switchTargetFor(
                        stable,
                        active == null ? "" : active.id);

        if (targetId.isEmpty()) {
            return;
        }

        RotClientProfile target = profiles.findById(targetId);

        if (target == null) {
            return;
        }

        boolean switched = controller.switchTo(targetId);

        if (switched) {
            if (rules.notify) {
                tell("Switched to profile \"" + target.name + "\" ("
                        + stable.displayName() + ").");
            }
        } else {
            tell("Could not switch to profile \"" + target.name + "\".");
        }
    }

    /** New world: forget the last context so the first area applies. */
    void onWorldChanged() {
        tracker.reset();
    }

    private static void tell(String text) {
        Minecraft client = Minecraft.getInstance();

        if (client != null && client.player != null) {
            client.player.sendSystemMessage(
                    RotClientChat.message(text, false, 0));
        }
    }
}
