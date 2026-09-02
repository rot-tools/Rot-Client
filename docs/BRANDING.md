# Rot Client Branding and Visual Identity

This document defines the Rot Client product identity and the canonical UI palette used by `RotClientTheme`, the dashboard, and the HUD.

## Product identity

| Item | Value |
| --- | --- |
| Display name | Rot Client |
| Original author identity | OgRudolf |
| Mod ID | `rotclient` |
| Java package | `fi.rotclient` |
| Playable artifact | `RotClient-2.0.1+mc26.2.jar` |
| Canonical command | `/rot` |
| Product dashboard header | `ROT CLIENT` |
| Functional mining module label | `MINING TRACKER` (feature module, not product name) |
| Icon path | `src/main/resources/assets/rotclient/icon.png` |
| Homepage / sources | https://github.com/rot-tools/Rot-Client |
| Issues | https://github.com/rot-tools/Rot-Client/issues |

User-facing strings, documentation, and repository metadata use **Rot Client**. Code artifacts use `rotclient`, `fi.rotclient`, and `RotClient` per Java and Fabric conventions.

## Canonical palette (ARGB)

These values match `RotClientTheme`. The interface uses a near-black foundation, a red interaction accent, violet secondary controls, and high-contrast white text.

| Token | ARGB | Role |
| --- | --- | --- |
| `BACKDROP` | `0xF208080A` | Near-black backdrop |
| `SURFACE` | `0xF518141C` | Primary surface / panel fill |
| `SURFACE_ALT` | `0xF5221A28` | Alternate card / surface fill |
| `FIELD` | `0xF50C0A10` | Input / field background |
| `FIELD_ACTIVE` | `0xF5322040` | Focused input / field background |
| `BORDER` | `0xFF4A3858` | Standard violet-grey border |
| `BORDER_BRIGHT` | `0xFFE11D48` | Selected / focused red accent |
| `VIOLET` | `0xFFA855F7` | HUD controls and selected chips |
| `TEXT` | `0xFFF8F5FF` | Primary near-white text |
| `TEXT_DIM` | `0xFFD4CBE0` | Secondary light text |
| `TEXT_MUTED` | `0xFF9B90B0` | Muted violet-grey text |
| `SUCCESS` | `0xFFC4B5FD` | Semantic success (violet, not red) |
| `WARNING` | `0xFFF4C06A` | Semantic warning |
| `ERROR` | `0xFFFF4D6D` | Semantic error |

Derived HUD and dashboard tokens in `RotClientTheme` (for example `HUD_BACKGROUND`, `HUD_HEADER`, `HUD_ACCENT`, `DIVIDER`, `BUTTON`, `CHART_LINE`) are built from these base values. New UI surfaces should reuse existing tokens rather than introducing ad hoc colors.

## Brand RGB references

For design mockups, documentation, and external assets outside Minecraft's ARGB draw path:

| Hex | Usage |
| --- | --- |
| `#08080A` | Deepest backdrop |
| `#18141C` | Primary surface |
| `#221A28` | Alternate surface / HUD header |
| `#E11D48` | Focus, selection, and module-on accent |
| `#A855F7` | HUD controls and selected chips |
| `#F8F5FF` | Primary text |
| `#E6DDF0` | Secondary text |
| `#C8BFD8` | Muted text |

## Icon usage

- Source file: `src/main/resources/assets/rotclient/icon.png`
- Use for mod metadata, repository front page, and in-game mod list entry.
- Do not stretch non-square crops; preserve the square asset.
- Do not recolor the icon to non-brand hues for official surfaces.
- Rot Client keeps its own **distinct icon design**; the historical palette reference does not make another project a dependency or current co-owner of these assets.

## Dashboard

`MiningUiScreen` applies the palette through `RotClientTheme` helpers:

- `drawBackdrop` — full-screen near-black wash (`BACKDROP`)
- `drawPanel` — module cards on `SURFACE` with `BORDER` outline
- `drawInset` — search fields and inputs (`FIELD` / `FIELD_ACTIVE`)
- `drawHeader` — module title strips (`HUD_HEADER` family)
- Primary actions use `BUTTON` / `BUTTON_HOVER`; selected rows use `SELECTED_ROW`

Module toggles, Session Analytics and Session History panels, and the searchable tracker selector all sit on this stack. Accent emphasis (focused borders, active module indicators) uses `BORDER_BRIGHT`.

## HUD

`RotClientHud` renders family-specific metrics on translucent blue-slate cards:

- Card background: `HUD_BACKGROUND` / `HUD_PANEL` family
- Header strip: `HUD_HEADER`
- Title / accent line: `HUD_ACCENT` (`BORDER_BRIGHT`, teal by default)
- Primary labels: `TEXT`
- Secondary metrics: `TEXT_DIM` / `TEXT_MUTED`
- Rate graph line: `CHART_LINE`; grid: `CHART_GRID`; well: `CHART_WELL`

HUD panels are movable, scalable, and screen-clamped. Visual identity must remain readable at minimum scale.

## Semantic colors

Use semantic tokens only for status meaning, never as decorative brand fill:

| Token | When to use |
| --- | --- |
| `SUCCESS` | Confirmed OK state, positive parity, enabled confirmation |
| `WARNING` | Caution, work-in-progress labels, non-fatal attention |
| `ERROR` | Failures, rejected operations, parity mismatch |

Do not use `SUCCESS`, `WARNING`, or `ERROR` for generic buttons, borders, or backgrounds. Generic chrome stays in the slate / teal / neutral family.

## Contrast rules

- Primary content on `SURFACE` and `HUD_PANEL`: use `TEXT`.
- Supporting labels and secondary metrics: `TEXT_DIM` or `TEXT_MUTED` only when the primary label is also present.
- Interactive focus: raise emphasis with `BORDER_BRIGHT` or `FIELD_ACTIVE`, not by switching to semantic green/yellow.
- Dividers and grid lines: use `DIVIDER` or `CHART_GRID`—avoid full-opacity `BORDER` for interior separators.
- Translucent backdrop (`BACKDROP`, HUD card alphas) must keep `TEXT` readable over typical in-game scenes; prefer panel fills behind dense text blocks.

## Historical palette provenance

Earlier project documentation associated a near-black maroon and red palette
with a Rot-family reference named RotProxy. Rot Client now uses an independent
slate and teal interface palette. This document does not assert current
RotProxy ownership, maintainer identity, repository availability, or continued
token parity. RotProxy is not a code or runtime dependency of Rot Client.

Rot Client's icon, mod ID, package, implementation, and product naming remain
independent. Future palette changes are governed by this document and
`RotClientTheme`, not by an external repository.

## Implementation reference

Primary source: `src/client/java/fi/rotclient/RotClientTheme.java`

UI consumers:

- `MiningUiScreen` — dashboard modules, selector, settings
- `RotClientHud` — in-world overlay cards and graphs
- `RotClientScreen` — shared screen chrome where applicable
