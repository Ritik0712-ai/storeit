import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Users, Folder, File, Download } from 'lucide-react'
import { shareService } from '../services/shareService'
import type { FileItem, Folder as FolderType } from '../types'

export default function SharedPage() {
  const { data: sharedItems, isLoading } = useQuery({
    queryKey: ['shared-with-me'],
    queryFn: shareService.getSharedWithMe,
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
      <h1 className="text-2xl font-semibold text-text-primary mb-6">Shared with me</h1>

      {!sharedItems?.length ? (
        <div className="flex flex-col items-center justify-center h-96 text-center">
          <Users className="w-16 h-16 text-text-secondary/50 mb-4" />
          <h2 className="text-lg font-medium text-text-primary mb-2">Nothing has been shared with you yet</h2>
          <p className="text-text-secondary">Files and folders shared with you will appear here</p>
        </div>
      ) : (
        <div className="border border-border rounded-lg overflow-hidden">
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border">
              <tr className="text-left text-sm text-text-secondary">
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Shared by</th>
                <th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Type</th>
              </tr>
            </thead>
            <tbody>
              {sharedItems.map((share) => (
                <tr key={share.id} className="border-b border-border last:border-0 hover:bg-bg-subtle">
                  <td className="px-4 py-3">
                    <Link
                      to={share.resourceType === 'folder' ? `/drive/${share.resourceId}` : '#'}
                      className="flex items-center gap-3 hover:text-primary"
                    >
                      {share.resourceType === 'folder' ? (
                        <Folder className="w-5 h-5 text-primary" />
                      ) : (
                        <File className="w-5 h-5 text-text-secondary" />
                      )}
                      <span className="font-medium text-text-primary">Resource {share.resourceId.slice(0, 8)}</span>
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary">{share.sharedByName || 'Unknown'}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`px-2 py-0.5 text-xs rounded-full ${
                        share.role === 'editor'
                          ? 'bg-primary/10 text-primary'
                          : 'bg-bg-subtle text-text-secondary'
                      }`}
                    >
                      {share.role}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-text-secondary capitalize">{share.resourceType}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
