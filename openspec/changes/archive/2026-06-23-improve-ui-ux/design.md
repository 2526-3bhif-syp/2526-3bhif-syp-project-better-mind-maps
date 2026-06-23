## Context

The current user interface needs some layout corrections and quick editing improvements:
1. Clarifying the sync labels in `LanguageManager.java`.
2. Fixing the statusbar key mappings in `main-view.fxml` and `MainController.java`.
3. Adding a shape grid and wrapping color grid to the HUD panel.

## Goals / Non-Goals

**Goals:**
- Correct statusbar shortcuts to align with the actual behavior (Tab for Add child, Enter for Add sibling, Space for Description, F2 / Dbl-Click for Edit, Del for Delete, Scroll for Zoom).
- Clarify sync status labels to "Sync Pending" (English) and "Sync ausstehend" (German), replacing the ambiguous single word "Ausstehend".
- Implement a wrapping quick-access Shape grid in the HUD panel with buttons representing all 8 standard shapes.
- Restructure the HUD color swatch strip into a wrapping `FlowPane` to prevent layout overflow.
- Enhance the hover styling of the HUD slide-out/collapse tab for better discoverability.

**Non-Goals:**
- Creating new editing keybinds or changing the core keyboard handler logic.
- Adding complex custom shapes beyond the 8 predefined ones.

## Decisions

### Decision 1: Wrap HUD elements in FlowPanes
To ensure the floating HUD menu looks clean and fits neatly within its 260px boundaries, we will wrap both the color swatches and the new shape buttons in a JavaFX `FlowPane` with horizontal/vertical gap spacing. 
- *Alternatives considered:* Keeping them in a single `HBox` was rejected because it causes overflow or forces the HUD to stretch excessively on smaller canvas viewports.

### Decision 2: Reference NodeStyleEditor shapes
Rather than duplicating the shapes list, we will directly reference `NodeStyleEditor.SHAPES` to build the shape button elements in the HUD. When clicked, each button will set the shape of the selected node and trigger a canvas redraw, matching the exact behavior of the full style editor.

### Decision 3: Correct Statusbar FXML structure
We will modify the `<bottom>` section of `main-view.fxml` to define the correct sequence of keys, bind their labels, and register new translation properties.

## Risks / Trade-offs

- **[Risk]** Visual crowding of the statusbar with 9 shortcut pairs.
  - **[Mitigation]** The statusbar is styled with a very clean, low-contrast monospace font (`Consolas`) and compact keycap borders, ensuring all items fit even on standard notebook screens without wrapping issues.
