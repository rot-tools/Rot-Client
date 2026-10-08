# 26.3 Market Watch and Plus trading candidate

Candidate `2.0.5+mc26.3` targets `port/26.3` only. This is experimental, automated-tested
code awaiting controlled Minecraft validation. No live purchase, listing or Bazaar order has
been executed during development. Older Minecraft releases are unchanged.

## Both editions: passive item matching

The watch form has a **Rarity / Reforge** editor. Reforge values are internal modifier IDs
such as `spicy`, `ancient` or `renowned`. Blank means any modifier; `none` requires known,
unreforged item data. Filters survive save/copy/reload with old profiles still readable.

API `item_bytes` is decoded with compressed/decompressed size limits. Canonical ID, modifier,
item UUID and quantity are separate from the auction UUID. Comparison groups also retain
rarity, visible name/category and a recursively sorted fingerprint of ExtraAttributes.
Enchantments, upgrades and other value-bearing attributes cannot inflate another variant's
reference. Per-instance UUID, creation timestamp and origin tag are excluded from that
fingerprint. Pets and enchanted books with generic IDs remain outside canonical automated
valuation. Legacy passive name alerts still work when item bytes are unavailable; this does
not establish sufficient evidence for a trade.

## Plus only: setup and spending

Open **Trading Setup** next to Market Watch/Profit Finder, or use `/rot markettrading`.
The Budget, Risk, Listing, Bazaar, Targets and Results pages are separate from ordinary watches.
Automation starts **off**, with **zero** purchase budgets. Saving settings does not arm it.
Each restart, disconnect, world/profile/account change or Esc stops it. `/rot markettrading stop`
also stops it. Turning it off cannot reverse a transaction already sent to the server.

Only explicitly pinned variants and checked, enabled watches are eligible. AH pins freeze
the item ID, rarity and modifier. Legacy/unknown variants and dynamic Bazaar discount watches
remain manual. Unchecking a target or an altered market/price/Purse invalidates a countdown.

For an observed exact live Purse `P`, retained cash `R`, configured percent `f`, per-trade cap
`T`, total buy budget `B`, and already committed intents `C`, the cap is:

```
available = max(0, P - R)
cap = min(available, floor(available * f / 100), T, max(0, B - C))
```

AH reserves the full purchase price **plus the configured maximum listing fee** before any
purchase is sent. Bazaar reserves the rounded-up order cost. The recorded total budget spans
the durable journal: stopping/restarting does not reset it and expected profits do not replenish
it. It is deliberately conservative when an outcome is uncertain. Listing fee has a separate
cap and the retained cash is checked again before listing. Unknown/rounded `Purse: 1.2M`
values do not authorize a purchase. An exact `Purse:`/`Piggy:` sidebar amount is required.

## Market checks and execution

AH comparisons exclude the proposed seller and the player's own auctions. They require
enough same-variant listings and distinct sellers, reject dominant seller concentration,
wide price distributions and abrupt observed reference changes. The resale price uses the
lowest valid competing BIN, less the configured undercut, rather than an inflated median.
A 5% resale-tax allowance and maximum listing fee reduce the estimated profit. Two distinct
server snapshots must support the comparison before the four-second deal countdown starts.
These are manipulation-risk heuristics; they cannot determine an item's guaranteed true value.

The AH controller opens the browser, searches the canonical base item name, selects BIN-only
and rarity, then opens the precise auction UUID. Before confirmation it verifies the live
item UUID, ID, rarity, modifier, attributes, quantity, exact price and current budget. Initial
support is limited to one uniquely identifiable, non-stackable-in-this-flow item at a time
(quantity one with an item UUID). It claims only that item's pickup action, observes unique
inventory ownership, and creates a BIN listing with verified price, duration and quoted fee.
Listing success requires a matching player-owned auction in the public snapshot.

Bazaar uses **buy orders followed by sell offers**, not instant-buy execution of an order-spread
estimate. The API's `buy_summary` is the sell-offer/instant-buy book and `sell_summary` is the
bid/instant-sell book. The controller checks book depth, weekly volume, spread and reference
change; limits quantity to 64 or the configured lower cap; and checks exact amount/unit price
in the order confirmation. It requires an anchored setup receipt, no pre-existing order for
the target or paginated order list, a unique full order owned by the current username, and
the exact acquired inventory delta before offering those units for sale. Ambiguous co-op
orders, partial fills and changed quantities stop or wait for manual review. It does not
cancel existing orders or collect unrelated coins. Bazaar seller identities are not exposed
by the public aggregate API, so AH seller-concentration checks cannot be applied to it.

