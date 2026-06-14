## Why

The current specification requires HTL Leonding infrastructure (HTL Leonding IF) for authentication and login. This restricts the application's usability to school-bound systems. Supporting local storage and standard username/password credentials (e.g., using JWT) allows users to create offline/local profiles anywhere and ensures secure segregation of their mind maps on shared machines.

## What Changes

- **BREAKING**: Remove HTL Leonding IF dependency from the account/auth requirement.
- Support local registration and login using standard username and password credentials.
- Add local SQLite storage for user credentials and profiles.
- Verify credentials locally and simulate/model session states with JWT.
- Segregate local mind maps by linking them to the authenticated user's ID.
- **MODIFIED**: Postpone active remote cloud synchronization to a future phase. In the current implementation, all synchronization operations must run purely locally, using a decoupled architecture (e.g., a mock synchronization service) to facilitate seamless future cloud integration.

## Capabilities

### New Capabilities

<!-- None -->

### Modified Capabilities

- `accounts`: Modify user accounts specification to support local storage of user profiles, local username/password verification (with JWT), and adapt cloud synchronization to run purely locally as a future-proof decoupled mock.
- `user-stories`: Update the registration scenario in US-3 to use standard username and password credentials instead of HTL Leonding-specific emails.

## Impact

- Database schema update: Create a `users` table in SQLite with `id`, `username`, and `password_hash` fields. Update the `mind_maps` table to include a `user_id` foreign key.
- UI flow: Implement a Login/Registration screen (`login-view.fxml` & `LoginController.java`) as the application startup view before showing the mind map overview.
- Authentication & Sessions: Implement a light JWT utility or token manager for verifying credentials and representing session state.
- Filtering: Update `OverviewController.java` to fetch and display only the current user's mind maps based on the session token.
- Synchronization Service: Create a decoupled `SyncService` interface that handles sync queueing, implemented as a local simulator for this phase.
