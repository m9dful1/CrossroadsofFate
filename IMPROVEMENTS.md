# Crossroads of Fate — Improvement Plan

This document details planned improvements to the game, organized by category. Each section describes the current state, the goal, implementation approach, and acceptance criteria.

---

## Human Tasks — What Claude Code Cannot Do

Claude Code can write all the Kotlin/Compose code, edit JSON, create data models, run builds and unit tests. The items below are things **you** must do yourself to make each improvement actually functional.

### 1. Mini-Game UIs (Lock Picking & Trading)
- **Playtest timing feel on a real device** — the lock picking sweet-spot window size, indicator sweep speed, and tap responsiveness can only be tuned by playing it. Claude Code will pick initial values but they will almost certainly need adjustment.
- **Visually verify animations** — pin color changes, shake animations, and the sweep indicator need to be seen on-screen to confirm they look right and are readable.
- **Test back-button / lifecycle edge cases** — app backgrounding mid-mini-game, rotation, interruptions. These require a running device.

### 2. Quest Expansion
- **Review and approve all narrative content** — quest names, objective descriptions, and trigger points are creative decisions. Claude Code will draft them but you need to verify they fit your story vision.
- **Playtest quest flow** — walk through each path to confirm objectives trigger at the right moments and progression feels natural. No unit test can verify pacing.

### 3. Quest Rewards System
- **Balance reward values** — which items/unlocks feel meaningful vs. overpowered or underwhelming is a game design judgment call.
- **Visually verify the reward popup** — confirm it's readable, well-positioned, and dismisses correctly on device.

### 4. Character Stats
- **Balance all stat numbers** — starting values, per-decision grants, path boosts, and gating thresholds all need playtesting to feel fair. Claude Code will set initial numbers but you'll need to iterate.
- **Decide which decisions get stat gates** — this is a design choice about where the game should feel restrictive vs. open.
- **Visually verify stat bars** in the character menu look correct on device.

### 5. Interactive Map Expansion
- **Set coordinate positions for new locations** — `coordinateX`/`coordinateY` values (0–1 range) determine where nodes appear on the visual map. You need to see the map on-screen and adjust positions until the layout looks good.
- **Verify discovery conditions flow** — play through to confirm locations unlock at the right story moments and don't leave the player stuck.
- **Provide background images for new locations** if you want unique art (Shadow Alley, Sacred Temple, etc.). Some drawables already exist; you'll need to decide if new ones are needed.

### 6. Choice Consequences and Reputation
- **Balance reputation change values** across all 10+ decisions — how fast should rep swing? What thresholds feel right for gating content? This requires play-feel judgment.
- **Review narrative coherence** — confirm that the decisions Claude Code tags with reputation changes actually make sense in context (e.g., "helping the merchant" should boost merchant rep, not guard rep).

### 7. Sound and Music
- **Source or create SFX audio files** — you have 4 background music tracks already (`music_town.ogg`, `music_wilderness.ogg`, `music_mystery.ogg`, `music_menu.ogg`), but there are **no SFX files at all**. You need to provide `.ogg`/`.mp3` files for: button tap, item acquired chime, quest completed fanfare, mini-game feedback sounds (pin lock, negotiation accept/reject). Claude Code cannot create audio files.
- **Source additional music tracks** if you want combat or temple-specific background music beyond the 4 you have.
- **Test audio playback on device** — crossfade timing, volume levels, audio focus behavior (e.g., phone call interruption) can only be verified on a real device.
- **Tune volume slider defaults** — initial volume levels need to sound right through actual speakers/headphones.

### 8. Dynamic Scenario Text
- **Write the actual dynamic narrative text** — Claude Code can build the `TextResolver` engine and add placeholder tokens, but the specific text like `"The guard eyes you {rep:guard:warily|with respect}"` is creative writing that should match your voice and story tone. Review every instance.
- **Verify text reads naturally** — conditional text can produce awkward sentences. Read through on device to catch any that sound robotic or disjointed.

### 9. Save Slot System
- **Test persistence across app kills** — force-stop the app, reboot the device, clear cache, and verify saves survive. This is a device-only test.
- **Test the overwrite warning UX** — confirm the confirmation dialog is clear and doesn't let you accidentally lose a save.

### 10. Transition Animations
- **Tune all animation durations and easing curves** — 300ms vs. 500ms crossfade, linear vs. ease-in-out, stagger delays on buttons. These are visual/feel decisions that require seeing them on-screen.
- **Test performance on a mid-range device** — animations that are smooth on a fast emulator may drop frames on real hardware. Profile if needed using Android Studio's layout inspector/profiler.
- **Verify accessibility** — confirm the "disable animations" setting actually disables everything and that the app is still fully usable without them.

