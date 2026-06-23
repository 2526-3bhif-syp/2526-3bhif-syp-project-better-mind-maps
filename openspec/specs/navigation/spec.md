## Purpose
Defines zooming, panning, keyboard hotkeys, and focus traversal within the mind map workspace.
## Requirements
### Requirement: Workspace Scaling and Panning
Scroll-wheeling on the viewport or performing a pinch-to-zoom gesture on the trackpad/touchscreen SHALL scale the canvas up or down (clamped between 0.2 and 5.0) centered on the cursor position. Clicking and dragging on empty space SHALL pan the canvas smoothly at 60 FPS.

#### Scenario: Panning the visual workspace
- **GIVEN** the workspace is open
- **WHEN** the user presses the primary mouse button on the background and drags it by 100 pixels horizontally
- **THEN** the canvas translateX shifts smoothly by 100 pixels

#### Scenario: Zooming the visual workspace using scroll wheel
- **GIVEN** the workspace scale is 1.0
- **WHEN** the user scrolls the mouse wheel upwards
- **THEN** the scale factor increases to 1.1 centered on the cursor position

#### Scenario: Zooming the visual workspace using pinch-to-zoom gesture
- **GIVEN** the workspace scale is 1.0
- **WHEN** the user performs a pinch gesture (zoom-in) on the viewport with a zoom factor of 1.2
- **THEN** the scale factor increases to 1.2 centered on the gesture cursor position

### Requirement: Directional Keyboard Focus Traversal
Directional keyboard keys (LEFT, RIGHT, UP, DOWN, TAB) SHALL shift focus dynamically across nodes.

#### Scenario: Sequential focus traversal using Tab
- **GIVEN** a mind map has multiple nodes
- **WHEN** the user presses the TAB key
- **THEN** the selection focus advances to the next node in the global node hierarchy

### Requirement: Statusbar Shortcut Key Information
The bottom statusbar of the mind map editor SHALL display current shortcut labels and their descriptions matching the key mapping:
- `Tab` ➔ Add child
- `Enter` ➔ Add sibling
- `Space` ➔ Description
- `F2 / Dbl-Click` ➔ Edit
- `Del` ➔ Delete
- `↑ ↓ ← →` ➔ Navigate
- `Scroll` ➔ Zoom
- `Ctrl+Z` ➔ Undo
- `Ctrl+Y` ➔ Redo

#### Scenario: Displaying statusbar shortcuts
- **GIVEN** the mind map workspace is open
- **WHEN** the user looks at the bottom statusbar
- **THEN** the shortcut key labels show "Tab", "Enter", "Space", "F2 / Dbl-Click", "Del", "Scroll", "Ctrl+Z", "Ctrl+Y", and the arrow keys, with corresponding localized description texts.

