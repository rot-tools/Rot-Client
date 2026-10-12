package fi.rotclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
 * Observation signals adapted from Odin SimonSays.kt, BSD-3-Clause.
 * Copyright (c) 2025, odtheking
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Plus-only, Minecraft-free Simon phase observation.
 * Sources: NoammAddons (CC0-1.0), revision 4164abc38500e041a805e03ca0f01b46e37bd8a5,
 * src/main/kotlin/com/github/noamm9/features/impl/floor7/devices/SimonSays.kt:
 * its BlockChangeEvent button-check handler separates playback from input.
 * Odin (BSD-3-Clause), revision b0058000511ef755dfc6379330a1d23404e08df2,
 * src/main/kotlin/com/odtheking/odin/features/impl/boss/SimonSays.kt:
 * its BlockUpdateEvent / TickEvent.Server handlers observe the button grid,
 * powered buttons and powered start-button resets.
 * Full source notices remain in docs/third-party/{NoammAddons,Odin}-LICENSE.txt.
 * Rot adaptation uses complete tick snapshots, preserves repeated separated
 * flashes, and advances only a matching powered edge. Simultaneous new flashes
 * have unknown order and suppress suggestions until the next round or restart.
 */
final class DungeonSimonObservationPolicy {
    enum Phase { IDLE, PLAYBACK, INPUT, COMPLETE }

    /** Odin's grid check requires more than half of the sixteen stone buttons. */
    static final int MIN_INPUT_BUTTONS = 9;

    record State(
            Phase phase,
            DungeonF7Policy.SimonState simon,
            Set<EmberDungeonPolicy.IntVec> poweredButtons,
            boolean startPowered,
            boolean ordered) {
        State {
            poweredButtons = Set.copyOf(poweredButtons);
        }
    }

    static State empty() {
        return new State(Phase.IDLE, DungeonF7Policy.SimonState.idle(), Set.of(), false, true);
    }

    /** Call for an observed local start use; the powered start edge also resets. */
    static State restart() {
        return new State(Phase.PLAYBACK, DungeonF7Policy.SimonState.idle(), Set.of(), false, true);
    }

    /**
     * litButtons contains button coordinates mapped from exact sea-lantern
     * backing blocks. presentButtons contains actual stone buttons, and
     * poweredButtons their powered subset. contextValid requires the intended
     * F7/P3 context, loaded device chunks and an enabled solver; false clears
     * all state, including edge memory, on world/context loss.
     */
    static State observe(
            State previous,
            Collection<EmberDungeonPolicy.IntVec> litButtons,
            Collection<EmberDungeonPolicy.IntVec> presentButtons,
            Collection<EmberDungeonPolicy.IntVec> poweredButtons,
            boolean startPowered,
            boolean contextValid) {
        if (!contextValid) return empty();
        State state = previous == null ? empty() : previous;
        Set<EmberDungeonPolicy.IntVec> lit = gridPositions(litButtons);
        Set<EmberDungeonPolicy.IntVec> present = gridPositions(presentButtons);
        Set<EmberDungeonPolicy.IntVec> powered = gridPositions(poweredButtons);
        powered.retainAll(present);
        boolean inputAvailable = present.size() >= MIN_INPUT_BUTTONS;

        // Odin resets when the start stone button becomes powered.
        if (startPowered && !state.startPowered()) state = restart();

        Phase phase = state.phase();
        DungeonF7Policy.SimonState simon = state.simon();
        boolean ordered = state.ordered();
        if ((phase == Phase.INPUT || phase == Phase.COMPLETE) && !inputAvailable) {
            // Button disappearance begins a new round, not the dark gap between flashes.
            phase = Phase.PLAYBACK;
            simon = DungeonF7Policy.SimonState.idle();
            ordered = true;
        }
        if (phase == Phase.IDLE) {
            // Attaching mid-input cannot reconstruct already missed playback.
            if (inputAvailable || lit.isEmpty()) {
                return new State(Phase.IDLE, DungeonF7Policy.SimonState.idle(), powered, startPowered, true);
            }
            phase = Phase.PLAYBACK;
        }
        if (phase == Phase.PLAYBACK) {
            Set<EmberDungeonPolicy.IntVec> rising = new LinkedHashSet<>(lit);
            rising.removeAll(simon.prevLit());
            List<EmberDungeonPolicy.IntVec> order = new ArrayList<>(simon.order());
            if (rising.size() == 1 && ordered) order.add(rising.iterator().next());
            if (rising.size() > 1) ordered = false;
            // Noamm's button appearance gate, with Odin's majority-grid check.
            // Dark frames alone never publish a partial playback as input.
            if (inputAvailable && lit.isEmpty()) {
                phase = Phase.INPUT;
                simon = new DungeonF7Policy.SimonState(order, ordered ? order : List.of(), lit, !ordered);
            } else {
                simon = new DungeonF7Policy.SimonState(order, List.of(), lit, true);
            }
        }
        if (phase == Phase.INPUT && ordered) {
            // Server-observed power is authoritative: local use and its acknowledgement
            // must not consume twice, and a held powered button is only one input.
            Set<EmberDungeonPolicy.IntVec> pressed = new HashSet<>(powered);
            pressed.removeAll(state.poweredButtons());
            if (pressed.size() == 1 && pressed.contains(simon.nextButton())) {
                simon = DungeonF7Policy.consumeNext(simon);
                if (simon.remaining().isEmpty()) phase = Phase.COMPLETE;
            } else if (pressed.size() > 1) {
                ordered = false;
                simon = new DungeonF7Policy.SimonState(simon.order(), List.of(), lit, true);
            }
        }
        // Input feedback lanterns never append to the recorded playback.
        return new State(phase, simon, powered, startPowered, ordered);
    }

    private static Set<EmberDungeonPolicy.IntVec> gridPositions(
            Collection<EmberDungeonPolicy.IntVec> positions) {
        Set<EmberDungeonPolicy.IntVec> result = new LinkedHashSet<>();
        if (positions != null) {
            for (EmberDungeonPolicy.IntVec position : positions) {
                if (position != null && DungeonF7Policy.isSimonButton(position.x(), position.y(), position.z())) {
                    result.add(position);
                }
            }
        }
        return result;
    }

    private DungeonSimonObservationPolicy() {}
}
