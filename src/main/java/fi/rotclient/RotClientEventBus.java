package fi.rotclient;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Small typed, reflection-free event bus for normalized Rot Client domain
 * observations. Minecraft callbacks publish immutable records; feature policy
 * remains independently testable.
 */
public final class RotClientEventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners =
            new ConcurrentHashMap<>();
    private final BiConsumer<Object, RuntimeException> errorHandler;

    public RotClientEventBus() {
        this((event, error) -> {
        });
    }

    public RotClientEventBus(
            BiConsumer<Object, RuntimeException> errorHandler) {
        this.errorHandler = Objects.requireNonNull(
                errorHandler, "errorHandler");
    }

    public <T> Subscription subscribe(
            Class<T> eventType,
            Consumer<? super T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        CopyOnWriteArrayList<Consumer<?>> bucket =
                listeners.computeIfAbsent(
                        eventType, ignored -> new CopyOnWriteArrayList<>());
        bucket.add(listener);
        return () -> {
            bucket.remove(listener);
            if (bucket.isEmpty()) {
                listeners.remove(eventType, bucket);
            }
        };
    }

    public int publish(Object event) {
        Objects.requireNonNull(event, "event");
        CopyOnWriteArrayList<Consumer<?>> bucket =
                listeners.get(event.getClass());
        if (bucket == null || bucket.isEmpty()) {
            return 0;
        }
        int delivered = 0;
        for (Consumer<?> raw : bucket) {
            try {
                deliver(raw, event);
                delivered++;
            } catch (RuntimeException error) {
                errorHandler.accept(event, error);
            }
        }
        return delivered;
    }

    public int listenerCount(Class<?> eventType) {
        CopyOnWriteArrayList<Consumer<?>> bucket = listeners.get(eventType);
        return bucket == null ? 0 : bucket.size();
    }

    @SuppressWarnings("unchecked")
    private static <T> void deliver(Consumer<?> listener, T event) {
        ((Consumer<T>) listener).accept(event);
    }

    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}
