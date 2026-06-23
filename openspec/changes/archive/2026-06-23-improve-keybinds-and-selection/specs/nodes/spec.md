## ADDED Requirements

### Requirement: Node Creation Keybinds
The system SHALL support creating nodes via keyboard shortcuts relative to the currently selected node. Pressing `Enter` (or `Insert`) SHALL create a new sibling node (a child of the selected node's parent). Pressing `Tab` SHALL create a new child node under the selected node.

#### Scenario: Add sibling node via Enter
- **WHEN** user selects a node and presses `Enter`
- **THEN** a prompt appears to add a new sibling node to the selected node

#### Scenario: Add child node via Tab
- **WHEN** user selects a node and presses `Tab`
- **THEN** a prompt appears to add a new child node to the selected node

### Requirement: Node Quick Edit Keybinds
The system SHALL provide keyboard and mouse shortcuts for rapid editing. Double-clicking a node SHALL open the inline title editor. Pressing `Spacebar` while a node is selected SHALL open the description editor.

#### Scenario: Double-click to rename
- **WHEN** user double-clicks a node
- **THEN** the inline title edit dialog is presented

#### Scenario: Spacebar for description
- **WHEN** user presses `Spacebar` on a selected node
- **THEN** the description edit dialog is presented

### Requirement: Deletion Confirmation
The system SHALL require user confirmation before deleting a node. The system SHALL listen to both the `Delete` and `Backspace` keys to trigger the deletion workflow.

#### Scenario: Press Backspace to delete
- **WHEN** user selects a node and presses `Backspace` (or `Delete`)
- **THEN** an alert dialog asks for confirmation before the node and its children are removed

## MODIFIED Requirements
### Requirement: Node Hierarchy and Creation
Every node other than the single root node MUST have a valid parent node. Deleting a parent node SHALL recursively delete all its descendants.

#### Scenario: Recursive deletion of child nodes
- **GIVEN** a node hierarchy Root -> NodeA -> NodeB
- **WHEN** the user deletes NodeA and confirms the deletion
- **THEN** both NodeA and NodeB are recursively deleted from the canvas and SQLite database

#### Scenario: Prevent deleting root node
- **GIVEN** a mind map is active
- **WHEN** the user attempts to delete the root node
- **THEN** the application blocks deletion and throws an exception
