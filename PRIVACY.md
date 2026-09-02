# Privacy

Rot Client is a client-side mod. Core tracking, configuration, Current Session state, Session History, UI preferences, lifetime totals, HUD settings, and diagnostic files are stored locally in the Minecraft instance.

## Network access

Rot Client requests public Bazaar data from Hypixel's Bazaar API to support
material price estimates. The request does not require a Hypixel API key.

When the user enables Price Tooltips, Rot Client also requests public Hypixel
Bazaar and item-resource data plus a public lowest-BIN price snapshot at
`https://moulberry.codes/lowestbin.json`. Price Tooltip quote requests are not
made while that module is disabled. These public responses are held in memory;
Rot Client does not write a runtime quote cache for this module.

Standard network metadata, such as the client IP address and request headers,
is visible to each remote service as part of a normal HTTPS request.

Slayer Carry Tracker has an optional Discord webhook integration. It is disabled
by default and sends only a carry progress or completion summary when the user
explicitly enables it and provides a Discord HTTPS webhook URL. The webhook URL
is stored in the local `rotclient.json` configuration. Rot Client does not add
the URL to diagnostics or repository files, and it does not send unrelated
chat, inventory, coordinates, credentials, or authentication tokens through
this integration.

The mod does not intentionally upload tracker state, configuration, diagnostic files, chat, screenshots, coordinates, credentials, authentication tokens, or unrelated personal files. Rot Client does not use telemetry, cloud sync, or remote analytics.

On multiplayer connections (Hypixel, Serveris, and other remote hosts), Rot Client scrubs outbound handshake traffic so servers do not receive the `rotclient` mod id in registered plugin channels and the client brand is sent as `vanilla` instead of `fabric`. Single-player integrated worlds are unchanged.

The repository also includes manually invoked development tasks,
`refreshSkyBlockData` and `auditSkyBlockMiningData`. When run without their
offline option (`--offline`), these tasks request the public Hypixel items,
collections, skills, and Bazaar endpoints. They do not send player, chat,
inventory, session, diagnostic, or authentication data. Validated responses are
cached under the ignored `build/skyblock-data-cache/` directory and are not
shipped as runtime session data.

## Local configuration and state

The local Rot Client configuration (`rotclient.json`) may contain:

- the selected material or gemstone;
- tracker and UI preferences;
- material and gemstone session and lifetime counters;
- active-time state;
- manual or detected Fortune values for material tracking;
- cached material pricing and tax settings;
- HUD position, scale, and graph history;
- QoL module toggles, keybinds, overlay positions, and the local Auto Clicker
  item whitelist (held-item SkyBlock ids, not player identity);
- editable Inventory Button commands/layouts and Storage Overlay preferences;
- Slayer drop-filter choices, local carry-price rules, optional Discord webhook
  URL, and bounded completed-carry history.

Automation-style QoL modules are disabled by default. They are development
features for Serveri, the SkyBlock world this client is built against.
Their settings remain local.

Storage Overlay keeps server-observed Ender Chest and Backpack page contents in
process memory for the current game run. The current implementation does not
write that page cache to disk. Completed Slayer carry history is stored locally
in `rotclient.json` and may include the entered player name, boss family/tier,
kill counts, configured/received coin amounts, status, and timestamps.

The durable Current Session is stored separately in
`rotclient-current-session.json`. It may contain an internal local session
identifier, display number, lifecycle and pause timestamps, selected-target and
parent-area segments, visited areas, canonical item identifiers and display
names, quantities, source/classification metadata, known/unknown state, and
current valuation status and gross estimate. This file is the single durable generic
session ledger; HUD and Analytics read it as projections rather than maintaining
another persistent copy.

Client UI appearance and workspace preferences are stored in
`rotclient-appearance.json` and `rotclient-workspace.json`. These files can
include colors, an instance-local custom background filename, window/layout
positions, open UI routes, scroll positions, and the currently selected local
History record. They do not own mining quantities.

Recovery handling can retain additional local copies of session data. An
unreadable Current Session or History file may be moved to
`rotclient-current-session.corrupt.json` or
`rotclient-session-history.corrupt.json`. A Current Session schema upgrade may
also create the one-shot backup `rotclient-current-session.json.bak`. These
recovery files can contain the same categories of local session information as
their source file and are not uploaded by Rot Client.

During a confirmed Start New operation, Rot Client may briefly create
`rotclient-session-start-new-transaction.json`. This bounded recovery marker
contains a schema version, one-way local session key, archive content
fingerprint, and time boundary. It contains no item rows, quantities, player
identity, chat, coordinates, credentials, or full Current Session payload. It
is removed after completion; an unreadable marker is retained for manual review
rather than overwritten.

