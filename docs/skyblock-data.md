# SkyBlock Data Foundation

Operational companion to the binding **SkyBlock Data Authority and Provenance** section in [`ARCHITECTURE.md`](ARCHITECTURE.md).

## What ships

Bundled under `src/main/resources/assets/rotclient/data/`:

| Artifact | Kind | Purpose |
| --- | --- | --- |
| `manifest.v1.json` | Generated + reviewed | Schema versions, refresh timestamps, source digests. |
| `domain-rules.v1.json` | Handwritten Rot Client | Stable IDs, aliases, family/source hints, explicit overrides. |
| `canonical-items.v1.json` | Generated canonical dataset | Normalized items runtime/domain code may reason against. |
| `bazaar-product-index.v1.json` | Generated static index | Product ID presence at last refresh (not prices). |
| `conflicts.v1.json` | Generated audit | Visible official vs domain vs enrichment disagreements. |
| `provenance.v1.json` | Generated audit | Source/license/provenance summary for the refresh. |
| `mining-resources.v2.json` | Handwritten Rot Client mechanics | Resource identities, physical evidence, official IDs, Fortune domain, optional mechanics, area requirements, and provenance. |

Dynamic Bazaar **prices** remain outside this package and are fetched at runtime by `BazaarPriceService` using sell-side instant-sell policy only.

## Refresh tooling

Development-time refresh (network allowed; never uploads user data):

```text
.\gradlew.bat refreshSkyBlockData --console=plain
```

The task:

1. Fetches the public Hypixel items, collections, skills, and Bazaar endpoints sequentially (no API key, no player payloads, and no retries).
2. Validates and atomically caches bounded raw snapshots under `build/skyblock-data-cache/` (not shipped).
3. Loads handwritten `domain-rules.v1.json`.
4. Optionally merges a local enrichment overlay if provided.
5. Reconciles into the canonical dataset and conflict/provenance reports.
6. Writes review artifacts under `build/skyblock-data-review/` by default.

Canonical adoption is an explicit maintainer action:

```text
.\gradlew.bat refreshSkyBlockData --args="--adopt" --console=plain
```

Use `--offline` to require already-valid cached copies. When an online request
fails, the refresh retains a valid prior cache instead of replacing it with a
partial or invalid response. Each cache and generated JSON file is replaced
through a temporary file and atomic move where the filesystem supports it.

Runtime never calls community repositories. Offline play keeps tracking; missing prices degrade valuation only.

`refreshSkyBlockData` does not rewrite `mining-resources.v2.json`. Upstream item
and Bazaar snapshots are generated facts; gameplay mechanics and identity
boundaries require review and therefore stay in a separate handwritten file.

## Mining reconciliation audit

Run the non-adopting developer audit with:

```text
.\gradlew.bat auditSkyBlockMiningData
.\gradlew.bat auditSkyBlockMiningData --args="--offline"
```

It acquires all four public snapshots, then compares the handwritten mining
layer against official items, Mining Collections, Bazaar products, and the
bundled canonical dataset. The Skills snapshot is retained as a validated
versioned input for future skill reconciliation; this report does not infer
block mechanics from it. The generated report is written to
`build/reports/skyblock-data/mining-reconciliation.md`; it never mutates the
canonical bundle. Rows may carry `OFFICIAL_MATCH`, `ALIAS_MATCH`,
`INTENTIONAL_MAPPING`, `CONFLICT`, `OFFICIAL_ONLY`, `ROT_ONLY`,
`COMMUNITY_REFERENCE_ONLY`, `LIVE_ONLY`, and `UNRESOLVED`. Multiple statuses
may apply because identity agreement and missing mechanics are independent.

## Authority application

When reconciling a field:

1. Prefer an explicit current Hypixel value.
2. Keep Rot Client domain stable IDs and gameplay routing keys unless a deliberate, reviewed migration changes them.
3. Record official IDs / Bazaar product IDs beside domain keys when they differ (for example domain `TITANIUM` vs official `TITANIUM_ORE`).
4. Accept community enrichment only for gaps, with provenance.
5. Emit conflicts instead of silent overrides.

## Observation semantics

`SkyBlockContentRegistry`, `SkyBlockItemIdentityResolver`, and the canonical dataset enrich known identities. They do **not** gate observation. Unknown stable IDs with credible gameplay provenance remain valid.

## Mining mechanics schema

`SkyBlockMiningResourceRegistry` loads `mining-resources.v2.json`. The current
reviewed layer contains all 25 Mining Collection identifiers returned by the
official Collections endpoint plus Titanium and Starfall. This expands
mechanics-family coverage from the earlier 13 families without inflating the
generated 89-row item snapshot.

Each row separates Rot's stable resource ID, official collection/item/drop and
Bazaar IDs, physical block candidate tokens, aliases, enchanted relations,
areas, Fortune category, optional Breaking Power/strength/yield, gemstone
metadata, confidence, evidence state, provenance, and notes. An absent value is
unknown or not verified; it is never replaced with a guess.

A physical `STONE` token is not equivalent to `HARD_STONE`. The runtime
detector also requires Crystal Hollows, Glacite Tunnels, or Glacite Mineshaft.
The same policy now applies to every mechanics row marked
`requiresAreaContext`, including Mithril, Titanium, Tungsten, Umber, Glacite,
and the generic gemstone family. This metadata is not a promise that every
resource already has an enabled runtime detector; current quantity still comes
from an observed Sack, action-bar, inventory, or another reviewed quantity
signal.

## Related reports

- [`docs/reports/skyblock-data-license-provenance.md`](reports/skyblock-data-license-provenance.md)
- [`docs/reports/skyblock-data-coverage.md`](reports/skyblock-data-coverage.md)
- [`docs/reports/skyblock-data-conflicts.md`](reports/skyblock-data-conflicts.md)
- [`docs/reports/skyblock-data-jar-audit.md`](reports/skyblock-data-jar-audit.md)
- [`docs/reports/mining-data-mechanics-foundation-2026-08-09.md`](reports/mining-data-mechanics-foundation-2026-08-09.md)
- [`docs/reports/mining-data-reconciliation-2026-08-09.md`](reports/mining-data-reconciliation-2026-08-09.md)
