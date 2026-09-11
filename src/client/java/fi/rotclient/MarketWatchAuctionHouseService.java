package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class MarketWatchAuctionHouseService {
    private static final long POLL_SECONDS = 30L;

    private static final String AUCTIONS_ENDPOINT =
            "https://api.hypixel.net/v2/skyblock/auctions?page=";

    private static final HttpClient CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread =
                        new Thread(
                                r,
                                "RotClient-MarketWatch-Auctions");
                thread.setDaemon(true);
                return thread;
            });

    private static volatile boolean started;

    private MarketWatchAuctionHouseService() {
    }

    static void start() {
        if (started) {
            return;
        }

        started = true;

        EXECUTOR.scheduleWithFixedDelay(
                MarketWatchAuctionHouseService::refresh,
                0L,
                POLL_SECONDS,
                TimeUnit.SECONDS);
    }

    private static void refresh() {
        try {
            refreshSnapshot();
        } catch (RuntimeException ignored) {
            // A malformed or temporary API response must not affect the client.
        }
    }

    static boolean refreshSnapshot() {
        MarketWatchAuctionPage firstPage =
                fetchPage(0);

        if (!usableFirstPage(firstPage)) {
            return false;
        }

        MarketWatchDataService.AuctionState current =
                MarketWatchDataService.currentAuctions();

        if (current != null
                && current.snapshot() != null
                && current.snapshot().lastUpdated()
                        == firstPage.lastUpdated()) {
            return false;
        }

        int totalPages = firstPage.totalPages();

        List<MarketWatchAuctionPage> pages =
                new ArrayList<>(totalPages);

        pages.add(firstPage);

        for (int pageNumber = 1;
                pageNumber < totalPages;
                pageNumber++) {

            MarketWatchAuctionPage page =
                    fetchPage(pageNumber);

            if (!sameSnapshot(
                    firstPage,
                    page,
                    pageNumber)) {
                return false;
            }

            pages.add(page);
        }

        MarketWatchAuctionSnapshot snapshot =
                MarketWatchAuctionSnapshotAssembler.assemble(
                        pages);

        if (snapshot == null) {
            return false;
        }

        long observedAtMillis =
                System.currentTimeMillis();

        MarketWatchDataService.publishAuctions(
                snapshot,
                observedAtMillis);

        MarketWatchRuntime.publishAuctionSnapshot(
                snapshot,
                observedAtMillis);


        return true;
    }

    private static MarketWatchAuctionPage fetchPage(
            int pageNumber) {

        if (pageNumber < 0) {
            return null;
        }

        try {
            URI uri =
                    URI.create(
                            AUCTIONS_ENDPOINT
                                    + pageNumber);

            HttpRequest request =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(15))
                            .header(
                                    "User-Agent",
                                    "RotClient/2.0.1+mc26.2")
                            .GET()
                            .build();

            HttpResponse<String> response =
                    CLIENT.send(
                            request,
                            HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonElement parsed =
                    JsonParser.parseString(
                            response.body());

            if (parsed == null
                    || !parsed.isJsonObject()) {
                return null;
            }

            return MarketWatchAuctionPageParser.parse(
                    parsed.getAsJsonObject());

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean usableFirstPage(
            MarketWatchAuctionPage page) {

        return page != null
                && page.page() == 0
                && page.totalPages() > 0
                && page.totalAuctions() >= 0
                && page.lastUpdated() >= 0L;
    }

    private static boolean sameSnapshot(
            MarketWatchAuctionPage first,
            MarketWatchAuctionPage candidate,
            int expectedPage) {

        return candidate != null
                && candidate.page() == expectedPage
                && candidate.lastUpdated()
                        == first.lastUpdated()
                && candidate.totalPages()
                        == first.totalPages()
                && candidate.totalAuctions()
                        == first.totalAuctions();
    }
}