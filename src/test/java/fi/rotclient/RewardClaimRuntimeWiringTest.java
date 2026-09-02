package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RewardClaimRuntimeWiringTest {
    @Test
    void dailyRewardClaimIsWiredThroughCatalogConfigChatAndGui() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/fi/rotclient/QolUtilityCatalog.java"));
        String extras = Files.readString(Path.of("src/main/java/fi/rotclient/QolSkyblockExtras.java"));
        String client = Files.readString(Path.of("src/client/java/fi/rotclient/RotClientClient.java"));
        String runtime = Files.readString(Path.of("src/client/java/fi/rotclient/RewardClaimRuntime.java"));
        String mixins = Files.readString(Path.of("src/client/resources/rotclient.client.mixins.json"));
        assertTrue(catalog.contains("\"qol.reward_claim\""));
        assertTrue(catalog.contains("Daily Reward Claim"));
        assertTrue(extras.contains("rewardClaimEnabled"));
        assertTrue(extras.contains("case \"qol.reward_claim\""));
        assertTrue(client.contains("RewardClaimRuntime.onChat"));
        assertTrue(client.contains("RewardClaimRuntime.shouldHideMessage"));
        assertTrue(runtime.contains("rewards.hypixel.net"));
        assertTrue(runtime.contains("HttpClient"));
        assertTrue(mixins.contains("GuiRewardClaimMixin"));
        assertFalse(runtime.contains("thatgravyboat"));
        assertFalse(catalog.toLowerCase().contains("rewardclaim"));
    }
}
