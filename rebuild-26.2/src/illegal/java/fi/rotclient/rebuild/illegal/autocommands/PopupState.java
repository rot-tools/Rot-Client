// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

/** Wall-clock timing avoids lag-dependent popup duration. */
public final class PopupState {
    private CommandShortcut.Notice notice;
    private long started;

    public void show(CommandShortcut.Notice notice, long now) {
        this.notice = notice;
        started = now;
    }

    public CommandShortcut.Notice notice() { return notice; }

    public float opacity(long now, double seconds) {
        if (notice == null) return 0;
        double elapsed = (now - started) / 1_000_000_000.0;
        if (elapsed < 0 || elapsed >= seconds) return 0;
        double fadeIn = Math.min(0.15, seconds * 0.15);
        double fadeOut = Math.min(0.4, seconds * 0.25);
        return (float) Math.min(1.0, Math.min(elapsed / fadeIn, (seconds - elapsed) / fadeOut));
    }

    public void clear() {
        notice = null;
    }
}
