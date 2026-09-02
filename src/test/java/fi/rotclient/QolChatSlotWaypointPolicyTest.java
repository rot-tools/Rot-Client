package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ChatCommandsPolicyTest {
    @Test
    void parsesPartyGuildAndPrivateBangCommands() {
        var party = ChatCommandsPolicy.parseIncoming(
                "Party > [MVP+] Alice: !coords").orElseThrow();
        assertEquals(ChatCommandsPolicy.Channel.PARTY, party.channel());
        assertEquals("Alice", party.sender());
        assertEquals("!coords", party.body());

        var guild = ChatCommandsPolicy.parseIncoming(
                "Guild > Bob [Member]: !ping").orElseThrow();
        assertEquals(ChatCommandsPolicy.Channel.GUILD, guild.channel());
        assertEquals("Bob", guild.sender());

        var priv = ChatCommandsPolicy.parseIncoming("From Carol: !invite").orElseThrow();
        assertEquals(ChatCommandsPolicy.Channel.PRIVATE, priv.channel());
        assertEquals("Carol", priv.sender());
        assertTrue(ChatCommandsPolicy.parseIncoming("Alice: hello").isEmpty());
        assertTrue(ChatCommandsPolicy.parseIncoming("Party > Alice: coords").isEmpty());
    }

    @Test
    void channelGatesAndCoordsReply() {
        var incoming = ChatCommandsPolicy.parseIncoming("Party > Alice: !co").orElseThrow();
        ChatCommandsPolicy.Context ctx = context(true);
        assertTrue(ChatCommandsPolicy.decide(incoming, true, false, true, true, ctx).isEmpty());
        var decision = ChatCommandsPolicy.decide(incoming, true, true, true, true, ctx).orElseThrow();
        assertEquals(ChatCommandsPolicy.ActionKind.REPLY, decision.kind());
        assertEquals("x: 10, y: 20, z: 30", decision.payload());
        assertEquals("pc x: 10, y: 20, z: 30",
                ChatCommandsPolicy.replyCommand(incoming.channel(), incoming.sender(), decision.payload()));
    }

    @Test
    void pingIsSkippedInPartyAndInviteIsPrivateOnly() {
        ChatCommandsPolicy.Context ctx = context(true);
        var partyPing = ChatCommandsPolicy.parseIncoming("Party > Alice: !ping").orElseThrow();
        assertTrue(ChatCommandsPolicy.decide(partyPing, true, true, true, true, ctx).isEmpty());
        var guildPing = ChatCommandsPolicy.parseIncoming("Guild > Alice: !ping").orElseThrow();
        assertEquals("Current Ping: 42ms",
                ChatCommandsPolicy.decide(guildPing, true, true, true, true, ctx).orElseThrow().payload());
        var partyInvite = ChatCommandsPolicy.parseIncoming("Party > Alice: !invite").orElseThrow();
        assertTrue(ChatCommandsPolicy.decide(partyInvite, true, true, true, true, ctx).isEmpty());
        var privInvite = ChatCommandsPolicy.parseIncoming("From Alice: !inv").orElseThrow();
        assertEquals("p invite Alice",
                ChatCommandsPolicy.decide(privInvite, true, true, true, true, ctx).orElseThrow().payload());
    }

    @Test
    void partyAdminCommandsRequireLeader() {
        var warp = ChatCommandsPolicy.parseIncoming("Party > Alice: !warp").orElseThrow();
        assertTrue(ChatCommandsPolicy.decide(warp, true, true, true, true, context(false)).isEmpty());
        assertEquals("party warp",
                ChatCommandsPolicy.decide(warp, true, true, true, true, context(true))
                        .orElseThrow().payload());
    }

    @Test
    void emotesReplaceWholeTokensOnAllowedChat() {
        assertEquals("hello ❤",
                ChatCommandsPolicy.applyEmotes("hello <3", true).orElseThrow());
        assertTrue(ChatCommandsPolicy.applyEmotes("/home", true).isEmpty());
        assertEquals("/pc ❤",
                ChatCommandsPolicy.applyEmotes("/pc <3", true).orElseThrow());
        assertEquals("pc ❤",
                ChatCommandsPolicy.applyEmotes("pc <3", true).orElseThrow());
        assertTrue(ChatCommandsPolicy.applyEmotes("hello <3", false).isEmpty());
    }

    @Test
    void shortcutsExpandFirstTokenAndCommandTargets() {
        String shortcuts = "ah=/ah\nbz -> /bz\nhi=hello";
        assertEquals("hello world", ChatCommandsPolicy.expandShortcut(true, shortcuts, "hi world"));
        assertEquals(
                "ah s",
                ChatCommandsPolicy.commandFromShortcut(true, shortcuts, "ah s").orElseThrow());
        assertEquals("ah s", ChatCommandsPolicy.chatFromShortcut(true, shortcuts, "ah s"));
        assertEquals("hello", ChatCommandsPolicy.chatFromShortcut(true, shortcuts, "hi"));
        assertTrue(ChatCommandsPolicy.commandFromShortcut(true, shortcuts, "hi").isEmpty());
    }

    @Test
    void chatRulesHideAndReplaceIncomingLines() {
        var rules = ChatCommandsPolicy.parseRules(
                "hide Watchdog\nreplace foo => bar\n# skip\nhide (broken");
        assertTrue(ChatCommandsPolicy.shouldHideIncoming(true, rules, "§c[WATCHDOG] banned"));
        assertFalse(ChatCommandsPolicy.shouldHideIncoming(true, rules, "hello foo"));
        assertEquals("hello bar", ChatCommandsPolicy.applyIncomingReplacements(true, rules, "hello foo"));
    }

    @Test
    void chatRulesRejectRiskyOrUnboundedExpressionsAndBadReplacementGroups() {
        String tooLong = "x".repeat(ChatCommandsPolicy.MAX_RULE_PATTERN_CHARS + 1);
        var rules = ChatCommandsPolicy.parseRules(String.join("\n",
                "hide (a+)+$",
                "hide (?<=prefix)value",
                "hide (a)\\1",
                "hide " + tooLong,
                "replace safe => $99",
                "hide accepted"));

        assertEquals(2, rules.size());
        assertTrue(ChatCommandsPolicy.shouldHideIncoming(
                true, rules, "accepted"));
        assertEquals(
                "safe",
                ChatCommandsPolicy.applyIncomingReplacements(
                        true, rules, "safe"));
    }

    @Test
    void chatRulesCapRuleCountAndInputLength() {
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < ChatCommandsPolicy.MAX_CHAT_RULES + 10; i++) {
            source.append("hide rule").append(i).append('\n');
        }
        var rules = ChatCommandsPolicy.parseRules(source.toString());

        assertEquals(ChatCommandsPolicy.MAX_CHAT_RULES, rules.size());
        assertFalse(ChatCommandsPolicy.shouldHideIncoming(
                true,
                List.of(new ChatCommandsPolicy.ChatRule(
                        true,
                        java.util.regex.Pattern.compile("suffix"),
                        "")),
                "x".repeat(ChatCommandsPolicy.MAX_RULE_INPUT_CHARS) + "suffix"));
    }

    private static ChatCommandsPolicy.Context context(boolean leader) {
        return new ChatCommandsPolicy.Context(
                "Local",
                10,
                20,
                30,
                42,
                120,
                "20.0",
                "Dwarven Mines",
                "Pickaxe",
                leader,
                List.of("Alice", "Bob"),
                0.1D);
    }
}

