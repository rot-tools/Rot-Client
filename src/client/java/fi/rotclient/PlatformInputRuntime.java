package fi.rotclient;

import org.lwjgl.sdl.SDLMouse;

/** Minecraft 26.3 platform cursor operations backed by LWJGL SDL3. */
public final class PlatformInputRuntime {
    private PlatformInputRuntime() {
    }

    public static void warpCursor(long windowHandle, double x, double y) {
        if (windowHandle == 0L) {
            return;
        }
        SDLMouse.SDL_WarpMouseInWindow(windowHandle, (float) x, (float) y);
    }

    public static void setCursorVisible(boolean visible) {
        if (visible) {
            SDLMouse.SDL_ShowCursor();
        } else {
            SDLMouse.SDL_HideCursor();
        }
    }
}
