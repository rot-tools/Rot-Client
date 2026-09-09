# Rot Client Testing and Runtime Validation

Testing is layered. Each level answers a different question, and no lower-cost check substitutes for observed in-game behavior.

## Test levels

- **Unit test:** Exercises isolated Java logic, arithmetic, parsing, routing, normalization, or state transitions without a running Minecraft client.
- **Integration-style local test:** Connects multiple pure-Java components, such as signed Sack parsing, gemstone matching, batch evaluation, and pending-signal preservation.
- **Client compilation:** Compiles the Minecraft-dependent client source against the configured Fabric and Minecraft mappings.
- **Clean build:** Recreates compiled outputs and packages normal and sources JARs from a clean build directory.
- **Controlled deployment:** Replaces only the previously installed Rot Client playable JAR after process, backup, uniqueness, and hash checks.
- **Runtime diagnostic validation:** Uses opt-in structured markers plus visible HUD/status behavior to prove what happened in Minecraft.

## Standard commands

Use Java 25 and the included Gradle wrapper.

```sh
./gradlew test testPlus --rerun-tasks
./gradlew compileClientJava compilePlusJava compilePlusClientJava
git diff --check
./gradlew clean build
```

### Windows PowerShell

Configure the current PowerShell process to use an installed Java 25 distribution, then run:

```powershell
.\gradlew.bat test testPlus --rerun-tasks --console=plain
.\gradlew.bat compileClientJava compilePlusJava compilePlusClientJava --console=plain
git diff --check
.\gradlew.bat clean build --console=plain
```

The historical shadow-ledger checkpoint `3b372d4` passed 505 tests. The latest
pre-audit checkpoint `8110ab4` passed 1,137 tests with no failures, errors, or
skips. Record the exact post-audit total in the checkpoint report rather than
silently treating either historical count as the current result.

## Runtime log privacy

Before attaching Minecraft, Prism, or launcher logs to issues or reviews, redact usernames, profile identifiers, local Windows paths, instance names, server payloads, and unrelated chat. Prefer the smallest sanitized excerpt that proves the Rot Client behavior under test.

## Playable JAR validation

`build/libs/` contains two playable JARs (`RotClient-2.0.1+mc26.2.jar` and
`RotClientPlus-2.0.1+mc26.2.jar`) plus `-sources.jar` extras. Only the two
playable files belong in `.minecraft/mods/`. Enable **one** in the launcher
(`rotclient` and `rotclientplus` break each other). See
[Which JAR](WHICH_JAR.md).

`verifyLegitJar` fails the legit ZIP if Plus-only tokens such as
`AutoClickerRuntime`, `FreecamRuntime`, `qol.auto_clicker`,
`CameraFreecamMixin`, or `rotclient.plus.mixins.json` are present.

Before replacement:

1. Confirm Minecraft is closed and the installed JARs are not locked. Do not terminate processes automatically.
2. List all installed filenames matching Rot Client or MiningTracker case-insensitively. Stop if a MiningTracker JAR is present, or if unexpected extra Rot Client versioned JARs remain besides the two current playable files.
3. Back up the prior installed Rot Client JARs outside the repository and verify SHA-256.
4. Record each built playable JAR's SHA-256 and size.
5. Remove only the verified prior Rot Client or MiningTracker JARs.
6. Copy both normal playable JARs; never copy the sources JAR.
7. Verify installed SHA-256 matches each built JAR.
8. Confirm no unrelated mod or repository file changed.

Do not launch Minecraft automatically as part of deployment. If replacement fails after removal, restore the verified prior JARs.

## Twelve-mod comparison checkpoint (2026-08-25)

Automated gate:

- `1,962` tests, `0` failures, `0` errors, `0` skipped across `284` suites.
- `compileClientJava` and `clean build` passed.
- Prism playable JAR SHA-256:
  `374790A9FF0B6AC00849995B0CE1631313B34A08490865F058BA65F315A0E114`.
