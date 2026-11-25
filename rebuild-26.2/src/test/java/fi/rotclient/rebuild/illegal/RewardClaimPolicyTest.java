package fi.rotclient.rebuild.illegal;

import java.net.URI;
import java.util.List;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RewardClaimPolicyTest {
    private static final String ID = "Ab12Cd34";
    private static final String PAGE = """
            <script>
            window.securityToken = "a&b+?";
            window.appData = '{"id":"Ab12Cd34","skippable":true,"activeAd":0,"rewards":[null,{"reward":"coins","rarity":"COMMON","amount":5000,"gameType":"SKYBLOCK"},{"reward":"dust","rarity":"RARE","amount":30},{"reward":"housing_package","rarity":"EPIC","amount":1,"package":"specialoccasion_reward_card_skull_dragon"}],"dailyStreak":{"value":7,"score":0,"highScore":18}}';
            window.i18n = {"type.coins":"{$game} Coins","type.dust":"Mystery Dust","housing.skull.dragon":"Dragon Head"};
            </script>
            """;

    @Test
    void preservesServerStreaksRewardTypesAndOriginalIndices() {
        var page = RewardClaimPolicy.parsePage(PAGE, ID);
        assertEquals("Streak 0  ·  Best 18", RewardClaimPolicy.streakSummary(page));
        assertEquals(3, page.rewards().size());
        assertEquals(1, page.rewards().getFirst().index());
        assertEquals("coins", page.rewards().getFirst().type());
        assertEquals("5,000 SkyBlock Coins", page.rewards().getFirst().title());
        assertEquals("dust", page.rewards().get(1).type());
        assertEquals("30 Mystery Dust", page.rewards().get(1).title());
        assertEquals("Dragon Head", page.rewards().get(2).title());
        var missing = RewardClaimPolicy.parsePage(PAGE.replace("\"score\":0,\"highScore\":18", "\"highScore\":null"), ID);
        assertEquals("Streak —  ·  Best —", RewardClaimPolicy.streakSummary(missing));
    }

    @Test
    void checksLinkHostPathAndPageIdentity() {
        String link = "https://rewards.hypixel.net/claim-reward/" + ID;
        assertEquals(ID, RewardClaimPolicy.findClaimId("Daily Rewards: " + link).orElseThrow());
        assertEquals(ID, RewardClaimPolicy.findClaimId(URI.create(link)).orElseThrow());
        assertTrue(RewardClaimPolicy.findClaimId("https://fake.rewards.hypixel.net/claim-reward/" + ID).isEmpty());
        assertTrue(RewardClaimPolicy.findClaimId(link + "X").isEmpty());
        assertTrue(RewardClaimPolicy.findClaimId(URI.create(link + "?key=value")).isEmpty());
        assertThrows(IllegalStateException.class, () -> RewardClaimPolicy.parsePage(PAGE, "Zz12Cd34"));
    }

    @Test
    void recognizesTheRewardBookLinkEvenWhenItsUrlIsHiddenBehindTheClickPrompt() {
        var official = URI.create("https://rewards.hypixel.net/claim-reward/" + ID);
        var prompt = Component.literal("✦CLICK TO CLAIM!✦")
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(official)));
        var rewardPage = Component.literal("\nDaily Reward\nVisit our website\n").append(prompt);
        assertEquals(ID, RewardClaim.findBookId(List.of(rewardPage)).orElseThrow());
        assertTrue(RewardClaim.findBookId(List.of(Component.literal("Ordinary book\n").append(prompt))).isEmpty());
        assertTrue(RewardClaim.findBookId(List.of(Component.literal("Daily Reward\nClick to claim!"))).isEmpty());
    }

    @Test
    void recognizesTheRewardBookBeforeTheSeparateChatLinkArrives() {
        var book = List.<Component>of(Component.literal("✦ Daily Reward ✦\nVisit our website to choose 1 of 3 random Reward Cards."));
        assertTrue(RewardClaim.isRewardBook(book));
        assertTrue(RewardClaim.findBookId(book).isEmpty());
        assertFalse(RewardClaim.isRewardBook(List.of(Component.literal("My Daily Reward notes."))));
        var systemMessage = "\nClick the link to visit our website and claim your reward: "
                + "https://rewards.hypixel.net/claim-reward/" + ID + "\n";
        assertEquals(ID, RewardClaimPolicy.findClaimId(systemMessage).orElseThrow());
    }

    @Test
    void onlyOffersRealChoicesOnPagesThatPermitDirectClaim() {
        var page = RewardClaimPolicy.parsePage(PAGE, ID);
        String query = RewardClaimPolicy.claimUrl(page, 1).getRawQuery();
        assertTrue(query.startsWith("option=1&"));
        assertTrue(query.contains("_csrf=a%26b%2B%3F"));
        assertThrows(IllegalArgumentException.class, () -> RewardClaimPolicy.claimUrl(page, 0));
        var locked = RewardClaimPolicy.parsePage(PAGE.replace("\"skippable\":true", "\"skippable\":false"), ID);
        assertThrows(IllegalArgumentException.class, () -> RewardClaimPolicy.claimUrl(locked, 1));
    }

    @Test
    void checksForClaimConfirmationInsteadOfAssumingEveryHttp200Succeeded() {
        assertTrue(RewardClaimPolicy.successfulClaim("{\"success\":true}"));
        assertTrue(RewardClaimPolicy.successfulClaim("{\"status\":\"ok\"}"));
        assertFalse(RewardClaimPolicy.successfulClaim("{\"success\":false}"));
        assertFalse(RewardClaimPolicy.successfulClaim("<html>Reward page</html>"));
    }
}
