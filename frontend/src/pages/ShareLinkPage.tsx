import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { Cloud, Lock, Download, Folder, File, AlertCircle } from 'lucide-react'
import { shareService } from '../services/shareService'

export default function ShareLinkPage() {
  const { token } = useParams<{ token: string }>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [needsPassword, setNeedsPassword] = useState(false)
  const [password, setPassword] = useState('')
  const [verifying, setVerifying] = useState(false)
  const [linkInfo, setLinkInfo] = useState<{
    resourceType: 'file' | 'folder'
    resourceName: string
    ownerName: string
  } | null>(null)

  useEffect(() => {
    loadLinkInfo()
  }, [token])

  const loadLinkInfo = async () => {
    if (!token) return
    setLoading(true)
    setError(null)

    try {
      const info = await shareService.getPublicLinkInfo(token)

      if (info.isExpired || info.isRevoked) {
        setError('This link has expired or been revoked.')
        return
      }

      setLinkInfo({
        resourceType: info.resourceType,
        resourceName: info.resourceName,
        ownerName: info.ownerName,
      })

      if (info.requiresPassword) {
        setNeedsPassword(true)
      }
    } catch (err: any) {
      if (err.response?.status === 404) {
        setError('This link does not exist.')
      } else {
        setError('Failed to load shared content.')
      }
    } finally {
      setLoading(false)
    }
  }

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!token || !password) return

    setVerifying(true)
    try {
      const result = await shareService.verifyPublicLinkPassword(token, password)
      if (result.valid) {
        setNeedsPassword(false)
      } else {
        setError('Incorrect password')
      }
    } catch {
      setError('Incorrect password')
    } finally {
      setVerifying(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-bg-subtle flex items-center justify-center">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-bg-subtle flex items-center justify-center p-4">
        <div className="text-center max-w-md">
          <AlertCircle className="w-16 h-16 text-danger mx-auto mb-4" />
          <h1 className="text-xl font-semibold text-text-primary mb-2">Unable to access</h1>
          <p className="text-text-secondary">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-bg-subtle">
      {/* Header */}
      <header className="h-14 bg-white border-b border-border flex items-center px-4">
        <Cloud className="w-7 h-7 text-primary mr-2" />
        <span className="font-semibold text-lg text-text-primary">CloudVault</span>
      </header>

      {/* Content */}
      <main className="max-w-2xl mx-auto p-8 mt-8">
        <div className="card p-6">
          {needsPassword ? (
            <div>
              <div className="flex items-center gap-3 mb-6">
                <Lock className="w-8 h-8 text-primary" />
                <div>
                  <h1 className="text-xl font-semibold text-text-primary">This link is password protected</h1>
                  <p className="text-text-secondary text-sm">Enter the password to access "{linkInfo?.resourceName}"</p>
                </div>
              </div>

              <form onSubmit={handlePasswordSubmit}>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  className="input mb-4"
                  autoFocus
                />
                {error && <p className="text-danger text-sm mb-4">{error}</p>}
                <button type="submit" disabled={verifying} className="btn-primary w-full">
                  {verifying ? 'Verifying...' : 'Access'}
                </button>
              </form>
            </div>
          ) : (
            <div>
              <div className="flex items-center gap-4 mb-6">
                {linkInfo?.resourceType === 'folder' ? (
                  <Folder className="w-12 h-12 text-primary" />
                ) : (
                  <File className="w-12 h-12 text-text-secondary" />
                )}
                <div>
                  <h1 className="text-xl font-semibold text-text-primary">{linkInfo?.resourceName}</h1>
                  <p className="text-text-secondary text-sm">
                    Shared by {linkInfo?.ownerName} via CloudVault
                  </p>
                </div>
              </div>

              {linkInfo?.resourceType === 'file' ? (
                <button className="btn-primary w-full flex items-center justify-center gap-2">
                  <Download className="w-5 h-5" />
                  Download
                </button>
              ) : (
                <div className="text-center py-8 text-text-secondary">
                  <Folder className="w-12 h-12 mx-auto mb-2 opacity-50" />
                  <p>Folder preview is not available in this view.</p>
                </div>
              )}
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
