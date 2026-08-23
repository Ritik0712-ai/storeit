import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useDropzone } from 'react-dropzone'
import { Grid, List, ChevronRight, Folder, File, Star, MoreVertical, Download, Trash2 } from 'lucide-react'
import { fileService } from '../services/fileService'
import { starService } from '../services/searchService'
import { useAppStore } from '../context/AppContext'
import { useToast } from '../components/common/Toast'
import { useUploadFiles } from '../hooks/useUpload'
import type { Folder as FolderType, FileItem, BreadcrumbItem } from '../types'

export default function DrivePage() {
  const { folderId } = useParams<{ folderId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const { viewMode, setViewMode, setCurrentFolder, uploads } = useAppStore()
  const { uploadFiles } = useUploadFiles()

  // Fetch folder contents
  const { data: folderData, isLoading } = useQuery({
    queryKey: ['folder', folderId || 'root'],
    queryFn: async () => {
      if (folderId) {
        return fileService.getFolder(folderId)
      }
      return fileService.getRootFolder()
    },
  })

  // Update breadcrumbs when folder changes
  useEffect(() => {
    const crumbs: BreadcrumbItem[] = [{ id: null, name: 'My Drive' }]
    if (folderId && folderData) {
      crumbs.push({ id: folderData.id, name: folderData.name })
    }
    setCurrentFolder(folderId || null, crumbs)
  }, [folderId, folderData, setCurrentFolder])

  // Toggle star mutation
  const starMutation = useMutation({
    mutationFn: async ({ type, id, starred }: { type: 'file' | 'folder'; id: string; starred: boolean }) => {
      if (starred) {
        await starService.unstar(type, id)
      } else {
        await starService.star(type, id)
      }
    },
    onSuccess: (_, { starred }) => {
      showToast('success', starred ? 'Removed from starred' : 'Added to starred')
      queryClient.invalidateQueries({ queryKey: ['folder'] })
    },
    onError: () => showToast('error', 'Failed to update star'),
  })

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async ({ type, id }: { type: 'file' | 'folder'; id: string }) => {
      if (type === 'folder') {
        await fileService.deleteFolder(id)
      } else {
        await fileService.deleteFile(id)
      }
    },
    onSuccess: () => {
      showToast('success', 'Moved to trash')
      queryClient.invalidateQueries({ queryKey: ['folder'] })
      queryClient.invalidateQueries({ queryKey: ['storage'] })
    },
    onError: () => showToast('error', 'Failed to delete'),
  })

  // Dropzone for file upload
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles) => {
      if (acceptedFiles.length > 0) {
        uploadFiles(acceptedFiles)
      }
    },
    noClick: true,
  })

  const items = folderData?.children || []
  const folders = items.filter((item): item is FolderType => 'parentFolderId' in item)
  const files = items.filter((item): item is FileItem => 'folderId' !== undefined && 'storagePath' in item)

  const handleItemClick = async (item: FolderType | FileItem) => {
    if ('parentFolderId' in item) {
      navigate(`/drive/${item.id}`)
    } else {
      try {
        const { downloadUrl } = await fileService.getDownloadUrl(item.id)
        window.open(downloadUrl, '_blank')
      } catch (error) {
        showToast('error', 'Failed to get file URL')
      }
    }
  }

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  }

  const getFileIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) return '🖼️'
    if (mimeType.startsWith('video/')) return '🎬'
    if (mimeType.startsWith('audio/')) return '🎵'
    if (mimeType.includes('pdf')) return '📄'
    if (mimeType.includes('word') || mimeType.includes('document')) return '📝'
    if (mimeType.includes('sheet') || mimeType.includes('excel')) return '📊'
    return '📁'
  }

  return (
    <div {...getRootProps()} className={`h-full ${isDragActive ? 'bg-primary/5' : ''}`}>
      <input {...getInputProps()} />

      <div className="p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-semibold text-text-primary">
            {folderData?.name || 'My Drive'}
          </h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded ${viewMode === 'grid' ? 'bg-primary/10 text-primary' : 'text-text-secondary hover:bg-bg-subtle'}`}
            >
              <Grid className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded ${viewMode === 'list' ? 'bg-primary/10 text-primary' : 'text-text-secondary hover:bg-bg-subtle'}`}
            >
              <List className="w-5 h-5" />
            </button>
          </div>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
          </div>
        ) : items.length === 0 ? (
          <EmptyState onUpload={() => {}} />
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-[repeat(auto-fill,minmax(160px,1fr))] gap-4">
            {folders.map((folder) => (
              <FileGridItem
                key={folder.id}
                item={folder}
                type="folder"
                isStarred={folder.isStarred}
                onClick={() => handleItemClick(folder)}
                onStar={() => starMutation.mutate({ type: 'folder', id: folder.id, starred: !!folder.isStarred })}
                onDelete={() => deleteMutation.mutate({ type: 'folder', id: folder.id })}
              />
            ))}
            {files.map((file) => (
              <FileGridItem
                key={file.id}
                item={file}
                type="file"
                isStarred={file.isStarred}
                size={formatSize(file.sizeBytes)}
                onClick={() => handleItemClick(file)}
                onStar={() => starMutation.mutate({ type: 'file', id: file.id, starred: !!file.isStarred })}
                onDelete={() => deleteMutation.mutate({ type: 'file', id: file.id })}
                icon={getFileIcon(file.mimeType)}
              />
            ))}
          </div>
        ) : (
          <div className="border border-border rounded-lg overflow-hidden">
            <table className="w-full">
              <thead className="bg-bg-subtle border-b border-border">
                <tr className="text-left text-sm text-text-secondary">
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Owner</th>
                  <th className="px-4 py-3 font-medium">Modified</th>
                  <th className="px-4 py-3 font-medium">Size</th>
                  <th className="px-4 py-3 w-20"></th>
                </tr>
              </thead>
              <tbody>
                {[...folders, ...files].map((item) => (
                  <tr
                    key={item.id}
                    onClick={() => handleItemClick(item)}
                    className="border-b border-border last:border-0 hover:bg-bg-subtle cursor-pointer"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        {'parentFolderId' in item ? (
                          <Folder className="w-5 h-5 text-primary" />
                        ) : (
                          <span className="text-xl">{getFileIcon((item as FileItem).mimeType)}</span>
                        )}
                        <span className="font-medium text-text-primary">{item.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary">You</td>
                    <td className="px-4 py-3 text-sm text-text-secondary">
                      {formatDate(item.updatedAt)}
                    </td>
                    <td className="px-4 py-3 text-sm text-text-secondary">
                      {'storagePath' in item ? formatSize((item as FileItem).sizeBytes) : '-'}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          const type = 'parentFolderId' in item ? 'folder' : 'file'
                          const id = item.id
                          starMutation.mutate({ type, id, starred: 'isStarred' in item && !!item.isStarred })
                        }}
                        className="p-1 hover:bg-bg-subtle rounded"
                      >
                        <Star className={`w-4 h-4 ${'isStarred' in item && item.isStarred ? 'fill-warning text-warning' : 'text-text-secondary'}`} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Upload tray */}
      {uploads.length > 0 && <UploadTray />}
    </div>
  )
}

