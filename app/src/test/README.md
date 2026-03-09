# Crossroads of Fate - Test Suite

This directory contains the comprehensive test suite for the Crossroads of Fate game.

## Test Structure

The tests are organized into several categories:

```
app/
└── src/
    ├── test/                          # Unit tests (JVM)
    │   └── java/com/spiritwisestudios/crossroadsoffate/
    │       ├── viewmodel/             # ViewModel tests
    │       ├── repository/            # Repository tests
    │       ├── data/                  # Data model tests
    │       └── util/                  # Utility tests
    └── androidTest/                   # Instrumented tests (Android)
        └── java/com/spiritwisestudios/crossroadsoffate/
            ├── ui/                    # UI component tests
            ├── db/                    # Database tests
            └── e2e/                   # End-to-end tests
```

## Running the Tests

### Unit Tests

Run all unit tests with:

```bash
./gradlew test
```

Run a specific test class:

```bash
./gradlew test --tests "com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModelTest"
```

### Instrumented Tests

Run all instrumented tests with:

```bash
./gradlew connectedAndroidTest
```

Run a specific instrumented test class:

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.spiritwisestudios.crossroadsoffate.ui.MainGameScreenTest
```

## Test Categories

### ViewModel Tests

Tests the game's business logic in isolation:
- Scenario loading and transitions
- Player decision handling
- Inventory management
- Quest progression

### Repository Tests

Tests data operations:
- Database CRUD operations
- JSON parsing
- Game state persistence

### Data Model Tests

Tests data model and serialization:
- Entity relationships
- JSON serialization/deserialization
- Type converters

### UI Tests

Tests the user interface:
- Screen rendering
- User interactions
- Navigation between screens

### Database Integration Tests

Tests database operations in an integrated environment:
- Saving and loading scenarios
- Player progress persistence
- Database migrations

### End-to-End Tests

Tests complete game flows:
- Game progression through multiple scenarios
- Quest completion
- Inventory item usage affecting gameplay

## Common Testing Patterns

1. **Arrange-Act-Assert**: All tests follow this pattern for clarity
2. **Dependency Injection**: Mock dependencies to isolate the component being tested
3. **Test Helpers**: Utility methods to reduce boilerplate in tests

## Test Coverage

To generate test coverage reports:

```bash
./gradlew jacocoTestReport
```

View the generated report at: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Adding New Tests

When adding new features to the game, follow these guidelines for adding tests:

1. Write tests before implementing the feature (TDD approach)
2. Ensure each test focuses on a single aspect of functionality
3. Use descriptive test names that clearly indicate what is being tested
4. Follow the existing patterns and organization

## Continuous Integration

All tests are automatically run in CI on:
- Pull requests to the main branch
- Merges to the main branch

The CI pipeline will report test failures and code coverage metrics. 