# Rot Client Lite release gate

Status: **blocked for public Modrinth publication**. The current `2.0.1+mc26.2`
pair is a development playtest. Compile, unit tests, and `verifyLegitJar` cover
the separated features; they do not establish server compliance or Minecraft
runtime behavior.

## Edition audit still required

- Move or remove the shared Dungeon ESP/solvers, terminal overlays and click
  helpers, puzzle assists, and F7 solver paths where they expose
  information or interactions outside passive HUD/QoL. Keep ordinary dungeon
  status and map HUD behavior intact while splitting the large shared runtime.
  F7 wrong-click cancellation and Dungeon Breaker block rewriting/secret-skip
  hooks now compile from Plus only; the shared F7 solver state and the four
  dungeon catalog parents still need separation. Terminal wrong-slot blocking,
  clicked-slot hiding, and solver tooltip suppression also compile from Plus;
  the visible Lite terminal overlay and its solver policy remain in scope.
- Diana burrow/rare-mob visual assists, Diana Profit/Share, Experiment Solver, Terminal Click Trails, and Terminal Simulator are Plus-owned. Revalidate their migrated settings in Minecraft.
- Auto Sprint's input override is now Plus-only. Audit every remaining input,
  click, macro, movement, and see-through-block behavior in Lite. An option being off by default is not a
  release decision.
- Sea-creature auto attack/delay and fishing hotspot radar/tracer are Plus-owned.
- Auto Clicker typed config, whitelists, and HUD pose are Plus-owned; old root
  values survive as opaque profile data. Revalidate the migrated whitelist,
  CPS settings, enable state, and HUD position in Plus at Minecraft runtime.
- Cheater Wardrobe and Superboom still leave typed settings in Lite. The
  Superboom click-trigger/timed swap-back state is Plus-only. Move the remaining
  settings incrementally, retaining old profile data and ordinary
  wardrobe/dungeon HUD behavior.
  Review other world-information assists alongside the mining and
  foraging helper settings in the broader Lite visual audit.
- Re-run `verifyLegitJar` with forbidden identifiers and class names for each
  newly separated module. Test old-profile migration and Plus behavior after
  every split.

## Validation before upload

Build the two playable JARs with Java 25 and run `test`, `testPlus`, `check`,
`clean build`, and `git diff --check`. Verify the SHA-256 of the actual copied
files. Launch a Fabric 26.2 Prism instance with **only Lite enabled** and test
`/rot`, dashboard, profile switching, legacy profile upgrade, commission HUD,
Party Finder, and every remaining Lite module's UI and input behavior on a
controlled server session. Record observations as runtime evidence; do not
infer them from green tests. Relaunch Plus separately to check retained
settings and feature behavior.

## Project metadata

Publish Lite only to the Rot Client Modrinth project; Plus has different
functionality and needs a separate project. Verify Minecraft `26.2`, Fabric,
Fabric API and Loader dependencies, version `2.0.1+mc26.2`, MIT license,
Rot Tools ownership, English plain-text description, credits, and changelog.
Fill all applicable content disclosures accurately, including generative-AI
usage where applicable. Do not upload AI-generated page or gallery images.
Verify rights for every bundled asset.

Rules checked 2026-09-15: [Modrinth Content Rules](https://modrinth.com/legal/rules)
and [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications).
