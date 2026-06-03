## Purpose
Maps all ten user stories of Better Mind Maps (creation, editing, accounts, management, presentation, zoom, arrange, connect, style, open) into Gherkin BDD scenarios.

## Requirements
### Requirement: User Stories Verification Suite
The application SHALL satisfy the functional expectations defined by all 10 core user stories verified via testable BDD scenarios.

#### Scenario: Verify US-1 Mind map creation
- **GIVEN** the student is on the main landing screen
- **WHEN** the student enters "Design Patterns" and clicks "New Mind Map"
- **THEN** a new mind map with a root node is successfully generated in SQLite

#### Scenario: Verify US-2 Mind map editing
- **GIVEN** a mind map is open and "Structural Patterns" is selected
- **WHEN** the student presses Enter and inputs "Adapter"
- **THEN** a child node "Adapter" is created and linked to "Structural Patterns" in the database

#### Scenario: Verify US-3 Account registration
- **GIVEN** a new student wants to register
- **WHEN** the student provides a verified HTL Leonding email and password
- **THEN** a user account is created and their data is local-segmented

#### Scenario: Verify US-4 Mind map deletion
- **GIVEN** a student is on the overview dashboard
- **WHEN** the student clicks Delete on the mind map "Compilers"
- **THEN** the map and all its child nodes are cascadingly deleted from the database

#### Scenario: Verify US-5 Presentation Mode
- **GIVEN** a professor has loaded a mind map
- **WHEN** the professor triggers Presentation Mode
- **THEN** the application goes fullscreen and dims non-focused nodes to 30% opacity

#### Scenario: Verify US-6 Zooming
- **GIVEN** the visual workspace is active
- **WHEN** the user scrolls the mouse wheel upward
- **THEN** the workspace canvas scale factor increases centered on the mouse position

#### Scenario: Verify US-7 Arranging Nodes
- **GIVEN** a node is placed at coordinates (300, 200)
- **WHEN** the student drags the node to coordinate (150, 200) and releases
- **THEN** the new coordinate (150, 200) is saved to SQLite

#### Scenario: Verify US-8 Connect Nodes
- **GIVEN** child nodes are linked to a parent
- **WHEN** the map is rendered
- **THEN** connecting lines are drawn center-to-center behind the nodes

#### Scenario: Verify US-9 Style Nodes
- **GIVEN** a node is selected
- **WHEN** the student highlights the node color to Red
- **THEN** the node card's border transitions to highlight red #e74c3c

#### Scenario: Verify US-10 Open Mind Map
- **GIVEN** the dashboard displays saved maps
- **WHEN** the student double-clicks the "Algebra II" card
- **THEN** the application fetches nodes from SQLite and loads the active editor workspace in under 1.0 second
