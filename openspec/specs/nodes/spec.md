## Purpose
Defines node creation, parent-child relations, inline text editing, visual dragging, recursive deletion, and collision-avoidance logic.

## Requirements
### Requirement: Node Hierarchy and Creation
Every node other than the single root node MUST have a valid parent node. Deleting a parent node SHALL recursively delete all its descendants.

#### Scenario: Recursive deletion of child nodes
- **GIVEN** a node hierarchy Root -> NodeA -> NodeB
- **WHEN** the user deletes NodeA
- **THEN** both NodeA and NodeB are recursively deleted from the canvas and SQLite database

#### Scenario: Prevent deleting root node
- **GIVEN** a mind map is active
- **WHEN** the user attempts to delete the root node
- **THEN** the application blocks deletion and throws an exception

### Requirement: Collision-Free Placement
The system SHALL place new child nodes automatically using a concentric angle-based algorithm that avoids coordinate overlapping.

#### Scenario: Auto placement under collision
- **GIVEN** a parent node has occupied coordinates in its immediate radius
- **WHEN** the user adds a new child node to this parent
- **THEN** the system iterates angles and radius rings until a collision-free coordinate is calculated and placed

### Requirement: Interactive Node Dragging
Users MUST be able to click and drag nodes freely, which updates connecting lines in real-time and persists new coordinates on release.

#### Scenario: Dragging a node to a new coordinate
- **GIVEN** the workspace canvas is visible
- **WHEN** the user drags a node from coordinate (100, 100) to (200, 300) and releases the mouse
- **THEN** the node's position updates to (200, 300) in the SQLite database and all connecting lines are dynamically redrawn
