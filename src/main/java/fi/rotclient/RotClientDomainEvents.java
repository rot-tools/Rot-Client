package fi.rotclient;

import java.util.List;

/** Immutable event contracts shared by client bridges and feature runtimes. */
public final class RotClientDomainEvents {
    private RotClientDomainEvents() {
    }

    public enum Source {
        SCOREBOARD,
        HYPIXEL_MOD_API,
        FABRIC_MESSAGE,
        FABRIC_SCREEN,
        LOCAL_DATA
    }

    public enum TextKind {
        GAME,
        ACTION_BAR
    }

    public record LocationChanged(
            Source source,
            String parentAreaId,
            String subArea,
            long observedAtMillis) {
        public LocationChanged {
            source = source == null ? Source.SCOREBOARD : source;
            parentAreaId = clean(parentAreaId);
            subArea = clean(subArea);
            observedAtMillis = Math.max(0L, observedAtMillis);
        }
    }

    public record ProfileChanged(
            Source source,
            String previousProfile,
            String currentProfile,
            long observedAtMillis) {
        public ProfileChanged {
            source = source == null ? Source.SCOREBOARD : source;
            previousProfile = clean(previousProfile);
            currentProfile = clean(currentProfile);
            observedAtMillis = Math.max(0L, observedAtMillis);
        }
    }

    public record TextObserved(
            Source source,
            TextKind kind,
            String plainText,
            long observedAtMillis) {
        public TextObserved {
            source = source == null ? Source.FABRIC_MESSAGE : source;
            kind = kind == null ? TextKind.GAME : kind;
            plainText = clean(plainText);
            observedAtMillis = Math.max(0L, observedAtMillis);
        }
    }

    public record ScoreboardObserved(
            Source source,
            List<String> lines,
            long observedAtMillis) {
        public ScoreboardObserved {
            source = source == null ? Source.SCOREBOARD : source;
            lines = lines == null ? List.of() : List.copyOf(lines);
            observedAtMillis = Math.max(0L, observedAtMillis);
        }
    }

    public record ScreenOpened(
            Source source,
            String screenClass,
            int width,
            int height,
            long observedAtMillis) {
        public ScreenOpened {
            source = source == null ? Source.FABRIC_SCREEN : source;
            screenClass = clean(screenClass);
            width = Math.max(0, width);
            height = Math.max(0, height);
            observedAtMillis = Math.max(0L, observedAtMillis);
        }
    }

    public record DataReloaded(
            Source source,
            String datasetId,
            long generation,
            long observedAtMillis) {
        public DataReloaded {
            source = source == null ? Source.LOCAL_DATA : source;
            datasetId = clean(datasetId);
            generation = Math.max(0L, generation);
            observedAtMillis = Math.max(0L, observedAtMillis);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
