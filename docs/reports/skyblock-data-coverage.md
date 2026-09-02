# SkyBlock Data — Coverage Report

**Generated from:** bundled `assets/rotclient/data/manifest.v1.json` after `refreshSkyBlockData` on 2026-08-08.

## Counts

| Metric | Value |
| --- | ---: |
| Canonical items | 89 |
| Domain rules | 89 |
| Surfaced conflicts | 6 |
| Bazaar product IDs indexed (presence only) | 2123 |
| Official Hypixel items observed at refresh | 5646 |

## Catalog scope

Mining-focused Rot Client domain set, including:

- Core ores/materials and enchanted forms (Gold, Diamond, Mithril, Titanium, Hard Stone, Cobblestone, Coal, Iron, Lapis, Redstone, Tungsten, Umber, Glacite)
- Twelve gemstone colors × five tiers
- Powder currency markers (Mithril / Gemstone / Glacite)

## Authority mix (canonical items)

Reconciled rows prefer official Hypixel IDs when matched. Domain stable IDs are retained for routing even when official IDs differ.

## Observation policy

Catalog / registry enrichment is **not** an allow-list. Unknown stable IDs with credible gameplay provenance remain valid observations (`SkyBlockItemIdentityResolver` preserves `catalogMatch=false`).

## Valuation policy

Static Bazaar index records product presence only. Live valuation remains Bazaar instant sell (gross) via existing sell-side policy. No buy-side substitution.
