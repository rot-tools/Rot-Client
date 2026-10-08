# Sky Mall / HOTM observation fixes — 26.3

This shared slice applies to both Lite and Plus. It changes observation and
display state only; it does not apply mining multipliers, credit tracker
quantities, infer a perk's remaining duration or alter Pickobulus's ability
parser.

## Confirmed problems

The runtime previously refreshed `skyMallSeenAt` on every tick from the cached
tab value. The tab snapshot itself is rebuilt every 250 ms even when its
server data does not change. An unchanged perk could therefore stay displayed
forever despite the existing maximum observation age. The same problem
affected repeated reads of an unchanged HOTM tooltip. Expiry was checked only
with the HUD toggle enabled, so a screen hint could hold stale state longer.

The lore parser also missed the current primary server form: `Your Current
Effect` followed by a `■` effect row. A Golden/Diamond Goblin effect can wrap
onto a second line. Generic `New buff:` messages are used by other HOTX trees
too; treating every such message as Sky Mall can display a Fig/Mangrove buff
as a mining perk.

## Sources and provenance

Primary repositories were fetched for this work on 2026-10-08. Their focused
sources remain at these pins:

- [SkyHanni HOTX format fixtures](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/data/hotx/HotxPatterns.kt):
  `Your Current Effect`, bullet rows, wrapped effects and the reused `New buff:`
  prefix.
- [SkyHanni mining perk families](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/api/HotmApi.kt):
  Mining Speed, Mining Fortune, Powder, Pickaxe Ability cooldowns,
  Golden/Diamond Goblin chance and Titanium drops.
- [Skyblocker Sky Mall message fixtures](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/test/java/de/hysky/skyblocker/skyblock/chat/filters/SkyMallFilterTest.java):
  exact day-change and six observed perk-message families.

These are LGPL repositories. Their implementations were not copied. The
parsers and bounded observation policy are original Rot code over the publicly
observable text forms. No upstream release-to-current difference is claimed
to prove a new Hypixel API break.

## Resulting behavior

- Unchanged tab values and unchanged lore within the same menu do not renew
  observation age. Rebuilding a local snapshot provides no proof of a new
  server selection. An expired/invalidation-blocked passive value cannot
  immediately resurrect itself.
- A changed perk, a newly opened HOTM menu or an explicit new-buff server
  message provides a new observation. A new day may choose the same buff;
  its explicit message remains usable even when the text is unchanged.
- The existing 20-minute bound is retained as **maximum observation age**.
  It is not a timer for the actual game's current-day phase. Exact day-change,
  HOTM reset and `DISABLED` rows invalidate the displayed perk immediately.
- World/profile changes clear observation sources. Disabling both Sky Mall
  readers clears the display while retaining passive fingerprints; simply
  reenabling the module in the same unchanged menu does not validate stale
  lore. Hint-only mode observes and expires state independently of HUD mode.
- HOTM observation scans only the actual chest container, and only the item
  named Sky Mall. A player-inventory item with that name cannot inject lore.
- Current-effect lore is bounded, supports the marked/wrapped actual format,
  and stops before another widget/section. Possible-buff lists and foreign
  HOTF messages provide no current Sky Mall observation.

Existing fixtures remain unchanged and supported, including `+100 Mining
Speed`, `+50 Mining Fortune`, `Current buff: +15% Powder`, and cooldown text
with singular/plural `Pickaxe Ability Cooldown` and `Cooldown Reduction`.
Observed numeric values are displayed without hardcoding or applying their
multiplier. Unverified text outside the known mining families is refused
rather than falsely assigning another tree's effect to Sky Mall.

## Validation and limits

`SkyMallObservationPolicyTest` adds eight focused regressions for unchanged
snapshot expiry, unchanged GUI expiry/reopening, invalidation/passive
reappearance, world/profile reset and backwards clock, real current-effect
lore, legacy/current perk families, foreign widget boundaries, and exact
invalidation rows. The coordinating task runs the final build and both
edition boundaries.

Runtime validation remains pending: current Sky Mall item lore, wrapped Goblin
perk, natural rollover, module/HUD/hint switches, player-inventory injection
check, profile switch and world reconnect. Without a changed observation or
explicit server message the client cannot prove that an identical perk text
was independently selected again. No exact remaining duration is claimed.
