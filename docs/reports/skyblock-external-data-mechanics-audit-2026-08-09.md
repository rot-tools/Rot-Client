# SkyBlock External Data and Mining Mechanics Audit

Date: 2026-08-09

Scope: Rot Client canonical SkyBlock data, mining mechanics, area detection, pricing semantics, and future mechanics readiness.
Repository baseline: branch `ui-redesign`, HEAD `abcd4c4f50f71ec80cc66187d03cdd9e2a7ba67b`.

## 1. Executive verdict

**EXTERNAL DATA / MECHANICS FOUNDATION NEEDS CORRECTION.**

The bundled item and Bazaar snapshots are current enough for identity reconciliation, the 12 gemstone families and their five tiers are complete, tier conversion ratios are correct, and the Bazaar instant-sell side is interpreted correctly. The foundation is not yet factually complete or calculation-ready because:

- only 13 of the 25 official Mining collection families are represented;
- all gemstone rows carry the same coarse and incorrect area set;
- `HARD_STONE` and `COBBLESTONE` are modeled as Ore Fortune instead of Block Fortune;
- Hard Stone, Tungsten, and several other retextured blocks cannot be identified safely from vanilla `BlockState` alone;
- `COAL_ORE` and `IRON_ORE` mix physical block identity with dropped-item/Bazaar identity;
- Breaking Power, block strength, yield, fortune subtype, and special mechanics have no canonical versioned schema;
- source provenance covers only the item endpoint even though the dataset also uses Bazaar data and researched mechanics;
- area detection contained one confirmed hierarchy error and retains several fragile substring matches.

The external-data pass made one isolated production correction: the Mines of Divan parent-area fix. A subsequent continuation from the same repository state added separately tested Current Session identity and production Sack-ingress hardening without changing the researched data verdict.

## 2. Sources checked

### Official Hypixel

