## Context
The Better Mind Maps application currently uses `Enter` to add a child node and `Tab` for spatial navigation. Deletion occurs immediately on pressing `Delete` (which often doesn't trigger on macOS keyboards where "Delete" is actually `Backspace`). The node selection highlight is a thin indigo stroke that can blend into nodes with blue backgrounds. These defaults are unintuitive for users accustomed to standard mind-mapping tools.

## Goals / Non-Goals
**Goals:**
- Align node creation shortcuts (Enter/Tab) with industry standards.
- Add quick-edit capabilities via Double-Click (rename) and Spacebar (description).
- Ensure node deletion is intentional (confirmation dialog) and works reliably across platforms (listening to both Delete and Backspace).
- Make the node selection state visible on all node colors using a prominent violet border and hard shadow.

**Non-Goals:**
- Full customization of keybinds via user settings.
- Changing the fundamental JavaFX Canvas rendering architecture.

## Decisions
- **Keybind Remapping**: 
  - Update `MainController.handleKeyPress` to map `KeyCode.TAB` to `promptAddChild` and `KeyCode.ENTER` / `KeyCode.INSERT` to a new sibling creation workflow.
  - Map `KeyCode.SPACE` to `showEditDescriptionDialog`.
  - Add listener for `KeyCode.BACK_SPACE` alongside `KeyCode.DELETE` to trigger deletion logic.
- **Mouse Event Remapping**: 
  - Update `createNodeView` to change double-click behavior from `showDescriptionPopup` to `promptEditNode`.
- **Deletion Confirmation**:
  - Introduce an `Alert` dialog (`AlertType.CONFIRMATION`) before calling `service.deleteNode`.
- **Selection Styling**:
  - Update `createNodeView` selection logic. We will apply a 2px stroke in `#8b5cf6` (violet) and use a hard `DropShadow` (e.g., `radius=0`, `offsetY=4`) to guarantee contrast against the background.

## Risks / Trade-offs
- **Risk**: Users might be confused by the sudden change in shortcuts. 
  → **Mitigation**: Standardized behavior is generally more intuitive and requested by the user. 
