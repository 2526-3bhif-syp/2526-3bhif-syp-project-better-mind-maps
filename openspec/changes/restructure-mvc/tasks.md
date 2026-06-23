## 1. Package Directories & Relocating Files

- [x] 1.1 Create subdirectories under `htl/leonding/at`: `model`, `controller`, `service`, `repository`, `util`
- [x] 1.2 Move `MindMap.java` and `Node.java` to the `model` package
- [x] 1.3 Move `MainController.java`, `OverviewController.java`, `AiChatController.java`, `NodeStyleEditor.java`, and `TutorialManager.java` to the `controller` package
- [x] 1.4 Move `MindMapService.java`, `SyncService.java`, and `LocalSimulatedSyncService.java` to the `service` package
- [x] 1.5 Move `MindMapRepository.java` and `DatabaseManager.java` to the `repository` package
- [x] 1.6 Move `JwtUtil.java`, `PasswordHasher.java`, `LanguageManager.java`, `SessionManager.java`, and `WindowsDarkMode.java` to the `util` package

## 2. Refactoring Java Code (Imports & Declarations)

- [x] 2.1 Update package declarations at the top of all moved Java files to match their new package names
- [x] 2.2 Update all import statements in the relocated Java files and in `App.java` to resolve cross-package references
- [x] 2.3 Refactor resource loading paths in `App.java`, `LoginController.java`, `OverviewController.java`, and `MainController.java` to use absolute classpath resolution (e.g., using `/htl/leonding/at/` or loading via `App.class.getResource(...)`)

## 3. FXML Controller Bindings

- [x] 3.1 Update the `fx:controller` reference in `login-view.fxml` to `htl.leonding.at.controller.LoginController`
- [x] 3.2 Update the `fx:controller` reference in `overview-view.fxml` to `htl.leonding.at.controller.OverviewController`
- [x] 3.3 Update the `fx:controller` reference in `main-view.fxml` to `htl.leonding.at.controller.MainController`
- [x] 3.4 Update the `fx:controller` reference in `ai-chat-view.fxml` to `htl.leonding.at.controller.AiChatController`

## 4. Verification & Testing

- [ ] 4.1 Run Maven clean compilation to verify the application builds without package or dependency errors
- [ ] 4.2 Start the application and perform a full integration test (Login, Dashboard navigation, opening a MindMap, styling nodes, using the AI panel) to verify no runtime `NullPointerException`s are thrown by resource resolution
