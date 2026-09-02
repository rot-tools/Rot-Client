package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reflection-isolated optional bridge for Hypixel Mod API 1.x. Rot Client
 * keeps its scoreboard fallback and has no compile/runtime dependency on the
 * API, while an installed API can provide location packets.
 */
final class OptionalHypixelModApiRuntime {
    private static final String MOD_ID = "hypixel-mod-api";
    private static final String API_CLASS = "net.hypixel.modapi.HypixelModAPI";
    private static final String LOCATION_PACKET =
            "net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket";
    private static final long RETRY_MILLIS = 5_000L;
    private static boolean registered;
    private static long nextAttemptMillis;

    private OptionalHypixelModApiRuntime() {
    }

    static void tick(Consumer<LocationPacket> sink, long nowMillis) {
        if (registered
                || sink == null
                || nowMillis < nextAttemptMillis
                || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        nextAttemptMillis = nowMillis + RETRY_MILLIS;
        try {
            Class<?> apiType = Class.forName(API_CLASS);
            Class<?> packetType = Class.forName(LOCATION_PACKET);
            Object api = apiType.getMethod("getInstance").invoke(null);
            apiType.getMethod("subscribeToEventPacket", Class.class)
                    .invoke(api, packetType);
            Consumer<Object> handler = packet ->
                    sink.accept(readLocationPacket(packet));
            apiType.getMethod("createHandler", Class.class, Consumer.class)
                    .invoke(api, packetType, handler);
            registered = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // API initialization may run after Rot Client; retry on a later tick.
        }
    }

    static List<String> locationHints(LocationPacket packet) {
        if (packet == null) {
            return List.of();
        }
        List<String> hints = new ArrayList<>();
        addHint(hints, packet.mode());
        addHint(hints, packet.map());
        addHint(hints, packet.lobbyName());
        return List.copyOf(hints);
    }

    private static LocationPacket readLocationPacket(Object packet) {
        if (packet == null) {
            return new LocationPacket("", "", "", "");
        }
        return new LocationPacket(
                stringMethod(packet, "getServerName"),
                optionalStringMethod(packet, "getLobbyName"),
                optionalStringMethod(packet, "getMode"),
                optionalStringMethod(packet, "getMap"));
    }

    private static String stringMethod(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static String optionalStringMethod(
            Object target,
            String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Optional<?> optional) {
                return optional.map(Object::toString).orElse("");
            }
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static void addHint(List<String> hints, String value) {
        if (value != null && !value.isBlank()) {
            hints.add(value.strip());
        }
    }

    record LocationPacket(
            String serverName,
            String lobbyName,
            String mode,
            String map) {
        LocationPacket {
            serverName = clean(serverName);
            lobbyName = clean(lobbyName);
            mode = clean(mode);
            map = clean(map);
        }

        private static String clean(String value) {
            return value == null ? "" : value.strip();
        }
    }
}
