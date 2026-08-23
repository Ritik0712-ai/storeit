import api from './api'
import type { Folder, FileItem, InitUploadResponse } from '../types'

export const fileService = {
  // Folder operations
  async createFolder(name: string, parentFolderId?: string | null): Promise<Folder> {
    const response = await api.post<Folder>('/folders', {
      name,
      parentFolderId: parentFolderId || null,
    })
    return response.data
  },

  async getFolder(id: string): Promise<Folder & { children: (FileItem | Folder)[] }> {
    const response = await api.get(`/folders/${id}`)
    return response.data
  },

  async updateFolder(id: string, data: { name?: string; parentFolderId?: string | null }): Promise<Folder> {
    const response = await api.patch<Folder>(`/folders/${id}`, data)
    return response.data
  },

  async deleteFolder(id: string): Promise<void> {
    await api.delete(`/folders/${id}`)
  },

  // File operations
  async initUpload(
    fileName: string,
    mimeType: string,
    sizeBytes: number,
    folderId?: string | null
  ): Promise<InitUploadResponse> {
    const response = await api.post<InitUploadResponse>('/files/init-upload', {
      fileName,
      mimeType,
      sizeBytes,
      folderId: folderId || null,
    })
    return response.data
  },

  async completeUpload(id: string, checksum?: string): Promise<FileItem> {
    const response = await api.post<FileItem>(`/files/${id}/complete-upload`, { checksum })
    return response.data
  },

  async getFile(id: string): Promise<FileItem> {
    const response = await api.get<FileItem>(`/files/${id}`)
    return response.data
  },

  async getDownloadUrl(id: string): Promise<{ downloadUrl: string }> {
    const response = await api.get<{ downloadUrl: string }>(`/files/${id}/download-url`)
    return response.data
  },

  async updateFile(id: string, data: { name?: string; folderId?: string | null }): Promise<FileItem> {
    const response = await api.patch<FileItem>(`/files/${id}`, data)
    return response.data
  },

  async deleteFile(id: string): Promise<void> {
    await api.delete(`/files/${id}`)
  },
}
