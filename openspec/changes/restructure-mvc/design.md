## Context

The Better Mind Maps application is a JavaFX program using SQLite for database persistence. Currently, almost all classes are located directly in the root package `htl.leonding.at`. The resource FXML files are located in `src/main/resources/htl/leonding/at`. Moving classes into technical layer subpackages is necessary to improve maintainability and follow MVC best practices.

## Goals / Non-Goals

**Goals:**
- Package classes into logical MVC/architectural layers: `model`, `controller`, `service`, `repository`, and `util`.
- Update all packages and import statements across all source files.
- Update FXML files to bind to the relocated controllers.
- Adjust resource loading paths to ensure they resolve correctly at runtime.
- Maintain identical functional behavior and UI.

**Non-Goals:**
- Clean up or refactor the code inside classes (e.g., leaving the "God Class" `MainController` as is).
- Introduce new features, APIs, or database migrations.
- Change project dependencies in `pom.xml`.

## Decisions

### 1. Choice of Package Strategy: Technical Layers (Option A)
- **Rationale**: For an application of this scale, structuring packages by technical layer (e.g., `model`, `controller`, `service`, `repository`, `util`) makes it very easy to locate classes by their technical role. Feature-based packaging is not yet necessary and would split the core `mindmap` classes into a single giant folder anyway.
- **Alternatives considered**: Feature-based packaging (rejected due to small size of the codebase).

### 2. Absolute Classpath / App-Relative Resource Loading
- **Rationale**: Currently, FXML controllers load other FXML resources using `getClass().getResource("view.fxml")`. This works when all classes are in the same package as the FXML files. However, when controllers are moved to `htl.leonding.at.controller` while FXML files stay in `htl.leonding.at`, relative paths will fail. We will use absolute classpath strings (`/htl/leonding/at/overview-view.fxml`) or load relative to `App.class` to prevent runtime `NullPointerException`s.
- **Alternatives considered**: Moving FXML files into matching `controller` directories in resources (rejected to avoid cluttering resource paths and keeping FXML structure clean).

## Risks / Trade-offs

- **Risk: JavaFX FXML Loading Failures** → *Mitigation*: Update all `fx:controller` attributes in FXML files to `htl.leonding.at.controller.<ControllerName>`. Change resource loading in `App.java`, `LoginController.java`, `OverviewController.java`, and `MainController.java` to absolute paths or reference them relative to `App.class`.
- **Risk: Database Connection String or Path Changes** → *Mitigation*: Database configurations in `DatabaseManager` do not depend on the class package, but we must verify that JDBC driver loading and classpath resource lookups remain intact after package relocation.
