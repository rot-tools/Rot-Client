package fi.rotclient;

import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client bridge that scrubs outbound multiplayer packets so servers never see
 * {@code rotclient} identifiers or a Fabric client brand.
 */
public final class ClientNetworkPrivacyRuntime {
    private ClientNetworkPrivacyRuntime() {
    }

    public static boolean shouldScrubOutbound() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return false;
            }
            // Status ping classloads ClientPacketListener if those getters run.
            return !client.isLocalServer();
        } catch (LinkageError ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean shouldBlockOutbound(Packet<?> packet) {
        try {
            if (!shouldScrubOutbound() || packet == null) {
                return false;
            }
            if (!(packet instanceof ServerboundCustomPayloadPacket spp)) {
                return false;
            }
            CustomPacketPayload payload = spp.payload();
            if (payload == null || payload.type() == null) {
                return false;
            }
            Identifier id = payload.type().id();
            return id != null && ClientNetworkPrivacyPolicy.isSensitiveNamespace(id.getNamespace());
        } catch (LinkageError ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static Packet<?> sanitizeOutbound(Packet<?> packet) {
        try {
            return sanitizeOutboundUnsafe(packet);
        } catch (LinkageError ignored) {
            return packet;
        } catch (RuntimeException ignored) {
            return packet;
        }
    }

    private static Packet<?> sanitizeOutboundUnsafe(Packet<?> packet) {
        if (!shouldScrubOutbound() || packet == null) {
            return packet;
        }
        if (!(packet instanceof ServerboundCustomPayloadPacket spp)) {
            return packet;
        }
        CustomPacketPayload payload = spp.payload();
        if (payload == null) {
            return packet;
        }
        if (payload instanceof BrandPayload brand
                && ClientNetworkPrivacyPolicy.shouldReplaceBrand(brand.brand())) {
            return new ServerboundCustomPayloadPacket(
                    new BrandPayload(ClientNetworkPrivacyPolicy.VANILLA_BRAND));
        }
        if (payload instanceof CommonRegisterPayload register) {
            Set<Identifier> filtered = filterIdentifiers(register.channels());
            if (filtered.size() != register.channels().size()) {
                return new ServerboundCustomPayloadPacket(
                        new CommonRegisterPayload(
                                register.version(),
                                register.protocol(),
                                filtered));
            }
        }
        if (payload instanceof RegistrationPayload register) {
            List<Identifier> filtered = filterIdentifierList(register.channels());
            if (filtered.size() != register.channels().size()) {
                return new ServerboundCustomPayloadPacket(
                        new RegistrationPayload(register.type(), filtered));
            }
        }
        return packet;
    }

    private static Set<Identifier> filterIdentifiers(Set<Identifier> channels) {
        if (channels == null || channels.isEmpty()) {
            return channels;
        }
        Set<Identifier> filtered = new HashSet<>();
        for (Identifier channel : channels) {
            if (channel == null
                    || !ClientNetworkPrivacyPolicy.isSensitiveNamespace(channel.getNamespace())) {
                filtered.add(channel);
            }
        }
        return filtered;
    }

    private static List<Identifier> filterIdentifierList(List<Identifier> channels) {
        if (channels == null || channels.isEmpty()) {
            return channels;
        }
        List<Identifier> filtered = new java.util.ArrayList<>(channels.size());
        for (Identifier channel : channels) {
            if (channel == null
                    || !ClientNetworkPrivacyPolicy.isSensitiveNamespace(channel.getNamespace())) {
                filtered.add(channel);
            }
        }
        return filtered;
    }
}
