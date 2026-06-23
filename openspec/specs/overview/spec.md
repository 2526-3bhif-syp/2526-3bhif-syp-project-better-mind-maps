## Purpose
Provides the high-level overview, objectives, and non-functional requirements (NFA) for the Better Mind Maps application.
## Requirements
### Requirement: Usability NFA
The user interface SHALL be self-explanatory and enable a student to create a new node in a maximum of three interactive steps.

#### Scenario: Creating a node under 3 steps
- **GIVEN** the student has selected a parent node
- **WHEN** the student presses the Tab key, types the node text "Abstraction", and presses Enter
- **THEN** the new node is created and rendered successfully under 3 interactive steps

### Requirement: Performance NFA
The application SHALL load mind maps instantly and render movement of nodes in the workspace at a fluid 60 FPS without noticeable latency.

#### Scenario: Loading a large mind map quickly
- **GIVEN** a saved mind map with 100 nodes exists in the SQLite database
- **WHEN** the user selects the mind map to load it
- **THEN** the map renders on the canvas in less than 1.0 second

### Requirement: Robustness NFA
The application SHALL sanitize inputs to prevent invalid states and handle child nodes gracefully during deletions.

#### Scenario: Input validation for empty text
- **GIVEN** the workspace is active
- **WHEN** the user attempts to enter empty text or whitespace for a node
- **THEN** the application rejects the update and displays a validation error

### Requirement: Reliability NFA
The application MUST persist all state changes immediately in SQLite local database transactions.

#### Scenario: Local data persistence on change
- **GIVEN** a student is modifying a node's position on the canvas
- **WHEN** the user releases the node after dragging
- **THEN** the new coordinates are successfully committed to the local database transaction

### Requirement: Simplified Sync Labeling
The overview dashboard cards SHALL display a simplified, localized status for pending synchronization. Specifically, when a mind map's local changes have not yet been synchronized, the sync badge SHALL display the text "Sync pending" in English and "Sync ausstehend" in German.

#### Scenario: Displaying pending sync status
- **GIVEN** a mind map has local changes that are not synced to the cloud
- **WHEN** the overview dashboard is rendered
- **THEN** the map's card displays a status badge with the text "Sync pending" (English) or "Sync ausstehend" (German)