final class SlotBindsPolicyTest {
    @Test
    void bindRequiresExactlyOneHotbarSlot() {
        assertTrue(SlotBindsPolicy.canBind(12, 36));
        assertFalse(SlotBindsPolicy.canBind(12, 13));
        assertFalse(SlotBindsPolicy.canBind(36, 40));
        assertFalse(SlotBindsPolicy.canBind(12, 12));
    }

    @Test
    void bindKeyCreatesRemovesAndRejects() {
        Map<Integer, Integer> binds = new LinkedHashMap<>();
        var start = SlotBindsPolicy.onBindKey(12, null, binds);
        assertEquals(SlotBindsPolicy.BindAction.START, start.action());
        var bad = SlotBindsPolicy.onBindKey(13, 12, binds);
        assertEquals(SlotBindsPolicy.BindAction.REJECT_NO_HOTBAR, bad.action());
        var done = SlotBindsPolicy.onBindKey(36, 12, binds);
        assertEquals(SlotBindsPolicy.BindAction.COMPLETE, done.action());
        binds = SlotBindsPolicy.apply(binds, done);
        assertEquals(36, binds.get(12));
        var remove = SlotBindsPolicy.onBindKey(12, null, binds);
        assertEquals(SlotBindsPolicy.BindAction.REMOVE, remove.action());
        binds = SlotBindsPolicy.apply(binds, remove);
        assertTrue(binds.isEmpty());
    }

