package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * One-tick attack/use/sneak pulses shared by legit helpers that only need a
 * click, and by Plus automations that live in {@code src/plusClient}.
 */
public final class ClickPulseHelper {
    private ClickPulseHelper() {
    }

    public static void pulseUse(Minecraft client) {
        if (client != null && client.options != null) {
            pulseClick(client, client.options.keyUse);
        }
    }

    public static void pulseAttack(Minecraft client) {
        if (client != null && client.options != null) {
            pulseClick(client, client.options.keyAttack);
        }
    }

    public static void setSneak(Minecraft client, boolean down) {
        if (client == null || client.options == null || client.options.keyShift == null) {
            return;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) client.options.keyShift)
                .rotclient$boundKey();
        if (bound != null) {
            KeyMapping.set(bound, down);
        }
        client.options.keyShift.setDown(down);
    }

    public static void pulseClick(Minecraft client, KeyMapping mapping) {
        if (client == null || mapping == null) {
            return;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping)
                .rotclient$boundKey();
        if (bound == null) {
            return;
        }
        KeyMapping.set(bound, true);
        KeyMapping.click(bound);
        KeyMapping.set(bound, false);
    }
}
