package fi.rotclient;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Three Weirdos speech and rotated NPC-to-chest offset adapted from NoammAddons
 * ThreeWeirdosSolver.kt, CC0-1.0, revision 4164abc38500e041a805e03ca0f01b46e37bd8a5.
 * Rot anchors NPC chat, requires the identified room and does not remove world blocks.
 */
public final class DungeonWeirdosObservationPolicy {
    private static final Pattern NPC = Pattern.compile("^\\[NPC] (\\w{1,32}): (.{1,256})$");
    private static final List<Pattern> SOLUTIONS = patterns(
            "The reward is not in my chest!",
            "At least one of them is lying, and the reward is not in .+'s chest\\.?",
            "My chest doesn't have the reward\\. We are all telling the truth\\.?",
            "My chest has the reward and I'm telling the truth!",
            "The reward isn't in any of our chests\\.?",
            "Both of them are telling the truth\\. Also, .+ has the reward in their chest\\.?");
    private static final List<Pattern> WRONG = patterns(
            "One of us is telling the truth!",
            "They are both telling the truth\\. The reward isn't in .+'s chest\\.",
            "We are all telling the truth!",
            ".+ is telling the truth and the reward is in his chest\\.",
            "My chest doesn't have the reward\\. At least one of the others is telling the truth!",
            "One of the others is lying\\.",
            "They are both telling the truth, the reward is in .+'s chest\\.",
            "They are both lying, the reward is in my chest!",
            "The reward is in my chest\\.",
            "The reward is not in my chest\\. They are both lying\\.",
            ".+ is telling the truth\\.",
            "My chest has the reward\\.");
    private DungeonWeirdosObservationPolicy() { }
    public record Speech(String npc, boolean solution) { }

    public static Optional<Speech> speech(String message) {
        if (message == null || message.length() > 512) return Optional.empty();
        var match = NPC.matcher(DungeonPolicy.normalize(message));
        if (!match.matches()) return Optional.empty();
        String text = match.group(2);
        if (SOLUTIONS.stream().anyMatch(p -> p.matcher(text).matches())) return Optional.of(new Speech(match.group(1),true));
        if (WRONG.stream().anyMatch(p -> p.matcher(text).matches())) return Optional.of(new Speech(match.group(1),false));
        return Optional.empty();
    }

    public static DungeonRoomDataPolicy.IntVec chest(int npcX, int npcZ, DungeonRoomDataPolicy.Rotation rotation) {
        return DungeonRoomDataPolicy.fromComp(1,69,0,
                new DungeonRoomDataPolicy.Rotation(rotation.degrees(),npcX,npcZ));
    }

    private static List<Pattern> patterns(String... patterns) {
        return java.util.Arrays.stream(patterns).map(Pattern::compile).toList();
    }
}
