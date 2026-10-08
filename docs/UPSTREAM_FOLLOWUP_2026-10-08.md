# 26.3 module follow-up — 2026-10-08

This continues the [first source refresh](UPSTREAM_REFRESH_2026-10-08.md).
Future maintenance is Minecraft **26.3 only**, for both Rot Client Lite and
Rot Client+. Historical 26.1.2 and 26.2 artifacts are retained without changes.

## Shared commission destination labels

The existing Mining Helpers module gains an opt-in **Commission Destinations**
setting. It renders Rot's existing public Dwarven landmark coordinates for
incomplete location-specific commissions. Duplicate tasks in one region share
one label. Completed/replaced tasks drop their label. With all-area labels also
enabled, active destinations use a gold `Commission:` label.

This original implementation compares the observable feature of Skyblocker's
[CommissionLabels](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/dwarven/CommissionLabels.java).
No LGPL implementation or new coordinates were copied. Generic mining tasks
and unknown regions do not invent a destination. No ore scanning, automatic
navigation, hidden-block observation or emissary interaction is added. This
shared display works even when the separate Commission Display HUD is off.

## Sources and compatibility

### Terminal state — Plus only

Rubix locks one goal per fully observed nine-pane menu. Configured left-only
costs choose the initial goal; later clicks do not choose a different goal.
Highlights, hover, wrong-click filters, queues and auto-clicks use the same
session state. Closing or replacing the menu clears the session.

Starts With records pending slots only when clicks are actually sent. Applied
SetSlot/Content packets confirm a matching slot only with a newer 15-bit
container state. Foreign containers, unchanged states and older corrections
do not acknowledge it. State wrap is handled. The local simulator uses a
separate explicit accepted-click callback. No Lite action implementation was
added to the shared packet bridge. Intrinsic-glint and constant-state server
behavior still need runtime testing.

### Pet menus and cocoon recovery

The [Pet follow-up](UPSTREAM_PET_FOLLOWUP_2026-10-08.md) fixes page/search
absence clearing the active pet. Global summary observations and exact menu
UUIDs improve authority without guessing tab UUIDs, rarity or XP curves.

The [Slayer follow-up](UPSTREAM_SLAYER_FOLLOWUP_AUDIT.md) uses Athen
`0869043ae54428e7761383a7f60f44d97cf206ac`, including cocoon fix `879d583861`.
Exact local cocoon/restart messages can suspend a still-active known owned
boss without crediting its fake death and recover through matching quest and
unique fresh body/nameplate evidence. The original bounded chain also rejects
post-recovery duplicate messages. Already credited deaths are never reversed;
death-before-cocoon ordering, ownerless T5 phases and new Rift body identity
remain open.

All ten repositories' default-branch metadata was rechecked. Athen and
Devonian have newer October 8 source commits; the feature-specific audits
record their exact pins. The other previously reviewed default snapshots
remain current for this comparison. Archived Nebulune remains a historical
reference rather than a maintained 26.3 dependency.

The [official patch list](https://hypixel.net/forums/skyblock-patch-notes.158/)
and [Public API v2 documentation](https://api.hypixel.net/) were rechecked.
No new REST schema migration was inferred from gameplay announcements. The
previous API backoff fixes and observed mining quantity authority remain.

Permitted derived rules keep their existing full third-party notices inside
both JARs. GPL/LGPL/AGPL sources are behavior references, not imported code.

## Manual checks

Test Lite and Plus separately in 26.3. Recheck Pet selection/XP and empty or
filtered Pets pages, location-specific commissions completing/replacing,
disabling the HUD while keeping destination labels enabled, and all-area
labels with destination highlighting. Verify no labels appear outside
Dwarven Mines. New behavior remains Ready for Runtime Test.

Further terminal/Slayer contracts and the final automated checkpoint are
recorded below after integration. Full upstream feature parity, exact
Pickobulus accounting and public-platform approval are not implied.

In Plus also test fixed Rubix goals with Queue/Clone/left-only settings,
intrinsic-glint Starts With items, delayed/duplicate slot updates, menu closes
and reopen, and local TermSim. Test exact cocoon/restart and a final normal
kill, duplicate messages, foreign simultaneous spawns, relog and drops/history.

## Automated checkpoint

Java 25 client compilation and the full 26.3 Gradle build pass with **2,481
shared + 236 Plus tests**, zero failures/errors/skips. `verifyLegitJar` and
`verifyDungeonJarBoundary` pass. There are 34 new regression cases. Independent
review caught and repaired stale terminal acknowledgements across disabling
and re-enabling the module.

Local Windows testing needed a native-encoding Gradle launcher setting for the
non-ASCII cache path. Java source compilation is explicitly UTF-8. CI's initial
run caught an old expected-version assertion, updated for 2.0.3. Neither issue
was bypassed by skipping tests. Minecraft runtime validation remains pending.