### 11. NPC Relationship System
- **Write NPC dialogue lines** — what the Guard Captain says at disposition 20 vs. 80 is creative content. Claude Code can scaffold the system and placeholder text, but you need to write (or at least review/edit) the actual dialogue.
- **Balance disposition thresholds** — at what disposition does the Shadow Broker reveal secrets? Requires playtesting.
- **Decide NPC portrait visuals** — if you want NPC images/avatars, you need to provide those assets.

### 12. Crafting System
- **Verify recipes make narrative sense** — does torch + cloth = signal_fire fit your world? Claude Code will implement what's in the doc but you decide if the combinations feel logical.
- **Balance recipe availability** — if a key recipe's ingredients are too hard or too easy to find, the crafting system breaks. Requires a playthrough to validate.
- **Decide recipe discovery method** — which recipes are found where? This ties into your narrative design.

### 13. Combat Mini-Game
- **Balance all combat numbers** — player HP, opponent HP, damage values, timing window size, round count. These determine whether combat is fun or frustrating. Requires extensive playtesting.
- **Tune the power meter timing** — the tap-to-set-power mechanic's speed and sweet spot need to feel satisfying on a touchscreen. Only testable on device.
- **Design opponent strategies** — what makes each of the 3+ opponent types feel different? Claude Code can implement varying AI weights but you need to validate the feel.
- **Decide loss consequences** — how harsh should losing be? Minor vs. meaningful penalties is a design decision.

### 14. Visual Map
- **Adjust all location coordinates** — plotting 12+ nodes on a 2D canvas will require visual iteration. The initial placement will almost certainly need tweaking after seeing it on-screen.
- **Test pinch-to-zoom and pan gestures** — gesture interaction feel can only be validated on a touchscreen device.
- **Provide or approve visual style** — parchment texture, node icons, fog-of-war opacity, region label fonts. Claude Code can implement a style but you need to see it and approve or direct changes.
- **Verify tap targets are large enough** — small nodes on a phone screen may be hard to hit. Needs device testing.

### Summary: The Big Recurring Themes

| Human Task | Applies To |
|---|---|
| **Playtest on a real device/emulator** | All 14 items |
| **Balance game numbers** (timing, damage, thresholds, rep values) | 1, 4, 6, 11, 13 |
| **Review/write narrative content** (dialogue, quest text, dynamic text) | 2, 6, 8, 11 |
| **Source audio asset files** (SFX — no SFX currently exist) | 7 |
| **Source image assets** (new location art, NPC portraits) | 5, 11, 14 |
| **Tune visual feel** (animation timing, map layout, UI positioning) | 1, 10, 14 |
| **Test lifecycle/persistence edge cases** on device | 1, 9 |

---

### 1. Mini-Game UIs (Lock Picking & Trading)
- Create `LockPickingScreen.kt` — full-screen Compose overlay with animated pin bars, sweeping indicator, tap-to-lock mechanic, timer bar, success/fail animations, and completion/rewards screen
- Create `TradingScreen.kt` — dialogue-style Compose screen with NPC portrait area, mood color indicator, 5 approach buttons, round counter, current/counter offer display, and deal summary screen
- Wire both screens into `MainGameScreen` (or a top-level composable) so they show when `isMiniGameActive` is true, dispatching `MiniGameInput` events to `GameViewModel.processMiniGameInput()`
- Handle edge cases in UI: timeout, back-button cancellation, all-pins-failed state
- Ensure `ActivityManager` grants rewards from mini-game results back to inventory

### 2. Quest Expansion
- Define 4 path quests (Guard, Merchant, Adventurer, Outlaw) in code/data, each with 3–4 objectives mixing scenario IDs and activity completions
- Define 3+ side quests discoverable through exploration or NPC interaction
- Add trigger logic in `QuestManager` (or `GameViewModel`) to activate path quests when the player reaches scenario 8 and picks a path
- Add trigger logic for side quests based on location visits, item possession, or activity completions
- Ensure quest objective progress updates on scenario arrival AND activity completion (not just scenario)
- Persist new quests through `PlayerProgress` save/load
- Update `CharacterMenuScreen` to display all quests with per-objective progress

### 3. Quest Rewards System
- Add a `rewards` field to the `Quest` data class (new `QuestRewards` data class with `items`, `locationsUnlocked`, `statsGranted`)
- Populate rewards on every existing and new quest definition
- Wire `QuestManager` quest-completion callback → `GameViewModel` → delegate to `InventoryManager.addItem()`, `ActivityManager` (unlock locations), and future `StatsManager`
- Create `QuestRewardPopup.kt` (or update existing) — Compose popup shown on quest completion displaying earned rewards
- Persist granted rewards in `PlayerProgress`

