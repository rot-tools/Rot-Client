package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import java.util.ArrayList;
import java.util.List;

/** Cooldown HUD and projection of existing accepted break callbacks; never credits a ledger. */
public final class PickobulusRuntime {
    private static PickobulusPolicy.Timer timer = new PickobulusPolicy.Timer();
    private static final PickobulusPolicy.ShotProjection shot = new PickobulusPolicy.ShotProjection();
    private static final PickobulusPolicy.Popup popup = new PickobulusPolicy.Popup();
    private static final PickobulusPolicy.Context context = new PickobulusPolicy.Context();
    private static String shotSelectionLabel = "";
    static void clear() {
        clearState();
        context.clear();
    }
    private static void clearState() {
        timer = new PickobulusPolicy.Timer();
        shot.reset(); popup.clear(); shotSelectionLabel = "";
    }
    static boolean hudVisible(QolUtilityConfig qol) {
        // HUD editor hit testing is configuration-only and must also work without a live world.
        return qol != null && qol.extras().pickobulusEnabled && qol.extras().pickobulusTimerHud;
    }
    static List<String> hudLines() {
        var config = RotClientClient.qolConfigPublic();
        long now = System.currentTimeMillis();
        if (!context(Minecraft.getInstance(), config.extras(), now)) return List.of();
        List<String> lines = new ArrayList<>();
        lines.add(timer.label(now));
        if (config.extras().pickobulusConfirmedCounts && shot.observedUse()) {
            lines.add("Last use accepted: " + shot.accepted() + " blocks");
            lines.add("Target at use (" + shotSelectionLabel + "): " + shot.selected() + " blocks"
                    + (shot.selectionChanged() ? " (partial — tracker changed)" : ""));
        }
        lines.addAll(QolClientFlavorSupport.hooks().pickobulusPreviewHudLines());
        return List.copyOf(lines);
    }
    static void onChat(Component component) {
        if (component == null) return;
        String text = component.getString();
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        var config = RotClientClient.qolConfigPublic().extras();
        if (!context(client, config, now)) return;
        if (PickobulusPolicy.used(text)) {
            var selection = RotClientClient.selectedSelection();
            if (!shot.start(now, selection.id())) return;
            shotSelectionLabel = selection.displayName();
            popup.clear();
            double cooldown = config.pickobulusCooldownSeconds;
            boolean observed = false;
            if (config.pickobulusAutoCooldown && client.player != null) {
                var lore = InventoryChromeRuntime.loreLines(client.player.getMainHandItem());
                var seconds = PickobulusPolicy.loreCooldown(lore);
                if (seconds.isPresent()) { cooldown = seconds.getAsDouble(); observed = true; }
            }
            // Lore is the base cooldown, potentially modified by SkyMall/perks. Await tab/server correction.
            String baseline = PickobulusPolicy.tabStatus(CommissionDisplayRuntime.tabLines(client)).orElse("");
            if (!timer.startUse(now, cooldown, baseline)) timer.reset();
            DiagnosticRecorder.record("PICKOBULUS_COOLDOWN", "seconds=" + cooldown + " lore=" + observed);
        } else if (PickobulusPolicy.ready(text)) timer.confirmReady(now);
    }
    static void tick(Minecraft client) {
        var config = RotClientClient.qolConfigPublic().extras();
        long now = System.currentTimeMillis();
        if (!context(client, config, now)) return;
        shot.observeSelection(RotClientClient.selectedSelection().id(), now);
        PickobulusPolicy.tabStatus(CommissionDisplayRuntime.tabLines(client)).ifPresent(status ->
                timer.observeTabStatus(status, now));
        if (!config.pickobulusReadyPopup) popup.clear();
        if (timer.takeReadyNotification(now)) {
            if (config.pickobulusReadyPopup) {
                popup.show(timer.authoritative() ? "Ability ready" : "Ability ready (estimated)", now);
            }
            if (config.pickobulusReadySound) client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5F, 1.4F);
        }
    }
    static String popup() {
        long now = System.currentTimeMillis();
        var config = RotClientClient.qolConfigPublic().extras();
        if (!context(Minecraft.getInstance(), config, now) || !config.pickobulusReadyPopup) {
            popup.clear();
            return "";
        }
        return popup.text(now);
    }
    static void acceptedBreak(int count, boolean selectedTarget) {
        long now = System.currentTimeMillis();
        if (!context(Minecraft.getInstance(), RotClientClient.qolConfigPublic().extras(), now)) return;
        shot.acceptedBreak(now, count, selectedTarget, RotClientClient.selectedSelection().id());
    }
    private static boolean context(Minecraft client, QolSkyblockExtras config, long now) {
        var observed = context.observe(client == null ? null : client.level,
                client == null ? null : client.getConnection(), RotClientClient.currentProfileId(),
                config != null && config.pickobulusEnabled, client != null && client.player != null,
                client != null && client.level != null && SkyBlockAreaDetector.isInSkyblock(), now);
        if (observed.reset()) clearState();
        return observed.active();
    }
}
