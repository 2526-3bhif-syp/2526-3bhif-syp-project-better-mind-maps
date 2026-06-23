## Why

Users currently face minor usability issues when logging in or navigating and interacting with mind maps:
1. Forms in the Login/Register view cannot be submitted by pressing the Enter key, requiring manual mouse clicks.
2. Zooming on trackpads using pinch-to-zoom gestures (spreading/pinching fingers) is not supported; only vertical two-finger scrolling works.

## What Changes

1. **Login View Submission**:
   - The "Sign In" button is configured as the default button for the Login view, enabling form submission by pressing the Enter key from the username or password fields.
2. **Pinch-to-Zoom Gesture**:
   - Support for `ZoomEvent` is added to the mind map workspace viewport, enabling native pinch-to-zoom gestures on trackpads/touch devices.

## Capabilities

### New Capabilities

*None*

### Modified Capabilities

- `navigation`: Adding support for pinch-to-zoom gesture events.

## Impact

- **Affected Files**:
  - `login-view.fxml` (default button setting).
  - `MainController.java` (zoom gesture event handler).
- **Dependencies**: None.
- **Breaking Changes**: None.
