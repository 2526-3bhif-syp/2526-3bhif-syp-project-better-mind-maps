## Context

The current user interaction has a few gaps in usability:
1. Keyboard interaction is limited: the login form requires clicking the "Sign In" button.
2. Navigation on trackpads is limited: zooming only works with mouse scroll wheels (or vertical scroll gestures), but not with native pinch-to-zoom gestures.

## Goals / Non-Goals

**Goals:**
* Enable login submission via the `Enter` key when typing credentials.
* Enable fluid pinch-to-zoom gestures on trackpads to scale the mind map canvas.

**Non-Goals:**
* Changing the behavior of `Tab` or `Enter` for node creation or node editing.
* Modifying the layout or database authentication logic of the login screen.

## Decisions

### 1. Default Button for Login view
* **Decision**: Set `defaultButton="true"` on the "Sign In" button in `login-view.fxml`.
* **Rationale**: JavaFX automatically routes the `Enter` key inside any text input fields to fire the default button's action. This requires zero Java controller adjustments.
* **Alternatives Considered**: Add individual `onAction` handlers on `usernameField` and `passwordField`. This would require controller code and manually requesting focus, which is redundant compared to JavaFX's native default button support.

### 2. Viewport ZoomEvent Registration
* **Decision**: Add a `setOnZoom` gesture handler on the `viewport` pane in `MainController.java`.
* **Rationale**: Trackpads trigger native `ZoomEvent` instances in JavaFX. Using `event.getZoomFactor()` allows scaling the canvas pivot-free and adjusting translating coordinates based on the gesture center, matching the existing scroll wheel mathematics.
* **Alternatives Considered**: Relying on touchpad scroll emulation. This is choppy and does not support pinch gestures.

## Risks / Trade-offs

* **[Risk]** ZoomEvent handling could conflict with existing ScrollEvent handling on some touchpads.
  * **Mitigation**: Consume the event immediately in both handlers and ensure scale factors are cleanly bounded between `0.2` and `5.0`.