### 4. Character Stats
- Add `stats: Map<String, Int>` field to `PlayerProgress` (strength, charisma, cunning, wisdom — default 1 each)
- Create `StatsManager` logic class (same pattern as `InventoryManager`) exposing a `StateFlow<Map<String, Int>>`
- Add `statsGranted` field to `Decision` or `ScenarioEntity` model and update `scenarios.json` for at least 5 scenarios
- Apply path-specific stat boosts when the player commits at scenario 8
- Apply minor stat boosts on activity completion
- Extend `Condition` sealed class (or add new variant) to support stat-check gating on decisions
- Update `CharacterMenuScreen` to render stat bars
- Update decision buttons in `MainGameScreen` to show stat requirements (grayed out if unmet)
- Add stat serialization to `Converters` / `TypeConverters.kt` and ensure Room persistence
- Wire `StatsManager` into `GameViewModel` aggregation and save/load flow

### 5. Interactive Map Expansion
- Define 8+ new `InteractiveMapLocation` entries (Shadow Alley, Scholar's Retreat, Wilderness Trail, Mentor's Cottage, Sacred Temple, Cursed Ruins, Council Chamber, Criminal Hideout) with coordinates, activities, and connections
- Add location data to the database seeding logic (or however locations are currently initialized)
- Assign at least 1 `LocationActivity` per new location
- Implement region grouping and connection graph between locations
- Add discovery conditions: prerequisite locations visited, items possessed, quests completed, scenario progression
- Wire discovery/unlock logic into `ActivityManager` or a new location manager
- Update `InteractiveMapScreen` to respect discovery conditions (hide undiscovered locations)

### 6. Choice Consequences and Reputation
- Add `reputation: Map<String, Int>` field to `PlayerProgress` (guard, merchant, scholar, underworld — default 0)
- Create `ReputationManager` logic class exposing `StateFlow<Map<String, Int>>`
- Add `reputationChanges` field to `Decision` data model and update `scenarios.json` for at least 10 decisions
- Apply reputation changes in `GameViewModel.onChoiceSelected()` flow
- Gate activities and decisions by reputation thresholds (extend `Condition` or activity requirements)
- Optionally affect trading prices in `TradingGame` based on merchant reputation
- Update `CharacterMenuScreen` to display reputation standings
- Add reputation serialization to `Converters` / `TypeConverters.kt` and ensure Room persistence
- Wire `ReputationManager` into `GameViewModel` aggregation and save/load flow

### 7. Sound and Music
- Add audio asset files (`.ogg`/`.mp3`) to `res/raw/` — at least 3 background tracks + SFX for taps, item acquisition, quest completion, mini-game feedback
- Create `AudioManager` class wrapping Android `MediaPlayer` (background music) and `SoundPool` (SFX)
- Map music tracks to location/context types (town, wilderness, temple, combat, menu)
- Implement crossfade logic on scenario transitions
- Respect Android audio focus and Activity lifecycle (`onPause`/`onResume`)
- Add volume sliders (music + SFX) and mute toggle to settings UI, persisted in `SharedPreferences`
- Integrate `AudioManager` into `GameViewModel` or `MainActivity` lifecycle

### 8. Dynamic Scenario Text
- Create `TextResolver` utility class that parses placeholder tokens (`{item:X}`, `{stat:X}`, `{rep:X}`, `{if:has:X}...{/if}`)
- Update `MainGameScreen` (or wherever scenario text is displayed) to pass text through `TextResolver` before rendering
- Provide `TextResolver` with current player state (inventory, stats, reputation)
- Update `scenarios.json` — add dynamic tokens to at least 5 scenarios
- Ensure fallback text renders cleanly when items/stats/rep conditions aren't met
- Add unit tests for token parsing and edge cases (nested tokens, missing data, malformed tokens)

### 9. Save Slot System
- Add `lastSavedTimestamp: Long` and `playtimeSeconds: Long` fields to `PlayerProgress`
- Create `SaveSlotScreen.kt` — Compose screen showing 3 slots with name, location, playtime, timestamp
- Update title screen flow: New Game → pick slot (overwrite warning), Load Game → pick slot
- Add "Save Game" option to `CharacterMenuScreen` that writes to the active slot's `playerId`
- Add delete-slot with confirmation dialog
- Track playtime in `GameViewModel` (increment timer while game is active)
- Update `GameRepository` / DAO queries to support listing all save slots and loading by `playerId`

