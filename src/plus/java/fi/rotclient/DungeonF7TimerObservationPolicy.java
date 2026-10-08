package fi.rotclient;

import java.util.EnumMap;
import java.util.Map;

/** Independent nominal timers from observed boss lines; unknown dialogue never guesses a phase.
 * Durations compared against Odin TickTimers.kt at 833e0533 (BSD-3-Clause).
 * Original Rot state integration. Wall-clock estimates assume nominal 20 TPS, not server proof.
 */
final class DungeonF7TimerObservationPolicy {
    enum Phase { NONE, MAXOR, STORM, GOLDOR, CORE, NECRON }
    record State(Phase phase, Map<DungeonAssistPolicy.F7Timer, Long> deadlines) {
        State { deadlines = Map.copyOf(deadlines); }
    }
    static State empty() { return new State(Phase.NONE, Map.of()); }
    static State observe(State previous, String chat, long now) {
        if (previous == null) previous = empty();
        if (now < 0 || now > Long.MAX_VALUE - 60_000L) return previous;
        String text = DungeonPolicy.normalize(chat);
        var event = DungeonAssistPolicy.f7TimerFromChat(text);
        Phase phase = previous.phase();
        if (text.equals("[BOSS] Storm: I should have known that I stood no chance.")) {
            return phase.ordinal() >= Phase.GOLDOR.ordinal() ? previous : new State(Phase.GOLDOR, Map.of());
        }
        if (text.equals("The Core entrance is opening!")) {
            return phase.ordinal() >= Phase.CORE.ordinal() ? previous : new State(Phase.CORE, Map.of());
        }
        Phase next = switch (event) {
            case MAXOR_START -> Phase.MAXOR;
            case STORM_START, STORM_PAD, STORM_PY -> Phase.STORM;
            case GOLDOR -> Phase.GOLDOR;
            case NECRON -> Phase.NECRON;
            default -> phase;
        };
        if (event == DungeonAssistPolicy.F7Timer.NONE) return previous;
        // Ignore delayed dialogue from an earlier phase in the same run.
        if (next.ordinal() < phase.ordinal()) return previous;
        var deadlines = new EnumMap<DungeonAssistPolicy.F7Timer, Long>(DungeonAssistPolicy.F7Timer.class);
        if (next == phase) deadlines.putAll(previous.deadlines());
        if (event == DungeonAssistPolicy.F7Timer.STORM_PAD) {
            deadlines.remove(DungeonAssistPolicy.F7Timer.STORM_START);
            deadlines.putIfAbsent(DungeonAssistPolicy.F7Timer.STORM_LIGHTNING,
                    now + DungeonAssistPolicy.STORM_LIGHTNING_MILLIS);
        }
        deadlines.putIfAbsent(event, now + DungeonAssistPolicy.f7TimerMillis(event));
        // Retain expired entries until phase change so duplicate chat cannot rearm them.
        return new State(next, deadlines);
    }
    static long displayedDeadline(State state, DungeonAssistPolicy.F7Timer timer, long now) {
        Long anchor = state.deadlines().get(timer);
        if (anchor == null || now < 0 || now > Long.MAX_VALUE - 60_000L) return 0L;
        boolean cycle = (timer == DungeonAssistPolicy.F7Timer.STORM_PAD && state.phase() == Phase.STORM)
                || (timer == DungeonAssistPolicy.F7Timer.GOLDOR && state.phase() == Phase.GOLDOR);
        if (!cycle || now < anchor) return anchor;
        long period = DungeonAssistPolicy.f7TimerMillis(timer);
        return now + period - (now - anchor) % period;
    }
    private DungeonF7TimerObservationPolicy() {}
}
