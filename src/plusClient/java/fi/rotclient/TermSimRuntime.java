package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

import java.util.Random;

/**
 * Client bridge for {@link TermSimPolicy}: hub, ping delay, local clicks.
 */
public final class TermSimRuntime {
    private static TermSimPolicy.Layout layout;
    private static int pendingTicks;
    private static int pendingSlot = -1;
    private static int pendingButton;
    private static long practiceStarted;
    private static final Random RNG = new Random();

    private TermSimRuntime() {
    }

    public static boolean isOpen() {
        Minecraft client = Minecraft.getInstance();
        return layout != null && client.screen instanceof TermSimScreen;
    }

    public static TermSimPolicy.Layout layout() {
        return layout;
    }

    public static void close() {
        layout = null;
        pendingTicks = 0;
        pendingSlot = -1;
    }

    public static void openHub() {
        TermSimSettings settings = settings();
        java.util.Map<TermSimPolicy.Kind, Integer> pbs = settings.showPbs()
                ? TermSimPersonalBestPolicy.parsePersonalBests(settings.pbs())
                : java.util.Map.of();
        open(TermSimPolicy.hub(pbs));
        practiceStarted = 0L;
    }

    public static void openRandom() {
        open(TermSimPolicy.randomPractice(RNG));
    }

    public static void openKind(TermSimPolicy.Kind kind) {
        if (kind == null || kind == TermSimPolicy.Kind.HUB) {
            openHub();
            return;
        }
        open(TermSimPolicy.generate(kind, RNG));
    }

    public static void onContainerTick(TermSimScreen screen) {
        if (layout == null || screen == null) {
            return;
        }
        if (layout.kind() == TermSimPolicy.Kind.MELODY) {
            TermSimPolicy.Layout next = TermSimPolicy.tickMelody(layout);
            if (next != layout) {
                layout = next;
                screen.apply(layout);
            }
        }
        if (pendingTicks > 0) {
            pendingTicks--;
            if (pendingTicks == 0 && pendingSlot >= 0) {
                int slot = pendingSlot;
                int button = pendingButton;
                pendingSlot = -1;
                applyClick(screen, slot, button);
            }
        }
    }

    public static boolean onSlotClicked(TermSimScreen screen, Slot slot, int button) {
        if (screen == null || slot == null || slot.index >= screen.chestSize()) {
            return true;
        }
        requestClick(screen, slot.index, button);
        return true;
    }

    public static void click(int slot, int button) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof TermSimScreen screen) || layout == null) {
            return;
        }
        requestClick(screen, slot, button);
    }

    static void openFromCommand(int pingMillis) {
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (!TermSimSettings.from(config).enabled()) TermSimSettings.enabled(config, true);
        if (pingMillis >= 0) {
            TermSimSettings.ping(config, pingMillis);
        }
        openHub();
    }

    private static void requestClick(TermSimScreen screen, int slot, int button) {
        int delay = TermSimPolicy.pingTicks(settings().ping());
        if (delay <= 0) {
            applyClick(screen, slot, button);
            return;
        }
        if (pendingSlot >= 0) {
            return;
        }
        pendingSlot = slot;
        pendingButton = button;
        pendingTicks = delay;
    }

    private static void applyClick(TermSimScreen screen, int slot, int button) {
        if (layout == null) {
            return;
        }
        if (layout.kind() == TermSimPolicy.Kind.HUB) {
            if (slot == TermSimPolicy.HUB_RESET) {
                TermSimSettings.pbs(RotClientClient.qolConfigPublic(), "");
                TermSimScreen.playClick();
                openHub();
                return;
            }
            TermSimPolicy.Kind choice = TermSimPolicy.hubChoice(slot);
            if (choice == null) {
                return;
            }
            TermSimScreen.playClick();
            if (slot == TermSimPolicy.HUB_RANDOM) {
                openRandom();
            } else {
                openKind(choice);
            }
            practiceStarted = System.currentTimeMillis();
            return;
        }
        TermSimPolicy.ClickResult result = TermSimPolicy.click(layout, slot, button);
        if (!result.accepted()) {
            return;
        }
        TermSimScreen.playClick();
        layout = result.layout();
        screen.apply(layout);
        if (result.complete()) {
            recordPb(layout.kind());
            openHub();
        }
    }

    private static void recordPb(TermSimPolicy.Kind kind) {
        if (practiceStarted <= 0L || kind == null || kind == TermSimPolicy.Kind.HUB) {
            return;
        }
        int millis = (int) Math.max(1L, System.currentTimeMillis() - practiceStarted);
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        var pbs = TermSimPersonalBestPolicy.parsePersonalBests(TermSimSettings.from(config).pbs());
        if (TermSimPersonalBestPolicy.recordPersonalBest(pbs, kind, millis)) {
            TermSimSettings.pbs(config, TermSimPersonalBestPolicy.writePersonalBests(pbs));
        }
    }

    private static void open(TermSimPolicy.Layout next) {
        layout = next;
        pendingTicks = 0;
        pendingSlot = -1;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.setScreen(TermSimScreen.create(next));
    }

    private static TermSimSettings settings() {
        return TermSimSettings.from(RotClientClient.qolConfigPublic());
    }
}
