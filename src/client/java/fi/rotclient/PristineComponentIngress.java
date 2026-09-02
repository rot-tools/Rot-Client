package fi.rotclient;

import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Pure boundary adapter for a real Hypixel Pristine system-message component.
 * Assigns a short-lived occurrence identity so duplicate callbacks of the same
 * component stay at-most-once while separate equal-text components remain
 * distinct deliveries.
 */
final class PristineComponentIngress {
    private static final long IDENTITY_RETENTION_MILLIS = 5_000L;
    private static final Map<Component, DeliveryIdentity> DELIVERY_IDENTITIES =
            new IdentityHashMap<>();
    private static long nextDeliveryIdentity;

    private PristineComponentIngress() {
    }

    record Parsed(
            String plainMessage,
            PristineMessageParser.Reward reward,
            String deliveryIdentity) {
        Parsed {
            plainMessage = plainMessage == null ? "" : plainMessage;
            deliveryIdentity = deliveryIdentity == null ? "" : deliveryIdentity;
            if (reward == null) {
                throw new IllegalArgumentException("reward cannot be null");
            }
        }
    }

    static Optional<Parsed> parse(Component message) {
        if (message == null) {
            return Optional.empty();
        }
        String plainMessage = message.getString().trim();
        PristineMessageParser.Reward reward =
                PristineMessageParser.parse(plainMessage);
        if (reward == null) {
            return Optional.empty();
        }
        return Optional.of(new Parsed(
                plainMessage,
                reward,
                deliveryIdentity(message)));
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
        String id = "pristine-component:" + ++nextDeliveryIdentity;
        DELIVERY_IDENTITIES.put(component, new DeliveryIdentity(id, now));
        return id;
    }

    private record DeliveryIdentity(String id, long lastSeenMillis) {
    }
}
