## MODIFIED Requirements

### Requirement: User Profile Management
The system SHALL support creating user accounts locally using standard username and password credentials stored in SQLite, and segmenting local mind map data by the logged-in user authenticated via JWT.

#### Scenario: Segregating local mind maps by user profile
- **GIVEN** Julian is registered and logged in with standard credentials
- **WHEN** Julian creates a mind map "Java Advanced" and logs out, and Max logs in with standard credentials
- **THEN** Max's workspace is empty and Max cannot access or view Julian's "Java Advanced" map

### Requirement: Cloud Synchronization
The system SHALL support a decoupled synchronization architecture to facilitate future integration with a central cloud storage server. In the current implementation, all synchronization operations MUST run purely locally as a simulation.

#### Scenario: Local simulation of cloud sync
- **GIVEN** the student is logged in and made offline changes to the mind map
- **WHEN** the simulated sync is triggered
- **THEN** the system updates the local sync queue and marks the mind map status as synced locally without attempting remote network communication
