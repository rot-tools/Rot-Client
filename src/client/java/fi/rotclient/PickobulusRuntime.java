package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import java.util.ArrayList;
import java.util.List;

/** Cooldown HUD and projection of existing accepted break callbacks; never credits a ledger. */
public final class PickobulusRuntime {
    private static PickobulusPolicy.Timer timer = new PickobulusPolicy.Timer();
    private static long shotAt = -1;
    private static int acceptedBlocks, acceptedTargetBlocks;
    private static String popup = "";
    private static long popupUntil;
    static void clear() {
        timer = new PickobulusPolicy.Timer(); shotAt = -1; acceptedBlocks = acceptedTargetBlocks = 0;
        popup = ""; popupUntil = 0;
    }
    static boolean hudVisible(QolUtilityConfig qol) {
        return qol.extras().pickobulusEnabled && qol.extras().pickobulusTimerHud;
    }
    static List<String> hudLines() {
        var config = RotClientClient.qolConfigPublic();
        List<String> lines = new ArrayList<>();
        lines.add(timer.label(System.currentTimeMillis()));
        if (config.extras().pickobulusConfirmedCounts && shotAt >= 0) {
            lines.add("Tracker accepted: " + acceptedBlocks + " blocks");
            lines.add("Selected target: " + acceptedTargetBlocks + " blocks");
        }
        lines.addAll(QolClientFlavorSupport.hooks().pickobulusPreviewHudLines());
        return List.copyOf(lines);
    }
    static void onChat(Component component) {
        if (component == null) return;
        String text = component.getString();
        long now = System.currentTimeMillis();
        if (PickobulusPolicy.used(text)) {
            shotAt = now; acceptedBlocks = acceptedTargetBlocks = 0;
            var config = RotClientClient.qolConfigPublic().extras();
            double cooldown = config.pickobulusCooldownSeconds;
            boolean observed = false;
            Minecraft client = Minecraft.getInstance();
            if (config.pickobulusAutoCooldown && client.player != null) {
                var lore = InventoryChromeRuntime.loreLines(client.player.getMainHandItem());
                boolean pickobulus = lore.stream().anyMatch(line -> CommissionDisplayPolicy.normalizeLine(line).contains("Pickobulus"));
                if (pickobulus) for (String line : lore) {
                    var seconds = PickobulusPolicy.seconds(line);
                    if (seconds.isPresent()) { cooldown = seconds.getAsDouble(); observed = true; }
                }
            }
            // Lore is the base cooldown, potentially modified by SkyMall/perks. Await tab/server correction.
            timer.start(now, cooldown, false);
            DiagnosticRecorder.record("PICKOBULUS_COOLDOWN", "seconds=" + cooldown + " lore=" + observed);
        } else if (PickobulusPolicy.ready(text)) timer.confirmReady(now);
    }
    static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return;
        var config = RotClientClient.qolConfigPublic().extras();
        if (!config.pickobulusEnabled) { popup = ""; return; }
        long now = System.currentTimeMillis();
        MiningLeftoverPolicy.parseAbility(CommissionDisplayRuntime.tabLines(client)).ifPresent(ability -> {
            if (!ability.name().equalsIgnoreCase("Pickobulus")) return;
            String status = ability.status().trim();
            if (status.equalsIgnoreCase("READY") || status.equalsIgnoreCase("AVAILABLE")) timer.confirmReady(now);
            else PickobulusPolicy.seconds("Cooldown: " + status).ifPresent(seconds -> timer.start(now, seconds, true));
        });
        if (timer.takeReadyNotification(now)) {
            if (config.pickobulusReadyPopup) {
                popup = timer.authoritative() ? "Ability ready" : "Ability ready (estimated)";
                popupUntil = now + 2_500;
            }
            if (config.pickobulusReadySound) client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5F, 1.4F);
        }
    }
    static String popup() { return System.currentTimeMillis() < popupUntil ? popup : ""; }
    static void acceptedBreak(int count, boolean selectedTarget) {
        long age = System.currentTimeMillis() - shotAt;
        if (shotAt < 0 || age < 0 || age > 5_000 || count <= 0) return;
        acceptedBlocks += count;
        if (selectedTarget) acceptedTargetBlocks += count;
    }
}
