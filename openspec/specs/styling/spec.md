## Purpose
Defines the visual design system tokens, node border/fill styles, gradients, drop shadows, active highlighting, and connecting line dimensions.
## Requirements
### Requirement: Visual Token Layout
The canvas background SHALL be #f8f9fa, and connecting lines SHALL be #adb5bd with a stroke width of 2.0 pixels drawn underneath the nodes center-to-center.

#### Scenario: Visual lines render correctly
- **GIVEN** a child node is linked to a parent
- **WHEN** the map is rendered on the canvas
- **THEN** a gray line of thickness 2.0 is drawn center-to-center underneath the nodes

### Requirement: Node Visual Style Segmentation
Root nodes SHALL render with a vertical blue gradient (#3498db to #2980b9) and bold white text, while child nodes render as solid white cards with soft borders (#b2bec3), charcoal text (#2c3e50), and a drop shadow.

#### Scenario: Normal child node appearance
- **GIVEN** a child node is rendered on the canvas
- **WHEN** the node is in its default state
- **THEN** it displays as a white card with a subtle drop shadow (radius 4.0, offset y 2.0, color #00000018) and a soft gray border

### Requirement: Selection Highlights
An active or selected node SHALL immediately highlight with a prominent violet border (`#8b5cf6`) of thickness 2.0 pixels and a hard drop shadow offset below the node (e.g., `DropShadow(radius=0, offsetY=4, color=#8b5cf6)`) to ensure visibility regardless of the node's fill color.

#### Scenario: Highlighting a selected node
- **GIVEN** a node is currently selected
- **WHEN** the node is drawn or selected
- **THEN** its border stroke transitions to `#8b5cf6` with a stroke width of 2.0 pixels and it renders with a hard drop shadow downward

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