One action is permitted per acknowledged container revision, at least 300 ms apart. Menu actions
must be unique and belong to the container, not similarly named player-inventory items. Sign
input requires a pending action and its expected prompt. Unknown menus, missing proofs, save
failure or timeout stop the controller; there is no blind fallback click or automatic retry.

## Outcomes and recovery

`rotclient-market-trades.json` is a bounded, schema-checked, atomically replaced **Plus-only**
journal. It persists intent before purchase/listing confirmation. Unsupported/future/corrupt
documents remain preserved and block trading. Purchase/listing outcomes that are not proven
remain marked for manual review and prevent a new arm. Do not delete that journal to retry
an uncertain purchase. Inspect actual inventory, orders and auctions first.

Results distinguish unsold positions from observed sales. AH sales require the recorded
listing UUID, seller, item UUID, price and creation/ending time to agree with
`/v2/skyblock/auctions_ended`. That endpoint is polled at most once per minute while owned
listings remain pending; missed history during an offline interval is not invented. Bazaar
requires the exact owned sell offer to show 100% filled. Coin collection remains manual.
The displayed **known-cost gross profit** deducts observed purchase cost and known listing
fee. It is explicitly **before sales tax** and is not a claim that coins were collected.
Sales whose listing fee is unknown after manual recovery do not enter that profit total.
No automatic purchase is performed on recovery.

## Controlled runtime checklist

1. Launch **26.3 Test** with exactly one edition. In Lite, confirm passive variant filters and
   legacy watches work and no trading workspace, commands, mixins or journal exist.
2. In Plus, begin with automation off. Verify zero budgets, missing Purse, unknown variants,
   unchecked watches, stale data and manipulated/sparse references cannot purchase.
3. Set a small recorded total/per-trade budget, retained cash, percentage and one cheap target.
   Check two snapshots and the four-second countdown. Test Esc and `/rot markettrading stop`.
4. Observe AH search, BIN/rarity settings, exact auction/item identity, live price and fund
   rechecks. Confirm claim, draft transfer, exact listing price/duration and fee. Record the
   listing UUID and real server outcome. If a prompt/button differs, keep the pending journal
   and capture that menu/title/lore; do not loosen checks to guess its action.
5. Test Bazaar separately with a small quantity and no existing orders for the product.
   Confirm buy-order price/amount/sign, exact receipt, partial/full fill distinction, owner,
   claim delta and sell offer. Test ambiguous co-op orders and unexpected menu rejection.
6. Verify one real AH/Bazaar sale contributes only once to pre-tax known-cost profit.
   Restart/disconnect during countdown and after a sent transaction. Confirm no duplicate
   buy and preserved pending outcome. Verify existing HUDs, inventory, Market Watch and mining
   accounting remain functional. Automated tests do not replace these observations.

## Primary research and provenance

The integration and policies were written for Rot Client. **No upstream implementation was
copied** for this change. Pinned observations/documentation:

- [Official Hypixel API](https://api.hypixel.net/): read-only snapshots, item bytes, auction identities
  and ended-auction fields. No purchase/sell REST API is assumed.
- [SAF auction flow, 05bcdc9](https://github.com/x1f4r/SAF/blob/05bcdc9305d23b0e256aa02516ea5806c144a6f6/docs/AUCTION_FLOW.md)
  and repository GUI fixtures (MIT): purchase/listing titles and fee/duration examples.
- [Skyblocker auction browser, f5cc879](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/auction/AuctionBrowserScreen.java)
  and sign/Bazaar order observations (LGPL-3.0): filter indices/selected markers and prompt/price grammar.
- [SkyHanni Bazaar order observations, a752834](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/features/inventory/bazaar/BazaarOrderApi.kt)
  and BazaarApi formats (LGPL-2.1): receipt, owner and filled-order text.
- [SkyCofl, f097655](https://github.com/Coflnet/SkyblockMod/tree/f097655cd8e5244e5eab18f8e790669b459763c4)
  (AGPL-3.0): BIN-view/confirm container observations.

This does not claim these public fixtures exactly match every current server GUI. Runtime
fixtures remain necessary. Lite public-release readiness is still governed by
[the existing release audit](MODRINTH_LITE_RELEASE.md).
