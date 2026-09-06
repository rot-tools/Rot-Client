package fi.rotclient;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dungeon cheat-list helpers for the Serveri.
 * Chat, item, and GUI contracts cover Door Highlight, Spirit Bear,
 * Breaker Helper, Close Chest, Auto Superboom, and Camera helper. Defaults stay
 * off at the parent module. Auto click / superboom remain cheat-tagged.
 */
public final class TempleDungeonPolicy {
    public enum DoorKeyEvent {
        NONE,
        WITHER_OBTAINED,
        WITHER_PICKED_UP,
        WITHER_DOOR_OPEN,
        BLOOD_OBTAINED,
        BLOOD_PICKED_UP,
        BLOOD_DOOR_OPEN
    }

    public enum KeySkull {
        NONE,
        WITHER,
        BLOOD
    }

    public record BreakerCharges(int current, int max) {
        public String hudLine() {
            return "Breaker " + current + "/" + max;
        }
    }

    public static final double MIN_CAMERA_DISTANCE = 1.0D;
    public static final double MAX_CAMERA_DISTANCE = 64.0D;
    public static final double DEFAULT_CAMERA_DISTANCE = 4.0D;
    public static final String WITHER_KEY_UUID = "2865274b-3097-394e-8149-ec629c72d850";
    public static final String BLOOD_KEY_UUID = "73f6d1f9-df41-3d1d-b98c-e1442d915885";
    public static final int SECRET_CLICKED_DEFAULT_SECONDS = 7;
    public static final int SECRET_CLICKED_MAX_SECONDS = 120;
    public static final int SECRET_CLICKED_COLOR = 0x66FFAA00;
    public static final int SECRET_LOCKED_COLOR = 0x66FF5555;

    private static final Pattern BREAKER_CHARGES =
            Pattern.compile("Charges:\\s*(\\d+)/(\\d+)");
    private static final Pattern CHEST_TITLE =
            Pattern.compile("^(chest|locked chest|wood chest|gold chest|diamond chest|emerald chest|obsidian chest|bedrock chest)$",
                    Pattern.CASE_INSENSITIVE);

    private TempleDungeonPolicy() {
    }

    public static DoorKeyEvent doorKeyEvent(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (text.isBlank()) {
            return DoorKeyEvent.NONE;
        }
        String folded = text.toLowerCase(Locale.ROOT);
        if (folded.equals("a wither key was picked up!")) {
            return DoorKeyEvent.WITHER_PICKED_UP;
        }
        if (folded.equals("a blood key was picked up!")) {
            return DoorKeyEvent.BLOOD_PICKED_UP;
        }
        if (folded.equals("the blood door has been opened!")) {
            return DoorKeyEvent.BLOOD_DOOR_OPEN;
        }
        if (folded.contains("has obtained wither key")) {
            return DoorKeyEvent.WITHER_OBTAINED;
        }
        if (folded.contains("opened a wither door")) {
            return DoorKeyEvent.WITHER_DOOR_OPEN;
        }
        if (folded.contains("has obtained blood key")) {
            return DoorKeyEvent.BLOOD_OBTAINED;
        }
        return DoorKeyEvent.NONE;
    }

    public static Optional<String> doorKeyTitle(DoorKeyEvent event) {
        return switch (event) {
            case WITHER_OBTAINED -> Optional.of("Wither Key");
            case WITHER_PICKED_UP -> Optional.of("Wither Key picked up");
            case WITHER_DOOR_OPEN -> Optional.of("Wither door opened");
            case BLOOD_OBTAINED -> Optional.of("Blood Key");
            case BLOOD_PICKED_UP -> Optional.of("Blood Key picked up");
            case BLOOD_DOOR_OPEN -> Optional.of("Blood door opened");
            case NONE -> Optional.empty();
        };
    }

