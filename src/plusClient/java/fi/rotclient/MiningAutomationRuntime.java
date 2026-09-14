package fi.rotclient;

import net.minecraft.client.Minecraft;

/** Plus-only automatic mining chat/command actions. */
final class MiningAutomationRuntime {
    private MiningAutomationRuntime() {
    }

    static void onMineshaftPortal() {
        QolSkyblockExtras extras = extras();
        if (extras.miningGlaciteEnabled && extras.miningGlaciteShaftParty) {
            sendParty("Mineshaft portal");
        }
    }

    static void onCommissionComplete() {
        QolSkyblockExtras extras = extras();
        if (extras.miningHelpersEnabled && extras.miningHelpersCallKing) {
            sendCommand(MiningLeftoverPolicy.kingCallCommand());
        }
    }

    static void onCorpseCoordinates(MiningLeftoverPolicy.CorpseCoords coordinates) {
        QolSkyblockExtras extras = extras();
        if (coordinates != null
                && extras.miningGlaciteEnabled
                && extras.miningGlacitePartyShare) {
            sendParty(coordinates.partyLine());
        }
    }

    static void onWormSeen(MiningLeftoverPolicy.WormKind kind) {
        QolSkyblockExtras extras = extras();
        if (kind != null && extras.miningScathaEnabled && extras.miningScathaParty) {
            sendParty(MiningLeftoverPolicy.scathaPartyLine(kind));
        }
    }

    static void onShaftEntered(String area) {
        QolSkyblockExtras extras = extras();
        if (extras.miningGlaciteEnabled && extras.miningGlaciteEnterParty) {
            sendParty(SkyBlockUtilityPolicy.shaftEnterLine(area));
        }
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private static void sendParty(String text) {
        sendCommand("pc " + text);
    }

    private static void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.connection == null
                || command == null || command.isBlank()) {
            return;
        }
        client.player.connection.sendCommand(
                command.startsWith("/") ? command.substring(1) : command);
    }
}
