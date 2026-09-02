package fi.rotclient;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure boundary adapter for a real Hypixel Sack system-message component.
 * Keeps component traversal, batch-window parsing, and token parsing in the
 * same production seam used by runtime ingestion tests.
 */
final class SackComponentIngress {
    private static final long IDENTITY_RETENTION_MILLIS = 5_000L;
    private static final Map<Component, DeliveryIdentity> DELIVERY_IDENTITIES =
            new IdentityHashMap<>();
    private static long nextDeliveryIdentity;

    private SackComponentIngress() {
    }

    record ParsedHover(
            String hoverText,
            SackChangeParser.ParseResult parseResult) {
        ParsedHover {
            hoverText = hoverText == null ? "" : hoverText;
            if (parseResult == null) {
                throw new IllegalArgumentException("parseResult cannot be null");
            }
        }
    }

    record Batch(
            String plainMessage,
            long coveredBatchMillis,
            String deliveryIdentity,
            List<ParsedHover> parsedHovers) {
        Batch {
            plainMessage = plainMessage == null ? "" : plainMessage;
            coveredBatchMillis = Math.max(0L, coveredBatchMillis);
            deliveryIdentity = deliveryIdentity == null ? "" : deliveryIdentity;
            parsedHovers = parsedHovers == null
                    ? List.of()
                    : List.copyOf(parsedHovers);
        }
    }

    static Optional<Batch> parse(Component message) {
        if (message == null) {
            return Optional.empty();
        }
        String plainMessage = message.getString().trim();
        if (!plainMessage.regionMatches(true, 0, "[Sacks]", 0, 7)) {
            return Optional.empty();
        }

        long coveredBatchMillis = SackBatchInterval.parseCoveredMillis(
                plainMessage).orElse(0L);
        List<ParsedHover> parsed = new ArrayList<>();
        for (String hoverText : SackHoverExtractor.changeHoverTexts(message)) {
            parsed.add(new ParsedHover(
                    hoverText,
                    SackChangeParser.parseWithDiagnostics(hoverText)));
        }
        return Optional.of(new Batch(
                plainMessage,
                coveredBatchMillis,
                deliveryIdentity(message),
                parsed));
    }

    private static synchronized String deliveryIdentity(Component component) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Component, DeliveryIdentity>> entries =
                DELIVERY_IDENTITIES.entrySet().iterator();
        while (entries.hasNext()) {
            if (now - entries.next().getValue().lastSeenMillis()
                    > IDENTITY_RETENTION_MILLIS) {
                entries.remove();
            }
        }
        DeliveryIdentity existing = DELIVERY_IDENTITIES.get(component);
        if (existing != null) {
            DELIVERY_IDENTITIES.put(
                    component,
                    new DeliveryIdentity(existing.id(), now));
            return existing.id();
        }
        String id = "sack-component:" + ++nextDeliveryIdentity;
        DELIVERY_IDENTITIES.put(component, new DeliveryIdentity(id, now));
        return id;
    }

    private record DeliveryIdentity(String id, long lastSeenMillis) {
    }
}
