# Rot Client Tracking Model

This document describes current live accounting separately from planned work. Client-observable events are imperfect, so accepted observations are evidence-based estimates rather than authoritative server accounting.

## 1. Material tracking overview

The live material selections are Coal, Iron, Gold, Lapis, Redstone, Emerald, Diamond, Quartz, combined Mithril + Titanium, Tungsten, and Umber. Each ordinary/Pure Ore pair shares one material ledger. The Mithril + Titanium selection tracks both materials simultaneously while preserving separate ledgers and a shared selection-level active-time view. Tungsten and Umber are independent Dwarven Metal targets.

Material tracking correlates target block observations with inventory, Compact, Mining Sack, Bazaar sale, Fortune, and price observations where supported. Resource reconciliation avoids adding overlapping inventory and Sack views as if they were independent gains.

## 2. Gemstone tracking overview

The searchable UI supports Ruby, Amber, Sapphire, Jade, Amethyst, Topaz, Jasper, Opal, Onyx, Aquamarine, Citrine, and Peridot. Each gemstone has isolated session and lifetime state.

The live gemstone ledger records accepted blocks and quantities by tier. It derives total gemstone items, Rough Equivalent, average per accepted block, active time, and hourly rate. Gemstone value and profit are unsupported.

## 3. Exact quantity sources

A material or gemstone block observation alone does not prove the exact number of items received. Exact or reconciled quantities come only from implemented client-visible sources that pass target, timing, sign, and deduplication checks.

For gemstones, current live quantity sources are:

- a positive matching Rough Gemstone change in a supported Gemstone Sack summary, correlated with direct-break context; and
- an exact valid `PRISTINE` message for Flawed Gemstones.

Inventory deltas and expanded Sack hover details are diagnostic-only observations. They do not authorize a live gemstone credit.

## 4. Direct-break correlation

When a selected gemstone block is directly targeted and changes to air while tracking is enabled, the tracker accepts a direct break, increments the selected gemstone's block ledger, and adds a timestamped signal to a pending batch.

Pending direct-break signals are grouped for later Rough Sack correlation. A matching live credit consumes the accepted signals assigned to that batch. Stale signals expire after 60 seconds.

## 5. Signed Sack handling

Gemstone Sack parsing preserves the sign of each observed change. A live Rough credit requires a positive amount, a supported summary source, the selected gemstone, and an eligible direct-break batch.

Zero or negative changes, wrong gemstones, unsupported sources, duplicates, and uncorrelated changes are rejected. A rejected non-positive or wrong-gemstone observation does not consume a valid batch for the selected gemstone. Repeated matching Sack fingerprints are deduplicated within 2.5 seconds.

Material Sack handling runs only for a material selection. Gemstone Sack handling runs only for an active gemstone selection.

## 6. PRISTINE handling

A valid `PRISTINE! You found ... Flawed <Gemstone> Gemstone!` observation credits the exact parsed Flawed quantity immediately when it matches the active selected gemstone and current context.

The tracker keeps a pending confirmation for up to 120 seconds. A later matching positive Flawed Sack entry confirms delivery and consumes the confirmation without adding a second live credit. Invalid, mismatched, or duplicate observations are rejected.

## 7. Session and lifetime ledger behavior

Material and gemstone states keep separate session and lifetime counters. Reset affects only the currently selected target's session state. Switching selection does not merge ledgers.

Material sessions may include blocks, reconciled raw-equivalent resources, Fortune, price estimates, sales, and active time. Gemstone sessions include blocks, tier quantities, Rough Equivalent, average per block, and active time; they do not include value or profit.

## 8. Auto-pause and active-time behavior

Accepted target mining starts or continues active time. When no accepted break occurs, active time advances only through the 60-second pause window and then stops. Rates use this capped active time rather than total wall-clock time.

Activity clocks are persisted safely when switching targets or closing the client, but they do not continue running while the client is closed. Tracking must be enabled explicitly for each launch.

## 9. Tracker-family isolation

The selected tracker family is checked before any family-specific route runs:

- material observations cannot credit a gemstone ledger;
- gemstone observations cannot credit a material ledger;
- one gemstone cannot credit another gemstone's ledger;
- material Fortune commands and detection reject gemstone selections without mutation;
- resets and activity transitions apply only to the selected target.

`TrackerConfig.selectedTarget()` returns Gold as a legacy compatibility fallback when a gemstone is selected. Callers must first prove that the selection is material-based; the fallback must never be used as routing authority.

## 10. Known limits

- Client-visible packet order, server formatting, lag, missed events, and upstream changes can affect observation and correlation.
- Transfers, purchases, chest loot, and unrelated inventory movement are not generalized live mining credits.
- Direct block detection cannot establish exact item quantity by itself.
- Gemstone value and profit are unavailable.
- The expanded ordinary/Pure Ore, Tungsten, and Umber matrix is automated-tested but still requires controlled Hypixel runtime validation.
- Diagnostics contain evidence from one recording; the absence of an event does not prove every runtime path.
- Rot Client is not affiliated with or approved by Hypixel and does not guarantee complete accuracy.

## 11. Transient engine and canonical Current Session

`RotClientCurrentSession` is always present and is the only durable generic
session ledger. Collection runs while it is ACTIVE. The
`/rotclient session stop` command pauses collection and closes open target/area
segments; `/rotclient session start` resumes collection and opens new segment
boundaries. The transient `MiningSessionEngine` owns classification,
correlation, deduplication, target parity, and diagnostic snapshots, but it
never owns a second persisted copy of Current Session quantities.

