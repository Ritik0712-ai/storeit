import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Star, Folder, File } from 'lucide-react'
import { starService } from '../services/searchService'
import { useToast } from '../components/common/Toast'

export default function StarredPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const { data: starredItems, isLoading } = useQuery({
    queryKey: ['starred'],
    queryFn: starService.getStarred,
  })

  const unstarMutation = useMutation({
    mutationFn: ({ type, id }: { type: 'file' | 'folder'; id: string }) =>
      starService.unstar(type, id),
    onSuccess: () => {
      showToast('success', 'Removed from starred')
      queryClient.invalidateQueries({ queryKey: ['starred'] })
    },
    onError: () => showToast('error', 'Failed to unstar'),
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
      <h1 className="text-2xl font-semibold text-text-primary mb-6">Starred</h1>

      {!starredItems?.length ? (
        <div className="flex flex-col items-center justify-center h-96 text-center">
          <Star className="w-16 h-16 text-text-secondary/50 mb-4" />
          <h2 className="text-lg font-medium text-text-primary mb-2">Star files and folders to find them here quickly</h2>
          <p className="text-text-secondary">Click the star icon on any item to add it to your starred list</p>
        </div>
      ) : (
        <div className="border border-border rounded-lg overflow-hidden">
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border">
              <tr className="text-left text-sm text-text-secondary">
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Type</th>
                <th className="px-4 py-3 w-20"></th>
              </tr>
            </thead>
            <tbody>
              {starredItems.map(({ type, item }) => (
                <tr key={`${type}-${item.id}`} className="border-b border-border last:border-0 hover:bg-bg-subtle">
                  <td className="px-4 py-3">
                    <Link
                      to={type === 'folder' ? `/drive/${item.id}` : '#'}
                      className="flex items-center gap-3 hover:text-primary"
                    >
                      {type === 'folder' ? (
                        <Folder className="w-5 h-5 text-primary" />
                      ) : (
                        <File className="w-5 h-5 text-text-secondary" />
                      )}
                      <span className="font-medium text-text-primary">{item.name}</span>
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary capitalize">{type}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => unstarMutation.mutate({ type, id: item.id })}
                      className="p-1 hover:bg-bg-subtle rounded text-warning"
                    >
                      <Star className="w-4 h-4 fill-current" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