- Exactly one Rot Client JAR was installed; Minecraft was not launched.
- 2026-08-26: first launch crashed because `ScreenCustomTooltipScrollMixin`
  injected `Screen.mouseScrolled`, which Minecraft 26.2 does not declare on
  `Screen`. The replacement JAR uses Fabric `ScreenMouseEvents.allowMouseScroll`.
- 2026-09-02: hover-box pan wraps `GuiGraphicsExtractor.tooltip` /
  `positionTooltip` and records wheel from `MouseHandler.onScroll`. The
  previous `setTooltipForNextFrame` `@ModifyVariable` crashed mixin apply.

The following remain **Ready for Runtime Test**, not runtime-verified:

- [ ] Start with Rot Client alone; confirm startup, config migration/backup,
  dashboard navigation, profile detection, and location changes.
- [ ] Leave the dashboard on a non-Overview page with a module Settings or HUD
  drawer open, close it, then press Right Shift; confirm the same page and
  drawer return. Closing the drawer first should reopen the page without it.
- [ ] Install Hypixel Mod API and confirm location events prefer the official
  packet when recognized while scoreboard fallback still works.
- [ ] Open a terminal, queue clicks, close it, then open an ordinary chest;
  confirm no stale terminal click reaches the new container.
- [ ] Exercise Chat Rules with valid, invalid, long, and replacement-group
  patterns; confirm chat remains responsive.
- [ ] Validate Diana rare-mob detection remains responsive at the reduced
  five-scans-per-second cadence; confirm burrow evidence appears only with the
  live Burrows widget plus Ancestral Spade and disappears within 750 ms.
- [ ] Start a new Slayer quest while the prior owned boss entity is still
  visible; confirm it cannot re-register or increment the new quest.
- [ ] Toggle Craft Helper off/on, compare tooltip totals against inventory plus
  observed Storage pages, and confirm disabled state hides recipe details.
- [ ] Open a dungeon reward chest and confirm the profit HUD identifies its
  values as bundled prices.
- [ ] Open inventory, dashboard, ordinary containers, and Storage; confirm the
  custom cursor remains visible above slots/tooltips and follows the physical
  pointer exactly.
- [ ] Click through multiple Storage pages quickly; confirm each transition
  preserves the latest pointer coordinate without recentering or pinning later
  movement.
- [ ] Open the HUD layout editor: drag the How to edit / Layout Editor / inspector
  cards off the HUDs, then drag and scroll-scale a QoL overlay.
- [ ] Hover a long item tooltip with Custom Tooltip + Infinite Scroll on; confirm
  the wheel moves lore instead of the inventory hotbar.
- [ ] With Etherwarp left-click warp and auto-shift on, click air vs a valid
  destination block; sneak should fire only on the valid block.
- [ ] Measure dashboard FPS while scrolling and dragging the HUD editor.
- [ ] Test one overlapping SkyBlock client at a time; confirm the local warning
  appears and the exact overlapping-client mixin gates do not remove unrelated
  Rot features.

## Runtime diagnostic procedure

Use a controlled test session and avoid publishing raw diagnostic output.

1. Select the target and reset its session.
2. Run `/rotclient record start`.
3. Enable the tracker and perform a small amount of known target activity.
4. Include a negative control, such as an unrelated material, wrong gemstone, unsupported Sack source, or non-mining transfer.
5. Run `/rotclient status` and compare the visible state with expected ledger math.
6. Stop mining and wait beyond the auto-pause window; verify active time and rate behavior.
7. Toggle the tracker OFF and ON and verify that disabled activity is not credited.
8. Switch between material and gemstone selections and verify transition isolation.
9. Run `/rotclient record stop`.
10. Inspect the structured markers for accepted signals, rejected controls, consumed batches, and live ledger mutations.

A diagnostic marker is evidence only when its surrounding selection, tracker state, source, and corresponding ledger mutation are understood.

### Engineering tracking-trace controls

