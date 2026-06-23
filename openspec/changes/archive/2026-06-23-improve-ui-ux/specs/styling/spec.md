## ADDED Requirements

### Requirement: HUD Quick Shape Selector
The floating Node Styling HUD panel SHALL display a grid of the 8 standard node shapes using their visual symbols. Clicking a shape button in this grid MUST immediately update the selected node's shape and redraw the canvas.

#### Scenario: User changes node shape via HUD
- **GIVEN** a node is selected and the HUD panel is visible
- **WHEN** the user clicks the "Pill" shape button (`⬬`) in the HUD shape grid
- **THEN** the node's shape is updated to `PILL` and the canvas is immediately redrawn with the new shape

### Requirement: HUD Wrapping Color Grid
The HUD color swatches SHALL be organized in a wrapping grid layout. The colors MUST NOT overflow the horizontal bounds of the HUD panel.

#### Scenario: Rendering HUD color grid
- **GIVEN** the HUD panel is displayed for a selected node
- **WHEN** the 25 preset color swatches are rendered
- **THEN** they wrap automatically into multiple rows to fit inside the panel's content area
