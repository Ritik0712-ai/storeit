import { NavLink } from 'react-router-dom'
import { Cloud, HardDrive, Users, Star, Trash2, Plus } from 'lucide-react'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fileService } from '../../services/fileService'
import NewItemMenu from '../drive/NewItemMenu'

export default function Sidebar() {
  const [showNewMenu, setShowNewMenu] = useState(false)

  const { data: storageData } = useQuery({
    queryKey: ['storage'],
    queryFn: () => fileService.getStorageUsed(),
    refetchInterval: 30000 // Refetch every 30s
  })

  const navItems = [
    { to: '/drive', icon: HardDrive, label: 'My Drive' },
    { to: '/shared', icon: Users, label: 'Shared with me' },
    { to: '/starred', icon: Star, label: 'Starred' },
    { to: '/trash', icon: Trash2, label: 'Trash' },
  ]

  const storageUsed = storageData?.storageUsed || 0
  const maxStorage = 5 * 1024 * 1024 * 1024 // 5 GB
  const storagePercentage = Math.min((storageUsed / maxStorage) * 100, 100)

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
  }

  return (
    <aside className="w-60 bg-bg-subtle border-r border-border flex flex-col">
      {/* Logo */}
      <div className="h-14 flex items-center px-4 border-b border-border">
        <Cloud className="w-7 h-7 text-primary mr-2" />
        <span className="font-semibold text-lg text-text-primary">CloudVault</span>
      </div>

      {/* New button */}
      <div className="px-3 py-3 relative">
        <button
          onClick={() => setShowNewMenu(!showNewMenu)}
          className="w-full btn-primary flex items-center justify-center gap-2"
        >
          <Plus className="w-5 h-5" />
          New
        </button>
        {showNewMenu && <NewItemMenu onClose={() => setShowNewMenu(false)} />}
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-2">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium mb-1 transition-colors ${
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-text-secondary hover:bg-border/50 hover:text-text-primary'
              }`
            }
          >
            <Icon className="w-5 h-5" />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Storage info */}
      <div className="px-4 py-3 border-t border-border">
        <p className="text-xs text-text-secondary">Storage</p>
        <div className="mt-1 h-1.5 bg-border rounded-full overflow-hidden">
          <div className="h-full bg-primary transition-all" style={{ width: `${storagePercentage}%` }} />
        </div>
        <p className="text-xs text-text-secondary mt-1">{formatSize(storageUsed)} of 5 GB used</p>
      </div>
    </aside>
  )
}
