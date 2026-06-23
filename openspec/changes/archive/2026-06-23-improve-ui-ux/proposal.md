## Why

The current UI of the application has several minor inconsistencies and friction points:
1. The sync status badge for unsynced changes displays only the ambiguous term `"Ausstehend"` (rendered in orange). Adding the word `"Sync"` (e.g. `"Sync pending"` / `"Sync ausstehend"`) is required to clarify the meaning.
2. The bottom statusbar lists incorrect or outdated keyboard shortcuts (e.g. associating Tab with Cycle instead of Add child, Enter with Add child instead of Add sibling) and misses the new Spacebar shortcut for editing node descriptions.
3. The bottom statusbar does not indicate that double-clicking a node also edits its text, which is an important discoverability helper.
4. The floating Node Style Menu (HUD) only allows changing colors, forcing users to open the full modal dialog just to choose basic shapes. Additionally, the horizontal color preset strip overflows the panel width on smaller screens.

## What Changes

1. **Sync Status Clarification**:
   - Change the sync status labels shown on the mind map cards and status bar. Update the German translation to include the word `"Sync"` so that it reads `"Sync ausstehend"` (instead of the ambiguous `"Ausstehend"`), and English translation to `"Pending Sync"` or `"Sync Pending"`.

2. **Statusbar Shortcuts Correction**:
   - Reorder and update the statusbar labels to accurately reflect the actual keybinds:
     - `Tab` ➔ Add child
     - `Enter` ➔ Add sibling
     - `Space` ➔ Description
     - `F2 / Dbl-Click` ➔ Edit
     - `Del` ➔ Delete
     - `↑ ↓ ← →` ➔ Navigate
     - `Scroll` ➔ Zoom
     - `Ctrl+Z` ➔ Undo
     - `Ctrl+Y` ➔ Redo

3. **Node Styling HUD Extensions**:
   - Reorganize the color strip into a wrapping `FlowPane` (grid) to avoid overflowing the panel.
   - Introduce a new quick Shape selector in the HUD using a wrapping `FlowPane` with buttons for the 8 standard shapes (`ROUNDED_RECT`, `PILL`, `ELLIPSE`, `DIAMOND`, `HEXAGON`, `STAR`, `PARALLELOGRAM`, `OCTAGON`), using their visual symbols.
   - Improve the visual design and hover state of the side-tab toggle button to make the collapse/expand feature more discoverable.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `overview`: Update the requirement for sync status text to be explicit and localized.
- `styling`: Update the requirements for the floating Node Styling HUD to include a quick shape selector grid and a wrapping color grid.

## Impact

- **Affected Files**:
  - `LanguageManager.java`: Translation keys update (`sb.addsibling`, `sb.editdesc`, `hud.shape` and sync labels).
  - `MainController.java`: Bind status bar labels, add HUD shape selector controls, adjust toggle tab styles, replace color HBox with FlowPane.
  - `main-view.fxml`: Correct status bar elements, keys, and `fx:id` references.
