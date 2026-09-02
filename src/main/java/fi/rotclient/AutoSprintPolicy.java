package fi.rotclient;

/**
 * Pure decision for Auto Sprint: when the module is enabled, the sprint input
 * expression is forced true so vanilla movement keeps the player sprinting
 * while moving forward.
 */
public final class AutoSprintPolicy {
    private AutoSprintPolicy() {
    }

    /**
     * @param originalSprintInput value of {@code Input.sprint()} before override
     * @param moduleEnabled       Auto Sprint module toggle
     * @return sprint input after Auto Sprint is applied
     */
    public static boolean resolveSprintInput(
            boolean originalSprintInput,
            boolean moduleEnabled) {
        return originalSprintInput || moduleEnabled;
    }
}
