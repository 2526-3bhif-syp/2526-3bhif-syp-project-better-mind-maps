## Context

The current application lacks a user registration and login flow, directly opening the mind-map overview on startup. To support secure, local multi-user segmentation on shared machines, we need user registration, credential verification, and session management. Additionally, while the specification outlines cloud synchronization, we must postpone remote connections and build a decoupled local-only simulator to allow future cloud integration without redesigning client presenters.

## Goals / Non-Goals

**Goals:**
- Implement username/password registration and login with local SQLite storage.
- Hash passwords securely using Java's built-in `PBKDF2WithHmacSHA256`.
- Generate and verify JSON Web Tokens (JWT) locally to manage session state.
- Segment mind maps by user by storing and filtering on a `user_id`.
- Architect a decoupled synchronization service that simulates sync operations locally to ensure future cloud sync compatibility.

**Non-Goals:**
- Active network communication with remote servers or real cloud sync backends.
- External cloud identity provider integration or OAuth client flow.

## Decisions

### Decision 1: Password Security
We will use native Java cryptography (`PBKDF2WithHmacSHA256` via `SecretKeyFactory`) to securely hash passwords with a unique salt for each user.
- **Alternatives considered**: Standard BCrypt/SCrypt libraries. Avoided to keep Maven dependency footprint minimal and avoid compilation/runtime issues on target machines.

### Decision 2: Local JWT Implementation
Since GSON is already imported, we will implement a lightweight `JwtUtil` class in native Java. It will encode a header, payload (with claims like `userId`, `username`, and `expiry`), and generate an HMAC-SHA256 signature using a locally generated secret key.
- **Alternatives considered**: Auth0 JWT or JJWT libraries. Avoided to minimize Maven setup complexity.

### Decision 3: Database Schema Modifications
We will add a `users` table to SQLite and update the `mind_maps` table with both `user_id` and a `sync_status` flag (to track whether changes are locally synced):
```sql
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL
);

-- Update/Create mind_maps to associate with user and sync state
CREATE TABLE IF NOT EXISTS mind_maps (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    user_id TEXT,
    sync_status TEXT DEFAULT 'PENDING', -- PENDING, SYNCED
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Decision 4: JavaFX Startup Flow
Modify `App.java` to load `login-view.fxml` first. Once the user is authenticated, we transition to the `overview-view.fxml`, passing the user's JWT token or session context.

### Decision 5: Decoupled Sync Service
We will define an interface `SyncService` containing `syncMap(String mapId)` and `getSyncStatus(String mapId)`. For this phase, we implement it as `LocalSimulatedSyncService`. It updates the SQLite `sync_status` to `SYNCED` after a short mock delay (e.g., in a background thread) to simulate upload to a remote server. This enables a drop-in replacement with a real cloud client in the future.

## Risks / Trade-offs

- **Risk**: Local database is accessible.
  - *Mitigation*: Secure password hashing makes offline dictionary attacks difficult. JWT secret key is generated at startup (or loaded securely) to prevent token tampering.
- **Risk**: Database schema migration for existing databases.
  - *Mitigation*: The `DatabaseManager.initialize()` will check if `user_id` and `sync_status` exist in `mind_maps` and dynamically add them if missing, ensuring backward compatibility.
