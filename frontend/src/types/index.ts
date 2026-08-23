// API Response Types
export interface User {
  id: string
  email: string
  displayName: string
  authProvider: 'local' | 'google'
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  user: User
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
}

export interface Folder {
  id: string
  name: string
  ownerId: string
  parentFolderId: string | null
  deletedAt: string | null
  createdAt: string
  updatedAt: string
  starCount?: number
  isStarred?: boolean
}

export interface FileItem {
  id: string
  name: string
  folderId: string | null
  ownerId: string
  storagePath: string
  mimeType: string
  sizeBytes: number
  uploadStatus: 'pending' | 'complete'
  deletedAt: string | null
  createdAt: string
  updatedAt: string
  isStarred?: boolean
}

export interface Share {
  id: string
  resourceType: 'file' | 'folder'
  resourceId: string
  sharedById: string
  sharedByName?: string
  sharedWithId: string
  sharedWithEmail?: string
  role: 'viewer' | 'editor'
  createdAt: string
}

export interface PublicLink {
  id: string
  token: string
  resourceType: 'file' | 'folder'
  resourceId: string
  createdById: string
  passwordHash: string | null
  expiresAt: string | null
  revoked: boolean
  createdAt: string
}

export interface Star {
  id: string
  userId: string
  resourceType: 'file' | 'folder'
  resourceId: string
  createdAt: string
}

export interface SearchResult {
  type: 'file' | 'folder'
  item: FileItem | Folder
  path: string
}

export interface TrashItem {
  type: 'file' | 'folder'
  item: FileItem | Folder
  originalPath: string
  deletedAt: string
}

export interface InitUploadResponse {
  fileId: string
  uploadUrl: string
  storagePath: string
}

export interface UploadProgress {
  fileId: string
  fileName: string
  progress: number
  status: 'pending' | 'uploading' | 'complete' | 'error'
  error?: string
}

export interface BreadcrumbItem {
  id: string | null
  name: string
}

export type ResourceType = 'file' | 'folder'
export type ShareRole = 'viewer' | 'editor'
