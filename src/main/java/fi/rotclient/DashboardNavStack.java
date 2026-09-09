package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dashboard back history. Overview is the root: an empty stack there
 * cannot close the Client UI.
 */
public final class DashboardNavStack {
    public static final int MAX_DEPTH = 16;

    public record Frame(
            String moduleId,
            String qolGroup,
            String focusId,
            boolean appearanceLanding,
            boolean hudLayoutLanding,
            boolean hudDrawer) {
        public Frame {
            moduleId = moduleId == null || moduleId.isBlank() ? "none" : moduleId;
            qolGroup = qolGroup == null ? "" : qolGroup;
            focusId = focusId == null ? "" : focusId;
        }

        public static Frame overview() {
            return new Frame("none", "", "", false, false, false);
        }

        public boolean isOverviewHome() {
            return "none".equals(moduleId)
                    && !appearanceLanding
                    && !hudLayoutLanding;
        }
    }

    private final ArrayList<Frame> frames = new ArrayList<>();

    public void push(Frame frame) {
        if (frame == null) {
            return;
        }
        if (!frames.isEmpty() && Objects.equals(frames.get(frames.size() - 1), frame)) {
            return;
        }
        frames.add(frame);
        while (frames.size() > MAX_DEPTH) {
            frames.remove(0);
        }
    }

    public Frame pop() {
        if (frames.isEmpty()) {
            return null;
        }
        return frames.remove(frames.size() - 1);
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public void clear() {
        frames.clear();
    }

    public int size() {
        return frames.size();
    }

    public List<Frame> snapshot() {
        return List.copyOf(frames);
    }
}
