import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Trash2, RotateCcw, AlertTriangle } from 'lucide-react'
import { trashService } from '../services/searchService'
import { useToast } from '../components/common/Toast'

export default function TrashPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [confirmDelete, setConfirmDelete] = useState<{ type: 'file' | 'folder'; id: string; name: string } | null>(null)

  const { data: trashItems, isLoading } = useQuery({
    queryKey: ['trash'],
    queryFn: trashService.getTrash,
  })

  const restoreMutation = useMutation({
    mutationFn: ({ type, id }: { type: 'file' | 'folder'; id: string }) =>
      trashService.restore(type, id),
    onSuccess: () => {
      showToast('success', 'Item restored')
      queryClient.invalidateQueries({ queryKey: ['trash'] })
    },
    onError: () => showToast('error', 'Failed to restore'),
  })

  const permanentDeleteMutation = useMutation({
    mutationFn: ({ type, id }: { type: 'file' | 'folder'; id: string }) =>
      trashService.permanentDelete(type, id),
    onSuccess: () => {
      showToast('success', 'Item permanently deleted')
      queryClient.invalidateQueries({ queryKey: ['trash'] })
      setConfirmDelete(null)
    },
    onError: () => showToast('error', 'Failed to delete'),
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    )
  }

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold text-text-primary mb-6">Trash</h1>

      {/* Warning banner */}
      <div className="bg-warning/10 border border-warning/20 rounded-lg p-4 mb-6 flex items-center gap-3">
        <AlertTriangle className="w-5 h-5 text-warning flex-shrink-0" />
        <p className="text-sm text-text-primary">Items in trash are permanently deleted after 30 days.</p>
      </div>

      {!trashItems?.length ? (
        <div className="flex flex-col items-center justify-center h-96 text-center">
          <Trash2 className="w-16 h-16 text-text-secondary/50 mb-4" />
          <h2 className="text-lg font-medium text-text-primary mb-2">Trash is empty</h2>
          <p className="text-text-secondary">Deleted items will appear here</p>
        </div>
      ) : (
        <div className="border border-border rounded-lg overflow-hidden">
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border">
              <tr className="text-left text-sm text-text-secondary">
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Original location</th>
                <th className="px-4 py-3 font-medium">Deleted</th>
                <th className="px-4 py-3 font-medium">Type</th>
                <th className="px-4 py-3 w-40"></th>
              </tr>
            </thead>
            <tbody>
              {trashItems.map(({ type, item, originalPath, deletedAt }) => (
                <tr key={`${type}-${item.id}`} className="border-b border-border last:border-0 hover:bg-bg-subtle">
                  <td className="px-4 py-3">
                    <span className="font-medium text-text-primary">{item.name}</span>
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary">{originalPath}</td>
                  <td className="px-4 py-3 text-sm text-text-secondary">
                    {new Date(deletedAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary capitalize">{type}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => restoreMutation.mutate({ type, id: item.id })}
                        className="btn-secondary text-sm py-1 px-2 flex items-center gap-1"
                      >
                        <RotateCcw className="w-3 h-3" />
                        Restore
                      </button>
                      <button
                        onClick={() => setConfirmDelete({ type, id: item.id, name: item.name })}
                        className="btn-danger text-sm py-1 px-2"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Confirm delete modal */}
      {confirmDelete && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-lg font-semibold mb-2">Delete forever?</h2>
            <p className="text-text-secondary mb-4">
              "{confirmDelete.name}" will be permanently deleted. This cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmDelete(null)} className="btn-secondary">
                Cancel
              </button>
              <button
                onClick={() =>
                  permanentDeleteMutation.mutate({
                    type: confirmDelete.type,
                    id: confirmDelete.id,
                  })
                }
                className="btn-danger"
              >
                Delete forever
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