- [SkyBlock API documentation](https://api.hypixel.net/): endpoint schema and Bazaar field semantics.
- `GET /v2/resources/skyblock/items`: 5,646 items; `lastUpdated=1786046142121`.
- `GET /v2/skyblock/bazaar`: 2,123 products; `lastUpdated=1786224793185`.
- `GET /v2/resources/skyblock/collections`: 25 Mining collection families; `lastUpdated=1786046849531`.
- `GET /v2/resources/skyblock/skills`: inspected for public static-data completeness; no mining block mechanics are exposed there.
- [Official Wiki closure announcement](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/): the former official wiki closed in July 2026. Its cached pages are historical Tier 1 evidence, not a maintained current source.

The downloaded public API responses were kept outside the repository. SHA-256:

| Snapshot | SHA-256 |
| --- | --- |
| Items | `BA315AE3DC8004328B0F028DC2688893C2E9F1170D5480541DB46E7F89F041A6` |
| Bazaar | `B1A89147C4241CEEF062D5892F05B1C6C59D033EE94EE982FF803A85F5317AA4` |
| Collections | `05B7D2E08E896B660EFBCE92DC54CBE82FA520CCBCB9CA59639A1B848539BC22` |
| Skills | `2F4B2F3EED14756BDB4190CBC5E94FFF7295DDD0C66EC1B55928203EF5B70DA5` |

### Current community wiki

Used as researched secondary evidence after the official wiki closure:

- [Breaking Power](https://hypixelskyblock.minecraft.wiki/w/Breaking_Power)
- [Mining Speed](https://hypixelskyblock.minecraft.wiki/w/Mining_Speed)
- [Mining Fortune](https://hypixelskyblock.minecraft.wiki/w/Mining_Fortune)
- [Pristine](https://hypixelskyblock.minecraft.wiki/w/Pristine)
- [Mining Spread](https://hypixelskyblock.minecraft.wiki/w/Mining_Spread)
- [Gemstone Spread](https://hypixelskyblock.minecraft.wiki/w/Gemstone_Spread)
- [Gemstone](https://hypixelskyblock.minecraft.wiki/w/Gemstone)
- [Mithril](https://hypixelskyblock.minecraft.wiki/w/Mithril)
- [Titanium](https://hypixelskyblock.minecraft.wiki/w/Titanium)
- [Hard Stone](https://hypixelskyblock.minecraft.wiki/w/Hard_Stone)
- [Tungsten](https://hypixelskyblock.minecraft.wiki/w/Tungsten)
- [Umber](https://hypixelskyblock.minecraft.wiki/w/Umber)
- [Glacite](https://hypixelskyblock.minecraft.wiki/w/Glacite)
- [Mines of Divan](https://hypixelskyblock.minecraft.wiki/w/Mines_of_Divan)
- [Starfall](https://hypixelskyblock.minecraft.wiki/w/Starfall)

Facts were summarized; wiki prose was not copied. Current community wiki content is CC BY-NC-SA 3.0.

### Community code and data

- Third-party community repositories, inspected at pinned revisions under copyleft (LGPL-family) and permissive (MIT) licenses for research only.

Only design patterns and public identifiers were compared. No third-party source code was copied.

## 3. Authority and conflict rules

Recommended authority order:

1. `OFFICIAL`: current Hypixel API identity, product presence, NPC price, collection membership, and official patch notes.
2. `RESEARCHED`: current community wiki mechanics corroborated by another independent source where practical.
3. `LIVE_OBSERVED`: existing sanitized Rot Client observations, only when already documented; never private runtime files.
4. `VERIFIED`: agreement between authoritative sources and a Rot invariant or focused test.
5. `UNCERTAIN`: plausible but not sufficiently supported.
6. `CONFLICT`: sources or model layers disagree materially.

Per-field reconciliation should use `MATCH`, `ALIAS`, `CONFLICT`, `OFFICIAL_ONLY`, `COMMUNITY_ONLY`, `ROTCLIENT_ONLY`, `LIVE_ONLY`, or `UNRESOLVED`. A stable Rot routing ID may differ from an official item ID, but both must be stored explicitly. An alias must never collapse physical block identity into dropped-item identity.

## 4. Official item reconciliation

### Current Rot rows

- 89 canonical rows.
- 86 rows resolve to an existing official item and an existing Bazaar product.
- 3 powder currency markers have neither official item IDs nor Bazaar products.
- 60 of the 89 rows are gemstones: 12 families times 5 tiers.
- 26 rows are raw/enchanted mining material forms.

### Official Mining collection baseline

The current collection endpoint exposes 25 mining families. Rot represents 13:

`COAL`, `COBBLESTONE`, `DIAMOND`, `GEMSTONE_COLLECTION`, `GLACITE`, `GOLD_INGOT`, `HARD_STONE`, `IRON_INGOT`, `INK_SACK:4`, `MITHRIL_ORE`, `REDSTONE`, `TUNGSTEN`, and `UMBER`.

Twelve official collection families are absent:

`EMERALD`, `ENDER_STONE`, `GLOWSTONE_DUST`, `GRAVEL`, `ICE`, `MYCEL`, `NETHERRACK`, `OBSIDIAN`, `QUARTZ`, `SAND`, `SAND:1`, and `SULPHUR_ORE`.

All twelve absent base products currently exist in Bazaar. Their enchanted and block forms should be researched before adding complete family relations.

### Exact ID findings

| Rot stable ID | Official item ID | Bazaar product | Finding |
| --- | --- | --- | --- |
| `TITANIUM` | `TITANIUM_ORE` | `TITANIUM_ORE` | `ALIAS`; the existing dual-key approach is correct. |
| `LAPIS_LAZULI` | `INK_SACK:4` | `INK_SACK:4` | `ALIAS`; correct for Hypixel. A community item-index repository spells its internal key `INK_SACK-4`, which needs a source-specific adapter. |
| `COAL_ORE` | `COAL_ORE` | `COAL` | `CONFLICT`; block identity and dropped-item identity are combined in one row. |
| `IRON_ORE` | `IRON_ORE` | `IRON_INGOT` | `CONFLICT`; block identity and dropped-item identity are combined in one row. |
| `HARD_STONE` | `HARD_STONE` | `HARD_STONE` | `MATCH`; must not alias from bare `STONE`. |
| Powder markers | none | none | `ROTCLIENT_ONLY`; valid semantic currencies, not item endpoint rows. |

`STONE`, `HARD_STONE`, `COAL`, `COAL_ORE`, `IRON_INGOT`, and `IRON_ORE` all exist as distinct official items where listed. The model needs separate `physicalBlockId`, `droppedItemId`, `canonicalResourceId`, and `valuationProductId` fields.

## 5. Bazaar reconciliation

Hypixel's names are from the order-creator perspective. A player instant-selling fills existing buy orders exposed in `sell_summary`. Therefore Rot's policy of reading `sell_summary` and falling back to `quick_status.sellPrice` for **instant sell gross** is correct. A common community convention uses `sellSummary.min` for instant buy and `sellSummary.max` for instant sell.

Current counts:

| Metric | Count |
| --- | ---: |
| Official products inspected | 2,123 |
| Rot mappings | 86 |
| Valid current mappings | 86 |
| Non-Bazaar Rot markers | 3 |
| Missing/invalid mapped product IDs | 0 |

Important limitations:

- `BazaarPriceService` requires both raw and enchanted prices before adding a material pair. A valid raw price can therefore become unavailable when only its enchanted counterpart is absent.
- The 2,123-product index proves product presence only; it is not 2,123 modeled canonical items.
- NPC price should be a separately labeled fallback from official `npc_sell_price`, never blended silently with Bazaar.
- Auction/lowest-BIN valuation remains outside this scope.
- `STARFALL`, `REFINED_TUNGSTEN`, and `REFINED_UMBER` are current Bazaar products but are not current Rot canonical rows.

## 6. Mining resource matrix

| Resource | Official/Bazaar ID | BP | Base yield | Fortune subtype | Context-sensitive block identity | Rot status |
| --- | --- | ---: | --- | --- | --- | --- |
| Pure Gold | `GOLD_INGOT` | 3 | 5 | Ore | Gold block means Pure Gold only in known mining context | supported |
| Pure Diamond | `DIAMOND` | 3 | 5 in current Rot model; external yield needs a direct current citation | Ore | Diamond block context required | supported |
| Mithril | `MITHRIL_ORE` | 4 | 1/2/5 by vein block | Dwarven Metal | Cyan clay collides with Hard Stone by area | supported |
| Titanium | `TITANIUM_ORE` | 5 | 2 | Dwarven Metal | Polished diorite requires mining context | supported |
| Hard Stone | `HARD_STONE` | 4 | 1 | **Block** | Stone and cyan clay require area/context | supported but unsafe classifier/fortune model |
| Tungsten | `TUNGSTEN` | 9 | cobble/slab/stairs 1; clay 3 | Dwarven Metal | Collides with ordinary cobblestone/clay | canonical; runtime intentionally disabled |
| Cobblestone | `COBBLESTONE` | 1 in custom mining areas | 1 | **Block** | Collides with Tungsten representation | supported filler |
| Umber | `UMBER` | 9 | terracotta 1/brown 2/red sandstone 3 | Dwarven Metal | Retextured blocks require Glacite context | canonical only |
| Glacite | `GLACITE` | 9 | current base yield not encoded | Dwarven Metal | Packed ice requires Glacite context | canonical only |
| Gemstones | tier-specific IDs | 6-9 | 3-5 Rough | Gemstone | stained glass/pane requires area and type context | supported families |
| Starfall | `STARFALL` | n/a | special 2% nearby-Fallen-Star ore drop plus mob sources | not a base ore | event/proximity context | missing special drop |

No Breaking Power or base-yield schema exists in the canonical dataset, so “known” in this matrix means researched, not machine-enforced.

## 7. Gemstone matrix

All current families are present and each has Rough, Flawed, Fine, Flawless, and Perfect IDs with valid Bazaar mappings.

| Family | BP | Current mining locations | Rot area metadata |
| --- | ---: | --- | --- |
| Ruby | 6 | Crystal Hollows; Glacite Tunnels; Mineshafts | too coarse/wrong |
| Amber | 7 | Goblin Holdout; Glacite Tunnels; Mineshafts | too coarse/wrong |
| Amethyst | 7 | Jungle; Glacite Tunnels; Mineshafts | too coarse/wrong |
| Jade | 7 | Mithril Deposits; Glacite Tunnels; Mineshafts | too coarse/wrong |
| Sapphire | 7 | Precursor Remnants; Glacite Tunnels; Mineshafts | too coarse/wrong |
| Opal | 7 | Smoldering Tomb; Mineshafts | too coarse/wrong |
| Topaz | 8 | Magma Fields; Glacite Tunnels; Mineshafts | too coarse/wrong |
| Jasper | 9 | Fairy Grotto; Mineshafts | too coarse/wrong |
| Aquamarine | 9 | Glacite Tunnels; Mineshafts | too coarse/wrong |
| Onyx | 9 | Glacite Tunnels; Mineshafts | too coarse/wrong |
| Citrine | 9 | Glacite Tunnels; Mineshafts | too coarse/wrong |
| Peridot | 9 | Glacite Tunnels; Mineshafts | too coarse/wrong |

Rot assigns `CRYSTAL_HOLLOWS` and `DWARVEN_MINES` to all 60 gemstone rows. This incorrectly implies universal Dwarven Mines availability and loses biome/subarea constraints. The four modern Glacite gemstone families are not Crystal Hollows families.

Tier equivalence is correct: Rough 1, Flawed 80, Fine 6,400, Flawless 512,000, Perfect 2,560,000 Rough Equivalent. The 12-color stained-glass mapping appears consistent, but block color alone is not sufficient provenance outside a confirmed mining area.

## 8. Mining mechanics findings

- Mining Fortune: every 100 guarantees one additional drop set; the remainder is the percentage chance of one more set.
- Effective subtype fortune is base Mining Fortune plus exactly the applicable subtype: Block, Ore, Dwarven Metal, or Gemstone.
- Rot detects only Mining, Ore, and Dwarven Metal Fortune. Block and Gemstone Fortune are absent from the detector/config calculation model.
- Pristine is evaluated for each Rough gemstone after initial Mining Fortune; Mining Fortune also applies to the Flawed output. Rot's documented Pristine-to-Fortune ordering is therefore directionally correct.
- Mining Spread applies to Blocks, Ores, and Dwarven Metals. Gemstone Spread is separate and applies to Gemstones.
- Mining time is `(Block Strength * 30 / Mining Speed)` ticks, subject to server ticks, soft-cap behavior, ore instant-mine exceptions, ping, and area-specific custom-mining availability.
- Breaking Power is an eligibility threshold, not Block Strength and not Mining Speed.

These mechanics should be versioned data plus pure calculation rules. They should not be inferred from item aliases or display names.

## 9. Breaking Power findings

Current researched matrix:

- BP 1: Coal Ore, Cobblestone, End Stone, Nether Quartz Ore, Netherrack, Stone.
- BP 2: Iron Ore, Lapis Ore.
- BP 3: vanilla/pure Gold, Diamond, Emerald, Redstone, plus Pure Coal/Iron/Lapis.
- BP 4: Hard Stone, Mithril, Obsidian.
- BP 5: Titanium.
- BP 6: Ruby.
- BP 7: Amber, Amethyst, Jade, Opal, Sapphire.
- BP 8: Sulphur, Topaz.
- BP 9: Umber, Tungsten, Glacite, Jasper, Aquamarine, Onyx, Citrine, Peridot.

Rot currently stores none of this canonically. All supported materials therefore have unknown machine-readable Breaking Power even when the researched value is known.

## 10. Area and location findings

Confirmed defect corrected:

- `Mines of Divan` was under `DWARVEN_MINES` and matched in `matchDwarvenSub`.
- It is a Crystal Hollows location inside Mithril Deposits.
- The enum parent and matcher now return `CRYSTAL_HOLLOWS`; a focused regression test was added.

Coverage gaps:

- Dwarven Mines misses known labels such as The Lift, Dwarven Tavern, Ironman's Guild, Barracks of Heroes, Grand Library, Royal Quarters, and Grandpa Wolf's Cave.
- Crystal Hollows misses nested structures such as Jungle Temple, Goblin Queen's Den, Lost Precursor City, and Dragon's Lair.
- Deep Caverns, Gold Mine, Crimson Isle/Smoldering Tomb, and several mining-relevant parent areas are absent.
- Glacite Tunnels, Great Glacite Lake, and Glacite Mineshaft are modeled as independent parents rather than island plus area layers. This may be practical for the HUD but is not a complete hierarchy.

Fragile patterns:

- any line containing `jungle` is treated as Crystal Hollows except two hardcoded exclusions;
- any line containing `dwarven`, `nucleus`, `base camp`, or `rampart` can classify a location;
- first-match scanning can accept unrelated prose before an actual location line;
- scoreboard-only parent detection ignores stronger server/island identifiers when available;
- singular `Glacite Mineshaft` may not cover every live formatting/plural variant.

## 11. Community mod comparison

Useful patterns to adopt:

- The Hypixel Mod API server-change mode is a strong primary island signal, with scoreboard information for finer location detail.
- Robust mining recognition combines area, block changes, clicks, sounds, patterns, and bounded time windows instead of trusting block state alone.
- A robust Bazaar fetch path uses a guarded snapshot update and clear instant-buy/instant-sell semantics.
- Parsing structured hover components for sack messages and then resolving source-specific item IDs is a reliable approach.
- Separating island/location IDs from subareas and recognizing ambiguity around Glacite Mineshaft signals improves accuracy.
- Keeping physical/item repository identifiers source-specific helps; for example `INK_SACK-4` is not silently treated as the Hypixel API spelling.

Patterns not to copy:

- Do not copy third-party source or data wholesale; retain provenance and implement Rot-owned abstractions.
- Do not inherit large mutable global singletons or feature-specific regexes as canonical truth.
- Do not make a community repository authoritative over current official API identity.
- Do not add automation, packet manipulation, movement/combat/mining macros, anti-cheat bypasses, hidden telemetry, or credential handling.

## 12. Source and signal reliability matrix

| Signal/source | Best use | Reliability | Main failure mode |
| --- | --- | --- | --- |
| Official items API | IDs, names, category, NPC sell price | `OFFICIAL` | no mining mechanics/physical blocks |
| Official Bazaar API | product presence and live order data | `OFFICIAL` | counterintuitive side names; volatile |
| Official collections API | mining-family baseline | `OFFICIAL` | does not include every mining-adjacent drop |
| Historical official wiki | historical mechanics/changelog | `OFFICIAL` but stale | closed and no longer maintained |
| Current community wiki | mechanics, locations, yields | `RESEARCHED` | editable secondary source |
| Server-change mode/API | parent island | high live signal | availability/version changes |
| Scoreboard location line | subarea | medium-high | formatting, localization, unrelated text |
| Structured sack hover | item and quantity gain | high with correlation | UI formatting changes, batching |
| Action bar/chat text | quantity/currency clues | medium | localization and collisions |
| Vanilla block state | candidate physical representation | low alone | retexturing and cross-area collisions |
| Inventory delta | quantity confirmation | medium-high | unrelated gains, compaction, timing |
| Sound/click/time window | corroboration | medium | latency and overlapping actions |

## 13. Top project bugs and mismatches

1. **P0/high - Hard Stone identity is not safe.** `TrackedMaterial.HARD_STONE` maps vanilla Stone globally. Current wiki also lists cyan clay in Crystal Hollows, while cyan clay can be Mithril elsewhere. Expected: area-gated candidate plus item/sack/inventory corroboration. Proposed regression: reject Stone outside Crystal Hollows/Glacite context and distinguish cyan clay by area.
2. **P0/high - Fortune subtype model is incomplete.** Hard Stone and Cobblestone are `ORE`; both should be `BLOCK`. Block/Gemstone Fortune are not detected. Expected: four subtypes and tests covering each supported family.
3. **P1/high - Gemstone locations are factually wrong.** All 60 rows use the same Dwarven/Crystal pair. Expected: family-specific location relations.
4. **P1/high - Canonical dataset covers only 13/25 Mining collection families.** Expected: explicit official-only coverage and phased additions.
5. **P1/medium - Physical and yielded identities are conflated for Coal and Iron.** Expected: separate block, drop, and valuation IDs.
6. **P1/medium - Canonical mechanics schema is absent.** Expected: versioned breaking-power/yield/strength/fortune/context data with provenance.
7. **P1/medium - Source manifest is incomplete.** Bazaar, Collections, current wiki, historical official wiki status, community comparison, licenses, and “data not copied” are absent.
8. **P1/medium - Area heuristics are overly broad.** Expected: server mode plus exact normalized subarea aliases, with ambiguous evidence remaining unknown.
9. **P2/medium - Bazaar pair loading is all-or-nothing.** Expected: independent raw/enchanted availability without inventing prices.
10. **P2/medium - Existing coverage report overemphasizes 89 rows/2,123 products.** Expected: family-level denominators and modeled-mechanics coverage.
11. **P2/medium - `STARFALL` is missing as a special/event mining-adjacent drop.** Expected: source type and proximity/event mechanic, not ore classification.
12. **P2/architecture - Built-in registry, domain JSON, and item relations duplicate truth.** Expected: deterministic generation from one handwritten source plus validation.

## 14. Safe fixes implemented

- Moved `MINES_OF_DIVAN` from the Dwarven Mines parent to Crystal Hollows.
- Moved its scoreboard matcher from `matchDwarvenSub` to `matchCrystalHollowsSub`.
- Added a regression test for both parent and canonical subarea.

No other production behavior was changed.

## 15. Issues not fixed due to parallel-work conflict

The following were documented, not edited:

- `TrackedMaterial.HARD_STONE` and `COBBLESTONE` FortuneType corrections.
- Block Fortune and Gemstone Fortune detection/configuration.
- Hard Stone/Tungsten/Cobblestone block-state disambiguation.
- `MiningResourceCatalog`, `MiningSessionEngine`, `MiningSessionShadowObserver`, sack ingestion, current-session accounting, valuation, reset, and pause behavior.
- raw/enchanted Bazaar pair loading.
- canonical gemstone area rewrite and registry/domain regeneration.
- any suspected session-persistence/accounting defect.

These files or ownership paths are actively modified by parallel work. Their exact behavior must be re-audited after integration.

## 16. Tests added

`SkyBlockAreaTest.minesOfDivanExposesCrystalHollowsParentAndCanonicalSubArea` proves that the exact scoreboard label resolves to:

- parent `CRYSTAL_HOLLOWS`;
- subarea `MINES_OF_DIVAN`.

The existing Crystal Hollows parent test now also includes Mines of Divan.

Tests that currently encode incomplete or outdated assumptions:

- `TrackedMaterialTest` validates only Ore and Dwarven Metal Fortune; it omits the required Block and Gemstone categories.
- area tests previously omitted Mines of Divan and do not exercise missing official subareas or false-positive prose.
- data-foundation tests validate current ID preservation but do not enforce physical-block/drop/product separation.
- coverage tests validate the 89-row bundle, not 25-family Mining collection coverage.

## 17. Data coverage metrics

### Items and conflicts

| Metric | Count |
| --- | ---: |
| Official items inspected | 5,646 |
| Official Mining collection families | 25 |
| Rot canonical rows | 89 |
| Rot represented Mining collection families | 13 |
| Official-only Mining collection families | 12 |
| Canonical rows matched to current official IDs | 86 |
| Explicit ID/display alias conflicts | 3 |
| Rot-only semantic currency markers | 3 |
| Unresolved official IDs | 3 |
| Community-only canonical rows | 0 |

### Bazaar

| Metric | Count |
| --- | ---: |
| Official products | 2,123 |
| Rot product mappings | 86 |
| Valid mappings | 86 |
| Non-Bazaar Rot markers | 3 |
| Invalid/missing mapped IDs | 0 |

### Mining mechanics

| Metric | Count |
| --- | ---: |
| `TrackedMaterial` resources | 7 |
| Researched BP values for those resources | 7 |
| Canonically encoded BP values | 0 |
| Encoded base yields | 7, including per-block profiles |
| Correctly modeled fortune subtype among 7 | 5 |
| Incorrect fortune subtype among 7 | 2 |
| Fortune subtypes detected | 2 of 4, plus base Mining Fortune |

### Gemstones

| Metric | Count |
| --- | ---: |
| Families | 12 of 12 |
| Tier rows | 60 of 60 |
| Valid official IDs | 60 of 60 |
| Valid Bazaar mappings | 60 of 60 |
| Complete tier relation edges | 48 of 48 |
| Correct family-specific area rows | 0 of 60 |
| Canonically encoded BP | 0 of 12 families |

### Areas

| Metric | Count |
| --- | ---: |
| Modeled parent values excluding unknown | 7 |
| Modeled subareas | 28 |
| Confirmed wrong parent before this audit | 1 |
| Known unmodeled location labels listed in this report | at least 11 |
| Broad fragile match groups | at least 5 |

## 18. Unresolved and needs live verification

- Exact current scoreboard strings and formatting for every nested mining location.
- Whether server-change mode is available and stable in Rot's supported Minecraft/Hypixel Mod API combination.
- Safe live discrimination of Stone/Hard Stone, cyan clay/Mithril/Hard Stone, cobblestone/Tungsten, terracotta/Umber, and packed ice/Glacite.
- Exact current base yield for Pure Diamond from a primary/current mechanics source.
- Whether Mining Spread-generated breaks emit the same observable signals as direct breaks for every material.
- Pristine, fortune, spread, sack batching, compaction, and inventory timing interactions under latency.
- Titanium base spawn-rate discrepancy between current community pages (0.5% versus 2% wording). Treat rate as `CONFLICT` until current patch evidence or a controlled live sample resolves it.
- Glacite Mineshaft island/subarea signal ambiguity.
- Remaining concurrent session/accounting behavior after integration.

## 19. Top 20 next actions

1. **P0:** integrate and test the Mines of Divan correction.
2. **P0:** add `BLOCK` and `GEMSTONE` FortuneType values without touching accounting until ownership is coordinated.
3. **P0:** change Hard Stone and Cobblestone to Block Fortune with regression tests.
4. **P0:** design an area/context-gated block candidate API for Hard Stone, Tungsten, Umber, Glacite, and gemstones.
5. **P0:** add live-safe Hard Stone false-positive tests.
6. **P1:** create a versioned `mining-mechanics.v1` schema: resource, physical blocks, BP, strength, yield, fortune subtype, spread type, areas, exceptions, confidence, provenance.
7. **P1:** separate canonical resource ID, official dropped-item ID, physical block ID, and valuation product ID.
8. **P1:** correct all gemstone family area relations.
9. **P1:** add the 12 missing official Mining collection families in reviewed slices.
10. **P1:** add full enchanted/block relations for each new family from official item IDs.
11. **P1:** extend provenance with Bazaar, Collections, wiki closure/currentness, community sources, licenses, and “not copied” notes.
12. **P1:** make coverage generation family- and mechanics-aware.
13. **P1:** use server mode for parent island and exact scoreboard aliases for subarea when available.
14. **P1:** replace broad `jungle`, `dwarven`, `nucleus`, `base camp`, and `rampart` substring rules with boundary/exact evidence.
15. **P1:** model Starfall as `SPECIAL_DROP` with mob and Fallen Star proximity sources.
16. **P2:** allow independent Bazaar raw/enchanted price availability.
17. **P2:** add an official Mining collection snapshot to offline refresh validation.
18. **P2:** add deterministic checks for alias uniqueness, no `STONE -> HARD_STONE`, tier completeness, cycles, and provenance.
19. **P2:** add a source-adapter layer for community-repository IDs such as `INK_SACK-4`.
20. **P2:** re-audit the accounting hot zone after the parallel branch work settles, using controlled runtime fixtures rather than private logs.

## 20. Final Git state

At report creation:

- branch remained `ui-redesign`;
- HEAD remained `abcd4c4f50f71ec80cc66187d03cdd9e2a7ba67b`;
- no branch switch, reset, clean, stash, staging, commit, push, merge, rebase, tag, release, deployment, or history rewrite occurred;
- protected untracked artifacts were not opened, read, copied, renamed, deleted, staged, or ignored;
- concurrent changes were preserved;
- the external-data portion initially owned this report and the three isolated Mines of Divan files named in sections 14 and 16; later checkpoint work added separately tested Sack-ingress and canonical item-identity corrections.

## Explicit answers A-T

**A. Is the current canonical mining dataset factually current?**
Partly. Current item/Bazaar IDs and gemstone tiers are strong, but family coverage, areas, mechanics, and provenance are incomplete or wrong.

**B. Which known mining resources/items are missing?**
At minimum the 12 official collection families listed in section 4, plus Stone as a distinct contextual block, Starfall as a special drop, and reviewed refined/enchanted relations where relevant.

**C. Which IDs are wrong or ambiguous?**
Titanium and Lapis are intentional aliases. Coal and Iron are semantically ambiguous because block IDs are used as stable resources while item/Bazaar IDs refer to drops. Powder IDs are semantic markers, not official item IDs.

**D. Are `TITANIUM`/`TITANIUM_ORE` semantics correct?**
Yes, if `TITANIUM` remains a Rot stable routing ID and `TITANIUM_ORE` remains the explicit official/Bazaar ID.

**E. Are `LAPIS_LAZULI`/`INK_SACK:4` semantics correct?**
Yes for Rot stable ID versus Hypixel ID. Add source-specific normalization for community-repository IDs like `INK_SACK-4`; do not replace the official colon ID.

**F. What is Starfall and how should it be modeled?**
It is item `STARFALL`, NPC-sellable and Bazaar-traded, obtained from Star Sentries/Treasure Hoarders and as a special nearby-Fallen-Star ore drop. Model it as a mining-adjacent `SPECIAL_DROP`, not a base ore or generic block yield.

**G. Which items are Bazaar products?**
All 86 mapped canonical item rows are current products. The three powder markers are not. The 12 missing collection bases and Starfall are products. NPC price is a separate fallback; unsupported non-Bazaar items remain unresolved unless an explicitly supported source exists.

**H. Is Bazaar side interpretation correct?**
Yes. `sell_summary` is the side consumed by player instant sells, and `quick_status.sellPrice` is the matching weighted fallback.

**I. Are gemstone families and tiers complete?**
Yes: 12 families, 5 tiers, 60 valid IDs, and complete tier relations.

**J. Are gemstone BP requirements correct?**
The researched values in section 7 are current. Rot does not encode them canonically, so the data foundation cannot validate them.

**K. Is Pristine -> Mining Fortune order correct?**
The documented order is correct: initial fortune, per-Rough Pristine replacement, then fortune on Flawed output. Exact stochastic/runtime attribution still needs controlled tests.

**L. Which Fortune subtype applies?**
Gold/Diamond and ordinary ores use Ore; Hard Stone/Cobblestone and similar blocks use Block; Mithril/Titanium/Tungsten/Umber/Glacite use Dwarven Metal; gemstones use Gemstone.

**M. Which resources require area/context?**
At least Hard Stone, Mithril cyan clay, Tungsten, Umber, Glacite, all gemstones, and pure ore block representations.

**N. Is Hard Stone identification safe?**
No. Vanilla Stone alone is insufficient, and cyan clay changes meaning by area.

**O. Are area lists complete enough?**
No. They cover major mining zones but omit several parents/nested labels and mix island/subarea layers.

**P. Which patterns are fragile?**
Broad substring checks for jungle, dwarven, nucleus, base camp, rampart, first-match scanning, and scoreboard-only parent inference.

**Q. Which tests encode outdated assumptions?**
The main issue is omission rather than an explicit false assertion: fortune tests cover only two subtypes, area tests omit known labels/false positives, and data tests accept conflated block/drop IDs and coarse gemstone areas.

**R. What useful community patterns should be adopted?**
Layered server-mode plus subarea detection, multi-signal mining correlation, structured hover parsing, source-specific ID adapters, immutable/guarded market snapshots, and explicit uncertainty.

**S. What should not be copied?**
Third-party code/data wholesale, community truth as official authority, sprawling mutable globals, unscoped regex truth, automation, telemetry, credentials, or gameplay-advantage packet behavior.

**T. What public data is currently ignored?**
Official Collections, NPC sell prices/categories/materials from Items, full Bazaar product coverage, official/current patch history, versioned location aliases, block strengths/BP/yields/fortune types, and carefully attributed community item-repository representations.

**EXTERNAL DATA / MECHANICS FOUNDATION NEEDS CORRECTION.**
