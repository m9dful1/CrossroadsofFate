# Crossroads of Fate

A narrative-driven RPG where choices shape your destiny, built with Android, Jetpack Compose, and Room database.

## Project Documentation

This repository includes a comprehensive Design Document that serves as the authoritative reference for all aspects of the game architecture, implementation, and modification guidelines.

## Using the Design Document

The `Design_Document.md` file is the central reference for understanding and modifying the Crossroads of Fate game. It contains:

- Complete architectural overview
- Data model specifications
- UI component descriptions
- State management patterns
- Game content structure
- Asset management details
- Modification guidelines for AI models and developers

### When to Use the Design Document

1. **Before Making Changes**: Always review the relevant sections of the Design Document before modifying code to understand the existing architecture and patterns.

2. **When Adding Features**: Follow the guidelines in the "AI Model Instructions" section for specific modification types.

3. **After Making Changes**: Update the Design Document using the specified update format to reflect any modifications you've made.

4. **When Onboarding**: New developers should read the entire Design Document to understand the codebase structure and design decisions.

### Design Document Sections

- **Game Architecture**: Overview of MVVM pattern and component relationships
- **Data Models**: Database entities and relationships
- **UI Components**: Screen hierarchy and Composable components
- **State Management**: StateFlow objects and reactive patterns
- **Game Content**: Scenario structure and quest system
- **Asset Management**: Image resources and content organization
- **Database Structure**: Room database configuration
- **Error Handling**: Logging and exception management
- **AI Model Instructions**: Guidelines for making changes to the game
- **Testing Guidelines**: Approaches for testing different aspects of the game
- **Change History**: Record of document updates

### Keeping the Design Document Updated

After making changes to the code, update the Design Document following the specified format:

```
## [Section Number]. [Section Name]

[Date of modification: YYYY-MM-DD]
[Description of changes]

### [Subsection Number].[Subsection Name]
[New or updated content]
```

## Development Setup

1. Clone the repository
2. Open the project in Android Studio
3. Review the Design Document before making any changes
4. Follow the architecture patterns established in the codebase

## License

[Include license information here] 