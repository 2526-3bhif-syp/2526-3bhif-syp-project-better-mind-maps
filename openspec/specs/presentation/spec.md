## Purpose
Defines fullscreen presentation mode, distraction-free visual theme controls, auto-centering panning, and sequential node-by-node keyboard slideshow traversal.

## Requirements
### Requirement: Fullscreen Presentation Trigger
The system SHALL support maximizing the workspace into fullscreen, hiding all sidebar controls, tree views, and non-essential toolbars.

#### Scenario: Entering presentation mode successfully
- **GIVEN** a professor is viewing a mind map in the standard workspace
- **WHEN** the professor clicks the Present button
- **THEN** the workspace transitions to fullscreen and all sidebar trees are hidden

### Requirement: Distraction-Free Focal Dimming
The system SHALL highlight the currently active node and dim all other nodes in the mind map to 30% opacity.

#### Scenario: Focal branch dimming
- **GIVEN** a node "Data Structures" is selected during a presentation
- **WHEN** the node becomes active
- **THEN** the selected node is highlighted at 100% opacity, and all other nodes dim to 30% opacity

### Requirement: Slide-based Traversal and Auto-Centering
The presenter SHALL be able to traverse nodes sequentially using direction keys or Space/Backspace, and the canvas SHALL auto-pan and auto-zoom to keep the active node centered.

#### Scenario: Advancing focus sequentially
- **GIVEN** the presenter is showing the mind map
- **WHEN** the presenter presses the SPACEBAR
- **THEN** the active highlight moves to the next logical child node and the canvas auto-pans to center it
