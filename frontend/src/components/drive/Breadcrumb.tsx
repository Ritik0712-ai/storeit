import { Link } from 'react-router-dom'
import { ChevronRight, Home } from 'lucide-react'
import { useAppStore } from '../../context/AppContext'

export default function Breadcrumb() {
  const { breadcrumbs } = useAppStore()

  return (
    <nav className="flex items-center gap-1 text-sm">
      {breadcrumbs.map((crumb, index) => (
        <div key={crumb.id ?? 'root'} className="flex items-center gap-1">
          {index > 0 && <ChevronRight className="w-4 h-4 text-text-secondary" />}
          {index === breadcrumbs.length - 1 ? (
            <span className="text-text-primary font-medium">{crumb.name}</span>
          ) : crumb.id ? (
            <Link
              to={crumb.id ? `/drive/${crumb.id}` : '/drive'}
              className="text-text-secondary hover:text-primary"
            >
              {crumb.name}
            </Link>
          ) : (
            <Link to="/drive" className="text-text-secondary hover:text-primary flex items-center">
              <Home className="w-4 h-4" />
            </Link>
          )}
        </div>
      ))}
    </nav>
  )
}
