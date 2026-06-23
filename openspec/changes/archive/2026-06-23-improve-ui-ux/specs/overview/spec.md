## ADDED Requirements

### Requirement: Simplified Sync Labeling
The overview dashboard cards SHALL display a simplified, localized status for pending synchronization. Specifically, when a mind map's local changes have not yet been synchronized, the sync badge SHALL display the text "Sync pending" in English and "Sync ausstehend" in German.

#### Scenario: Displaying pending sync status
- **GIVEN** a mind map has local changes that are not synced to the cloud
- **WHEN** the overview dashboard is rendered
- **THEN** the map's card displays a status badge with the text "Sync pending" (English) or "Sync ausstehend" (German)
