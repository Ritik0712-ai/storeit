import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Star, Share2, Download, Trash2, FolderInput, MoreVertical, Edit2, Folder } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { fileService } from '../../services/fileService'
import { starService } from '../../services/searchService'
import { useToast } from '../common/Toast'
import type { FileItem, Folder as FolderType } from '../../types'

interface ContextMenuProps {
  item: FileItem | FolderType
  type: 'file' | 'folder'
  position: { x: number; y: number }
  onClose: () => void
  onRename: () => void
  onShare: () => void
  onMove: () => void
}

export default function ContextMenu({
  item,
  type,
  position,
  onClose,
  onRename,
  onShare,
  onMove,
}: ContextMenuProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const menuRef = useRef<HTMLDivElement>(null)
  const [isRenaming, setIsRenaming] = useState(false)
  const [newName, setNewName] = useState(item.name)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose()
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [onClose])

  const starMutation = useMutation({
    mutationFn: () => {
      const isStarred = (item as any).isStarred
      if (isStarred) {
        return starService.unstar(type, item.id)
      }
      return starService.star(type, item.id)
    },
    onSuccess: () => {
      showToast('success', (item as any).isStarred ? 'Removed from starred' : 'Added to starred')
      queryClient.invalidateQueries({ queryKey: ['folder'] })
      queryClient.invalidateQueries({ queryKey: ['starred'] })
      onClose()
    },
    onError: () => showToast('error', 'Failed to update star'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => {
      if (type === 'file') {
        return fileService.deleteFile(item.id)
      }
      return fileService.deleteFolder(item.id)
    },
    onSuccess: () => {
      showToast('success', `${type === 'file' ? 'File' : 'Folder'} moved to trash`)
      queryClient.invalidateQueries({ queryKey: ['folder'] })
      queryClient.invalidateQueries({ queryKey: ['trash'] })
      onClose()
    },
    onError: () => showToast('error', 'Failed to delete'),
  })

  const downloadMutation = useMutation({
    mutationFn: async () => {
      if (type === 'file') {
        const { downloadUrl } = await fileService.getDownloadUrl(item.id)
        window.open(downloadUrl, '_blank')
      }
    },
    onError: () => showToast('error', 'Failed to download'),
  })

  const handleRename = async () => {
    if (newName && newName !== item.name) {
      try {
        if (type === 'file') {
          await fileService.updateFile(item.id, { name: newName })
        } else {
          await fileService.updateFolder(item.id, { name: newName })
        }
        showToast('success', 'Renamed successfully')
        queryClient.invalidateQueries({ queryKey: ['folder'] })
      } catch {
        showToast('error', 'Failed to rename')
      }
    }
    onClose()
  }

  return (
    <div
      ref={menuRef}
      className="fixed bg-white border border-border rounded-lg shadow-lg py-1 min-w-[180px] z-50"
      style={{ left: position.x, top: position.y }}
    >
      {isRenaming ? (
        <div className="px-3 py-2">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleRename()
              if (e.key === 'Escape') onClose()
            }}
            className="input text-sm"
            autoFocus
          />
          <div className="flex gap-2 mt-2">
            <button onClick={handleRename} className="btn-primary text-xs py-1 px-2">
              Save
            </button>
            <button onClick={() => setIsRenaming(false)} className="btn-secondary text-xs py-1 px-2">
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <>
          <button
            onClick={() => starMutation.mutate()}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-bg-subtle"
          >
            <Star className="w-4 h-4" />
            {(item as any).isStarred ? 'Remove from starred' : 'Add to starred'}
          </button>
          <button
            onClick={onShare}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-bg-subtle"
          >
            <Share2 className="w-4 h-4" />
            Share
          </button>
          {type === 'file' && (
            <button
              onClick={() => downloadMutation.mutate()}
              className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-bg-subtle"
            >
              <Download className="w-4 h-4" />
              Download
            </button>
          )}
          <button
            onClick={() => setIsRenaming(true)}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-bg-subtle"
          >
            <Edit2 className="w-4 h-4" />
            Rename
          </button>
          <button
            onClick={onMove}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-bg-subtle"
          >
            <FolderInput className="w-4 h-4" />
            Move
          </button>
          <div className="border-t border-border my-1" />
          <button
            onClick={() => deleteMutation.mutate()}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-bg-subtle text-danger"
          >
            <Trash2 className="w-4 h-4" />
            Delete
          </button>
        </>
      )}
    </div>
  )
}
