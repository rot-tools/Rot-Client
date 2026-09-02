package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Owns independent mutable values per normalized SkyBlock profile. Unknown
 * profile state is deliberately transient and is never exported.
 */
public final class ProfileScopedState<T> {
    private final Supplier<T> factory;
    private final Map<String, T> profiles = new LinkedHashMap<>();
    private T unknownState;

    public ProfileScopedState(Supplier<T> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        this.factory = factory;
    }

    public T forProfile(String profileName) {
        String key = SkyBlockProfileIdentity.canonicalize(profileName);
        if (SkyBlockProfileIdentity.UNKNOWN.equals(key)) {
            if (unknownState == null) {
                unknownState = requireValue();
            }
            return unknownState;
        }
        return profiles.computeIfAbsent(key, ignored -> requireValue());
    }

    public void clearUnknown() {
        unknownState = null;
    }

    public Map<String, T> snapshotKnownProfiles() {
        return Map.copyOf(profiles);
    }

    private T requireValue() {
        T value = factory.get();
        if (value == null) {
            throw new IllegalStateException("profile state factory returned null");
        }
        return value;
    }
}
