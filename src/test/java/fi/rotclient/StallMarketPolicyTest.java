package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class StallMarketPolicyTest {
    @Test
    void bazaarAndBinScreensMatchStall() {
        assertTrue(StallMarketPolicy.isBazaar("Bazaar ➜ Ores", 54));
        assertFalse(StallMarketPolicy.isBazaar("Bazaar ➜ Ores", 27));
        assertTrue(StallMarketPolicy.isBinAuctionInit("BIN Auction View", 54));
        assertTrue(StallMarketPolicy.isBinAuctionConfirm("Confirm Purchase", 27));
        assertTrue(StallMarketPolicy.isBinAuction("BIN Auction View", 54));
    }

    @Test
    void searchCleansItemNameAndFindsSearchSlot() {
        assertEquals(
                "Enchanted Diamond",
                StallMarketPolicy.cleanItemNameForSearch("§a[Lvl 1] Enchanted Diamond (x64)"));
        List<StallMarketPolicy.SlotView> slots = List.of(
                new StallMarketPolicy.SlotView(
                        45,
                        "minecraft:name_tag",
                        "§aSearch",
                        List.of("§7Click to search")),
                new StallMarketPolicy.SlotView(
                        10,
                        "minecraft:diamond",
                        "Enchanted Diamond",
                        List.of()));
        assertEquals(45, StallMarketPolicy.findSearchSlot(slots));
        assertEquals(
                List.of("Enchanted Diamond"),
                StallMarketPolicy.visibleBazaarItems(slots));
    }

    @Test
    void sellInstantlyParsesTotalAndBlocksLeftClick() {
        List<String> lore = List.of("§7Total: 2,500,000 coins");
        assertEquals(2_500_000L, StallMarketPolicy.extractSellInstantlyAmount(lore));
        StallMarketPolicy.SellBlock blocked = StallMarketPolicy.sellProtection(
                true,
                true,
                "Bazaar ➜ Instant Sell",
                "Sell Instantly",
                lore,
                0,
                false,
                1_000_000L);
        assertTrue(blocked.block());
        assertTrue(blocked.message().contains("Sell Instantly"));
        StallMarketPolicy.SellBlock allowed = StallMarketPolicy.sellProtection(
                true,
                true,
                "Bazaar ➜ Instant Sell",
                "Sell Instantly",
                lore,
                0,
                true,
                1_000_000L);
        assertFalse(allowed.block());
    }

    @Test
    void sellSacksBlocksAnyClickWithoutCtrl() {
        List<String> lore = List.of("You earn: 12,000,000 coins");
        StallMarketPolicy.SellBlock blocked = StallMarketPolicy.sellProtection(
                true,
                true,
                "NPC ➜ Sell",
                "Sell Sacks Now",
                lore,
                1,
                false,
                1_000_000L);
        assertTrue(blocked.block());
        assertEquals("Sell Sacks Now", blocked.buttonName());
        List<String> tip = StallMarketPolicy.sellProtectionTooltip(
                true,
                true,
                "NPC ➜ Sell",
                "Sell Inventory Now",
                lore,
                1_000_000L);
        assertTrue(tip.stream().anyMatch(line -> line.contains("Sell Protection")));
    }

    @Test
    void angryCoopBlocksForeignSellerAndClaimAll() {
        List<String> lore = List.of("Seller: Steve", "Click to claim!");
        assertEquals(
                "Steve",
                StallMarketPolicy.foreignActor(
                        lore, "henri", StallMarketPolicy.CoopScreen.SELLER).orElseThrow());
        StallMarketPolicy.CoopBlock claimAll = StallMarketPolicy.coopProtection(
                true,
                true,
                "Manage Auctions",
                "Claim All",
                List.of(),
                true,
                "",
                false);
        assertTrue(claimAll.block());
        StallMarketPolicy.CoopBlock foreign = StallMarketPolicy.coopProtection(
                true,
                true,
                "Manage Auctions",
                "Enchanted Diamond",
                lore,
                false,
                "Steve",
                false);
        assertTrue(foreign.block());
        assertTrue(foreign.message().contains("Steve"));
        StallMarketPolicy.CoopBlock ctrl = StallMarketPolicy.coopProtection(
                true,
                true,
                "Manage Auctions",
                "Claim All",
                List.of(),
                true,
                "",
                true);
        assertFalse(ctrl.block());
    }

    @Test
    void binStatusAndSlotsMatchStallBinGui() {
        StallMarketPolicy.AuctionStatus buying = StallMarketPolicy.updateAuctionStatus(
                StallMarketPolicy.AuctionStatus.INIT,
                "minecraft:gold_block",
                "Buy Item Right Now",
                "Click to purchase!");
        assertEquals(StallMarketPolicy.AuctionStatus.AUCTION_BUYING, buying);
        assertEquals(31, StallMarketPolicy.actionSlot(buying));
        assertTrue(StallMarketPolicy.shouldHighlightBinSlot(buying, 31));
        StallMarketPolicy.AuctionStatus confirm = StallMarketPolicy.updateAuctionStatus(
                buying,
                "minecraft:green_terracotta",
                "Confirm",
                "");
        assertEquals(StallMarketPolicy.AuctionStatus.AUCTION_CONFIRMING, confirm);
        assertEquals(11, StallMarketPolicy.actionSlot(confirm));
        assertTrue(StallMarketPolicy.shouldHighlightBinSlot(confirm, 11));
    }

    @Test
    void listingHighlightComparesAgainstLowestBin() {
        assertEquals(
                StallMarketPolicy.HighlightKind.UNDER,
                StallMarketPolicy.listingHighlight(900_000D, 1_000_000D));
        assertEquals(
                StallMarketPolicy.HighlightKind.OVER,
                StallMarketPolicy.listingHighlight(1_200_000D, 1_000_000D));
        assertEquals(
                StallMarketPolicy.HighlightKind.NONE,
                StallMarketPolicy.listingHighlight(1_000_000D, 1_000_000D));
        assertEquals(
                1_250_000D,
                StallMarketPolicy.listingPrice(List.of("Buy it now: 1,250,000 coins")),
                0.001D);
    }

    @Test
    void amountParserAcceptsStallSuffixes() {
        assertEquals(1000L, StallMarketPolicy.parseAmountString("1000"));
        assertEquals(2000L, StallMarketPolicy.parseAmountString("2k"));
        assertEquals(3_000_000L, StallMarketPolicy.parseAmountString("3m"));
        assertEquals(1_500_000_000L, StallMarketPolicy.parseAmountString("1.5b"));
        assertThrows(NumberFormatException.class, () -> StallMarketPolicy.parseAmountString("nope"));
        assertEquals("2.5M", StallMarketPolicy.formatCoins(2_500_000L));
    }
}