`/rotclient debug tracking`, `/rotclient debug tracking status`,
`/rotclient debug tracking on`, `/rotclient debug tracking off`, and
`/rotclient debug tracking clear` are engineering controls. They are separate
from `/rotclient record start|stop`, do not replace canonical Current Session
accounting, and must not be presented as normal gameplay controls.

## Shadow ledger and canonical OTHERS runtime procedure

Prerequisites: Current Session is ACTIVE. `/rotclient record start` opens the
diagnostic recorder and ensures collection is aligned, but the recorder does
not own the `MiningSessionEngine` lifecycle or Current Session quantities.

1. Select a material target (for example Gold) and enable tracking.
2. Mine the target; verify live `/rotclient status` block and resource totals.
3. Run `/rotclient shadow status`; confirm `TARGET_MINED` quantities match live session totals and parity reports MATCH.
4. Mine an off-target catalogued material (for example Hard Stone while Gold is selected).
5. Wait for the Mining Sack delivery (may arrive 5–10 seconds after the break).
6. Re-run `/rotclient shadow status`; confirm transient `OTHER_MINED` includes Hard Stone and `TARGET_MINED` is unchanged.
7. Verify the authoritative Gold target ledger did not change from the Hard Stone activity. For an approved persistent OTHERS source, also verify canonical Current Session and HUD OTHERS increased exactly once.
8. Run `/rotclient record stop`; confirm the shadow session is retained or cleared per engine policy.

### Verified shadow session (checkpoint `3b372d4`, Gold + Hard Stone)

Controlled runtime validation with Gold selected and diagnostic recording active confirmed:

- Target parity MATCH: 371 live blocks / 371 shadow blocks.
- Target raw-equivalent parity MATCH: 27,206 live / 27,206 shadow.
- OTHER_MINED Hard Stone: 2,235 Hard Stone, 1 Enchanted Hard Stone.
- Delayed Hard Stone Mining Sack correlation succeeded at approximately 6.15 seconds.
- Final shadow ledger: 33 `TARGET_MINED` entries, 3 `OTHER_MINED` entries.
- Target parity aggregate: 0 mismatches.
- Live Gold session totals unchanged by shadow observation.

## Shadow/canonical boundary runtime checklist

- [ ] `TARGET_MINED` shadow quantities match live target session totals (parity MATCH).
- [ ] Off-target Hard Stone credits appear only under `OTHER_MINED`.
- [ ] Delayed sack delivery (5–10 s) still correlates in shadow.
- [ ] Duplicate sack delivery does not create a second shadow entry.
- [ ] The authoritative selected-target ledger remains unchanged by off-target observation.
- [ ] For an approved persistent mining OTHERS source, Current Session and HUD OTHERS increase exactly once; transient target parity remains a read-only mirror.

## Gemstone runtime checklist

- [ ] Accepted direct breaks produce the expected block count.
- [ ] A positive matching Rough Gemstone Sack change credits the exact accepted amount.
- [ ] A valid `PRISTINE` message credits the exact Flawed amount.
- [ ] Flawed Sack confirmation does not create a second credit.
- [ ] Tier quantities, total items, Rough Equivalent, and average per block agree mathematically.
- [ ] Repeated delivery does not create duplicate live credits.
- [ ] A wrong gemstone is rejected and does not consume the selected gemstone's pending signal.
- [ ] A negative Sack change is rejected where the event is observable and does not consume a valid batch.
- [ ] Material state remains unchanged throughout gemstone selection.
- [ ] Active time caps at the 60-second auto-pause window after mining stops.
- [ ] HUD movement, scaling, settings labels, and screen clamping behave correctly.
- [ ] Reset clears only the selected gemstone session and leaves material and other gemstone state intact.

## Material positive-control checklist

- [ ] Select a supported material target and reset its session.
- [ ] Verified block activity changes the expected material block count.
- [ ] Exact inventory or Sack resource observation updates the expected material ledger where supported.
- [ ] Manual or automatic Fortune commands work for the material selection.
- [ ] Reset clears only the selected material target's session state.
- [ ] No gemstone session or lifetime state changes.

