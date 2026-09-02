# Contributing to Rot Client

Contributions should preserve tracker accuracy, family isolation, user privacy, and a reviewable Git history.

Rot Client was originally authored under the OgRudolf identity (historical attribution) and is developed publicly at [rot-tools/Rot-Client](https://github.com/rot-tools/Rot-Client). Use a focused branch and open a pull request for review before merging changes.

## Development setup

Rot Client targets Minecraft `26.2`, Java `25`, Fabric Loader `0.19.3`, and Fabric API `0.155.2+26.2`. Use the included Gradle wrapper rather than a separately installed Gradle version.

Before proposing a change, run with Java 25:

```sh
./gradlew test --rerun-tasks
./gradlew compileClientJava
git diff --check
./gradlew clean build
```

See [docs/TESTING.md](docs/TESTING.md) for the validation layers, controlled deployment rules, diagnostic markers, and runtime checklists.

UI and branding changes should follow [docs/BRANDING.md](docs/BRANDING.md) for palette tokens, icon usage, and contrast rules. Reuse `RotClientTheme` constants rather than ad hoc colors.

## Tracking invariants

- Material routes may mutate only the selected material target's state.
- Gemstone routes may mutate only the selected gemstone's state.
- Switching between material and gemstone selections must not leak counts, timing, Fortune, or prices between families.
- `TrackerConfig.selectedTarget()` is a legacy Gold compatibility fallback for material callers. New code must check the selection family before using it.
- Fortune commands and live Fortune detection are material-only. Gemstone rejection must not mutate Fortune state.
- A gemstone block observation establishes direct-break context and block count; it does not by itself prove an exact item quantity.
- Rough Gemstone credit requires a positive matching Sack change and a consumable direct-break batch.
- Exact `PRISTINE` Flawed credit is immediate; a later matching Flawed Sack change confirms delivery and must not credit it again.
- Rejected non-positive or wrong-gemstone Sack events must not consume a valid pending direct-break batch.
- Diagnostic-only observers must never mutate persistent state or authorize live credits.
- `RotClientCurrentSession` is the single durable generic session ledger. HUD and Analytics are read-only projections and must not own another durable quantity store.
- One accepted generic observation may produce at most one Current Session item mutation. Target-family gains must never leak into OTHERS.
- Pause closes open Current Session target/area segments; Resume opens new segment boundaries without rewriting prior segments.
- Session History is an immutable archive, never a live ledger. Start New is archive-first; an archive failure must preserve the original Current Session. If publishing the next Current Session fails, the compensating History removal and any rollback failure must remain explicit.
- Frozen valuation must not change when live Bazaar prices later change, and pricing must never modify item quantities.
- Coverage of all 25 official Mining Collection keys is registry coverage, not proof that every mechanics, alias, or canonical item field is resolved.

## Future accounting work

Confidently correlated non-target mining observations can enter canonical Current Session OTHERS through the existing single terminal mutation path. Transient `TARGET_MINED` mirroring, parity, and diagnostics remain engine-owned and must not become another persistent ledger. Session History 2.0 stores immutable Current Session freezes and preserves legacy schema v1 records.

`MOB`, `CHEST`, and `CURRENCY` are represented in History. MOB has bounded live
Current Session ingest; Powder Chest Tracker projects CHEST and CURRENCY rows.
Controlled Hypixel runtime evidence is still required. Implement or extend each
source with stable identifiers, source classification, quantity authority,
deduplication, expiry, target-family isolation, and runtime evidence before
claiming Hypixel-verified behavior. Currency must remain separate from
item-derived value.

Start New spans two local files rather than one storage transaction. Preserve
the archive-first ordering and compensating rollback behavior, but do not claim
crash atomicity: a process exit between replacements remains a recovery case.

## Tests and runtime validation

Add or update focused tests for pure-Java behavior, parsing, routing, migration, state normalization, and layout math. Client compilation is required for Minecraft-dependent code. A clean build proves packaging, not in-game correctness.

Changes to event correlation, source attribution, persistence, or HUD totals require controlled in-game validation with positive and relevant negative controls. Record what was observed without committing raw logs or screenshots.

## Code and documentation style

- Keep source, UI strings, comments, commit messages, and public documentation in English.
- Save text and Java files as UTF-8 without a byte-order mark.
- Avoid trailing whitespace and keep one final newline.
- Document whether a behavior is implemented, test-verified, runtime-tested, experimental, planned, or unsupported.
- Do not claim perfect accuracy, guaranteed profit, or Hypixel endorsement.
- Document automation-style and development scanner/interaction modules as
  Hypixel-protocol-compatible Serveri utilities, not live Hypixel
  production features. Keep them opt-in and disabled by default, and preserve
  the planned official-server safety boundary.
- Add an explicit `QolNumberSettings.spec` for every numeric QoL control; do not
  rely on the generic fallback range.
- Do not add personal machine paths or usernames, launcher instance names, raw diagnostic files, screenshots, tokens, secrets, or machine-specific Java settings.
- Use `/rot` in documentation for current command examples. Historical changelog entries may retain the command names that were current at release.

## Git practices

- Keep commits narrowly scoped and stage only reviewed files.
- Do not combine documentation, runtime deployment, and unrelated cleanup automatically.
- Do not force-push shared branches.
- Do not rewrite history to remove an ordinary current-tree artifact; delete it in a normal cleanup commit.
- Do not commit build outputs, runtime configs, local backups, logs, screenshots, or temporary files.
- Review the complete staged diff before committing and verify the branch and remote state before pushing.

## Reporting issues

Include the Rot Client version, Minecraft/Fabric versions, selected target, expected behavior, observed behavior, and reproducible steps. Provide only the smallest sanitized diagnostic excerpt necessary; never post credentials or an unreviewed raw log.
