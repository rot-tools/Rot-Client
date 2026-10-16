// SPDX-License-Identifier: GPL-3.0-only
// AUTISM Client 5.0 AutoSignModule and AutismSignEditScreenMixin by Melonik/contributors.
// Rot Tools: 26.3 adapter and market sign-entry ownership guard (2026-10-16).
package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public final class AutismSignRuntime {
    private static final String ID = AutismModuleSettings.SIGN;
    private AutismSignRuntime() {}

    public static boolean allowed() {
        return AutismModuleSettings.enabled(RotClientClient.qolConfigPublic(), ID)
                && !MarketTradeRuntime.enabled();
    }

    public static boolean fill(SignBlockEntity sign, Font font, String[] messages) {
        var config = RotClientClient.qolConfigPublic();
        if (!allowed() || font == null || messages == null
                || !AutismPortPolicy.mayFillSign(messages, AutismModuleSettings.bool(config, ID + ".edit_existing"))) return false;
        int width = sign == null ? 90 : sign.getMaxTextLineWidth();
        for (int line = 0; line < Math.min(4, messages.length); line++) {
            messages[line] = font.plainSubstrByWidth(AutismModuleSettings.text(config, ID + ".line_" + (line + 1)), width);
        }
        return AutismModuleSettings.bool(config, ID + ".auto_done");
    }
}