    public static KeySkull keySkull(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return KeySkull.NONE;
        }
        String id = uuid.toLowerCase(Locale.ROOT);
        if (id.equals(WITHER_KEY_UUID)) {
            return KeySkull.WITHER;
        }
        if (id.equals(BLOOD_KEY_UUID)) {
            return KeySkull.BLOOD;
        }
        return KeySkull.NONE;
    }

    public static boolean keyDropClass(DungeonPolicy.DungeonClass dungeonClass, boolean allClasses) {
        if (allClasses) {
            return true;
        }
        return dungeonClass == DungeonPolicy.DungeonClass.ARCHER
                || dungeonClass == DungeonPolicy.DungeonClass.MAGE;
    }

    public static String keyDropTitle(KeySkull skull) {
        return switch (skull) {
            case WITHER -> "Wither Key Dropped";
            case BLOOD -> "Blood Key Dropped";
            case NONE -> "";
        };
    }

    public static boolean keyPickupClearsDrop(String chat) {
        DoorKeyEvent event = doorKeyEvent(chat);
        return event == DoorKeyEvent.WITHER_OBTAINED
                || event == DoorKeyEvent.WITHER_PICKED_UP
                || event == DoorKeyEvent.BLOOD_OBTAINED
                || event == DoorKeyEvent.BLOOD_PICKED_UP;
    }

    public static boolean lockedChestChat(String chat) {
        return DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT).contains("chest is locked");
    }

    public static int clampSecretStaySeconds(int seconds) {
        return Math.max(1, Math.min(SECRET_CLICKED_MAX_SECONDS, seconds));
    }

    public static boolean shouldBoxSecretClick(
            boolean enabled,
            boolean inDungeon,
            boolean inBoss,
            boolean boxInBoss,
            boolean secretBlock) {
        return enabled && inDungeon && secretBlock && (!inBoss || boxInBoss);
    }

    public static boolean isSpiritBearHologram(String hologram) {
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        return text.contains("spirit bear") || text.equals("spirit bear");
    }

    public static boolean isSecretBlockId(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("chest")
                || id.contains("lever")
                || id.contains("skull")
                || id.contains("player_head")
                || id.contains("button")
                || id.contains("wither_essence");
    }

    public static boolean shouldBlockBreakerOnSecret(
            boolean preventSecrets,
            boolean holdingBreaker,
            String blockId) {
        return preventSecrets && holdingBreaker && isSecretBlockId(blockId);
    }

    public static Optional<BreakerCharges> parseBreakerCharges(String loreLine) {
        if (loreLine == null || loreLine.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = BREAKER_CHARGES.matcher(DungeonPolicy.normalize(loreLine));
        if (!matcher.find()) {
            return Optional.empty();
        }
        int current = Integer.parseInt(matcher.group(1));
        int max = Integer.parseInt(matcher.group(2));
        if (max <= 0) {
            return Optional.empty();
        }
        return Optional.of(new BreakerCharges(Math.max(0, current), max));
    }

    public static boolean isDungeonBreakerItem(String name, String skyblockId) {
        String id = skyblockId == null ? "" : skyblockId.toUpperCase(Locale.ROOT);
        String text = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return id.contains("DUNGEON_STONE")
                || id.contains("BREAKER")
                || text.contains("dungeon breaker");
    }

    public static boolean isSuperboomItem(String name, String skyblockId) {
        String id = skyblockId == null ? "" : skyblockId.toUpperCase(Locale.ROOT);
        String text = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return id.contains("SUPERBOOM") || text.contains("superboom");
    }

    public static boolean shouldAutoCloseChest(boolean enabled, String title) {
        if (!enabled) {
            return false;
        }
        String text = DungeonPolicy.normalize(title).toLowerCase(Locale.ROOT);
        return CHEST_TITLE.matcher(text).matches();
    }

    public static double clampCameraDistance(double distance) {
        if (Double.isNaN(distance) || Double.isInfinite(distance)) {
            return DEFAULT_CAMERA_DISTANCE;
        }
        return Math.min(MAX_CAMERA_DISTANCE, Math.max(MIN_CAMERA_DISTANCE, distance));
    }

    public static float cameraZoom(boolean moduleOn, boolean clip, boolean custom, double distance, float vanilla) {
        if (!moduleOn) {
            return vanilla;
        }
        float target = custom ? (float) clampCameraDistance(distance) : vanilla;
        return clip ? target : vanilla;
    }

    /**
     * Queue Terms is a stub warning. Rot Client still exposes
     * an experimental terminal click queue ({@link DungeonLeftoverPolicy}).
     */
    public static boolean queueTermsSupported() {
        return DungeonLeftoverPolicy.queueTermsSupported();
    }
}
