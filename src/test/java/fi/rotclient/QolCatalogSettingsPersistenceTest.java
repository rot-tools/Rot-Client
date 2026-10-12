package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Catalog controls must change the persisted configuration they advertise. */
class QolCatalogSettingsPersistenceTest {
    @Test
    void everyCatalogToggleNumberColorAndEnumHasAWritableConfigurationRoute() {
        List<String> failures = new ArrayList<>();
        for (var module : QolUtilityCatalog.modules()) {
            for (var setting : module.settings()) {
                QolUtilityConfig config = new QolUtilityConfig();
                TrackerConfig tracker = new TrackerConfig();
                String id = setting.id();
                switch (setting.type()) {
                    case TOGGLE -> {
                        // Lighting is owned by the live FullbrightNightPolicy;
                        // its migration/state contracts have a separate test.
                        if (id.startsWith("qol.fullbright.")) break;
                        Boolean tracked = MiningTrackerConfigBridge.readBoolean(tracker, id);
                        if (tracked != null) {
                            assertTrue(MiningTrackerConfigBridge.writeBoolean(tracker, id, !tracked), id);
                            assertEquals(!tracked, MiningTrackerConfigBridge.readBoolean(tracker, id), id);
                            break;
                        }
                        Boolean old = config.readBoolean(id);
                        if (old == null) { failures.add(id + ": missing toggle route"); break; }
                        config.writeBoolean(id, !old);
                        if (old.equals(config.readBoolean(id))) failures.add(id + ": toggle inert");
                    }
                    case NUMBER -> {
                        Double old = config.readNumber(id);
                        if (old == null) { failures.add(id + ": missing numeric route"); break; }
                        var spec = QolNumberSettings.spec(id);
                        if (spec == null) { failures.add(id + ": missing number bounds"); break; }
                        double next = spec.fromFraction(old == spec.fromFraction(1) ? 0 : 1);
                        if (!config.writeNumber(id, next) || config.readNumber(id) == null
                                || Math.abs(config.readNumber(id) - next) > 0.0001)
                            failures.add(id + ": numeric value did not round-trip " + next);
                    }
                    case COLOR -> {
                        Integer old = config.readColor(id);
                        if (old == null) { failures.add(id + ": missing color route"); break; }
                        int next = old ^ 0x00FFFFFF;
                        if (!config.writeColor(id, next) || !Integer.valueOf(next).equals(config.readColor(id)))
                            failures.add(id + ": color inert");
                    }
                    case ENUM -> {
                        for (String option : setting.enumOptions()) {
                            if (!config.writeEnum(id, option) || !option.equals(config.readEnum(id)))
                                failures.add(id + ": enum option " + option + " does not round-trip");
                        }
                    }
                    default -> { /* Text/keybind/action controls have their own input contracts. */ }
                }
            }
        }
        assertEquals(List.of(), failures);
    }
}
