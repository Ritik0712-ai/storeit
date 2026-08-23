import { useState } from 'react'
import { X, Link, Mail, Copy, Check, ExternalLink } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { shareService } from '../../services/shareService'
import { useToast } from '../common/Toast'
import type { ShareRole, ResourceType } from '../../types'

interface ShareModalProps {
  isOpen: boolean
  onClose: () => void
  resourceType: ResourceType
  resourceId: string
  resourceName: string
}

export default function ShareModal({
  isOpen,
  onClose,
  resourceType,
  resourceId,
  resourceName,
}: ShareModalProps) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [activeTab, setActiveTab] = useState<'people' | 'link'>('link')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<ShareRole>('viewer')
  const [linkExpiry, setLinkExpiry] = useState<string>('')
  const [linkPassword, setLinkPassword] = useState('')
  const [createdLink, setCreatedLink] = useState<string | null>(null)

  const createShareMutation = useMutation({
    mutationFn: () => shareService.createShare(resourceType, resourceId, email, role),
    onSuccess: () => {
      showToast('success', 'Shared successfully')
      setEmail('')
      queryClient.invalidateQueries({ queryKey: ['shared-with-me'] })
    },
    onError: () => showToast('error', 'Failed to share'),
  })

  const createLinkMutation = useMutation({
    mutationFn: () => {
      const options: { expiresAt?: string | null; password?: string | null } = {}
      if (linkExpiry) {
        options.expiresAt = new Date(linkExpiry).toISOString()
      }
      if (linkPassword) {
        options.password = linkPassword
      }
      return shareService.createPublicLink(resourceType, resourceId, options)
    },
    onSuccess: (data) => {
      setCreatedLink(data.shareUrl)
      showToast('success', 'Link created')
    },
    onError: () => showToast('error', 'Failed to create link'),
  })

  if (!isOpen) return null

  const copyLink = () => {
    if (createdLink) {
      navigator.clipboard.writeText(createdLink)
      showToast('success', 'Link copied to clipboard')
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg">
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <h2 className="text-lg font-semibold">Share "{resourceName}"</h2>
          <button onClick={onClose} className="btn-ghost p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="px-6 py-4">
          {/* Tabs */}
          <div className="flex gap-4 border-b border-border mb-4">
            <button
              onClick={() => setActiveTab('people')}
              className={`pb-2 text-sm font-medium ${
                activeTab === 'people'
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Mail className="w-4 h-4 inline mr-1" />
              People
            </button>
            <button
              onClick={() => setActiveTab('link')}
              className={`pb-2 text-sm font-medium ${
                activeTab === 'link'
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              <Link className="w-4 h-4 inline mr-1" />
              Get link
            </button>
          </div>

          {activeTab === 'people' ? (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1">
                  Email address
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="input"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1">
                  Permission
                </label>
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value as ShareRole)}
                  className="input"
                >
                  <option value="viewer">Viewer</option>
                  <option value="editor">Editor</option>
                </select>
              </div>
              <button
                onClick={() => createShareMutation.mutate()}
                disabled={!email || createShareMutation.isPending}
                className="btn-primary w-full"
              >
                {createShareMutation.isPending ? 'Sharing...' : 'Share'}
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {createdLink ? (
                <div className="space-y-3">
                  <div className="flex items-center gap-2 p-3 bg-bg-subtle rounded-lg">
                    <Link className="w-5 h-5 text-text-secondary" />
                    <input
                      type="text"
                      value={createdLink}
                      readOnly
                      className="flex-1 bg-transparent text-sm"
                    />
                    <button onClick={copyLink} className="btn-ghost p-1">
                      <Copy className="w-4 h-4" />
                    </button>
                  </div>
                  <button
                    onClick={() => setCreatedLink(null)}
                    className="text-sm text-primary hover:underline"
                  >
                    Create new link
                  </button>
                </div>
              ) : (
                <>
                  <div>
                    <label className="block text-sm font-medium text-text-primary mb-1">
                      Link expiry (optional)
                    </label>
                    <input
                      type="datetime-local"
                      value={linkExpiry}
                      onChange={(e) => setLinkExpiry(e.target.value)}
                      className="input"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-text-primary mb-1">
                      Password protection (optional)
                    </label>
                    <input
                      type="password"
                      value={linkPassword}
                      onChange={(e) => setLinkPassword(e.target.value)}
                      placeholder="Enter password"
                      className="input"
                    />
                  </div>
                  <button
                    onClick={() => createLinkMutation.mutate()}
                    disabled={createLinkMutation.isPending}
                    className="btn-primary w-full"
                  >
                    {createLinkMutation.isPending ? 'Creating...' : 'Create link'}
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
