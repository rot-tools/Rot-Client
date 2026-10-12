package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SkyBlockMarketItemIdentityPolicyTest {
    @Test
    void petPriceKeysUseNeuRarityOrdinalsIncludingSpecialPhoenix() {
        List<String> rarities = List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY",
                "MYTHIC", "DIVINE", "SPECIAL", "VERY_SPECIAL");
        for (int index = 0; index < rarities.size(); index++) {
            String info = "{\"type\":\"PHOENIX\",\"tier\":\"" + rarities.get(index) + "\",\"exp\":1000000}";
            assertEquals("PHOENIX;" + index,
                    SkyBlockMarketItemIdentityPolicy.quoteId("PET", info, "Phoenix", "Pets"));
        }
        assertEquals("GOLDEN_DRAGON;4", SkyBlockMarketItemIdentityPolicy.petQuoteId(
                "{\"type\":\"GOLDEN_DRAGON\",\"tier\":\"LEGENDARY\"}"));
    }

    @Test
    void malformedAndUnrecognizedPetsDoNotRequestAGenericPetPrice() {
        for (String info : List.of("", "broken", "[]", "{}", "{\"type\":42,\"tier\":\"SPECIAL\"}",
                "{\"type\":\"PHOENIX\",\"tier\":7}",
                "{\"type\":\"PHOENIX\",\"tier\":\"NEW_RARITY\"}",
                "{\"type\":\"PHOENIX;3\",\"tier\":\"SPECIAL\"}")) {
            assertEquals("", SkyBlockMarketItemIdentityPolicy.petQuoteId(info));
        }
        assertEquals("", SkyBlockMarketItemIdentityPolicy.petQuoteId(null));
        assertEquals("", SkyBlockMarketItemIdentityPolicy.petQuoteId("x".repeat(65_537)));
    }

    @Test
    void onlyVerifiedBaitSackIconsResolveTheMisleadingHookId() {
        assertEquals("FISH_BAIT", SkyBlockMarketItemIdentityPolicy.quoteId(
                "COMMON_HOOK", "", "§fFish Bait", "§8Bait Sack"));
        assertEquals("COMMON_HOOK", SkyBlockMarketItemIdentityPolicy.quoteId(
                "COMMON_HOOK", "", "Fish Bait", "Inventory"));
        assertEquals("COMMON_HOOK", SkyBlockMarketItemIdentityPolicy.quoteId(
                "COMMON_HOOK", "", "Common Hook", "Bait Sack"));
        assertEquals("", SkyBlockMarketItemIdentityPolicy.quoteId(
                "COMMON_HOOK", "", "Unknown Fish Bait", "Bait Sack"));
        assertEquals("FISH_BAIT", SkyBlockMarketItemIdentityPolicy.quoteId(
                "FISH_BAIT", "", "Fish Bait", "Inventory"));
        assertEquals("ASPECT_OF_THE_END", SkyBlockMarketItemIdentityPolicy.quoteId(
                "ASPECT_OF_THE_END", "", "AOTE", "Bait Sack"));
        assertTrue(SkyBlockMarketItemIdentityPolicy.isBaitSackTitle("Large Bait Sack"));
        assertFalse(SkyBlockMarketItemIdentityPolicy.isBaitSackTitle("Fishing Bag"));
    }
}
