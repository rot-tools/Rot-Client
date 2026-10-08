# Plus mining marker and preview state audit — 2026-10-08

Scope: Minecraft **26.3**, Rot Client+ only. Puzzler and the Pickobulus
world preview remain physically in `src/plus` / `src/plusClient`. The shared
Pickobulus cooldown HUD is maintained separately. This slice does not change
mining ledgers, item quantities, or the live tracker's evidence rules.

## Primary sources and reuse

Skyblocker `main` was checked at
`f5cc8799c6d9c0c0a75bcba525bc1487245c31b0`
(2026-10-06T14:04:40-04:00). Its checked-in license is LGPL v3, rather than
MIT. These sources were used for behavior/protocol comparison; no source
implementation was copied into Rot's MIT code.

| Pinned source | Observation used |
| --- | --- |
| [Puzzler.java](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/dwarven/Puzzler.java) | Exact NPC prefix, ten-arrow challenge, origin and orientation agree with Rot's existing coordinates. |
| [PuzzlerTest.java](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/test/java/de/hysky/skyblocker/skyblock/dwarven/PuzzlerTest.java) | An actual ten-arrow message replaces Rot's artificial two-/four-arrow test fixtures. |
| [PickobulusHelper.java](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/dwarven/PickobulusHelper.java) | Requires the named held-item ability and a mining context; treats supported areas separately. |
| [ItemAbility.java](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/utils/ItemAbility.java) | Optional `⦾` prefix and right-click activation on ability headers, handled by shared `PickobulusPolicy`. |
| [LICENSE](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/LICENSE) | LGPL v3; behavior-only comparison for this slice. |

Direct official Puzzler/HOTM wiki links currently redirect to the July 2026
wiki closure page. Historical search snippets therefore do not prove current
server geometry. No new server radius, projectile offset, cooldown, or exact
block count is asserted from those snippets or from another client's code.

## Concrete repairs

- `PuzzlerPolicy`: accept exactly ten arrows after the authentic NPC prefix;
  ignore unrelated/forwarded chat. Retire the old answer on subsequent NPC
  replies, expiry, clock reversal, world/player/config identity changes,
  Hypixel profile changes, disabled settings or leaving the valid area.
- `PuzzlerRuntime`: render a thin, depth-tested marker slightly above the
  target tile's upper surface. This avoids the floor-face z-fighting in the
  previous full-block fill. It does not alter world blocks or click/mine.
- `PickobulusPreviewPolicy`: reject extreme signed offsets before squaring
  to prevent integer overflow. Clamp radius to 1–5 and range to 4–64.
  Cache immutable results with a maximum of 1,331 candidates, a 250 ms scan
  interval and a 500 ms freshness limit.
- `PickobulusPreviewRuntime`: require the actual Pickobulus ability header,
  live SkyBlock presence and a recognized mining location. HUD and render
  reads invalidate stale world/player/config/profile/area/tracker/tool/lore
  or shape settings before using counts. Aim changes or misses clear the old
  footprint on the next client tick; they do not bypass the scan budget.
- Preview classification snapshots the selected tracker once per scan.
  Gemstone selection cannot use the compatibility Gold material fallback.
  Ambiguous candidates use the strict reviewed area gate, even when selected;
  the live tracker's explicit-target override is deliberately unchanged.
  Multiple classifiers still contribute at most one candidate per position.

Only already-loaded chunks are read. A held item removed, a disabled preview,
or invalid context yields no preview rows. A fresh scan may resume after the
250 ms budget permits; the renderer never performs a footprint scan.

## Validation and remaining work

Six Puzzler tests and twelve preview tests cover real input/orientation,
authentic reply retirement, scope/expiry/clock lifecycle, integer extremes,
bounded settings, immutable/capped publication, scan pacing, context/aim
changes, strict Hard Stone area evidence and material/gemstone isolation.
Root runs compilation, the complete shared/Plus tests and `verifyLegitJar`;
this audit does not claim those results before that run finishes.

Minecraft runtime validation remains required:

1. Talk to Puzzler in Dwarven Mines; check the marked tile and retirement after
   completion/wrong-answer chat, leaving the area and disabling the helper.
2. With an actual Pickobulus tool held, enable the Plus preview and change
   aim, radius/shape, tracker, profile and held item; old counts/boxes must
   retire before the next valid scan.
3. Compare estimated candidates against controlled throws in each supported
   mining area. Terrain exposure, projectile geometry, server modifiers and
   decorative block lookalikes remain sources of prediction error.

The preview remains **estimated candidates**, not an authoritative future
break list or loot projection. The server's total destroyed-block chat does
not identify target-specific quantities. No forecast is credited to Current
Session, material or gemstone accounting. Gold Mine is not currently a
recognized Rot area enum and is not invented by this repair; extending area
coverage requires separate evidence and tests.
