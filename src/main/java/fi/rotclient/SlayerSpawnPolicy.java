package fi.rotclient;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local server spawn announcements correlated with recent, identified entities. */
public final class SlayerSpawnPolicy {
    public static final long ENTITY_WINDOW_MILLIS = 500L;
    public static final long RESOLUTION_WINDOW_MILLIS = 1_500L;

    /*
     * Announcement contract adapted from Starred's Athen (BSD-3-Clause):
     * https://github.com/skies-starred/Athen/blob/21ada452fd8f2085174b87e267fc2cc96683c016/src/main/kotlin/foo/starred/athen/api/slayers/resolver/base/GenericSlayerBossResolver.kt
     * Copyright (c) 2025-2026, Starred. See docs/third-party/Athen-LICENSE.txt.
     * The ambiguity checks below are Rot's implementation; ownership is never
     * guessed from proximity, an entity's facing direction, or a websocket.
     */
    private static final Pattern ANNOUNCEMENT = Pattern.compile(
            "^SLAYER BOSS! The (.+?) (IV|III|II|V|I) spawned!$");

    public record Announcement(SlayerPolicy.SlayerType type, int tier) {
    }

    public record Candidate(
            int entityId,
            SlayerPolicy.SlayerType bodyFamily,
            long addedAtMillis,
            SlayerPolicy.EntityDescriptor descriptor,
            boolean alive) {
    }

    public record Resolution(int entityId, SlayerPolicy.EntityDescriptor descriptor) {
    }

    public static final class State {
        private Announcement pending;
        private long announcedAtMillis;
        private Announcement lastAnnouncement;
        private long lastAnnouncementAtMillis;

        public boolean observe(String line, long nowMillis) {
            Optional<Announcement> parsed = announcement(line);
            return parsed.isPresent() && observeVerifiedTransition(parsed.get(), nowMillis);
        }

        /** Only a separately verified local lifecycle may supply an internal announcement. */
        public boolean observeVerifiedTransition(Announcement announcement, long nowMillis) {
            if (announcement == null || announcement.type() == null || nowMillis < 0L
                    || announcement.tier() < 1 || announcement.tier() > maxTier(announcement.type())) {
                return false;
            }
            if (announcement.equals(lastAnnouncement)
                    && nowMillis >= lastAnnouncementAtMillis
                    && nowMillis - lastAnnouncementAtMillis <= RESOLUTION_WINDOW_MILLIS) {
                return false;
            }
            pending = announcement;
            announcedAtMillis = nowMillis;
            lastAnnouncement = pending;
            lastAnnouncementAtMillis = nowMillis;
            return true;
        }

        public Optional<Resolution> resolve(List<Candidate> candidates, String localPlayer, long nowMillis) {
            if (pending == null) {
                return Optional.empty();
            }
            if (nowMillis < announcedAtMillis
                    || nowMillis - announcedAtMillis > RESOLUTION_WINDOW_MILLIS) {
                pending = null;
                return Optional.empty();
            }
            if (localPlayer == null || !localPlayer.matches("[A-Za-z0-9_]{1,16}") || candidates == null) {
                return Optional.empty();
            }
            Candidate match = null;
            for (Candidate candidate : candidates) {
                if (candidate == null || !candidate.alive()
                        || candidate.bodyFamily() != pending.type()
                        || candidate.addedAtMillis() > nowMillis
                        || candidate.addedAtMillis() < Math.max(0L, announcedAtMillis - ENTITY_WINDOW_MILLIS)
                        || candidate.addedAtMillis() - announcedAtMillis > ENTITY_WINDOW_MILLIS) {
                    continue;
                }
                SlayerPolicy.EntityDescriptor descriptor = candidate.descriptor();
                // A second fresh body with pending metadata must remain an ambiguity.
                if (descriptor == null) {
                    return Optional.empty();
                }
                if (descriptor.role() != SlayerPolicy.EntityRole.BOSS
                        || descriptor.type() != pending.type()
                        || (descriptor.tier() > 0 && descriptor.tier() != pending.tier())
                        || (!descriptor.owner().isBlank()
                        && !descriptor.owner().equalsIgnoreCase(localPlayer))) {
                    continue;
                }
                if (match != null && match.entityId() != candidate.entityId()) {
                    return Optional.empty();
                }
                match = candidate;
            }
            if (match == null) {
                return Optional.empty();
            }
            SlayerPolicy.EntityDescriptor descriptor = match.descriptor();
            Resolution result = new Resolution(match.entityId(), new SlayerPolicy.EntityDescriptor(
                    descriptor.role(), descriptor.type(), pending.tier(), localPlayer,
                    descriptor.displayName(), descriptor.bigMiniboss(), descriptor.attunement()));
            pending = null;
            return Optional.of(result);
        }

        public void reset() {
            pending = null;
            lastAnnouncement = null;
            announcedAtMillis = 0L;
            lastAnnouncementAtMillis = 0L;
        }
    }

    private SlayerSpawnPolicy() {
    }

    public static Optional<Announcement> announcement(String rawLine) {
        Matcher matcher = ANNOUNCEMENT.matcher(SlayerPolicy.normalize(rawLine));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        int tier = switch (matcher.group(2)) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            default -> 0;
        };
        for (SlayerPolicy.SlayerType type : SlayerPolicy.SlayerType.values()) {
            if (type.aliases().stream().noneMatch(alias -> alias.equals(matcher.group(1)))) {
                continue;
            }
            return tier <= maxTier(type) ? Optional.of(new Announcement(type, tier)) : Optional.empty();
        }
        return Optional.empty();
    }

    private static int maxTier(SlayerPolicy.SlayerType type) {
        return switch (type) {
            case REVENANT, TARANTULA, VAMPIRE -> 5;
            default -> 4;
        };
    }
}
