package fi.rotclient;

/**
 * Bounded vertical scroll state for Rot Client content panes.
 *
 * <p>{@code maxScroll = max(0, contentHeight - viewportHeight)} and
 * {@code scrollPixels} is always clamped to {@code [0, maxScroll]}.
 */
final class RotClientScrollState {
    record Thumb(int top, int height) {
        int bottom() {
            return top + height;
        }

        boolean contains(int y) {
            return y >= top && y < bottom();
        }
    }

    private double displayedScroll;
    private double targetScroll;
    private long lastAdvanceNs;
    private int contentHeight;
    private int viewportHeight;
    private boolean thumbDragging;
    private int thumbGrabOffset;

    void setContentHeight(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        clamp();
    }

    void setViewportHeight(int viewportHeight) {
        this.viewportHeight = Math.max(0, viewportHeight);
        clamp();
    }

    /**
     * Updates both bounds atomically then clamps. Prefer this when both
     * values are known so an intermediate state cannot leave scroll unbounded.
     */
    void setBounds(int contentHeight, int viewportHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        this.viewportHeight = Math.max(0, viewportHeight);
        clamp();
    }

    int scrollPixels() {
        return (int) Math.round(displayedScroll);
    }

    /**
     * Subpixel displayed offset. Draw with a fractional pose translate of
     * {@code displayedScroll - scrollPixels} so 144 Hz does not stair-step.
     */
    double displayedScroll() {
        return displayedScroll;
    }

    int contentHeight() {
        return contentHeight;
    }

    int viewportHeight() {
        return viewportHeight;
    }

    int maxScroll() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    boolean canScroll() {
        return maxScroll() > 0;
    }

    void setScrollPixels(int pixels) {
        displayedScroll = pixels;
        targetScroll = pixels;
        clamp();
    }

    void scrollBy(int deltaPixels) {
        if (!canScroll() || deltaPixels == 0) {
            clamp();
            return;
        }
        displayedScroll += deltaPixels;
        targetScroll = displayedScroll;
        clamp();
    }

    /**
     * Minecraft wheel: positive verticalAmount usually means scroll up
     * (content moves down / offset decreases). Uses the actual wheel delta so
     * trackpads do not jump a full step per tiny event.
     */
    void scrollBySteps(double verticalAmount, int stepPixels) {
        if (!canScroll() || verticalAmount == 0.0D) {
            clamp();
            return;
        }
        targetScroll -= verticalAmount * Math.max(1, stepPixels);
        clampTargetOnly();
    }

    /**
     * Ease displayed offset toward the wheel target. Call once per frame
     * before reading {@link #scrollPixels()} or {@link #displayedScroll()}.
     */
    void advance(long nowNs) {
        if (lastAdvanceNs <= 0L || nowNs <= lastAdvanceNs) {
            lastAdvanceNs = nowNs;
            easeTowardTarget(1.0D / 60.0D);
            return;
        }
        double dt = Math.min(0.05D, (nowNs - lastAdvanceNs) / 1_000_000_000.0D);
        lastAdvanceNs = nowNs;
        easeTowardTarget(dt);
    }

    void advanceSeconds(double dtSeconds) {
        lastAdvanceNs = System.nanoTime();
        easeTowardTarget(dtSeconds);
    }

    /** Jump displayed offset to the target. Used by tests and thumb drags. */
    void snap() {
        displayedScroll = targetScroll;
        lastAdvanceNs = 0L;
        clamp();
    }

    void reset() {
        displayedScroll = 0;
        targetScroll = 0;
        lastAdvanceNs = 0L;
        thumbDragging = false;
        thumbGrabOffset = 0;
    }

