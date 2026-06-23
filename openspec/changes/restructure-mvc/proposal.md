## Why

Currently, all Java classes (models, controllers, repositories, services, utilities) are located unsorted in the root package `htl.leonding.at`. This violates the Model-View-Controller (MVC) pattern, leading to high coupling, poor codebase scannability, and difficulty maintaining or scaling the project. Implementing a structured packaging system will establish a clean separation of concerns.

## What Changes

- Move data models (`MindMap`, `Node`) to the `htl.leonding.at.model` package.
- Move UI controllers and managers (`MainController`, `OverviewController`, `AiChatController`, `NodeStyleEditor`, `TutorialManager`) to the `htl.leonding.at.controller` package.
- Move service classes (`MindMapService`, `SyncService`, `LocalSimulatedSyncService`) to the `htl.leonding.at.service` package.
- Move repositories and database logic (`MindMapRepository`, `DatabaseManager`) to the `htl.leonding.at.repository` package.
- Move cross-cutting utility classes (`JwtUtil`, `PasswordHasher`, `LanguageManager`, `SessionManager`, `WindowsDarkMode`) to the `htl.leonding.at.util` package.
- Update Java package declarations and imports in all moved files.
- Update the `fx:controller` attributes in all FXML files (`login-view.fxml`, `overview-view.fxml`, `main-view.fxml`, `ai-chat-view.fxml`) to reflect the new package path for the controllers.
- Update JavaFX FXML loading logic to use absolute classpath resource resolving to avoid `NullPointerException` errors.

## Capabilities

### New Capabilities
- `mvc-packaging`: Restructuring the project package layout according to the MVC pattern.

### Modified Capabilities
- None

## Impact

- All Java classes except the main entry class `App.java` will be relocated to respective subpackages.
- Git history for relocated classes will be preserved using move commands.
- No changes to dependencies in `pom.xml`.
- Minimal impact on app logic (pure refactoring).
