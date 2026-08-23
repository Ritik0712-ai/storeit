import { create } from 'zustand'
import type { BreadcrumbItem, UploadProgress } from '../types'

interface AppState {
  // Current folder navigation
  currentFolderId: string | null
  breadcrumbs: BreadcrumbItem[]
  setCurrentFolder: (folderId: string | null, breadcrumbs: BreadcrumbItem[]) => void

  // View preferences
  viewMode: 'grid' | 'list'
  setViewMode: (mode: 'grid' | 'list') => void

  sortBy: 'name' | 'date' | 'size'
  sortOrder: 'asc' | 'desc'
  setSort: (by: 'name' | 'date' | 'size', order: 'asc' | 'desc') => void

  // Upload tray
  uploads: UploadProgress[]
  addUpload: (upload: UploadProgress) => void
  updateUpload: (fileId: string, update: Partial<UploadProgress>) => void
  removeUpload: (fileId: string) => void
  clearCompleted: () => void

  // Selection
  selectedItems: Set<string>
  toggleSelection: (id: string) => void
  selectAll: (ids: string[]) => void
  clearSelection: () => void
}

export const useAppStore = create<AppState>((set) => ({
  currentFolderId: null,
  breadcrumbs: [{ id: null, name: 'My Drive' }],
  setCurrentFolder: (folderId, breadcrumbs) =>
    set({ currentFolderId: folderId, breadcrumbs }),

  viewMode: 'grid',
  setViewMode: (mode) => set({ viewMode: mode }),

  sortBy: 'name',
  sortOrder: 'asc',
  setSort: (by, order) => set({ sortBy: by, sortOrder: order }),

  uploads: [],
  addUpload: (upload) =>
    set((state) => ({ uploads: [...state.uploads, upload] })),
  updateUpload: (fileId, update) =>
    set((state) => ({
      uploads: state.uploads.map((u) =>
        u.fileId === fileId ? { ...u, ...update } : u
      ),
    })),
  removeUpload: (fileId) =>
    set((state) => ({
      uploads: state.uploads.filter((u) => u.fileId !== fileId),
    })),
  clearCompleted: () =>
    set((state) => ({
      uploads: state.uploads.filter((u) => u.status !== 'complete'),
    })),

  selectedItems: new Set(),
  toggleSelection: (id) =>
    set((state) => {
      const newSelection = new Set(state.selectedItems)
      if (newSelection.has(id)) {
        newSelection.delete(id)
      } else {
        newSelection.add(id)
      }
      return { selectedItems: newSelection }
    }),
  selectAll: (ids) => set({ selectedItems: new Set(ids) }),
  clearSelection: () => set({ selectedItems: new Set() }),
}))