### 10. Transition Animations
- Wrap scenario text/content display in `AnimatedContent` or `Crossfade` keyed on scenario ID
- Add staggered fade/scale-in animation to decision buttons on scenario load
- Use `AnimatedVisibility` with slide transitions for map and character menu overlays
- Add a brief toast-style fade popup for item acquisition
- Crossfade between background images on scenario change
- Add an accessibility setting to disable animations (persist in `SharedPreferences`)
- Ensure animations perform smoothly (test on mid-range device/emulator)

### 11. NPC Relationship System
- Create `NPC` data class (`id`, `name`, `role`, `disposition`, `interactionHistory`, `availableDialogue`)
- Create `NPCManager` logic class exposing NPC state via `StateFlow`
- Define at least 4 named NPCs (Guard Captain, Merchant Elder, Shadow Broker, Wandering Scholar)
- Add NPC data to `PlayerProgress` persistence (new field + type converter)
- Implement disposition changes from NPC_INTERACTION activity dialogue choices
- Add disposition-threshold branching: different content/quests/prices at high vs. low disposition
- Wire `NPCManager` into `GameViewModel` aggregation and save/load flow
- Update relevant UI screens to show NPC dialogue and disposition indicators

### 12. Crafting System
- Create `CraftingRecipe` data class and `CraftingManager` logic class with a recipe registry
- Define at least 6 recipes (e.g., torch+cloth→signal_fire, holy_key+ancient_knowledge→blessed_artifact, etc.)
- Create `CraftingScreen.kt` — Compose screen accessible from character menu and CRAFTING map activities
- Show discovered vs. undiscovered recipes; gray out recipes with missing ingredients
- Implement recipe discovery logic (found through exploration, NPCs, or quest rewards)
- On craft: consume ingredients from `InventoryManager`, add result item
- Optionally gate recipes by stat requirements
- Ensure crafted items are recognized by scenario conditions and activity requirements
- Wire `CraftingManager` into `GameViewModel`

### 13. Combat Mini-Game
- Create `CombatGame` class extending `MiniGame` with `CombatState` (HP, round, opponent intent) and `CombatInput` subclasses (Attack, Defend, Special)
- Implement turn-based rock-paper-scissors core with timing-based power meter
- Integrate player stats (Strength → damage, Cunning → special power) — depends on **Item 4**
- Define at least 3 opponent types with different AI strategies
- Create `CombatScreen.kt` — split-screen Compose UI with health bars, action buttons, timing meter, hit/miss animations, victory/defeat screen
- Register `CombatGame` in `MiniGameManager`
- Wire combat rewards (items, stat boosts) and loss penalties (minor rep/stat penalty) through `ActivityManager` → `GameViewModel`
- Connect COMBAT activities on the interactive map to launch the combat mini-game

### 14. Visual Map
- Replace (or add alternative to) the `LazyColumn` list in `InteractiveMapScreen` with a Compose `Canvas`-based 2D map
- Plot location nodes at their `(coordinateX, coordinateY)` positions scaled to screen dimensions
- Draw connection lines between linked locations
- Style nodes: bright for visited, dim for unvisited-but-discovered, hidden for undiscovered
- Implement tap detection on nodes → select → show name/activity count → tap again to open detail
- Add pinch-to-zoom and pan gesture support (`transformable` modifier)
- Apply fantasy/parchment visual style, region labels, fog-of-war on undiscovered areas
- Optionally add animated travel line when moving between locations

---

## 1. Mini-Game UIs

### Current State
Two mini-games have complete game logic but no visual Compose screens:
- **Lock Picking** (`LockPickingGame.kt`): Timing-based pin mechanic with 3 difficulty tiers (2/3/4 pins, 45-75s time limits). Logic handles pin generation, sweet-spot calculation, and failure tracking.
- **Trading** (`TradingGame.kt`): 6-round negotiation with 4 NPC personalities (Greedy, Friendly, Stubborn, Balanced) and 5 player approaches (Polite, Firm, Walk Away, Flattery, Point Out Flaws). Tracks NPC mood.

`MiniGameManager` coordinates game state via `StateFlow`, and `GameViewModel` already exposes `currentMiniGame`, `currentMiniGameState`, `isMiniGameActive`, and `lastMiniGameResult` to the UI layer.

### Goal
Build playable Compose screens for both mini-games so that interactive map activities actually launch visual gameplay.

### Implementation

#### 1a. Lock Picking UI
- Full-screen overlay triggered when `isMiniGameActive` is true and current game is a `LockPickingGame`
- Visual representation of pins (vertical bars) with a moving indicator sweeping across a sweet-spot zone
- Tap-to-lock mechanic: player taps when the indicator is in the green zone to lock a pin
- Timer bar showing remaining time
- Visual feedback: pin color changes on success (green) or failure (red shake animation)
- Completion screen showing score and rewards

