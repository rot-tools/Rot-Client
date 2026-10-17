// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import org.junit.jupiter.api.Test;

/** Detect 26.3 descriptor/field changes without starting Minecraft or a server. */
class AutismMixinTargetTest {
    private static ClassModel read(String type) throws Exception {
        try (var in = AutismMixinTargetTest.class.getClassLoader().getResourceAsStream(type + ".class")) {
            assertNotNull(in, type);
            return ClassFile.of().parse(in.readAllBytes());
        }
    }
    private static void method(String type, String name, String descriptor) throws Exception {
        assertTrue(read(type).methods().stream().anyMatch(m -> m.methodName().equalsString(name)
                && m.methodType().equalsString(descriptor)), type + "." + name + descriptor);
    }
    private static void field(String type, String name, String descriptor) throws Exception {
        assertTrue(read(type).fields().stream().anyMatch(f -> f.fieldName().equalsString(name)
                && f.fieldType().equalsString(descriptor)), type + "." + name);
    }
    @Test void movementAndPacketTargetsMatchTheActualMinecraftDependency() throws Exception {
        method("net/minecraft/client/player/KeyboardInput", "tick", "()V");
        method("net/minecraft/network/Connection", "send", "(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V");
        field("net/minecraft/client/multiplayer/MultiPlayerGameMode", "destroyBlockPos", "Lnet/minecraft/core/BlockPos;");
    }
    @Test void signCameraAndUseTargetsMatch26_3() throws Exception {
        String sign = "net/minecraft/client/gui/screens/inventory/AbstractSignEditScreen";
        method(sign, "init", "()V");
        method(sign, "tick", "()V");
        field(sign, "messages", "[Ljava/lang/String;");
        field(sign, "sign", "Lnet/minecraft/world/level/block/entity/SignBlockEntity;");
        method("net/minecraft/client/Camera", "update", "(Lnet/minecraft/client/DeltaTracker;)V");
        method("net/minecraft/client/Camera", "alignWithEntity", "(F)V");
        method("net/minecraft/client/Camera", "getMaxZoom", "(F)F");
        method("net/minecraft/client/MouseHandler", "turnPlayer", "(D)V");
        method("net/minecraft/client/Minecraft", "startUseItem", "()V");
        field("net/minecraft/client/Minecraft", "rightClickDelay", "I");
    }
    @Test void airJumpAndAtomicSwapTargetsMatch26_3() throws Exception {
        String living="net/minecraft/world/entity/LivingEntity";
        method(living,"aiStep","()V");method(living,"jumpFromGround","()V");
        field(living,"jumping","Z");field(living,"noJumpDelay","I");
        method("net/minecraft/world/entity/Entity","collide","(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;");
        method("net/minecraft/client/multiplayer/MultiPlayerGameMode","handleContainerInput",
                "(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V");
        method("net/minecraft/world/inventory/Slot","getContainerSlot","()I");
        field("net/minecraft/world/inventory/Slot","index","I");
    }
}
