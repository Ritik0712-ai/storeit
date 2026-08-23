-- CloudVault - Complete Database Setup
-- Run this in Supabase SQL Editor (one-time setup)

-- 1. Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Drop tables if they exist (for clean setup - safe for fresh DB)
DROP TABLE IF EXISTS stars CASCADE;
DROP TABLE IF EXISTS public_links CASCADE;
DROP TABLE IF EXISTS shares CASCADE;
DROP TABLE IF EXISTS files CASCADE;
DROP TABLE IF EXISTS folders CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 3. Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'local',
    profile_picture_url VARCHAR(500),
    mobile_number VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email ON users(email);

-- 4. Create refresh_tokens table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- 5. Create folders table
CREATE TABLE folders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_folders_owner_id ON folders(owner_id);
CREATE INDEX idx_folders_parent_folder_id ON folders(parent_folder_id);
CREATE INDEX idx_folders_deleted_at ON folders(deleted_at) WHERE deleted_at IS NULL;

-- 6. Create files table
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_files_folder_id ON files(folder_id);
CREATE INDEX idx_files_owner_id ON files(owner_id);
CREATE INDEX idx_files_deleted_at ON files(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_files_name_trgm ON files USING GIN (name gin_trgm_ops);

-- 7. Create shares table
CREATE TABLE shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(10) NOT NULL CHECK (resource_type IN ('file', 'folder')),
    resource_id UUID NOT NULL,
    shared_by_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_with_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL CHECK (role IN ('viewer', 'editor')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_share UNIQUE (resource_type, resource_id, shared_with_id)
);
CREATE INDEX idx_shares_shared_with_id ON shares(shared_with_id);
CREATE INDEX idx_shares_resource ON shares(resource_type, resource_id);

-- 8. Create public_links table
CREATE TABLE public_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(64) NOT NULL UNIQUE,
    resource_type VARCHAR(10) NOT NULL CHECK (resource_type IN ('file', 'folder')),
    resource_id UUID NOT NULL,
    created_by_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255),
    expires_at TIMESTAMPTZ,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_public_links_token ON public_links(token);

-- 9. Create stars table
CREATE TABLE stars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_type VARCHAR(10) NOT NULL CHECK (resource_type IN ('file', 'folder')),
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_star UNIQUE (user_id, resource_type, resource_id)
);
CREATE INDEX idx_stars_user_id ON stars(user_id);

-- 10. Create storage bucket for cloudvault-files
INSERT INTO storage.buckets (id, name, public)
VALUES ('cloudvault-files', 'cloudvault-files', false)
ON CONFLICT (id) DO NOTHING;

-- 11. Storage policies (allow authenticated users to upload/download)
DROP POLICY IF EXISTS "Auth users can upload" ON storage.objects;
CREATE POLICY "Auth users can upload" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'cloudvault-files');

DROP POLICY IF EXISTS "Auth users can view files" ON storage.objects;
CREATE POLICY "Auth users can view files" ON storage.objects
FOR SELECT TO authenticated
USING (bucket_id = 'cloudvault-files');

DROP POLICY IF EXISTS "Auth users can update files" ON storage.objects;
CREATE POLICY "Auth users can update files" ON storage.objects
FOR UPDATE TO authenticated
USING (bucket_id = 'cloudvault-files');

DROP POLICY IF EXISTS "Auth users can delete files" ON storage.objects;
CREATE POLICY "Auth users can delete files" ON storage.objects
FOR DELETE TO authenticated
USING (bucket_id = 'cloudvault-files');

-- 12. Create storage bucket for profile pictures (public)
INSERT INTO storage.buckets (id, name, public)
VALUES ('cloudvault-profiles', 'cloudvault-profiles', true)
ON CONFLICT (id) DO NOTHING;

-- 13. Profile pictures policies
DROP POLICY IF EXISTS "Auth users can upload profile picture" ON storage.objects;
CREATE POLICY "Auth users can upload profile picture" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'cloudvault-profiles');

DROP POLICY IF EXISTS "Auth users can update profile picture" ON storage.objects;
CREATE POLICY "Auth users can update profile picture" ON storage.objects
FOR UPDATE TO authenticated
USING (bucket_id = 'cloudvault-profiles');

DROP POLICY IF EXISTS "Anyone can view profile pictures" ON storage.objects;
CREATE POLICY "Anyone can view profile pictures" ON storage.objects
FOR SELECT TO public
USING (bucket_id = 'cloudvault-profiles');

-- Done!
SELECT 'CloudVault schema created successfully!' as status;