#### 1b. Trading UI
- Dialogue-style screen with NPC portrait area and mood indicator
- Player sees the current offer and NPC's counter-offer
- 5 approach buttons presented as dialogue choices
- Round counter (1-6) and progress indicator
- NPC mood visualized as a color gradient (hostile red to friendly green)
- Final deal summary showing profit/loss and rewards

### Acceptance Criteria
- Both mini-games are visually playable from interactive map activities
- `MiniGameInput` events are sent from UI taps to `GameViewModel.processMiniGameInput()`
- Results flow back through `ActivityManager` and grant rewards to inventory
- Games handle edge cases: timeout, cancellation (back button), all pins failed

---

## 2. Quest Expansion

### Current State
One quest exists: "The Crossroads of Fate" with 3 objectives tied to reaching scenarios 4, 5, and 8. Quest completion is automatic and grants no rewards. The `QuestManager` supports multiple active/completed quests and per-objective tracking.

### Goal
Add path-specific and side quests that give players goals beyond following the main narrative, with tangible rewards on completion.

### Implementation

#### 2a. Path Quests
Trigger when the player commits to a career path at scenario 8:
- **Guard Path**: "Sworn Protector" — complete guard training, investigate disturbances, earn guard_badge
- **Merchant Path**: "Fortune Seeker" — negotiate a trade deal, acquire merchant_seal, establish a trade route
- **Adventurer Path**: "Into the Unknown" — explore ancient ruins, decipher runes, find ancient_artifact
- **Outlaw Path**: "Shadow's Edge" — infiltrate the criminal hideout, pull off a heist, earn infernal_mark

Each path quest has 3-4 objectives mixing scenario progression and interactive map activities.

#### 2b. Side Quests
Available regardless of path, discovered through exploration or NPC interaction:
- "The Lost Torch" — find a torch (unlocks Ancient Ruins exploration activity)
- "Merchant's Favor" — complete 3 trading activities to earn merchant_seal
- "Rumors of the Past" — gather gossip at 3 different locations to unlock scholar_recommendation

#### 2c. Quest Rewards
Wire `QuestManager` completion to grant rewards:
- Inventory items (keys, tools, faction tokens)
- Location unlocks (add to `ActivityManager.unlockedLocations`)
- Stat boosts (once character stats are implemented)

### Acceptance Criteria
- At least 4 path quests and 3 side quests are defined and functional
- Quests trigger based on scenario progression or activity completion
- Completing a quest grants at least one tangible reward
- Quest progress persists across save/load cycles
- Character menu displays all quests with objective progress

---

## 3. Quest Rewards System

### Current State
Quest completion sets `isCompleted = true` but triggers no downstream effects. The infrastructure for granting items (`InventoryManager.addItem`) and unlocking locations (`ActivityManager`) exists but is not connected to quest completion.

### Goal
Make quest completion meaningful by granting rewards that unlock new content.

### Implementation
- Add a `rewards` field to the `Quest` data class: `val rewards: QuestRewards`
- `QuestRewards` contains: `items: List<String>`, `locationsUnlocked: List<String>`, `statsGranted: Map<String, Int>` (future-proofed for stats)
- When `QuestManager` completes a quest, `GameViewModel` reads the rewards and delegates to the appropriate managers
- UI shows a reward summary popup on quest completion

### Acceptance Criteria
- Completing a quest grants its defined rewards automatically
- Player sees a notification or popup showing what they earned
- Rewards are persisted in `PlayerProgress`

---

## 4. Character Stats

### Current State
Player state consists of: current scenario, inventory (set of item name strings), quests, visited locations, and completed activities. There are no numeric attributes.

### Goal
Add character attributes that evolve based on player choices and gate certain decisions or activities.

### Implementation

#### 4a. Data Model
Add to `PlayerProgress`:
```kotlin
val stats: Map<String, Int> = mapOf(
    "strength" to 1,
    "charisma" to 1,
    "cunning" to 1,
    "wisdom" to 1
)
```
Add a `StatsManager` logic class following the same pattern as `InventoryManager`.

#### 4b. Stat Changes
- Scenario decisions can grant stat points (add a `statsGranted` field to `Decision` or `ScenarioEntity`)
- Path choices give significant boosts: Guard +2 Strength, Merchant +2 Charisma, Adventurer +2 Wisdom, Outlaw +2 Cunning
- Activity completion grants minor stat boosts based on activity type

#### 4c. Stat Gating
- Decisions can require minimum stats: "You need Charisma 3 to persuade the guard"
- Extend `Condition` to support stat checks alongside item checks
- Activities can have stat requirements in addition to item requirements

