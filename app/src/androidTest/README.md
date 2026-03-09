# Crossroads of Fate - Instrumented Tests

This directory contains the Android instrumented tests for the Crossroads of Fate game. These tests run on actual Android devices or emulators and test the application in a real environment.

## Instrumented Test Categories

### UI Tests (`ui/`)

Tests the user interface components using Jetpack Compose testing tools:
- `MainGameScreenTest`: Tests the main game screen UI and interactions
- More UI tests will be added as the game evolves

### Database Tests (`db/`)

Tests the Room database operations in a real Android environment:
- `DatabaseIntegrationTest`: Tests saving and loading game data in the actual SQLite database

### End-to-End Tests (`e2e/`)

Tests complete game flows from start to finish:
- `GameFlowTest`: Tests navigating through the game's storyline
- Tests quest completion and game state changes during play

## Running the Tests

### Prerequisites

- Android device or emulator
- Android SDK properly configured
- USB debugging enabled on physical devices

### Running All Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Running Specific Tests

To run a specific test class:

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.spiritwisestudios.crossroadsoffate.ui.MainGameScreenTest
```

To run a specific test method:

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.spiritwisestudios.crossroadsoffate.e2e.GameFlowTest#startNewGameFlow_reachesFirstObjective
```

## Testing Best Practices

1. **UI Testing**:
   - Use semantic matchers (like `onNodeWithText`, `onNodeWithContentDescription`)
   - Implement proper waiting mechanisms for asynchronous UI updates
   - Test edge cases like screen rotations

2. **Database Testing**:
   - Test database migrations when schema changes
   - Verify data integrity after operations
   - Test edge cases like database corruption recovery

3. **End-to-End Testing**:
   - Test complete user flows
   - Verify game state is preserved correctly
   - Test handling of unexpected user interactions

## Test Reports

After running tests, reports are available at:
- HTML: `app/build/reports/androidTests/connected/`
- XML: `app/build/outputs/androidTest-results/`

## Adding New Tests

When adding new tests:
1. Follow the existing package structure
2. Use descriptive test method names (format: `methodUnderTest_condition_expectedResult`)
3. Add proper documentation explaining test purpose
4. Keep tests independent and idempotent 