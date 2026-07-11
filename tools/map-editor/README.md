# Exploration Map Editor

A zero-dependency, browser-based editor for the game's exploration maps
(`app/src/main/assets/maps.json`). Rendering mirrors `ExplorationScreen.kt`
(same theme colors, seeded ground detail, nav-grid size, badge styling), so
what you lay out here is what the game draws.

## Run

```bash
python3 tools/map-editor/serve.py        # default port 8765
```

Open http://127.0.0.1:8765. Saving writes straight to
`app/src/main/assets/maps.json` (previous version backed up alongside as
`maps.json.bak`).

## Workflow

1. Pick a map from the dropdown (or **＋ Map** / duplicate).
2. Tools: **Select** (move/resize, corner handles on obstacles), **Obstacle**
   (drag a rectangle), **Entity** (click to place; set type STORY/NPC/EXIT in
   the panel), **Decor**, **Spawn**.
3. Edit details in the right-hand panel: map name/size/theme colors, the
   scenario `locationNames` the map covers, per-entity icon/label/dialog/exit
   target. The icon palette sets the placement brush and restyles the current
   selection.
4. Watch the **Validation** panel — it runs the same checks as the unit tests:
   exactly one STORY marker per map, exits valid and reciprocal, every
   entity reachable from spawn on the game's 25-unit nav grid, and every
   scenario location covered by some map.
5. **Save** (Ctrl+S). Then run the content net for certainty:

```bash
./gradlew testDebugUnitTest --tests "com.spiritwisestudios.crossroadsoffate.data.ExplorationMapCatalogTest"
```

## Keyboard

| Key | Action |
| --- | ------ |
| V / O / E / D / S | Switch tool |
| Delete | Remove selection |
| Arrows (+Shift) | Nudge 1 (10) units |
| Ctrl+Z | Undo |
| Ctrl+S | Save |

## Constants to keep in sync

`PLAYER_RADIUS` (12) and `NAV_CELL` (25) at the top of `editor.js` mirror
`ExplorationManager`. If those change in Kotlin, change them here too so the
reachability validation stays truthful.
