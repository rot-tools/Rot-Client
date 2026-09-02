package fi.rotclient;

/**
 * Packet handlers can be entered once on Netty's network thread before
 * Minecraft schedules the same handler on the client thread. Rot Client
 * only handles the replayed client-thread pass.
 */
public final class ClientThreadGuard {
    private ClientThreadGuard() {
    }

    public static boolean shouldHandle(boolean runningOnClientThread) {
        return runningOnClientThread;
    }
}
