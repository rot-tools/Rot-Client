package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchAuctionSnapshotAssemblerTest {
    @Test
    void assemblesPagesInPageOrder() {
        MarketWatchAuctionPage pageOne =
                page(
                        1,
                        2,
                        2,
                        12345L,
                        auction("second"));

        MarketWatchAuctionPage pageZero =
                page(
                        0,
                        2,
                        2,
                        12345L,
                        auction("first"));

        MarketWatchAuctionSnapshot snapshot =
                MarketWatchAuctionSnapshotAssembler.assemble(
                        List.of(pageOne, pageZero));

        assertNotNull(snapshot);
        assertEquals(12345L, snapshot.lastUpdated());
        assertEquals(2, snapshot.totalAuctions());
        assertEquals(2, snapshot.auctions().size());

        assertEquals(
                "first",
                snapshot.auctions().get(0).uuid());

        assertEquals(
                "second",
                snapshot.auctions().get(1).uuid());
    }

    @Test
    void rejectsMixedApiUpdates() {
        MarketWatchAuctionSnapshot snapshot =
                MarketWatchAuctionSnapshotAssembler.assemble(
                        List.of(
                                page(
                                        0,
                                        2,
                                        2,
                                        100L,
                                        auction("one")),
                                page(
                                        1,
                                        2,
                                        2,
                                        200L,
                                        auction("two"))));

        assertNull(snapshot);
    }

    @Test
    void rejectsMissingPage() {
        MarketWatchAuctionSnapshot snapshot =
                MarketWatchAuctionSnapshotAssembler.assemble(
                        List.of(
                                page(
                                        0,
                                        2,
                                        2,
                                        100L,
                                        auction("one"))));

        assertNull(snapshot);
    }

    @Test
    void rejectsDuplicatePageNumber() {
        MarketWatchAuctionSnapshot snapshot =
                MarketWatchAuctionSnapshotAssembler.assemble(
                        List.of(
                                page(
                                        0,
                                        2,
                                        2,
                                        100L,
                                        auction("one")),
                                page(
                                        0,
                                        2,
                                        2,
                                        100L,
                                        auction("two"))));

        assertNull(snapshot);
    }

    @Test
    void removesDuplicateAuctionUuid() {
        MarketWatchAuction shared =
                auction("same");

        MarketWatchAuctionSnapshot snapshot =
                MarketWatchAuctionSnapshotAssembler.assemble(
                        List.of(
                                page(
                                        0,
                                        2,
                                        2,
                                        100L,
                                        shared),
                                page(
                                        1,
                                        2,
                                        2,
                                        100L,
                                        shared)));

        assertNotNull(snapshot);
        assertEquals(1, snapshot.auctions().size());
    }

    private static MarketWatchAuctionPage page(
            int page,
            int totalPages,
            int totalAuctions,
            long lastUpdated,
            MarketWatchAuction... auctions) {

        return new MarketWatchAuctionPage(
                page,
                totalPages,
                totalAuctions,
                lastUpdated,
                List.of(auctions));
    }

    private static MarketWatchAuction auction(
            String uuid) {

        return new MarketWatchAuction(
                uuid,
                "Test Item",
                "weapon",
                "LEGENDARY",
                1000L,
                2000L,
                1_000_000L,
                0L,
                true,
                "item-data");
    }
}