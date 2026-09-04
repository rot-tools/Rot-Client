# SkyBlock Data — License & Provenance Audit

**Date:** 2026-08-08
**Milestone:** online-data-foundation
**Policy:** [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md) — SkyBlock Data Authority and Provenance
**Ops:** [`docs/skyblock-data.md`](../skyblock-data.md)

## Decision summary

| Source | License / terms | Rot Client use | Redistribution decision |
| --- | --- | --- | --- |
| Hypixel Public API (`/v2/resources/skyblock/items`, `/v2/skyblock/bazaar`) | Public API; no key for these resources | Primary authority for official item IDs/metadata and Bazaar product identity; runtime sell-side prices for valuation only | Facts normalized into Rot Client–owned JSON. No Hypixel private data. No player payloads uploaded. |
| Third-party community repositories | Copyleft (LGPL-family) | Research / enrichment / reference only | **Cite-only.** Do **not** copy source trees or parsers into Rot Client. |
| Rot Client Research catalogs | Rot Client research extracts (facts JSON) | Optional enrichment overlay input for refresh tooling | Facts already extracted with provenance; no wholesale third-party tree. |
| Rot Client domain rules | MIT (this repository) | Handwritten stable IDs, aliases, family metadata, explicit dual-key overrides | Shipped as `domain-rules.v1.json`. |

## Hard stops honored

- No wholesale third-party source-tree copy.
- No LGPL parsers vendored into the MIT mod JAR.
- No redistribution decision that would force a license change for Rot Client.
- Public metadata refresh does not upload user/session/inventory/chat data.
- Bundled non-Rot works (Adobe Source Sans OFL, vanilla Minecraft pack paths)
  are listed in `THIRD_PARTY.md` and `NOTICE`.

## Provenance requirements met

- Every non-trivial enrichment/override carries provenance in the canonical item rows and `provenance.v1.json`.
- Conflicts remain visible in `conflicts.v1.json` (for example domain `TITANIUM` vs official `TITANIUM_ORE`).
- Generated upstream metadata and handwritten domain rules are separate files.

## Runtime networking

- Gameplay tracking remains functional offline.
- Dynamic market prices stay in `BazaarPriceService` (instant sell / gross; no buy-side substitution).
- Static metadata is loaded from bundled local snapshots only.
