package fi.rotclient;

/**
 * Farm Keys: remap attack/jump while the cheat is on, optionally
 * lock look. Crop-farming helper, not dungeon-key farming.
 */
public final class FarmKeysPolicy {
    private FarmKeysPolicy() {
    }

    public static boolean shouldLockCamera(boolean moduleOn, boolean lockCamera) {
        return moduleOn && lockCamera;
    }

    public static boolean shouldRemap(boolean moduleOn, String bind) {
        return moduleOn && bind != null && !bind.isBlank();
    }
}