function EmptyState({ onUpload }: { onUpload: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center h-96 text-center">
      <Folder className="w-16 h-16 text-text-secondary/50 mb-4" />
      <h2 className="text-lg font-medium text-text-primary mb-2">This folder is empty</h2>
      <p className="text-text-secondary mb-6">Upload your first file or create a folder to get started</p>
      <button onClick={onUpload} className="btn-primary">
        Upload your first file
      </button>
    </div>
  )
}

function FileGridItem({
  item,
  type,
  isStarred,
  size,
  icon,
  onClick,
  onStar,
  onDelete,
}: {
  item: FolderType | FileItem
  type: 'folder' | 'file'
  isStarred?: boolean
  size?: string
  icon?: string
  onClick: () => void
  onStar: () => void
  onDelete: () => void
}) {
  return (
    <div
      onClick={onClick}
      className="card-hover p-4 cursor-pointer group"
    >
      <div className="flex flex-col items-center text-center">
        {type === 'folder' ? (
          <Folder className="w-12 h-12 text-primary mb-2" />
        ) : (
          <span className="text-4xl mb-2">{icon || '📁'}</span>
        )}
        <p className="text-sm font-medium text-text-primary truncate w-full">{item.name}</p>
        {size && <p className="text-xs text-text-secondary mt-1">{size}</p>}
      </div>
      <div className="flex justify-center gap-1 mt-2 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={(e) => {
            e.stopPropagation()
            onStar()
          }}
          className="p-1 hover:bg-bg-subtle rounded"
        >
          <Star className={`w-4 h-4 ${isStarred ? 'fill-warning text-warning' : 'text-text-secondary'}`} />
        </button>
        <button
          onClick={(e) => {
            e.stopPropagation()
            onDelete()
          }}
          className="p-1 hover:bg-bg-subtle rounded"
        >
          <Trash2 className="w-4 h-4 text-text-secondary" />
        </button>
      </div>
    </div>
  )
}

function UploadTray() {
  const { uploads, updateUpload, removeUpload, clearCompleted } = useAppStore()

  return (
    <div className="fixed bottom-4 right-4 w-80 bg-white border border-border rounded-lg shadow-lg overflow-hidden">
      <div className="p-3 border-b border-border flex items-center justify-between bg-bg-subtle">
        <span className="text-sm font-medium">Uploads</span>
        <button onClick={clearCompleted} className="text-xs text-text-secondary hover:text-text-primary">
          Clear completed
        </button>
      </div>
      <div className="max-h-64 overflow-y-auto">
        {uploads.map((upload) => (
          <div key={upload.fileId} className="p-3 border-b border-border last:border-0">
            <div className="flex items-center gap-3">
              <File className="w-4 h-4 text-text-secondary flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-sm truncate">{upload.fileName}</p>
                <div className="mt-1 h-1 bg-border rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${
                      upload.status === 'error'
                        ? 'bg-danger'
                        : upload.status === 'complete'
                        ? 'bg-success'
                        : 'bg-primary'
                    }`}
                    style={{ width: `${upload.progress}%` }}
                  />
                </div>
              </div>
              {upload.status === 'complete' && (
                <button onClick={() => removeUpload(upload.fileId)} className="text-success">
                  ✓
                </button>
              )}
              {upload.status === 'error' && (
                <button
                  onClick={() => {
                    updateUpload(upload.fileId, { status: 'pending', progress: 0 })
                  }}
                  className="text-danger text-xs"
                >
                  Retry
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
