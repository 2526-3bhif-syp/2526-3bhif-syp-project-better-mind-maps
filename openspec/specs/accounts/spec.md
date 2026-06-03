## Purpose
Defines user profile creation, local segmentation, and secure cross-device synchronization of mind maps.

## Requirements
### Requirement: User Profile Management
The system SHALL support creating user accounts using verified HTL Leonding credentials and segmenting local data by logged-in user.

#### Scenario: Segregating local mind maps by user profile
- **GIVEN** Julian is registered and logged in on a shared machine
- **WHEN** Julian creates a mind map "Java Advanced" and logs out, and Max logs in
- **THEN** Max's workspace is empty and Max cannot access or view Julian's "Java Advanced" map

### Requirement: Cloud Synchronization
The system SHALL synchronize mind maps automatically to a central cloud server when an active network connection exists.

#### Scenario: Syncing offline updates on restore
- **GIVEN** the student is logged in and made offline changes to the mind map
- **WHEN** the internet connection is restored
- **THEN** the system automatically uploads all local changes from the offline queue to the central server
