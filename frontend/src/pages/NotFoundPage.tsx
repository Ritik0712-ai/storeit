import { Link } from 'react-router-dom'
import { FileQuestion } from 'lucide-react'

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center h-full text-center p-8">
      <FileQuestion className="w-20 h-20 text-text-secondary/50 mb-4" />
      <h1 className="text-2xl font-semibold text-text-primary mb-2">Page not found</h1>
      <p className="text-text-secondary mb-6">The page you're looking for doesn't exist or you don't have access.</p>
      <Link to="/drive" className="btn-primary">
        Go to My Drive
      </Link>
    </div>
  )
}
