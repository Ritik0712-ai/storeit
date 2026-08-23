import { useState } from 'react'
import { FolderPlus, Upload } from 'lucide-react'
import { useAppStore } from '../../context/AppContext'
import { useUploadFiles } from '../../hooks/useUpload'

interface NewItemMenuProps {
  onClose: () => void
}

export default function NewItemMenu({ onClose }: NewItemMenuProps) {
  const { currentFolderId } = useAppStore()
  const [showFolderInput, setShowFolderInput] = useState(false)
  const [folderName, setFolderName] = useState('')
  const { createFolder, pickFiles } = useUploadFiles()

  const handleCreateFolder = async () => {
    if (folderName.trim()) {
      await createFolder(folderName.trim(), currentFolderId)
      setFolderName('')
      setShowFolderInput(false)
      onClose()
    }
  }

  const handleFileUpload = async () => {
    await pickFiles()
    onClose()
  }

  return (
    <div className="absolute left-3 right-3 top-full mt-1 bg-white border border-border rounded-lg shadow-lg py-1 z-50">
      {showFolderInput ? (
        <div className="px-3 py-2">
          <input
            type="text"
            placeholder="Folder name"
            value={folderName}
            onChange={(e) => setFolderName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleCreateFolder()
              if (e.key === 'Escape') setShowFolderInput(false)
            }}
            autoFocus
            className="input text-sm"
          />
          <div className="flex gap-2 mt-2">
            <button onClick={handleCreateFolder} className="btn-primary text-sm py-1 px-3">
              Create
            </button>
            <button onClick={() => setShowFolderInput(false)} className="btn-secondary text-sm py-1 px-3">
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <>
          <button
            onClick={handleFileUpload}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm text-text-primary hover:bg-bg-subtle"
          >
            <Upload className="w-4 h-4" />
            Upload files
          </button>
          <button
            onClick={() => setShowFolderInput(true)}
            className="w-full flex items-center gap-3 px-3 py-2 text-sm text-text-primary hover:bg-bg-subtle"
          >
            <FolderPlus className="w-4 h-4" />
            New folder
          </button>
        </>
      )}
    </div>
  )
}
