import api from './api'
import type { Folder, FileItem, SearchResult } from '../types'

export const searchService = {
  async search(params: {
    q?: string
    type?: 'all' | 'file' | 'folder'
    from?: string
    to?: string
  }): Promise<SearchResult[]> {
    const response = await api.get<SearchResult[]>('/search', { params })
    return response.data
  },
}

export const starService = {
  async getStarred(): Promise<Array<{ type: 'file' | 'folder'; item: FileItem | Folder }>> {
    const response = await api.get('/stars')
    return response.data
  },

  async star(resourceType: 'file' | 'folder', resourceId: string): Promise<void> {
    await api.post(`/stars/${resourceType}/${resourceId}`)
  },

  async unstar(resourceType: 'file' | 'folder', resourceId: string): Promise<void> {
    await api.delete(`/stars/${resourceType}/${resourceId}`)
  },
}

export const trashService = {
  async getTrash(): Promise<
    Array<{ type: 'file' | 'folder'; item: FileItem | Folder; originalPath: string; deletedAt: string }>
  > {
    const response = await api.get('/trash')
    return response.data
  },

  async restore(resourceType: 'file' | 'folder', resourceId: string): Promise<void> {
    await api.post(`/trash/${resourceType}/${resourceId}/restore`)
  },

  async permanentDelete(resourceType: 'file' | 'folder', resourceId: string): Promise<void> {
    await api.delete(`/trash/${resourceType}/${resourceId}`)
  },
}
