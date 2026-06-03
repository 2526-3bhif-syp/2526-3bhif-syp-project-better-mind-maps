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
An active or selected node SHALL immediately highlight with a prominent red border (#e74c3c) of thickness 3.0 pixels.

#### Scenario: Highlighting a selected node
- **GIVEN** a node is currently selected
- **WHEN** the node is drawn or selected
- **THEN** its border stroke transitions to #e74c3c with a stroke width of 3.0 pixels
