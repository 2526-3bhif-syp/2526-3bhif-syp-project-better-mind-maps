## Purpose
Defines zooming, panning, keyboard hotkeys, and focus traversal within the mind map workspace.

## Requirements
### Requirement: Workspace Scaling and Panning
Scroll-wheeling on the viewport SHALL scale the canvas up or down (clamped between 0.2 and 5.0) centered on the mouse position. Clicking and dragging on empty space SHALL pan the canvas smoothly at 60 FPS.

#### Scenario: Panning the visual workspace
- **GIVEN** the workspace is open
- **WHEN** the user presses the primary mouse button on the background and drags it by 100 pixels horizontally
- **THEN** the canvas translateX shifts smoothly by 100 pixels

#### Scenario: Zooming the visual workspace
- **GIVEN** the workspace scale is 1.0
- **WHEN** the user scrolls the mouse wheel upwards
- **THEN** the scale factor increases to 1.1 centered on the cursor position

### Requirement: Directional Keyboard Focus Traversal
Directional keyboard keys (LEFT, RIGHT, UP, DOWN, TAB) SHALL shift focus dynamically across nodes.

#### Scenario: Sequential focus traversal using Tab
- **GIVEN** a mind map has multiple nodes
- **WHEN** the user presses the TAB key
- **THEN** the selection focus advances to the next node in the global node hierarchy