    Thumb thumb(int trackTop, int trackBottom, int minimumThumbHeight) {
        int trackHeight = Math.max(1, trackBottom - trackTop);
        if (!canScroll()) {
            return new Thumb(trackTop, trackHeight);
        }
        int minimum = Math.max(1, Math.min(minimumThumbHeight, trackHeight));
        int height = Math.max(
                minimum,
                (int) ((long) trackHeight * Math.max(1, viewportHeight)
                        / Math.max(1, contentHeight)));
        height = Math.min(trackHeight, height);
        int travel = Math.max(0, trackHeight - height);
        int top = trackTop + (int) Math.round(
                travel * (displayedScroll / (double) Math.max(1, maxScroll())));
        return new Thumb(top, height);
    }

    boolean beginThumbDrag(
            int mouseY,
            int trackTop,
            int trackBottom,
            int minimumThumbHeight) {
        if (!canScroll()) {
            return false;
        }
        Thumb thumb = thumb(trackTop, trackBottom, minimumThumbHeight);
        if (!thumb.contains(mouseY)) {
            return false;
        }
        thumbDragging = true;
        thumbGrabOffset = mouseY - thumb.top();
        return true;
    }

    boolean clickTrack(
            int mouseY,
            int trackTop,
            int trackBottom,
            int minimumThumbHeight) {
        if (!canScroll() || mouseY < trackTop || mouseY >= trackBottom) {
            return false;
        }
        Thumb thumb = thumb(trackTop, trackBottom, minimumThumbHeight);
        if (thumb.contains(mouseY)) {
            return false;
        }
        scrollBy(mouseY < thumb.top() ? -viewportHeight : viewportHeight);
        return true;
    }

    boolean dragThumbTo(
            int mouseY,
            int trackTop,
            int trackBottom,
            int minimumThumbHeight) {
        if (!thumbDragging || !canScroll()) {
            return false;
        }
        Thumb thumb = thumb(trackTop, trackBottom, minimumThumbHeight);
        int travel = Math.max(0, trackBottom - trackTop - thumb.height());
        if (travel == 0) {
            setScrollPixels(0);
            return true;
        }
        int desiredTop = mouseY - thumbGrabOffset;
        int clampedTop = Math.max(trackTop, Math.min(trackTop + travel, desiredTop));
        setScrollPixels((int) Math.round(
                (clampedTop - trackTop) * (maxScroll() / (double) travel)));
        return true;
    }

    boolean endThumbDrag() {
        if (!thumbDragging) {
            return false;
        }
        thumbDragging = false;
        thumbGrabOffset = 0;
        return true;
    }

    boolean isThumbDragging() {
        return thumbDragging;
    }

    boolean intersects(int y, int height, int clipTop, int clipBottom) {
        return y + height > clipTop && y < clipBottom;
    }

    /**
     * Content height from a scrolled layout pass.
     * {@code drawStartY} must be {@code viewportTop - scrollPixels}.
     * {@code drawEndY} is the Y after the last content row.
     */
    static int measureContentHeight(int drawStartY, int drawEndY) {
        return Math.max(0, drawEndY - drawStartY);
    }

    private void easeTowardTarget(double dt) {
        displayedScroll = RotClientEase.expToward(
                displayedScroll, targetScroll, dt, RotClientEase.SCROLL_STIFFNESS);
        clamp();
    }

    private void clampTargetOnly() {
        int max = maxScroll();
        if (targetScroll < 0.0D) {
            targetScroll = 0.0D;
        } else if (targetScroll > max) {
            targetScroll = max;
        }
        if (max == 0) {
            displayedScroll = 0.0D;
            targetScroll = 0.0D;
            thumbDragging = false;
            thumbGrabOffset = 0;
        }
    }

    private void clamp() {
        int max = maxScroll();
        if (displayedScroll < 0.0D) {
            displayedScroll = 0.0D;
        } else if (displayedScroll > max) {
            displayedScroll = max;
        }
        if (targetScroll < 0.0D) {
            targetScroll = 0.0D;
        } else if (targetScroll > max) {
            targetScroll = max;
        }
        if (max == 0) {
            displayedScroll = 0.0D;
            targetScroll = 0.0D;
            thumbDragging = false;
            thumbGrabOffset = 0;
        }
    }
}
