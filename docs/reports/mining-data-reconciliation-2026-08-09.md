# Mining Data Reconciliation

Date: 2026-08-09

Branch baseline: `ui-redesign` at
`abcd4c4f50f71ec80cc66187d03cdd9e2a7ba67b` with an intentionally dirty,
uncommitted integration tree.

## Verdict

The reviewed mining layer represents all 25 collection families returned by
the current public Hypixel Collections endpoint plus Titanium and Starfall.
The current official-versus-Rot audit reports no ID or Bazaar conflicts and no
official-only Mining Collection families. It deliberately reports 26 of 27
rows as unresolved because missing mechanics, physical evidence, Sack aliases,
or canonical item coverage remain unknown rather than guessed.

## Public inputs

The audit fetched only public endpoints and sent no player or runtime data:

- [Hypixel Items API](https://api.hypixel.net/v2/resources/skyblock/items)
- [Hypixel Collections API](https://api.hypixel.net/v2/resources/skyblock/collections)
- [Hypixel Skills API](https://api.hypixel.net/v2/resources/skyblock/skills)
- [Hypixel Bazaar API](https://api.hypixel.net/v2/skyblock/bazaar)

Mechanics and locations were reviewed against:

- [Hypixel Wiki: Blocks](https://wiki.hypixel.net/Blocks)
- [Hypixel Wiki: Mining Fortune](https://wiki.hypixel.net/Mining_Fortune)
- [Hypixel Wiki: Pristine](https://wiki.hypixel.net/Pristine)
- [Hypixel Wiki: Deep Caverns](https://wiki.hypixel.net/Deep_Caverns)

Public community references remained comparison sources for gaps and design
patterns. No third-party code or dataset was copied.

## Snapshot audit

| Dataset | Bytes | SHA-256 |
| --- | ---: | --- |
| Items | 5,059,768 | `BA315AE3DC8004328B0F028DC2688893C2E9F1170D5480541DB46E7F89F041A6` |
| Collections | 82,772 | `05B7D2E08E896B660EFBCE92DC54CBE82FA520CCBCB9CA59639A1B848539BC22` |
| Skills | 117,118 | `2F4B2F3EED14756BDB4190CBC5E94FFF7295DDD0C66EC1B55928203EF5B70DA5` |
| Bazaar | 3,597,104 | `D7C0C4E1FC0C341297FB78C42712706AA56F6DA71DBB5DAD94433E460A157BE1` |

The newest source timestamp reported by the generated reconciliation was
`2026-08-09T14:24:21.907Z` (Bazaar).

## Reconciliation result

| Measure | Result |
| --- | ---: |
| Official Mining Collections | 25 |
| Rot Mining Collection rows | 25 |
| Total reviewed resource rows | 27 |
| Canonical item coverage | 11 / 27 |
| Conflicts | 0 |
| Official-only Mining Collections | 0 |
| Unresolved rows | 26 |

Statuses are intentionally non-exclusive. A row can match official identity
while remaining unresolved for a missing base yield or verified Sack alias.
The separate canonical item refresh still emits six review rows: intentional
Titanium and Lapis stable-ID mappings, one retained display-name alias, and
three Rot-only powder markers without official item IDs. Those remain visible
in `conflicts.v1.json`; they are not mining-mechanics ID conflicts.
The complete generated matrix remains a build artifact at
`build/reports/skyblock-data/mining-reconciliation.md` and is not adopted into
the canonical bundle.

## Confirmed mechanics additions

| Resource | Breaking Power | Block Strength | Fortune |
| --- | ---: | ---: | --- |
| Mithril (prismarine variant) | 4 | 800 | Dwarven Metal |
| Titanium | 5 | 2,000 | Dwarven Metal |
| Tungsten | 9 | 5,600 | Dwarven Metal |
| Umber | 9 | 5,600 | Dwarven Metal |
| Glacite | 9 | 6,000 | Dwarven Metal |

Mithril has multiple physical variants with different strengths, so the row
states which variant its scalar value represents. Unknown base yields and
family-specific gemstone mechanics remain absent.

## Context and locations

- Every mechanics row marked `requiresAreaContext` now uses the shared
  `MiningBlockEvidencePolicy`; the registry remains enrichment, not an
  observation allow-list.
- Hard Stone, Mithril, and Titanium are the currently active material callers.
- Tungsten, Umber, Glacite, and generic gemstone context remain metadata for
  future detector integrations.
- Deep Caverns and the exact labels Gunpowder Mines, Lapis Quarry, Pigmen's
  Den, Slimehill, Diamond Reserve, and Obsidian Sanctuary are canonical.
- Partial names and narrative sentences remain unknown.

Block evidence opens a correlation context only. It never creates item
quantity. Sack, action-bar, inventory, or another reviewed quantity signal
remains authoritative.

## Update and performance safety

- Four public requests are sequential, bounded to 16 MiB each, and have no
  retry loop.
- Valid cache files survive a network failure; invalid or missing offline data
  fails closed.
- Cache, review output, and report files use temporary files and atomic moves
  where supported.
- Refresh output is review-only by default; source adoption requires
  `--adopt`.
- The nearby 15 x 11 x 15 scan remains one immutable 2,475-position frame per
  tick, shared across detectors. Plans are bounded to 64 detectors.
- The profiler remains diagnostic-only.

## Remaining unknowns

- Family-specific gemstone physical/context tables and scalar mechanics.
- Verified Sack aliases for most collection families.
- Authoritative base yields and strength variants for most resources.
- Exact live formatting for every added Deep Caverns location.
- Hardware/runtime timing for the shared scan.
- Live confirmation that every newly gated detector receives the expected
  parent area during transitions and temporary scoreboard gaps.
- Controlled runtime revalidation of the integrated Resume valuation crash fix.

Diana was not implemented. No data was staged, committed, pushed, deployed, or
uploaded by this phase.
