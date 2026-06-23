## ADDED Requirements

### Requirement: MVC Architecture Packaging
The system classes SHALL be structured into subpackages under `htl.leonding.at` based on their functional and architectural roles.

#### Scenario: Subpackage organization
- **WHEN** the project source directories are inspected
- **THEN** models reside in `model`, views and controllers in `controller`, business logic in `service`, persistence in `repository`, and helper classes in `util`

### Requirement: Robust Resource Loading
The system SHALL use absolute classpath or central class-relative paths to resolve and load all JavaFX FXML resource files.

#### Scenario: View switching without resource errors
- **WHEN** the application starts up or navigates between login, overview, and main workspace screens
- **THEN** the required FXML files are resolved and loaded correctly without throwing NullPointerException
