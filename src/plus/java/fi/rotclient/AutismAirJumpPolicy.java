// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client AirJumpModule (Melonik and contributors).
// Rot Tools: explicit lifecycle and local-player-only collision policy.
package fi.rotclient;

public final class AutismAirJumpPolicy {
    private boolean extraJump = true, hasHeldJump;
    private String mode = "";
    private long lastHeld, lastNow;

    public void update(String nextMode, boolean grounded, boolean jumpHeld, long now) {
        if (!mode.equals(nextMode) || now < lastNow) reset();
        mode = nextMode;
        lastNow = now;
        if (grounded) extraJump = true;
        if (jumpHeld) { hasHeldJump = true; lastHeld = now; }
    }

    public boolean mayJump() {
        return "JumpFreely".equals(mode) || "DoubleJump".equals(mode) && extraJump;
    }

    public void jumped(boolean grounded) { if (!grounded) extraJump = false; }

    public boolean ghostActive(long now, double speed) {
        return "GhostBlock".equals(mode) && hasHeldJump && now >= lastHeld
                && now >= lastNow && now - lastHeld <= graceMillis(speed);
    }

    public static long graceMillis(double speed) {
        if (!Double.isFinite(speed)) return 300;
        return 300 + (long) (Math.clamp((speed - .22) / .58, 0, 1) * 600);
    }

    /** Clip only downward movement at the block-height plane beneath this player. */
    public static double clipDown(double feetY, double resolvedY) {
        if (!Double.isFinite(feetY) || !Double.isFinite(resolvedY) || resolvedY >= 0) return resolvedY;
        return Math.max(resolvedY, Math.floor(feetY) - feetY);
    }

    public void reset() { extraJump = true; hasHeldJump = false; mode = ""; lastHeld = lastNow = 0; }
}
