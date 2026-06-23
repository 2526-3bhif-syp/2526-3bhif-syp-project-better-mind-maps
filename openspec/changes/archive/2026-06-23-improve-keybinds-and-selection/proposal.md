## Why

The current mind map editing experience has a few friction points that slow down users. Currently, adding child/sibling nodes does not follow established mind-mapping conventions (e.g. Tab for child, Enter for sibling). The node selection highlight (a thin indigo border) is invisible when the node itself is blue. Furthermore, deleting nodes currently lacks a confirmation step and does not work properly on Mac keyboards (which use Backspace instead of Forward Delete). Finally, quickly renaming nodes or editing descriptions requires using context menus instead of intuitive double-click or spacebar shortcuts.

## What Changes

- **Keybind Updates for Node Editing:**
  - **Tab**: Add a new child node to the currently selected node.
  - **Enter**: Add a new sibling node to the currently selected node (same parent).
  - **Spacebar**: Open the description editor for the selected node.
  - **Double-Click**: Open the inline title editor for the selected node (instead of description).
  - **Backspace / Delete**: Delete the currently selected node.
- **Confirmation Dialog:** Add a confirmation alert before deleting a node.
- **Selection Styling:** Change the node selection indicator to a 2px violet (`#8b5cf6`) stroke with a hard drop-shadow directly below the node, ensuring visibility regardless of the node's background color.

## Capabilities

### New Capabilities
- (none)

### Modified Capabilities
- `nodes`: Update the requirements for node editing keybinds (Enter, Tab, Space, Delete/Backspace, Double-Click) and adding confirmation for deletion.
- `styling`: Update the requirements for the visual styling of a selected node (violet border + hard shadow).

## Impact

- **Code:** `MainController.java` (`handleKeyPress`, `createNodeView`, etc.) will be modified.
- **UX:** Users accustomed to the old `Enter` (add child) and `Tab` (navigate) behavior will need to adapt to the new, more industry-standard conventions.
- **Dependencies:** None.