    @Test
    void swapUsesNonHotbarSlotAndHotbarButton() {
        assertEquals(12, SlotBindsPolicy.swapInventorySlot(12, 36).orElseThrow());
        assertEquals(0, SlotBindsPolicy.swapHotbarButton(12, 36).orElseThrow());
        assertEquals(12, SlotBindsPolicy.swapInventorySlot(36, 12).orElseThrow());
        assertEquals(0, SlotBindsPolicy.swapHotbarButton(36, 12).orElseThrow());
    }

    @Test
    void lineDisplayGatesHoverAndShift() {
        assertTrue(SlotBindsPolicy.shouldDrawLines(
                SlotBindsPolicy.LineDisplay.HOVER, false, true, false));
        assertFalse(SlotBindsPolicy.shouldDrawLines(
                SlotBindsPolicy.LineDisplay.HOVER_SHIFT, false, true, false));
        assertTrue(SlotBindsPolicy.shouldDrawLines(
                SlotBindsPolicy.LineDisplay.HOVER_SHIFT, false, true, true));
        assertTrue(SlotBindsPolicy.shouldDrawLines(
                SlotBindsPolicy.LineDisplay.NONE, true, false, false));
        assertFalse(SlotBindsPolicy.shouldDrawLines(
                SlotBindsPolicy.LineDisplay.NONE, false, true, true));
    }
}

final class WaypointPolicyTest {
    @Test
    void parsesPartyAndAllChatCoordinates() {
        var party = WaypointPolicy.parseChat(
                "Party > [MVP+] Alice: x: 1, y: 2, z: 3 extra", true, true).orElseThrow();
        assertEquals("Alice", party.sender());
        assertEquals(1, party.x());
        assertTrue(party.fromParty());
        var all = WaypointPolicy.parseChat(
                "Alice: x: 4, y: 5, z: 6", false, true).orElseThrow();
        assertEquals(4, all.x());
        assertFalse(all.fromParty());
        assertTrue(WaypointPolicy.parseChat(
                "Party > Alice: x: 1, y: 2, z: 3", false, true).isEmpty());
        assertTrue(WaypointPolicy.parseChat(
                "Alice: hello", true, true).isEmpty());
    }

    @Test
    void rejectsOwnPingUnlessPersonalEnabled() {
        assertFalse(WaypointPolicy.shouldAcceptOwnPing("Local", "Local", false));
        assertTrue(WaypointPolicy.shouldAcceptOwnPing("Local", "Local", true));
        assertTrue(WaypointPolicy.shouldAcceptOwnPing("Alice", "Local", false));
    }

    @Test
    void addRejectsBoundsAndDuplicates() {
        List<WaypointPolicy.Marker> existing = new ArrayList<>();
        var added = WaypointPolicy.add(
                true, existing, "Alice", 1, 2, 3, 1000L, WaypointPolicy.CHAT_DURATION_MS, 0);
        assertEquals(WaypointPolicy.AddStatus.ADDED, added.status());
        existing.add(added.marker());
        assertEquals(WaypointPolicy.AddStatus.DUPLICATE,
                WaypointPolicy.add(true, existing, "Bob", 1, 2, 3, 1000L, 1L, 1).status());
        assertEquals(WaypointPolicy.AddStatus.OUT_OF_BOUNDS,
                WaypointPolicy.add(true, existing, "Bob", 9000, 0, 0, 1000L, 1L, 1).status());
        assertTrue(added.marker().expired(1000L + WaypointPolicy.CHAT_DURATION_MS + 1L));
        assertFalse(added.marker().expired(1000L + 10L));
    }

    @Test
    void pingModeNormalizesLegacyDefault() {
        assertEquals(WaypointPolicy.PING_OFF, WaypointPolicy.normalizePingMode("Default"));
        assertTrue(WaypointPolicy.lookTargetPingEnabled("Look Target"));
        assertFalse(WaypointPolicy.lookTargetPingEnabled("Off"));
    }
}
