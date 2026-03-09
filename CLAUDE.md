# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Crossroads of Fate is an Android narrative RPG built with Kotlin, Jetpack Compose, and Room database. Players make choices that shape the story through branching scenario paths.

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

Test configuration: Robolectric SDK 31, `isIncludeAndroidResources = true`, `isReturnDefaultValues = true`. Unit tests use in-memory Room databases with `Converters()` type converter added via `.addTypeConverter()`.

## Architecture

MVVM with a coordinator pattern. The key layers:

- **Model**: Room entities (`ScenarioEntity`, `PlayerProgress`) in `data/models/`
- **Repository**: `GameRepository` abstracts database + JSON asset loading
- **Logic Managers**: Single-responsibility classes that own state:
  - `InventoryManager` — player inventory (exposes `StateFlow<Set<String>>`)
  - `QuestManager` — quest tracking (exposes active/completed `StateFlow<List<Quest>>`)
  - `ActivityManager` — location-based activity completion (owns completion state — `LocationActivity` models are stateless)
  - `MiniGameManager` — mini-game coordination
- **ViewModel**: `GameViewModel` is a coordinator — it delegates to managers, aggregates state into `PlayerProgress`, and persists via repository. It does NOT contain business logic directly.
- **UI**: Compose screens observe StateFlows. No business logic in UI layer (no quest logic in `LaunchedEffect`).

**Dependency injection**: `GameViewModelFactory` injects `GameRepository` into `GameViewModel`. The ViewModel constructs its own logic managers internally.

**Data flow for player choices**: `onChoiceSelected()` → resolve branching via `LeadsTo.Simple`/`LeadsTo.Conditional` → delegate to `InventoryManager`/`QuestManager` → aggregate into `PlayerProgress` → save via `GameRepository`.

## Key Conventions

- **Scenarios** are defined in `app/src/main/assets/scenarios.json` and loaded into Room at app init
- **Background images** are resolved dynamically by name via `painterForName()` in `UiUtils.kt` — no hardcoded `when` blocks for drawable mapping
- **Type converters**: All consolidated in a single `Converters` class in `data/models/TypeConverters.kt` using `@ProvidedTypeConverter`. Registered via `.addTypeConverter(Converters())` at database level only — do not add `@TypeConverters` on individual entities
- **Database** uses `fallbackToDestructiveMigration()` — no migration files
- **Player reset** clears only `player_progress` table (via `deleteAll()`), not the entire database — scenarios stay loaded
- **Branching logic**: `LeadsTo` is a sealed class (`Simple`/`Conditional`) with a custom `LeadsToDeserializer` for Gson
- **Logging**: Use `Timber` everywhere — never `println`
- **Data models**: Each model class has its own file — `Decision.kt`, `MapLocation.kt`, `LocationActivity.kt` etc. are separate from their entity files

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

- **Unit tests** use Mockito/MockK for repository mocking and Robolectric for Android context
- **ViewModel tests** mock `GameRepository`, use `TestDispatcher` for coroutines, and run on Robolectric SDK 28
- **Logic manager tests** (`InventoryManagerTest`, `QuestManagerTest`) test business logic in isolation
- **Database integration tests** (`DatabaseIntegrationTest`) use in-memory Room databases
- Test utilities: `TestDatabaseUtil.createTestDatabase()` and `TestDataFactory` in `util/TestUtils.kt`

## Package Structure

All source under `com.spiritwisestudios.crossroadsoffate`:
- `data/` — Room database, DAOs, entities, type converters
- `repository/` — `GameRepository`
- `logic/` — `InventoryManager`, `QuestManager`, `ActivityManager`
- `viewmodel/` — `GameViewModel`, `GameViewModelFactory`
- `ui/` — Compose screens (`MainGameScreen`, `TitleScreen`, `MapScreen`, `CharacterMenuScreen`, etc.)
- `minigames/` — Mini-game framework and implementations (`LockPickingGame`, `TradingGame`)
- `util/` — `ErrorLogger`
