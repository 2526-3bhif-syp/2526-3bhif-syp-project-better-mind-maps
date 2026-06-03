## Purpose
Defines the functional requirements and persistence behaviors for creating, loading, saving, and deleting mind maps.

## Requirements
### Requirement: Mind Map Creation
A mind map MUST be created with a non-empty name and automatically initialize a central root node positioned at coordinates (400, 300) with no parent.

#### Scenario: Creating a new mind map successfully
- **GIVEN** the user is on the overview dashboard
- **WHEN** the user creates a new mind map with the name "Computer Science"
- **THEN** a mind map is created with a root node named "Computer Science" placed at coordinate (400, 300)

#### Scenario: Rejects empty name on creation
- **GIVEN** the user is on the overview dashboard
- **WHEN** the user attempts to create a mind map with an empty or whitespace name
- **THEN** the system throws an IllegalArgumentException and blocks creation

### Requirement: Mind Map Persistence
The application MUST immediately serialize and persist all mind map metadata and associated nodes into the local SQLite database.

#### Scenario: Cascading deletion of a mind map
- **GIVEN** a mind map with multiple nodes is stored in the database
- **WHEN** the user deletes the mind map
- **THEN** the mind map entry and all its associated nodes are cascadingly deleted from the database