#### 4d. UI
- Character menu shows stat values with visual bars
- Decision buttons show stat requirements (grayed out if unmet, with requirement text)

### Acceptance Criteria
- 4 stats tracked and persisted in PlayerProgress
- At least 5 scenarios grant stat changes based on choices
- At least 3 decisions are gated by stat requirements
- Stats display in the character menu
- Stats survive save/load cycles

---

## 5. Interactive Map Expansion

### Current State
4 locations defined (Town Square, Merchant Quarters, Guard Training Grounds, Ancient Ruins) out of a potential 46+ based on scenario locations. The `InteractiveMapLocation` entity, DAO, repository methods, and UI screen are all fully built.

### Goal
Populate the interactive map with locations that match the game's scenarios, each with thematic activities.

### Implementation

#### 5a. Location Definitions
Add locations matching existing scenario locations:
- **Shadow Alley** — Investigation, NPC Interaction (tied to Outlaw path)
- **Scholar's Retreat** — Puzzle, Exploration (tied to Adventurer/Wisdom path)
- **Wilderness Trail** — Exploration, Combat (connects to Ancient Ruins)
- **Mentor's Cottage** — NPC Interaction, Crafting (late-game reflection)
- **Sacred Temple** — Puzzle, Exploration (requires holy_key)
- **Cursed Ruins** — Combat, Investigation (requires infernal_mark)
- **Council Chamber** — NPC Interaction, Trading (endgame politics)
- **Criminal Hideout** — Minigame (lockpicking), Investigation (Outlaw path)

#### 5b. Connection Graph
Define meaningful connections between locations so the map feels like a navigable world, not a flat list. Group by region:
- **Town Center**: Town Square, Merchant Quarters, Town Hall
- **Outskirts**: Guard Training Grounds, Wilderness Trail, Mentor's Cottage
- **Hidden**: Shadow Alley, Criminal Hideout (require discovery)
- **Remote**: Ancient Ruins, Sacred Temple, Cursed Ruins (require items/discovery)

#### 5c. Discovery Conditions
Locations unlock through:
- Visiting prerequisite locations
- Possessing specific items
- Completing quests (via `unlockedLocations`)
- Reaching certain scenarios in the main story

### Acceptance Criteria
- At least 12 total interactive map locations (8 new + 4 existing)
- Each location has at least 1 activity
- Locations are grouped by region with logical connections
- Hidden locations require discovery conditions
- Activities tie into the quest and reward systems

---

## 6. Choice Consequences and Reputation

### Current State
Choices affect which scenario the player sees next and which items they receive. There is no tracking of factional alignment or NPC disposition beyond the branching path.

### Goal
Add a reputation/faction system so choices have cumulative effects beyond immediate branching.

### Implementation

#### 6a. Reputation Tracks
Add to `PlayerProgress`:
```kotlin
val reputation: Map<String, Int> = mapOf(
    "guard" to 0,
    "merchant" to 0,
    "scholar" to 0,
    "underworld" to 0
)
```
Add a `ReputationManager` logic class.

#### 6b. Reputation Effects
- Scenario choices adjust reputation (helping the guard captain: +2 guard, -1 underworld)
- Reputation gates content: high guard rep unlocks guard-exclusive activities, low rep locks them
- Trading prices affected by merchant reputation
- NPC dialogue varies based on relevant reputation

#### 6c. Reputation in Scenarios
Add an optional `reputationChanges` field to `Decision`:
```json
"reputationChanges": { "guard": 2, "underworld": -1 }
```

### Acceptance Criteria
- 4 reputation tracks persisted in PlayerProgress
- At least 10 decisions affect reputation
- At least 3 activities or decisions are gated by reputation thresholds
- Character menu displays reputation standings

---

## 7. Sound and Music

### Current State
No audio of any kind. The game is entirely silent.

### Goal
Add ambient background music and sound effects to enhance immersion.

### Implementation

#### 7a. Background Music
- Create an `AudioManager` class that wraps Android `MediaPlayer`
- Define music tracks per location type (town, wilderness, temple, combat, menu)
- Crossfade between tracks on scenario transitions
- Respect Android audio focus and lifecycle (pause on `onPause`, resume on `onResume`)

#### 7b. Sound Effects
- Button tap sounds on decision selection
- Item acquired chime
- Quest completed fanfare
- Mini-game feedback sounds (pin locked, negotiation accept/reject)

#### 7c. Settings
- Volume sliders for music and SFX (persist in SharedPreferences)
- Mute toggle accessible from character menu or title screen

