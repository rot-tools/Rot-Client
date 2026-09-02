# SkyBlock Data — Conflict Report

Source of truth: `src/main/resources/assets/rotclient/data/conflicts.v1.json` after the 2026-08-08 refresh.

Conflicts are **surfaced, not silently overwritten**.

| Stable ID | Field | Official | Other | Resolution |
| --- | --- | --- | --- | --- |
| `TITANIUM` | hypixelItemId | `TITANIUM_ORE` | `TITANIUM` | `KEEP_DOMAIN_STABLE_ID` — routing key stays `TITANIUM`; official/Bazaar id `TITANIUM_ORE` |
| `LAPIS_LAZULI` | hypixelItemId | `INK_SACK:4` | `LAPIS_LAZULI` | `KEEP_DOMAIN_STABLE_ID` — routing key stays `LAPIS_LAZULI`; official/Bazaar id `INK_SACK:4` |
| `ENCHANTED_REDSTONE` | displayName | Enchanted Redstone Dust | Enchanted Redstone | `KEEP_DOMAIN_DISPLAY_ADD_OFFICIAL_ALIAS` |
| `MITHRIL_POWDER` | hypixelItemId | _(none)_ | `MITHRIL_POWDER` | `UNVERIFIED_OFFICIAL_ID` — currency marker; official powder uses tiered item ids |
| `GEMSTONE_POWDER` | hypixelItemId | _(none)_ | `GEMSTONE_POWDER` | `UNVERIFIED_OFFICIAL_ID` — currency marker |
| `GLACITE_POWDER` | hypixelItemId | _(none)_ | `GLACITE_POWDER` | `UNVERIFIED_OFFICIAL_ID` — currency marker |

## Notes

- Live accounting / `TrackedMaterial` Titanium routing was already dual-keyed (`TITANIUM` + `TITANIUM_ORE` bazaar). The data foundation makes that conflict explicit and auditable.
- Powder currency keys are intentionally domain-owned until a reviewed mapping to official tiered powder item ids is approved.
- Community enrichment that disagrees with an official Hypixel fact is recorded as `KEEP_OFFICIAL` and does not replace primary authority.
