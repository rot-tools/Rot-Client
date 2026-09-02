package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rules for keeping Rot Client invisible on multiplayer handshakes: vanilla
 * brand instead of {@code fabric}, and no {@code rotclient} namespace in
 * outbound channel registration payloads.
 */
public final class ClientNetworkPrivacyPolicy {
    public static final String MOD_ID = "rotclient";
    public static final String VANILLA_BRAND = "vanilla";

    private ClientNetworkPrivacyPolicy() {
    }

    public static boolean isSensitiveNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        return MOD_ID.equals(namespace.toLowerCase(Locale.ROOT));
    }

    public static boolean isSensitiveChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return false;
        }
        String lower = channel.toLowerCase(Locale.ROOT);
        return lower.startsWith(MOD_ID + ":") || lower.contains(MOD_ID);
    }

    public static Set<String> filterOutboundChannels(Set<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return channels;
        }
        Set<String> filtered = new HashSet<>();
        for (String channel : channels) {
            if (!isSensitiveChannel(channel)) {
                filtered.add(channel);
            }
        }
        return filtered;
    }

    public static List<String> filterOutboundChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return channels;
        }
        List<String> filtered = new ArrayList<>(channels.size());
        for (String channel : channels) {
            if (!isSensitiveChannel(channel)) {
                filtered.add(channel);
            }
        }
        return filtered;
    }

    public static boolean shouldReplaceBrand(String brand) {
        return brand == null || !VANILLA_BRAND.equals(brand);
    }
}
