## 1. Database Schema Update

- [x] 1.1 Modify `DatabaseManager.java` to initialize the `users` table with id, username, password_hash, and salt.
- [x] 1.2 Update `DatabaseManager.java` to dynamically check and alter the `mind_maps` table, adding `user_id` and `sync_status` if they don't exist.
- [x] 1.3 Update the `MindMap` class to include `userId` and `syncStatus` fields.
- [x] 1.4 Modify `MindMapRepository` to load, save, and delete maps using the active user's ID and include `sync_status` mapping.

## 2. Authentication & Synchronization Utilities

- [x] 2.1 Implement a password hashing helper (`PasswordHasher.java`) using Java's native PBKDF2 algorithm.
- [x] 2.2 Implement a local JSON Web Token signer and verifier (`JwtUtil.java`) in native Java.
- [x] 2.3 Implement a global `SessionManager.java` to hold the current user session/JWT.
- [x] 2.4 Define a decoupled `SyncService` interface and implement a `LocalSimulatedSyncService` that simulates cloud uploads by updating `sync_status` to `SYNCED` locally.

## 3. UI and Navigation

- [x] 3.1 Create `login-view.fxml` containing registration and login UI elements.
- [x] 3.2 Create `LoginController.java` to handle form submissions, database lookups, and session storage.
- [x] 3.3 Update `App.java` to launch `login-view.fxml` as the first scene.
- [x] 3.4 Update `OverviewController.java` to filter mind maps by the logged-in user ID decoded from the active JWT.
- [x] 3.5 Add a visual sync status indicator in the UI that triggers simulated sync via `SyncService` when a map is saved.

## 4. Validation and Clean up

- [x] 4.1 Run the compiler and verify all tasks complete correctly.
- [x] 4.2 Sync the specifications from this change to the main specifications using the OpenSpec workflow.