## Slayer foundation runtime checklist

Before marking the Slayer foundation runtime verified:

- [ ] Enable one Slayer module at a time and confirm disabled modules have no
  HUD, highlight, alert, command, or party-message side effect.
- [ ] Spawn and defeat one owned boss from each of the six Slayer families;
  verify family, Roman tier, owner, timer, kill count, and no duplicate count.
- [ ] Observe regular and strongest-tier minibosses; verify the configured
  distance gate and exactly one chat/title alert per entity.
- [ ] Verify boss, miniboss, and Inferno demon highlight colors, widths,
  ownership filter, and depth behavior independently.
- [ ] Add an other-player carry with `/rotclient slayer carry add`, open the
  manager, and verify only the matching owner/type/tier advances on death.
- [ ] Verify party announcement is sent only when both Carry Tracker and its
  explicit announcement option are enabled.
- [ ] Trigger a reviewed rare-drop line and verify the stored drop count plus
  bosses-since-drop reset without changing unrelated Slayer statistics.
- [ ] Change world or disconnect and verify active entity projections clear
  without erasing session statistics or the live carry list.
- [ ] Move the three foundation Slayer HUDs in the HUD editor and confirm their positions
  persist across restart.
- [ ] Enable Cocoon Alert, trigger exactly `YOU COCOONED YOUR SLAYER BOSS`, and
  verify one title/sound plus a movable countdown from 6.0 seconds to hidden.
- [ ] Attack Quazii and Typhoeus through ASHEN, AURIC, SPIRIT, and CRYSTAL;
  verify Dagger Swap selects the correct fire/maw dagger family, waits the
  configured delay/variance, and stops right-clicking once `td_attune_mode`
  matches.
- [ ] Verify Enderman Laser Hider suppresses only Guardian beams anchored to
  other-player Voidglooms. Own-boss beams remain visible, and carry-boss beams
  follow `Show for carries`.

The expanded matrix must additionally verify RNG Meter chance projection,
persistent completed-carry history across restart, configured carry-package
pricing, webhook disabled/invalid/valid behavior, and per-family/per-drop filter
editing. Auto Soulcry, Attunement, Vengeance, sound filtering, and known Big
Drops are implemented and automated-tested but still require controlled runtime
evidence.

## Legacy migration checklist

- [ ] With only `miningtracker.json` present, first launch creates `rotclient.json` by byte-copy.
- [ ] Original `miningtracker.json` remains unchanged.
- [ ] With only `miningtracker-session-history.json` present, first launch creates `rotclient-session-history.json` by byte-copy.
- [ ] Original history file remains unchanged.
- [ ] When `rotclient.json` already exists, legacy config is not overwritten.

## Current Session and Session History 2.0 runtime checklist

Session History 2.0 is automated-test validated but has not yet passed this
post-audit controlled Minecraft checklist:

- [ ] Resume an existing Current Session after restart and verify the next
  accepted quantity adds exactly once without clearing prior rows.
- [ ] Pause Current Session and verify collection remains off until Resume.
- [ ] Confirm active duration excludes the exact paused interval and that both
  durations survive archive/reload.
- [ ] Confirm Pause closes open target/area segments and Resume opens new
  boundaries without changing the previously closed segments.
- [ ] Change target and parent area; verify the archive contains closed,
  non-overlapping target and area segments with the expected boundaries.
- [ ] Verify canonical item rows, source/classification, known/unknown state,
  quantity, and valuation are unchanged by opening or copying History.
- [ ] Refresh Bazaar prices after archive and verify frozen historical values do
  not change.
- [ ] Confirm Start New writes exactly one schema v2 archive before opening the
  next display-numbered empty Current Session.
- [ ] Exercise an archive write failure in a controlled fixture or safe runtime
  setup and verify the original session identity, lifecycle, and rows survive.
- [ ] Exercise failure after archive creation and verify compensating History
  restoration is reported as either successful or failed; never label this
  two-file sequence a single storage transaction.