### Acceptance Criteria
- At least 3 distinct background music tracks play based on location context
- Decision buttons play a tap sound
- Item acquisition and quest completion have audio feedback
- Audio respects app lifecycle (no sound when backgrounded)
- Player can adjust or mute volume

---

## 8. Dynamic Scenario Text

### Current State
Scenario text is static JSON strings. Decisions have `fallbackText` for when conditions aren't met, but narrative text doesn't reference player state.

### Goal
Make scenario text feel personalized by inserting dynamic references to player state.

### Implementation
- Define placeholder tokens in scenario text: `{item:holy_key}`, `{stat:charisma}`, `{rep:guard}`
- Add a `TextResolver` utility that replaces tokens with contextual text before display
- Examples:
  - `"The {item:holy_key} glows warmly in your pack."` (only shown if player has the item)
  - `"The guard eyes you {rep:guard:warily|with respect}."` (conditional on reputation)
- Conditional text blocks: `{if:has:torch}The torch lights your way.{/if}`

### Acceptance Criteria
- `TextResolver` parses and replaces tokens in scenario text
- At least 5 scenarios use dynamic text references
- Missing items or low stats produce appropriate fallback text
- No raw tokens are ever displayed to the player

---

## 9. Save Slot System

### Current State
One hardcoded player ID: `"default_player"`. `PlayerProgress` uses `playerId` as its primary key, so the schema already supports multiple saves.

### Goal
Let players maintain multiple save files to explore different paths.

### Implementation
- Add a `SaveSlotScreen` showing 3 save slots with: slot name, current scenario location, playtime, last saved timestamp
- Add `lastSavedTimestamp` and `playtimeSeconds` fields to `PlayerProgress`
- Title screen flow: New Game -> pick slot (overwrite warning if occupied), Load Game -> pick slot
- "Save Game" option in character menu writes to the current slot
- Delete slot option with confirmation

### Acceptance Criteria
- 3 save slots available on title screen
- Each slot shows meaningful info (location, timestamp)
- New game warns before overwriting an existing slot
- Deleting a save requires confirmation
- Loading a save restores all state (inventory, quests, stats, map progress)

---

## 10. Transition Animations

### Current State
Scenario transitions are instant — the screen content just swaps. No visual transitions between screens or on UI interactions.

### Goal
Add polish through Compose animations on key transitions.

### Implementation
- **Scenario transitions**: Crossfade or slide animation when `_currentScenario` changes (use `AnimatedContent` or `Crossfade`)
- **Decision buttons**: Scale/fade-in animation when a new scenario loads (staggered appearance)
- **Screen overlays**: Slide-in from edge for map and character menu (use `AnimatedVisibility` with `slideInHorizontally`)
- **Item acquisition**: Brief toast-style popup with fade animation when an item is added to inventory
- **Background images**: Crossfade between background images on scenario change

### Acceptance Criteria
- Scenario changes use a visible transition (not instant swap)
- Map and character menu animate in/out
- Decision buttons animate on appearance
- Animations are smooth (no frame drops on mid-range devices)
- Animations can be disabled in settings for accessibility

---

## 11. NPC Relationship System

### Current State
NPCs exist only as scenario text. The Trading game has NPC personalities but no persistence. No NPC is tracked across multiple interactions.

### Goal
Create persistent NPCs that remember past interactions and change behavior accordingly.

### Implementation

#### 11a. NPC Data Model
```kotlin
data class NPC(
    val id: String,
    val name: String,
    val role: String,
    val disposition: Int = 50,  // 0-100 scale
    val interactionHistory: List<String> = emptyList(),
    val availableDialogue: List<String> = emptyList()
)
```
Add an `NPCManager` logic class and persist NPC state in PlayerProgress.

#### 11b. Key NPCs
- **Guard Captain** — met during Guard path, disposition affects guard reputation outcomes
- **Merchant Elder** — met during Merchant path, offers better trades at high disposition
- **Shadow Broker** — met during Outlaw path, reveals secrets at high disposition
- **Wandering Scholar** — met during Adventurer path, provides lore and puzzle hints

#### 11c. Interaction Effects
- Dialogue choices in NPC_INTERACTION activities affect disposition
- High disposition: NPC offers exclusive quests, better prices, secret info
- Low disposition: NPC refuses service, warns others (reputation penalty)
- NPCs reference past interactions in dialogue

### Acceptance Criteria
- At least 4 named NPCs with persistent disposition
- NPC disposition changes based on player choices
- At least 2 NPCs offer different content based on disposition thresholds
- NPC state persists across save/load

---

## 12. Crafting System

### Current State
`ActivityType.CRAFTING` is defined in the enum but no crafting logic exists. Inventory is a flat set of string item names with no metadata.

