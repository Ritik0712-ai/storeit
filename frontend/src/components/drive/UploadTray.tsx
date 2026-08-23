import { X, CheckCircle, AlertCircle } from 'lucide-react'
import { useAppStore } from '../../context/AppContext'

export default function UploadTray() {
  const { uploads, removeUpload, clearCompleted } = useAppStore()

  if (uploads.length === 0) return null

  return (
    <div className="fixed bottom-4 left-1/2 -translate-x-1/2 bg-white border border-border rounded-lg shadow-lg p-4 w-96 z-40">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-medium text-sm">
          {uploads.filter((u) => u.status === 'complete').length} of {uploads.length} uploads complete
        </h3>
        <div className="flex gap-2">
          <button
            onClick={clearCompleted}
            className="text-xs text-primary hover:underline"
          >
            Clear completed
          </button>
        </div>
      </div>
      <div className="space-y-2 max-h-48 overflow-y-auto">
        {uploads.map((upload) => (
          <div key={upload.fileId} className="flex items-center gap-3">
            {upload.status === 'complete' ? (
              <CheckCircle className="w-4 h-4 text-success flex-shrink-0" />
            ) : upload.status === 'error' ? (
              <AlertCircle className="w-4 h-4 text-danger flex-shrink-0" />
            ) : (
              <div className="w-4 h-4 flex-shrink-0">
                <div className="animate-spin rounded-full h-4 w-4 border-2 border-primary border-t-transparent" />
              </div>
            )}
            <div className="flex-1 min-w-0">
              <p className="text-sm truncate">{upload.fileName}</p>
              {upload.status === 'uploading' && (
                <div className="h-1 bg-border rounded mt-1">
                  <div
                    className="h-full bg-primary rounded transition-all"
                    style={{ width: `${upload.progress}%` }}
                  />
                </div>
              )}
              {upload.status === 'error' && (
                <p className="text-xs text-danger">{upload.error || 'Upload failed'}</p>
              )}
            </div>
            <button
              onClick={() => removeUpload(upload.fileId)}
              className="text-text-secondary hover:text-text-primary"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