- [ ] Runtime-check startup reconciliation after a prepared-only marker, an
  archive-before-current interruption, and an already-published Current
  Session. Automated fixtures cover all three boundaries.
- [ ] Load representative schema v1 History, add a v2 archive, and verify both
  remain readable.
- [ ] Confirm `MOB` bounded ingest and Powder Chest Tracker CHEST/CURRENCY
  projection in a controlled run; do not advertise them as Hypixel
  runtime-verified until that evidence exists.

## Required diagnostic markers

The current gemstone pipeline emits these core markers:

| Marker | Meaning |
| --- | --- |
| `GEMSTONE_BLOCK_CHANGE` | A gemstone block became air; records whether direct targeting matched. |
| `GEMSTONE_DIRECT_BREAK` | A direct break was accepted into the pending batch. |
| `GEMSTONE_LEDGER_BLOCK` | Live persistent gemstone block state mutated. |
| `GEMSTONE_PRISTINE_OBSERVED` | A valid `PRISTINE` Flawed reward was parsed. |
| `GEMSTONE_SACK_CHANGE` | A signed gemstone Sack entry was parsed. |
| `GEMSTONE_SACK_CHANGE_REJECTED` | A non-positive or unsupported-source Sack entry was rejected. |
| `GEMSTONE_SACK_CORRELATION` | Correlation and final batch decision details. |
| `GEMSTONE_WOULD_CREDIT` | The validated pipeline determined a credit candidate. |
| `GEMSTONE_SIGNAL_CONSUMED` | A Rough credit consumed direct-break signals. |
| `GEMSTONE_SACK_CONFIRMATION` | A Flawed Sack entry confirmed pending `PRISTINE` credit without new credit. |
| `GEMSTONE_LEDGER_GAIN` | Live persistent gemstone tier state mutated. |

`GEMSTONE_INVENTORY_DELTA`, `GEMSTONE_SACK_MESSAGE`, `GEMSTONE_SACK_HOVER`, `GEMSTONE_SACK_HOVER_LINE`, and `GEMSTONE_SACK_OBSERVED` belong to diagnostic-only observation. They must not be interpreted as live credits without the final ledger marker.

## Rules for claiming validation

- Unit tests prove isolated logic under the tested inputs.
- Integration-style local tests prove the connected pure-Java path exercised by that test.
- `compileClientJava` proves client-source compilation against the configured dependencies.
- A clean build proves packaging, not in-game behavior.
- Matching built and installed JAR hashes proves deployed artifact identity.
- Runtime diagnostic markers and visible state prove observed in-game behavior for that session.
- Absence of an event in one runtime recording does not prove the event's runtime handling.
- A green CI run does not by itself make a feature release-ready.
- A feature may be called runtime-tested only after both positive and relevant negative controls are observed.

## Mining-session automated vs remaining regression coverage

### Historical shadow coverage at checkpoint `3b372d4`

- `TARGET_MINED` / `OTHER_MINED` category separation.
- Exclusion of target-mined items from `OTHER_MINED` totals.
- Delivery deduplication and correlation consumption.
- Reset and selection-transition isolation (engine tests).
- 7,320 ms material correlation regression (Hard Stone sack delay).

### Additional current automated coverage

- Full 23-selection tracker matrix: 11 live material targets plus all 12
  gemstones, exact material routing, ordinary/Pure Ore block-family coverage,
  raw/enchanted/enchanted-block inventory and Sack identity coverage,
  selected/wrong gemstone isolation,
  target-family exclusion from OTHERS, correlated Hard Stone OTHERS acceptance,
  per-selection reset boundaries, and TrackerStore/Current Session restart
  round-trips. The same matrix runs across every known area and `UNKNOWN`,
  asserting that location is attribution rather than a selected-target gate.
  Tungsten and Umber are asserted as separate live targets.
- The final canonical TARGET handoff runs a complete cross-selection matrix:
  every material and gemstone is rejected outside its active selection,
  combined Mithril + Titanium accepts both members, and unknown target IDs fail
  closed. Matching inventory and Sack raw totals reconcile once for every
  selectable material.