`/rotclient record start` opens the local diagnostic recorder and ensures
collection is aligned with an ACTIVE Current Session. It does not create a
separate session or transfer lifecycle ownership to diagnostics.

Current category boundaries:

- `TARGET_MINED` is mirrored transiently from already-accepted authoritative
  target events for parity and diagnostics. It is not persisted as another
  target ledger.
- `OTHER_MINED` classifies off-target resources correlated through direct-break
  context and Sack/inventory evidence. A confidently accepted observation can
  pass through one terminal mutation into canonical Current Session exactly
  once; HUD OTHERS and Analytics read that persisted row.
- `CHEST_LOOT` and `CURRENCY` feed the Powder Chest Tracker as a read-only
  Current Session projection (opened chests, loot rows, powder currencies)
  with an independent HUD. Chest chat/Sack deduplication and unmatched-event
  expiry still need controlled Hypixel runtime confirmation.
- `MOB` has bounded live Current Session ingest: player-caused melee and
  projectile damage, Combat/Slayer/Dungeon Sack, action-bar `+N` lines,
  rare-drop chat, and `+N` coins inside the kill window. Displayed Magic Find
  is session context only. Remote-ability rewards remain unimplemented.

Hard Stone is catalogued for `OTHER_MINED` and has controlled shadow-correlation
evidence, but it is not a selectable live target. Persistent Current Session
Mining Sack ingress has controlled runtime evidence for non-target Mithril and
Titanium. Tungsten and Umber are selectable, isolated live targets whose new
runtime paths still require controlled Hypixel confirmation.

Session Analytics builds a privacy-safe view model for the dashboard,
`/rotclient session status`, and clipboard copy. Canonical generic quantities
and their persisted price status come from Current Session; transient target
parity remains engine-owned. Bazaar valuation uses instant sell (gross) for
supported item rows only. Currency quantities are shown separately and never
contribute to resolved item value.

## 12. Session History 2.0 (canonical lifecycle freeze)

`Start New` first freezes the canonical `RotClientCurrentSession`, writes that immutable freeze to Session History, and only then creates the next empty Current Session. If the archive write fails, the existing Current Session identity, lifecycle, and item quantities remain intact. If publishing the next Current Session fails after archive creation, Rot Client restores the in-memory original and attempts to remove the new archive; a failed compensating removal is surfaced. A paused Current Session may also be saved explicitly through the dashboard or `/rotclient session save`; active Current Sessions are rejected.

A schema v2 freeze stores the display number, start/stop boundary, active and paused durations, closed target and parent-area segments, canonical item rows, and each row's source, mining classification, area, known/unknown state, price status, and frozen gross valuation. The price-book observation timestamp is retained without storing the product-price map. Stale prices are marked `STALE` at the freeze boundary without mutating the live Current Session. Currency is always archived as price-unsupported and excluded from item value.

The dedicated local file is `rotclient-session-history.json` under the Fabric config directory. It is completely separate from `TrackerStore` / `rotclient.json`. Schema v1 records remain readable and are preserved when a v2 record upgrades the document. At most 20 newest sessions are retained. Writes use a same-directory temporary file that is flushed and forced before `ATOMIC_MOVE` replacement, with same-directory replace as fallback. Malformed JSON, unsupported schema versions, duplicate IDs or fingerprints, mismatched content fingerprints, invalid lifecycle durations, invalid segments or rows, and unsafe display text fail closed without crashing startup. Corrupt files may be quarantined to a fixed privacy-safe filename.

History does not store live ledger events, raw chat, coordinates, player names, UUIDs, server addresses, API payloads, diagnostic paths, Current Session IDs, event/correlation IDs, or full product-price maps. HUD and Analytics remain read-only live projections; an immutable history freeze is not a second live ledger.

On first launch after upgrading from MiningTracker, Rot Client byte-copies `miningtracker-session-history.json` to `rotclient-session-history.json` when the new file does not exist. The legacy history file is not deleted or modified.

Automatic final archival occurs only through confirmed `Start New`. Manual saving requires PAUSED state. Exact duplicate freezes are idempotent. Clearing history requires a short-lived confirmation token; mismatched or expired tokens do not clear history and consume the pending authorization. Opening or copying a historical record is read-only and does not replace Current Session or Analytics. History operations do not trigger Bazaar requests or mutate live tracker totals.

History and Current Session are separate files, so Start New is not an atomic
two-file transaction. Reported write failures use the compensating path above,
but a process crash between file replacements can leave an archive/current
mismatch that requires recovery review.

The v2 row schema retains `MOB`, `CHEST`, and `CURRENCY` sources without
reclassification. MOB has bounded live ingest and Powder Chest Tracker projects
CHEST/CURRENCY rows. Representative Powder Chest and MOB paths have controlled
runtime evidence; broader reward formats, remote-ability attribution, and the
latest MOB noise-filter correction remain open.

Proposed events require stable item identifiers, explicit source classification, quantity, timestamp, price-resolution state, and deduplication data. The design must reject manual transfers and purchases, deduplicate chest chat against delivery, expire unmatched signals, keep currency separate from item value, and exclude non-target items from target averages.

Further source categories must remain diagnostic/shadow-first until unit and
integration tests plus controlled runtime positive and negative evidence
demonstrate safe source attribution and family isolation. Existing accepted
mining OTHERS may update canonical Current Session and HUD OTHERS exactly once;
no new source may change persistence, HUD totals, or authoritative target
accounting before its own evidence is approved.
