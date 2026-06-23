## ADDED Requirements

### Requirement: Statusbar Shortcut Key Information
The bottom statusbar of the mind map editor SHALL display current shortcut labels and their descriptions matching the key mapping:
- `Tab` ➔ Add child
- `Enter` ➔ Add sibling
- `Space` ➔ Description
- `F2 / Dbl-Click` ➔ Edit
- `Del` ➔ Delete
- `↑ ↓ ← →` ➔ Navigate
- `Scroll` ➔ Zoom
- `Ctrl+Z` ➔ Undo
- `Ctrl+Y` ➔ Redo

#### Scenario: Displaying statusbar shortcuts
- **GIVEN** the mind map workspace is open
- **WHEN** the user looks at the bottom statusbar
- **THEN** the shortcut key labels show "Tab", "Enter", "Space", "F2 / Dbl-Click", "Del", "Scroll", "Ctrl+Z", "Ctrl+Y", and the arrow keys, with corresponding localized description texts.
