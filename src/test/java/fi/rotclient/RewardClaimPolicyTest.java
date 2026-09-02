package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class RewardClaimPolicyTest {
    private static final String PAGE = """
            <html><script>
            window.securityToken = "csrf-token";
            window.appData = '{"id":"Ab12Cd34","skippable":true,"activeAd":0,"rewards":[{"reward":"coins","rarity":"COMMON","amount":5000,"gameType":"SKYBLOCK"},{"reward":"dust","rarity":"RARE","amount":2},{"reward":"housing_package","rarity":"EPIC","amount":1,"package":"specialoccasion_reward_card_skull_dragon"}],"dailyStreak":{"value":4,"score":4,"highScore":20},"ad":{"duration":15,"link":"https://store.hypixel.net"}}';
            window.i18n = {"type.coins":"{$game} Coins","type.dust":"Mystery Dust","housing.skull.dragon":"Dragon Head"};
            </script></html>
            """;

    @Test
    void extractsEightCharacterClaimIdsFromChat() {
        Optional<String> id = RewardClaimPolicy.findClaimId(
                "Click the link to visit our website and claim your reward: https://rewards.hypixel.net/claim-reward/Ab12Cd34");
        assertEquals("Ab12Cd34", id.orElseThrow());
        assertTrue(RewardClaimPolicy.findClaimId("no rewards today").isEmpty());
    }

    @Test
    void parsesHypixelClaimPagePayload() {
        RewardClaimPolicy.ParsedPage page = RewardClaimPolicy.parsePage(PAGE);
        assertEquals("Ab12Cd34", page.id());
        assertEquals("csrf-token", page.csrfToken());
        assertTrue(page.skippable());
        assertEquals(4, page.streakValue());
        assertEquals(20, page.streakBest());
        assertEquals(3, page.rewards().size());
        assertEquals("5000× Skyblock Coins", page.rewards().get(0).title());
        assertEquals("2× Mystery Dust", page.rewards().get(1).title());
        assertEquals("Dragon Head", page.rewards().get(2).title());
        assertEquals(0, RewardClaimPolicy.adWaitSeconds(page, true));
        assertTrue(RewardClaimPolicy.claimUrl(page, 1).contains("option=1"));
        assertTrue(RewardClaimPolicy.claimUrl(page, 1).contains("csrf-token"));
    }

    @Test
    void adWaitAppliesOnlyWhenThePageIsNotSkippable() {
        RewardClaimPolicy.ParsedPage locked = new RewardClaimPolicy.ParsedPage(
                "id", "tok", false, 0, 1, 1, 1, 12, "", java.util.List.of(), Map.of());
        assertEquals(12, RewardClaimPolicy.adWaitSeconds(locked, true));
        assertEquals(0, RewardClaimPolicy.adWaitSeconds(locked, false));
        assertEquals(0xFFA855F7, RewardClaimPolicy.rarityColor("EPIC"));
    }

    @Test
    void vanityAndMissingI18nFallBackToReadableNames() {
        assertEquals(
                "Wave Dance",
                RewardClaimPolicy.titleFor("add_vanity", "RARE", 1, "", "", "wave_dance", Map.of()));
    }
}
