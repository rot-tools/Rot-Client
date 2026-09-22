package fi.rotclient;

import java.util.Set;

/**
 * User-facing evidence state for QoL parent modules.
 *
 * <p>Implementation availability and runtime evidence are intentionally
 * separate. A module only reaches {@link Status#READY} after a code review and
 * a controlled Minecraft validation; passing automated tests alone never
 * promotes a module. A ready module shows no status banner at all.</p>
 */
public final class QolModuleEvidence {
    private static final Set<String> RUNTIME_VERIFIED = Set.of();

    private QolModuleEvidence() {
    }

    public static Status status(QolUtilityCatalog.ModuleDef module) {
        if (module == null || !module.runtimeReady()) {
            return Status.UPCOMING;
        }
        if (module.wip()) {
            return Status.WORK_IN_PROGRESS;
        }
        return RUNTIME_VERIFIED.contains(module.id())
                ? Status.READY
                : Status.NEEDS_TESTING;
    }

    public static boolean runtimeVerified(String moduleId) {
        return moduleId != null && RUNTIME_VERIFIED.contains(moduleId);
    }

    public static Set<String> runtimeVerifiedIds() {
        return RUNTIME_VERIFIED;
    }

    public enum Status {
        READY(""),
        NEEDS_TESTING("Needs testing"),
        WORK_IN_PROGRESS("Work in progress"),
        UPCOMING("Upcoming");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** Whether the dashboard should draw a status banner for this state. */
        public boolean showsBanner() {
            return this != READY;
        }
    }
}
