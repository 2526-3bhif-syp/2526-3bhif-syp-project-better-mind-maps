## MODIFIED Requirements

### Requirement: Selection Highlights
An active or selected node SHALL immediately highlight with a prominent violet border (`#8b5cf6`) of thickness 2.0 pixels and a hard drop shadow offset below the node (e.g., `DropShadow(radius=0, offsetY=4, color=#8b5cf6)`) to ensure visibility regardless of the node's fill color.

#### Scenario: Highlighting a selected node
- **GIVEN** a node is currently selected
- **WHEN** the node is drawn or selected
- **THEN** its border stroke transitions to `#8b5cf6` with a stroke width of 2.0 pixels and it renders with a hard drop shadow downward
