package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClientNetworkPrivacyPolicyTest {
    @Test
    void sensitiveNamespaceMatchesRotClientOnly() {
        assertTrue(ClientNetworkPrivacyPolicy.isSensitiveNamespace("rotclient"));
        assertTrue(ClientNetworkPrivacyPolicy.isSensitiveNamespace("RotClient"));
        assertFalse(ClientNetworkPrivacyPolicy.isSensitiveNamespace("fabric"));
        assertFalse(ClientNetworkPrivacyPolicy.isSensitiveNamespace("minecraft"));
    }

    @Test
    void sensitiveChannelMatchesRotClientIdentifiers() {
        assertTrue(ClientNetworkPrivacyPolicy.isSensitiveChannel("rotclient:tracker"));
        assertTrue(ClientNetworkPrivacyPolicy.isSensitiveChannel("fabric:rotclient"));
        assertFalse(ClientNetworkPrivacyPolicy.isSensitiveChannel("minecraft:brand"));
        assertFalse(ClientNetworkPrivacyPolicy.isSensitiveChannel("c:register"));
    }

    @Test
    void filterOutboundChannelsRemovesRotClientEntries() {
        Set<String> filtered = ClientNetworkPrivacyPolicy.filterOutboundChannels(
                Set.of("minecraft:register", "rotclient:tracker", "c:register"));
        assertEquals(Set.of("minecraft:register", "c:register"), filtered);
        List<String> filteredList = ClientNetworkPrivacyPolicy.filterOutboundChannels(
                List.of("rotclient:qol_overlay", "fabric:hello"));
        assertEquals(List.of("fabric:hello"), filteredList);
    }

    @Test
    void vanillaBrandIsReplacementTarget() {
        assertTrue(ClientNetworkPrivacyPolicy.shouldReplaceBrand("fabric"));
        assertTrue(ClientNetworkPrivacyPolicy.shouldReplaceBrand(null));
        assertFalse(ClientNetworkPrivacyPolicy.shouldReplaceBrand("vanilla"));
    }
}
