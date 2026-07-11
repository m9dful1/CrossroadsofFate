# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Crossroads of Fate is an Android narrative RPG built with Kotlin, Jetpack Compose, and Room database. Players make choices that shape the story through branching scenario paths, character stats, faction reputation, and mini-games.

## Build & Test Commands

```bash
# Build the project
./gradlew assembleDebug

# Run all unit tests (Robolectric-based, no device needed)
./gradlew testDebugUnitTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModelTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Clean build
./gradlew clean assembleDebug
```

Test configuration: `isIncludeAndroidResources = true`, `isReturnDefaultValues = true`. Robolectric SDK is set per test class via `@Config` (28 for ViewModel/audio tests, 31 for repository tests). Unit tests use in-memory Room databases with `Converters()` type converter added via `.addTypeConverter()`.

## Architecture

MVVM with a coordinator pattern. The key layers:

- **Model**: Room entities (`ScenarioEntity`, `PlayerProgress`, `InteractiveMapLocation`) in `data/models/`
- **Repository**: `GameRepository` abstracts database + JSON asset loading
- **Logic Managers** (`logic/`): Single-responsibility classes that own state, each exposing StateFlows:
  - `InventoryManager` — player inventory (`StateFlow<Set<String>>`)
  - `QuestManager` — quest tracking (active/completed `StateFlow<List<Quest>>`)
  - `StatsManager` — character stats (strength, charisma, cunning, wisdom)
  - `ReputationManager` — faction reputation (guard, merchant, scholar, underworld)
  - `ActivityManager` — location-based activity completion and location unlocks (owns completion state — `LocationActivity` models are stateless)
  - `MiniGameManager` — mini-game registry and active-game state
  - `GameAudioManager` — music/SFX playback; volume and mute settings persisted in SharedPreferences; per-location music via `getMusicTrackForLocation()`
  - `TextResolver` — stateless object that resolves dynamic placeholder tokens in scenario text against inventory/stats/reputation
- **ViewModel**: `GameViewModel` is a coordinator — it delegates to managers, aggregates state into `PlayerProgress`, and persists via repository. It does NOT contain business logic directly.
- **UI**: Compose screens observe StateFlows. No business logic in UI layer (no quest logic in `LaunchedEffect`).

**Dependency injection**: `GameViewModelFactory` injects `GameRepository` into `GameViewModel`. The ViewModel constructs its own logic managers internally.

**Data flow for player choices**: `onChoiceSelected()` → resolve branching via `LeadsTo.Simple`/`LeadsTo.Conditional` → delegate item/quest effects to `InventoryManager`/`QuestManager`, apply per-choice `statsGranted`/`reputationChanges` via `StatsManager`/`ReputationManager` → aggregate into `PlayerProgress` → save via `GameRepository`.

**Mini-game framework** (`minigames/`): `MiniGameFramework.kt` defines the abstract `MiniGame` contract (`initialize()` / `processInput()` / `checkCompletion()` / `getProgress()`) plus `MiniGameState`, `MiniGameInput`, and `MiniGameResult`. Concrete games (`LockPickingGame`, `TradingGame` in `minigames/games/`) are registered with `MiniGameManager`. UI lives in `ui/minigames/` — `MiniGameOverlay` dispatches to the per-game screen composables.

## Key Conventions

- **Scenarios** are defined in `app/src/main/assets/scenarios.json` and loaded into Room at app init. `docs/scenario-authoring-guide.md` is the authoritative reference for the JSON format: decision conditions (item/stat/reputation-gated), `statsGranted`, `reputationChanges`, and dynamic text tokens
- **Background images** are resolved dynamically by name via `painterForName()` in `UiUtils.kt` — no hardcoded `when` blocks for drawable mapping
- **Type converters**: All consolidated in a single `Converters` class in `data/models/TypeConverters.kt` using `@ProvidedTypeConverter`. Registered via `.addTypeConverter(Converters())` at database level only — do not add `@TypeConverters` on individual entities
- **Database** uses `fallbackToDestructiveMigration()` — no migration files; just bump the version in `GameDatabase` when the schema changes
- **Player reset** clears only `player_progress` table (via `deleteAll()`), not the entire database — scenarios stay loaded
- **Branching logic**: `LeadsTo` is a sealed class (`Simple`/`Conditional`) with a custom `LeadsToDeserializer` for Gson
- **Logging**: Use `Timber` everywhere — never `println`
- **Data models**: Each model class has its own file — `Decision.kt`, `MapLocation.kt`, `LocationActivity.kt` etc. are separate from their entity files
- **Debug tooling**: `DebugMenuScreen` exposes state manipulation via `debug*` methods on `GameViewModel` (e.g. `debugGetAllScenarioIds`)

## Design Document Rule

When modifying code, you MUST update `Design_Document.md` to reflect changes using this format:
```
## [Section Number]. [Section Name]
[Date of modification: YYYY-MM-DD]
[Description of changes]
### [Subsection Number].[Subsection Name]
[New or updated content]
```

## Testing Patterns

- **Unit tests** use Mockito for repository mocking and Robolectric for Android context
- **ViewModel tests** mock `GameRepository` and use `TestDispatcher` for coroutines
- **Logic manager tests** (`InventoryManagerTest`, `QuestManagerTest`, `StatsManagerTest`, `ReputationManagerTest`, `TextResolverTest`, `GameAudioManagerTest`) test business logic in isolation
- **Database integration tests** (`DatabaseIntegrationTest`, instrumented) use in-memory Room databases
- Test utilities: `TestDatabaseUtil.createTestDatabase()` and `TestDataFactory` in `util/TestUtils.kt`

## Package Structure

All source under `com.spiritwisestudios.crossroadsoffate`:
- `data/` — Room database, DAOs, entities, type converters
- `repository/` — `GameRepository`
- `logic/` — the logic managers listed above
- `viewmodel/` — `GameViewModel`, `GameViewModelFactory`
- `ui/` — Compose screens (`MainGameScreen`, `TitleScreen`, `MapScreen`, `InteractiveMapScreen`, `CharacterMenuScreen`, `DebugMenuScreen`, etc.); `ui/minigames/` for mini-game screens
- `minigames/` — mini-game framework; `minigames/games/` for implementations
- `util/` — `ErrorLogger`
