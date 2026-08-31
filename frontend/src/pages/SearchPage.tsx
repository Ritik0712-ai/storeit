import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Search as SearchIcon, Folder, File } from 'lucide-react'
import { searchService } from '../services/searchService'

export default function SearchPage() {
  const [searchParams] = useSearchParams()
  const query = searchParams.get('q') || ''
  const type = searchParams.get('type') || 'all'

  const { data: results, isLoading } = useQuery({
    queryKey: ['search', query, type],
    queryFn: () => searchService.search({ q: query, type: type as 'all' | 'file' | 'folder' }),
    enabled: !!query,
  })

  if (!query) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold text-text-primary mb-6">Search</h1>
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <SearchIcon className="w-16 h-16 text-text-secondary/50 mb-4" />
          <p className="text-text-secondary">Enter a search term to find files and folders</p>
        </div>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    )
  }

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold text-text-primary mb-2">
        Search results for "{query}"
      </h1>
      <p className="text-text-secondary mb-6">{results?.length || 0} results found</p>

      {!results?.length ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <SearchIcon className="w-16 h-16 text-text-secondary/50 mb-4" />
          <h2 className="text-lg font-medium text-text-primary mb-2">No results for "{query}"</h2>
          <p className="text-text-secondary">Try searching with different keywords</p>
        </div>
      ) : (
        <div className="border border-border rounded-lg overflow-hidden">
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border">
              <tr className="text-left text-sm text-text-secondary">
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Path</th>
                <th className="px-4 py-3 font-medium">Type</th>
              </tr>
            </thead>
            <tbody>
              {results.map(({ type, item, path }) => (
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
                  <td className="px-4 py-3 text-sm text-text-secondary">{path}</td>
                  <td className="px-4 py-3 text-sm text-text-secondary capitalize">{type}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