- Ordinary Coal, Iron, Gold, Lapis, Redstone, Emerald, Diamond, and Quartz block
  profiles are covered separately from their Pure Ore profiles. Ambiguous non-target Mithril/Titanium
  evidence remains area-gated even though the explicitly selected target does
  not.
- Durable Current Session persistence, restart plus next event, rejection of
  credits while paused, Resume collection, target auto-pause independence, and
  unknown stable IDs.
- Current Session valuation with explicit resolved, unavailable, unsupported,
  and stale states; pricing does not mutate quantities.
- Powder Chest atomic item/currency classification, duplicate replay handling,
  disabled-state rejection, and currency exclusion from item-derived value.
- Session History 2.0 freeze, archive-before-reset ordering, archive-failure
  preservation, exact prior-History restoration outcomes, active/paused durations,
  target/area segments, immutable item rows, frozen valuation, privacy bounds,
  schema v1 coexistence, and bounded Start New recovery before archive, between
  the two files, and after publish. This is recovery logic, not a claim of
  filesystem or power-loss atomicity across both files.
- Resume-time high-scale Bazaar normalization and malformed-product peer
  survival without Current Session quantity loss.

### Still planned / not fully covered

- `CHEST_LOOT` chat/Sack deduplication and expiry of unmatched chest events.
- Remote-ability MOB rewards (outside the bounded melee/projectile window).
- Controlled Minecraft runtime validation of Session History 2.0 and the
  72-module QoL matrix, including the Slayer foundation, mechanics, storage,
  and inventory-button slices.
- Runtime retest of the latest MOB Magic Find/noise-filter correction and
  expansion beyond the representative Powder Chest/MOB cases already observed.
- Hypixel production use of Serveri QoL automation (not in scope).

### QoL checkpoint validation

The legit catalog contains **110** wired parents. Rot Client+ contains **131**.
Before claiming a module runtime verified, test its switch, settings drawer,
reset behavior, persistence across restart, and the exact visual/input effect. Start with UI move/resize/snap,
Inventory Overlay, Price/Info Tooltips, Viewmodel, Item Scale, Experiments,
Wardrobe, Harp, GFS, Sell, Ghosts, and Camera.

Inventory Overlay persistence checklist:

- [ ] Equip necklace/cloak/belt/gloves and a pet, open survival inventory, and
  confirm the four bars and pet slot show those items.
- [ ] Fully close Minecraft, launch again, join SkyBlock, and open inventory
  without opening Stats or Pets first. The same equipment and pet should show.
- [ ] Unequip the pet in Pets, then confirm the pet slot returns to `+` after
  the Pets menu has finished loading.

Wardrobe Swapper controlled checklist:

- [ ] With the module disabled, its nine binds send no `/wd` command.
- [ ] Bind Wardrobe 1 and Wardrobe 9 to distinct keyboard or mouse inputs.
- [ ] While stationary, verify the hidden Armor Sets flow selects container slot
  36 for Wardrobe 1 and slot 44 for Wardrobe 9, then closes normally.
- [ ] Hold a movement key before pressing a bind; verify no swap starts.
- [ ] Begin moving after `/wd` but before the click; verify the pending swap is
  cancelled and the hidden container closes without an equipment click.
- [ ] Verify an unrelated chest title is never consumed as an Armor Sets menu.
- [ ] Verify equipped and empty slot markers do not produce an invalid click.
- [ ] Reset the module and confirm all nine binds clear while `Require
  Stationary` returns to enabled.

Automation-style modules are tested only on the owner's
Serveri. Verify the same GUI-title,
`ExtraAttributes` id, chat, scoreboard, and command contracts that the runtime
implementation expects; do not introduce a second Serveri-only logic path.

New source-category changes must be tested in diagnostic/shadow mode first.
Current Session remains the only durable generic ledger, and existing target
accounting and read-only HUD/Analytics projections remain the regression
baseline until runtime evidence approves a new ingress path.
