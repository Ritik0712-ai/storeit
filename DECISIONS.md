# CloudVault - Decisions Log

This document records architectural and implementation decisions made during development.

## Decisions Made During Phase 1 (Project Setup)

### Database Schema
- Using `gen_random_uuid()` as the default for UUID primary keys
- Implemented soft delete with `deleted_at` timestamp on `files` and `folders`
- Cascading deletes for shares when resources are deleted

### JWT Authentication
- Access tokens: 15-minute expiry
- Refresh tokens: 7-day expiry, stored in HttpOnly cookie
- JWT secret must be at least 256 bits (32 bytes)

### Supabase Storage
- Using REST API for signed URLs instead of SDK
- Storage path format: `{userId}/{fileId}/{filename}`
- Files are stored as private, accessible only via signed URLs

## Decisions Made During Phase 2 (Database & Entities)

### Naming Conventions
- Database columns use snake_case
- Java entities use camelCase with @Column annotations
- DTOs use camelCase for JSON serialization

### Entity Relationships
- Folder ↔ Folder (self-referential for parent/child)
- File → Folder (nullable, file can be at root level)
- All entities have @CreationTimestamp and @UpdateTimestamp

## Decisions Made During Phase 3 (Auth Implementation)

### Password Hashing
- Using BCrypt with default strength (10)
- Passwords minimum 8 characters

### OAuth Flow
- Using authorization code flow (not implicit)
- Google OAuth handled via Spring Security OAuth2 Client
- OAuth users don't have password_hash set

### Token Storage
- Access token: returned to client, stored in memory
- Refresh token: stored in database with hash, set as HttpOnly cookie

## Decisions Made During Phase 4 (File/Folder Management)

### Upload Flow
1. Client calls `init-upload` → server creates file record, returns signed upload URL
2. Client uploads directly to Supabase Storage
3. Client calls `complete-upload` → server marks upload as complete

### Folder Deletion
- Deleting a folder sets its `deleted_at`
- All nested files/folders are cascade-deleted (soft delete)
- Implemented via recursive query at delete time

## Decisions Made During Phase 5 (Sharing)

### Permission Resolution
Exact order (as specified in schema):
1. Owner → EDIT
2. Active share (viewer/editor) → VIEW or EDIT
3. Active public link → VIEW only
4. Deny

### Share Revocation
- Revoking a share removes it from the database
- Revoking a public link sets `revoked = true` (preserves for auditing)

## Decisions Made During Phase 6 (Frontend)

### State Management
- Zustand for global state (auth, app preferences, upload queue)
- TanStack Query for server state (caching, refetching)

### File Upload
- Using native XHR for progress tracking
- Direct upload to Supabase Storage (bypasses backend)
- Optimistic UI updates with rollback on error

## Decisions Made During Phase 7 (Search & Trash)

### Search
- Case-insensitive search on file/folder names
- Searches user's own files + files shared with user
- Returns results with full path for display

### Trash
- Items in trash for 30+ days are auto-deleted (future feature)
- Restoring a folder restores all children
- Permanent delete requires confirmation

## Future Improvements (Not in V1)

- File versioning
- File previews (images, PDFs)
- File tagging
- Activity logs
- Desktop sync client
- Mobile apps
- Admin dashboard