When upgrading from MiningTracker, Rot Client may copy `miningtracker.json` to `rotclient.json` on first launch if the new file does not already exist. The legacy `miningtracker.json` file is not deleted or modified.

Minecraft instance management, backups, synchronization, and file sharing are controlled by the user and their launcher or operating system.

## Optional diagnostics

Diagnostic recording is disabled by default. It starts only when the user runs `/rotclient record start` and stops with `/rotclient record stop` or when the client closes. The file is written locally under the Fabric configuration directory as `rotclient-diagnostic-<timestamp>.log`.

A diagnostic file may contain the minimum client-observable details needed to validate tracker routing and accounting, including:

- timestamps and parsed event types;
- selected target or gemstone and tracker state;
- numeric block, item, inventory, Sack, tier, Fortune, price, timing, correlation, and deduplication counters;
- material, gemstone, tier, item, and Sack display text used to identify or correlate an event;
- limited Sack hover-line text and a codepoint-safe representation used to diagnose formatting or icon characters;
- acceptance, rejection, signal-consumption, and ledger-mutation outcomes.

These records can include limited in-game display text associated with a relevant event. They are not intended to collect unrelated chat, private messages, player coordinates, account credentials, authentication data, screenshots, or unrelated personal files. The recorder replaces line breaks and tabs in each field but does not anonymize all display text.

Diagnostic-only observers do not authorize or mutate live tracker credits. Their output is evidence for controlled validation, not a guarantee of accuracy.

## Optional Session Analytics summaries

Session Analytics can copy a plain-text session summary to the
Minecraft clipboard when the user runs `/rotclient session copy` or uses
the dashboard Copy Summary control. Analytics reads the durable Current Session
as its canonical generic item source and may combine it with transient target
and parity context from the mining-session engine. The summary contains only
aggregated category counts, bounded resource quantities, parity status, and
derived Bazaar instant-sell (gross) item valuation. It does not include raw
chat, coordinates, player names, UUIDs, session or event identifiers,
diagnostic file paths, or API payloads. Transient engine state is not persisted
to `TrackerStore` and never becomes a second durable Current Session ledger.

## Optional Session History

Session History 2.0 stores immutable freezes of the canonical Current Session
in `rotclient-session-history.json` under the Minecraft config directory.
Confirmed Start New archives a freeze before creating the next session. Manual
save accepts only a PAUSED Current Session. Every stored record has a closed
time boundary and may contain the display number, active and paused durations,
closed target and parent-area segments, canonical item rows, source and mining
classification, known/unknown state, and frozen valuation status and gross
value. The price-book observation timestamp may be retained, but the full
product-price map is not.

History is separate from `TrackerStore` and never stores live ledger events,
raw chat, coordinates, player names, UUIDs, server addresses, API payloads,
diagnostic paths, Current Session identifiers, event/correlation identifiers,
or full product-price maps. At most 20 newest sessions are retained. Stored
valuation is frozen at archive time and is not updated by later Bazaar
refreshes. Schema v1 records remain readable. Copied historical summaries are
labeled as stored sessions and remain privacy-bounded. Malformed history files,
unsupported schema versions, forged fingerprints, and unsafe retained text fail
closed to an unavailable empty state. Session History does not use cloud sync,
telemetry, or additional networking.

When upgrading from MiningTracker, Rot Client may copy
`miningtracker-session-history.json` to `rotclient-session-history.json` on
first launch if the new file does not already exist. The legacy history file
is not deleted or modified.

## Sharing diagnostic files

Diagnostic files remain local unless the user chooses to share them. Before sharing, review the entire file and remove any information you do not want to disclose. Share only the smallest relevant excerpt through a trusted channel.

Minecraft and launcher runtime logs must also be redacted before they are attached publicly. Game logs can contain usernames, profile identifiers, local Windows paths, Prism/launcher instance names, server data, and third-party chat output that is unrelated to Rot Client.

Repository documentation should not include raw diagnostic logs, screenshots, personal machine paths or usernames, launcher instance names, tokens, or other local identifiers.

## Data retention and removal

Rot Client does not operate a server that retains user diagnostic or tracker data. Local configuration, History, Current Session, recovery (`.corrupt.json` / `.bak` / Start New transaction marker), development cache, and diagnostic files remain until the user, developer, operating system, or launcher removes them. Removing the mod does not necessarily remove those local files automatically.

## Changes

This notice should be updated whenever network behavior, local storage, or diagnostic content changes materially.
