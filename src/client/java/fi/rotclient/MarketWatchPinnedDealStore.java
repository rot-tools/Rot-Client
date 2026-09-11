package fi.rotclient;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

final class MarketWatchPinnedDealStore {

    private static final int MAX_PINNED =
            3;

    private static final ArrayDeque<MarketWatchPinnedDeal> PINS =
            new ArrayDeque<>();

    private MarketWatchPinnedDealStore() {
    }

    static synchronized boolean toggle(
            MarketWatchOpportunity opportunity) {

        if (opportunity == null
                || opportunity.id() == null
                || opportunity.id().isBlank()) {

            return false;
        }

        Iterator<MarketWatchPinnedDeal> iterator =
                PINS.iterator();

        while (iterator.hasNext()) {

            MarketWatchPinnedDeal pinned =
                    iterator.next();

            if (opportunity.id()
                    .equals(
                            pinned.id())) {

                iterator.remove();

                return false;
            }
        }

        PINS.addFirst(
                MarketWatchPinnedDeal.from(
                        opportunity));

        while (PINS.size()
                > MAX_PINNED) {

            PINS.removeLast();
        }

        return true;
    }

    static synchronized boolean remove(
            String opportunityId) {

        if (opportunityId == null
                || opportunityId.isBlank()) {

            return false;
        }

        Iterator<MarketWatchPinnedDeal> iterator =
                PINS.iterator();

        while (iterator.hasNext()) {

            MarketWatchPinnedDeal pinned =
                    iterator.next();

            if (pinned != null
                    && opportunityId.equals(
                            pinned.id())) {

                iterator.remove();

                return true;
            }
        }

        return false;
    }

    static synchronized void clear() {
        PINS.clear();
    }

    static synchronized boolean contains(
            String opportunityId) {

        if (opportunityId == null
                || opportunityId.isBlank()) {

            return false;
        }

        for (MarketWatchPinnedDeal pinned
                : PINS) {

            if (pinned != null
                    && opportunityId.equals(
                            pinned.id())) {

                return true;
            }
        }

        return false;
    }

    static synchronized List<MarketWatchPinnedDeal> all() {
        return List.copyOf(
                PINS);
    }
}