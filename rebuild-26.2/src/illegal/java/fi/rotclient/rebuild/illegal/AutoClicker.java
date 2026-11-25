package fi.rotclient.rebuild.illegal;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

final class AutoClicker {
    private final AutoClickerSettings settings;
    private final AutoClickerCadence left = new AutoClickerCadence();
    private final AutoClickerCadence right = new AutoClickerCadence();
    private ClientLevel level;
    private int actions;
    private String checkedServer;
    private String checkedAddress;
    private boolean allowedServer;

    AutoClicker(AutoClickerSettings settings) {
        this.settings = settings;
    }

    void tick(Minecraft client) {
        actions = 0;
        if (!settings.enabled || !inPlayableWorld(client)) {
            level = null;
            left.disarm();
            right.disarm();
            return;
        }
        if (level != client.level) {
            level = client.level;
            left.disarm();
            right.disarm();
        }

        var leftKey = AutoClickerSettings.inputKey(settings.left.binding);
        var rightKey = AutoClickerSettings.inputKey(settings.right.binding);
        boolean leftHeld = left.active(settings.left.enabled && isHeld(client, leftKey));
        boolean rightHeld = right.active(settings.terminatorOnly
                ? client.options.keyUse.isDown() : settings.right.enabled && isHeld(client, rightKey));
        if ((!leftHeld && !rightHeld) || client.player.isUsingItem()) {
            clearCadence();
            return;
        }

        ItemStack item = client.player.getMainHandItem();
        var data = item.get(DataComponents.CUSTOM_DATA);
        var tag = data == null || !(settings.terminatorOnly || settings.skipDungeonBreaker || settings.whitelistOnly)
                ? null : data.copyTag();
        var extra = tag == null ? null : tag.getCompound("ExtraAttributes").orElse(null);
        String id = attribute(tag, extra, "id");
        boolean block = hitBlock(client);
        var random = ThreadLocalRandom.current();
        if (settings.terminatorOnly) {
            left.clear();
            if (rightHeld && !block && id.equals("TERMINATOR") && !client.options.keyAttack.isDown()) {
                if (right.tick(AutoClickerCadence.variedRate(settings.left.cps, settings.jitter, random))) {
                    actions = IllegalEdition.AUTO_ATTACK;
                }
            } else {
                right.clear();
            }
            return;
        }
        if (settings.skipDungeonBreaker && id.equals("DUNGEONBREAKER")) {
            clearCadence();
            return;
        }
        if (settings.whitelistOnly) {
            String uuid = attribute(tag, extra, "uuid");
            String name = item.isEmpty() ? "" : item.getHoverName().getString();
            leftHeld &= matchesItem(settings.left, uuid, id, name);
            rightHeld &= matchesItem(settings.right, uuid, id, name);
        }
        int ownership = (leftHeld ? mouseOwnership(client, leftKey) : 0)
                | (rightHeld ? mouseOwnership(client, rightKey) : 0);
        leftHeld &= !client.options.keyAttack.isDown() || (ownership & IllegalEdition.REPLACE_VANILLA_ATTACK) != 0;
        rightHeld &= !client.options.keyUse.isDown() || (ownership & IllegalEdition.REPLACE_VANILLA_USE) != 0;
        // A filtered lane must not take over any vanilla mapping.
        actions = (leftHeld ? mouseOwnership(client, leftKey) : 0)
                | (rightHeld ? mouseOwnership(client, rightKey) : 0);
        if (leftHeld && block && settings.allowBreaking) {
            clearCadence();
            actions |= IllegalEdition.AUTO_BREAK;
            return;
        }
        if (client.gameMode.isDestroying()) return;
        if (leftHeld) {
            if (left.tick(AutoClickerCadence.variedRate(settings.left.cps, settings.jitter, random))) {
                actions |= IllegalEdition.AUTO_ATTACK;
            }
        } else {
            left.clear();
        }
        if (rightHeld) {
            if (right.tick(AutoClickerCadence.variedRate(settings.right.cps, settings.jitter, random))) {
                actions |= IllegalEdition.AUTO_USE;
            }
        } else {
            right.clear();
        }
    }

    int takeActions(Minecraft client) {
        int pending = actions;
        actions = 0;
        return pending != 0 && settings.enabled && client.level == level
                && inPlayableWorld(client) && !client.player.isUsingItem() ? pending : 0;
    }

    private void clearCadence() {
        left.clear();
        right.clear();
    }

    private boolean inPlayableWorld(Minecraft client) {
        if (!client.isWindowActive() || client.level == null || client.player == null
                || !client.player.isAlive() || client.player.isSpectator() || client.gameMode == null
                || client.gui.screen() != null || client.gui.overlay() != null || !client.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        if (client.hasSingleplayerServer()) return true;
        var server = client.getCurrentServer();
        String address = server == null ? null : server.ip;
        if (!Objects.equals(checkedServer, settings.testServer) || !Objects.equals(checkedAddress, address)) {
            allowedServer = serverAllowed(false, settings.testServer, address);
            checkedServer = settings.testServer;
            checkedAddress = address;
        }
        return allowedServer;
    }

    static boolean serverAllowed(boolean singleplayer, String configuredServer, String currentServer) {
        if (singleplayer) return true;
        if (currentServer == null || !ServerAddress.isValidAddress(currentServer)) return false;
        var connected = ServerAddress.parseString(currentServer);
        if (isHypixelHost(connected.getHost())) return true;
        if (configuredServer.isBlank() || !ServerAddress.isValidAddress(configuredServer)) return false;
        var configured = ServerAddress.parseString(configuredServer);
        return configured.getHost().equalsIgnoreCase(connected.getHost()) && configured.getPort() == connected.getPort();
    }

    static boolean isHypixelAddress(String address) {
        return address != null && ServerAddress.isValidAddress(address)
                && isHypixelHost(ServerAddress.parseString(address).getHost());
    }

    private static boolean isHypixelHost(String host) {
        String name = host.toLowerCase(Locale.ROOT);
        if (name.endsWith(".")) name = name.substring(0, name.length() - 1);
        return name.equals("hypixel.net") || name.endsWith(".hypixel.net");
    }

    private static boolean matchesItem(AutoClickerSettings.Button button, String uuid, String id, String name) {
        return button.whitelist.contains(uuid) || button.whitelist.contains(id) || button.whitelist.contains(name);
    }

    static boolean canBind(Minecraft client, InputConstants.Key key) {
        return key != InputConstants.UNKNOWN && (AutoClickerSettings.isMouseBinding(key)
                || (!client.options.keyAttack.matches(key) && !client.options.keyUse.matches(key)));
    }

    private static boolean isHeld(Minecraft client, InputConstants.Key key) {
        if (!canBind(client, key)) return false;
        long window = client.getWindow().handle();
        return AutoClickerSettings.isMouseBinding(key)
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
    }

    private static int mouseOwnership(Minecraft client, InputConstants.Key key) {
        if (!AutoClickerSettings.isMouseBinding(key)) return 0;
        return (client.options.keyAttack.matches(key) ? IllegalEdition.REPLACE_VANILLA_ATTACK : 0)
                | (client.options.keyUse.matches(key) ? IllegalEdition.REPLACE_VANILLA_USE : 0);
    }

    private static boolean hitBlock(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
        var state = client.level.getBlockState(hit.getBlockPos());
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
    }

    private static String attribute(CompoundTag tag, CompoundTag extra, String key) {
        if (tag == null) return "";
        var value = tag.getString(key).filter(text -> !text.isBlank());
        return value.isPresent() ? value.get() : extra == null ? "" : extra.getString(key).orElse("");
    }
}