### Goal
Let players combine inventory items to create new ones, giving items purpose beyond condition-gating.

### Implementation

#### 12a. Recipe System
```kotlin
data class CraftingRecipe(
    val id: String,
    val name: String,
    val ingredients: List<String>,
    val result: String,
    val description: String,
    val requiredStat: Pair<String, Int>? = null  // optional stat requirement
)
```
Add a `CraftingManager` with a registry of recipes.

#### 12b. Recipes
- torch + cloth = signal_fire (used in wilderness scenarios)
- holy_key + ancient_knowledge = blessed_artifact (powerful endgame item)
- merchant_seal + guard_badge = council_pass (unlocks Council Chamber)
- clues + rumors = investigation_report (unlocks hidden quest)

#### 12c. UI
- Crafting screen accessible from character menu or CRAFTING activities on the map
- Shows available recipes (discovered through exploration/NPCs)
- Grayed-out recipes show missing ingredients
- Crafting animation on success

### Acceptance Criteria
- At least 6 crafting recipes defined
- Crafting consumes ingredients and produces result item
- Recipes are discoverable (not all visible from start)
- Crafting UI shows available and locked recipes
- Crafted items are usable in scenarios and activities

---

## 13. Combat Mini-Game

### Current State
`ActivityType.COMBAT` exists and Guard Training Grounds has a "Combat Training" activity, but no combat mechanics are implemented.

### Goal
Build a turn-based or reflex-based combat mini-game.

### Implementation

#### 13a. Combat Design (Turn-Based with Timing)
- Player and opponent take turns choosing actions: Attack, Defend, Special
- Rock-paper-scissors core: Attack beats Special, Special beats Defend, Defend beats Attack
- Timing element: a power meter appears on attack — tap to set power (adds skill component)
- Stats influence combat: Strength affects damage, Cunning affects special move power
- 3-5 rounds per fight

#### 13b. Combat Game Class
- `CombatGame` extends `MiniGame` abstract class
- `CombatState` tracks HP, round, opponent intent
- `CombatInput` subclasses: `Attack`, `Defend`, `Special` with timing value

#### 13c. Combat UI
- Split screen: player on bottom, opponent on top
- Health bars for both sides
- Action buttons with timing meter
- Hit/miss animations
- Victory/defeat screen with rewards

### Acceptance Criteria
- Combat mini-game is playable from COMBAT activities on the interactive map
- At least 3 opponent types with different strategies
- Player stats (Strength, Cunning) affect combat outcomes
- Combat rewards items and experience
- Losing combat has consequences (minor stat/reputation penalty) but doesn't block progress

---

## 14. Visual Map

### Current State
Interactive map is rendered as a scrollable list (`LazyColumn`). Each `InteractiveMapLocation` has `coordinateX` and `coordinateY` float fields (0-1 range) that are defined but unused.

### Goal
Render locations on a 2D visual map with connection lines and interactive nodes.

### Implementation

#### 14a. Map Canvas
- Use Compose `Canvas` to draw a stylized map background
- Plot location nodes at their (coordinateX, coordinateY) positions scaled to screen size
- Draw connection lines between linked locations
- Visited locations shown as bright nodes, unvisited as dim/locked
- Undiscovered locations hidden entirely

#### 14b. Interaction
- Tap a node to select it (shows location name and activity count)
- Tap again or tap "View" to open the location detail view (existing `LocationDetailView`)
- Pinch-to-zoom and pan for larger maps
- Current location highlighted with a pulsing indicator

#### 14c. Visual Style
- Parchment/fantasy map aesthetic matching the game's dark theme
- Region labels for location clusters
- Fog of war effect on undiscovered areas
- Animated travel line when moving between locations

### Acceptance Criteria
- Locations render at their coordinate positions on a 2D canvas
- Connection lines are visible between linked locations
- Tapping a node opens location details
- Undiscovered locations are hidden
- Map is scrollable/zoomable if content exceeds screen

---

## Implementation Priority

Suggested order based on dependencies and impact:

| Phase | Items | Rationale |
|-------|-------|-----------|
| 1 | 4 (Stats), 6 (Reputation) | Foundation — other features depend on these |
| 2 | 2 (Quests), 3 (Quest Rewards), 5 (Map Expansion) | Content — fills the world with things to do |
| 3 | 1 (Mini-Game UIs), 13 (Combat) | Gameplay — makes activities playable |
| 4 | 8 (Dynamic Text), 11 (NPCs), 12 (Crafting) | Depth — enriches existing content |
| 5 | 10 (Animations), 7 (Sound), 14 (Visual Map) | Polish — elevates the experience |
| 6 | 9 (Save Slots) | Quality of life — useful once there's enough content to replay |
