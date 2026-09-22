# Example profiles

Rot Client ships a few example profiles. They are **offered, not installed**:
the profile store starts empty and only grows when the user asks. Visuals →
Profiles → **Examples** lists them, and **ADD** / **ADD ALL** creates a normal
profile from one. The new profile is added *without* switching to it, so the
player's current settings are never touched. If they have no profiles yet, the
current setup is saved as "My Setup" first and stays active, because otherwise
the periodic autosave would overwrite the new example with the live settings.

Every example turns on the **Custom Scoreboard** and the **Pet HUD**, then adds
the HUDs that suit it:

| Example | Adds |
| --- | --- |
| Everyday | Performance HUD |
| Mining | Mining Tracker, Powder Chest, Commission Display |
| Dungeons | Dungeon HUD with map, Blood Timers, Performance HUD |
| Slayer | Slayer Display, Slayer Progress, Slayer Stats |
| Fishing | Fishing HUD, Sea Creatures |

Examples only use modules from the shared catalog, so they behave the same in
Rot Client and Rot Client+, and none of them touches an automation option.

## Where they live

`src/main/resources/assets/rotclient/profile-presets/`

- `index.json`: the ids to offer, in display order.
- `<id>.json`: one file per example.

```json
{
  "id": "mining",
  "name": "Mining",
  "summary": "One line shown under the name.",
  "highlights": ["Custom Scoreboard", "Pet HUD", "Mining Tracker"],
  "modules": ["qol.custom_scoreboard", "qol.pet_hud", "qol.commission_display"],
  "settings": { "qol.dungeon_watcher.blood_timers": true },
  "miningTracker": true,
  "powderChestHud": true,
  "layout": [
    { "anchor": "TOP_LEFT", "elements": ["mining_tracker"] },
    { "anchor": "TOP_LEFT", "elements": ["pet", "commission"] },
    { "anchor": "TOP_RIGHT", "elements": ["powder_chest"] }
  ]
}
```

- `modules` are catalog ids switched on. `settings` are child toggles by id.
  Anything not listed keeps the ordinary Rot Client default.
- `miningTracker` / `powderChestHud` are the two standalone mining HUDs, which
  are profile settings rather than QoL modules. The Powder Chest HUD is on in a
  fresh profile, so an example switches it off unless it says otherwise.
- The Custom Scoreboard is never in `layout`. It aligns itself to the right
  edge, vertically centred, and examples leave it alone.

## HUD placement

Layouts are **anchored stacks**, not coordinates. An anchor is one of
`TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `MIDDLE_LEFT`, `MIDDLE_RIGHT`,
`BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`.

- Elements in a stack go top to bottom, in the order listed, 4px apart.
  Put anything that can grow (the Mining Tracker, the Powder Chest HUD) last in
  its stack, or alone in its column, so growing never runs into a neighbour.
- Several stacks on the same left or right anchor sit side by side, each after
  the previous one, so columns never overlap.
- Positions are resolved once, when the example is added, against the game's
  GUI-scaled window size and each HUD's footprint (`PresetHudSizes`). After
  that they are ordinary profile settings the player can drag in the HUD
  editor (`/rot edit`).
- Footprints are what a HUD *occupies*, which is not always its drag box. The
  Dungeon HUD draws a 128px map beside its panel, so it is listed as 288x150.
  Keep them a little generous.
- Everything is clamped onto the screen. When a layout does not fit a window
  (`PresetHudLayout.fits`: something off screen, or two HUDs overlapping) the
  Examples page says so before anything is added. Mining needs about 652px of
  GUI-scaled width, so it does not fit at 1080p with GUI scale 3 (640 wide).
  Lower the GUI scale or move the HUDs in the HUD editor.
- Vanilla draws potion-effect icons in the top-right corner, and the Custom
  Scoreboard sits at the right edge, vertically centred. Right-anchored HUDs
  can meet either; HUD Layout has a Hide Effects option.

Element ids are the HUD editor's ids (`pet`, `performance`, `commission`,
`slayer`, `slayer_progress`, `slayer_stats`, `dungeon`, `dungeon_watcher`,
`fishing`, ...) plus `mining_tracker` and `powder_chest`. The full list is
`PresetHudSizes`.

## Adding or changing an example

1. Edit or add `<id>.json` and list the id in `index.json`.
2. If it uses a HUD that has no footprint yet, add one to `PresetHudSizes`, and
   a visibility rule to `RotClientProfilePresetTest#visible`.
3. Run the tests. A broken example is skipped at runtime so it can never break
   the Profiles page, which means **the tests are what catch it**:
   `RotClientProfilePresetTest` checks that every example loads, has the
   scoreboard and Pet HUD, turns on exactly what it says, places every HUD it
   makes visible (and only those), and stays on screen with no overlaps, also
   clear of the scoreboard, on 640x360 up to 1920x1080.

## Hand-placing a layout

To use an exact hand-made arrangement, create a profile, position its HUDs in
the HUD editor, and take the pose values from that profile in
`rotclient-profiles.json` (`profiles[].settings.qolUtilities`, for example
`petHudX` / `petHudY`, plus `miningHudX/Y` and `powderChestHudX/Y`). Convert
each to the nearest corner as an offset and express it as a stack. Keeping
placements as anchors is what stops one player's window size from becoming
everyone's default.
