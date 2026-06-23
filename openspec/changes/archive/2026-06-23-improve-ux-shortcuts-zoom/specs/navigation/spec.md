## ADDED Requirements

*None*

## MODIFIED Requirements

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
