# Mining Data & Mechanics Foundation 2.0

Date: 2026-08-09

Branch: `ui-redesign`

Scope: mining resource mechanics, context safety, Fortune domains, fallback
scan cost, Sack delivery identity, area authority, and tests.

## Outcome

Rot Client now has a versioned, handwritten mining-mechanics layer beside the
generated item and Bazaar snapshots. It covers all 25 current official Mining
Collection identifiers and two special mining resources, Titanium and
Starfall. The existing generated 89-item snapshot remains unchanged.

| Measure | Before | After |
| --- | ---: | ---: |
| Official Mining Collection families represented | 13 | 25 |
| Special mining-resource definitions | 0 | 2 |
| Fortune subtypes in runtime config/detection | 2 | 4 |
| Fallback world reads with six detectors | 14,850/tick | 2,475/tick |

## Authority and research

Primary facts were checked on 2026-08-09 against:

- [Hypixel SkyBlock Collections API](https://api.hypixel.net/v2/resources/skyblock/collections)
- [Hypixel SkyBlock Items API](https://api.hypixel.net/v2/resources/skyblock/items)
- [Hypixel Bazaar API](https://api.hypixel.net/v2/skyblock/bazaar)
- [Hypixel Wiki: Hard Stone](https://wiki.hypixel.net/Hard_Stone)
- [Hypixel Wiki: Mining Fortune](https://wiki.hypixel.net/Mining_Fortune)
- [Hypixel Wiki: Breaking Power](https://wiki.hypixel.net/Breaking_Power)
- [Hypixel Wiki: Pristine](https://wiki.hypixel.net/Pristine)

The Collections endpoint returned exactly 25 Mining keys. Items and Bazaar
confirmed the reviewed identity/product mappings. Current wiki mechanics
confirm that base Mining Fortune combines with one applicable resource subtype
and that Hard Stone uses Block Fortune in its documented mining contexts.

Public community references were comparison points for data organization and
detector design. No source code or dataset was copied:

- Third-party community repositories (research reference only)

Controlled Rot Client runtime evidence remains authoritative for live behavior:
real Mithril Sack ingestion, real `Last 9s`, restart persistence, pause/resume,
persistent Current Session projection, and target-family exclusion are already
observed. This pass awaits a new controlled runtime check.

## Identity model

`mining-resources.v2.json` separates physical block candidate tokens, Rot's
stable resource ID, official collection/item/drop IDs, and Bazaar valuation ID.
It also supports aliases, enchanted relations, areas, Fortune category,
nullable Breaking Power/strength/yield, gemstone metadata, confidence,
evidence state, provenance, and notes. Unknown values remain absent. The file
is handwritten, separate from generated snapshots, and never an allow-list.

Examples:

- Rot `TITANIUM` maps to official/Bazaar `TITANIUM_ORE`.
- Rot `LAPIS_LAZULI` preserves official legacy `INK_SACK:4`.
- physical `IRON_ORE` is separate from dropped/Bazaar `IRON_INGOT`.
- physical `STONE` is insufficient to identify `HARD_STONE`.

## Runtime hardening

### Hard Stone

`MiningBlockEvidencePolicy` rejects Hard Stone block evidence when the parent
area is unknown or unsupported. It accepts Crystal Hollows, Glacite Tunnels,
and Glacite Mineshaft. Packet and snapshot paths share the gate. Quantity still
comes from inventory/Sack deltas; breaks only provide correlation context.

### Detector cost

The old fallback loop scanned 15 x 11 x 15 blocks separately per detector.
Six detectors meant 14,850 world reads each client tick. A shared immutable
`MiningBreakScanFrame` captures the same 2,475 positions once. Per-material
matching and packet callbacks remain independent. `BREAK_SCAN_PROFILE` reports
aggregate timing every 100 frames only while diagnostics are active and records
no coordinates.

### Sack deduplication

The old 2.5-second fingerprint could not distinguish legitimate equal
resource/quantity/source messages. `SackComponentIngress` now assigns a
short-lived component occurrence identity. The same component retains its ID;
separate equal components receive separate IDs. This supplements rather than
replaces context, ledger-event, and temporal dedupe.

### Area authority

Area strings now match normalized standalone scoreboard location names.
Narrative fragments cannot authorize context-gated resource evidence.

## Test matrix

| Area | Automated evidence |
| --- | --- |
| Official collection coverage | Exact set equality for all 25 API keys |
| Identity boundaries | Iron, Titanium, Lapis, and nullable Gravel assertions |
| Schema safety | invalid yield range and missing provenance rejection |
| Hard Stone valid context | Crystal Hollows, Glacite Tunnels, Mineshaft accepted |
| Hard Stone invalid context | Dwarven Mines and Unknown rejected |
| Catalog policy | unseen resource remains observable; no allow-list behavior |
| Fortune | Mining plus Block/Ore/Dwarven Metal/Gemstone parsing |
| Scan geometry | 2,475 positions; 14,850 to 2,475 at six detectors |
| Sack duplicate | same component identity remains stable and credits once |
| Sack legitimate repetition | separate equal components use separate contexts |
| Target/accounting isolation | existing canonical Current Session suites retained |
| Area false positives | narrative/fuzzy fragments rejected |

Final local verification: `gradlew clean test build` passed with 1,095 tests,
0 failures, 0 errors, and 0 skipped. `git diff --check` passed; Git only emitted
the repository's existing LF-to-CRLF conversion notices.

## Remaining risks and runtime checklist

- Verify Hard Stone in a supported area and ordinary Stone outside it.
- Capture `BREAK_SCAN_PROFILE` during a representative Current Session; the
  present performance proof is structural rather than a hardware benchmark.
- Verify separate equal Sack occurrences with real Hypixel timing if feasible.
- Context requirements for Mithril, Titanium, Tungsten, Umber, Glacite, and
  gemstones remain metadata until each detector path is hardened deliberately.
- Several collection families retain `UNKNOWN` Fortune or null mechanics rather
  than an unverified value.
- No Diana/Mob Loot feature was implemented.
