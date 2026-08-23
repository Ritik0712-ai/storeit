import { useState, useRef } from 'react'
import { X, Upload, Loader2, Camera } from 'lucide-react'
import { useAuthStore } from '../../context/AuthContext'
import { authService } from '../../services/authService'
import { useToast } from './Toast'

interface ProfileSettingsModalProps {
  onClose: () => void
}

export default function ProfileSettingsModal({ onClose }: ProfileSettingsModalProps) {
  const { user, login } = useAuthStore() // reuse login to update the user object in context
  const { showToast } = useToast()
  
  const [displayName, setDisplayName] = useState(user?.displayName || '')
  const [mobileNumber, setMobileNumber] = useState(user?.mobileNumber || '')
  const [profilePictureUrl, setProfilePictureUrl] = useState(user?.profilePictureUrl || '')
  
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!displayName.trim()) return

    setIsSaving(true)
    try {
      const updatedUser = await authService.updateProfile({
        displayName: displayName.trim(),
        mobileNumber: mobileNumber.trim(),
        profilePictureUrl
      })
      // update the auth store with the new user data
      // we can simulate this by just manually updating the state or if our store supports it
      login(updatedUser, localStorage.getItem('access_token') || '') // Re-using login to set user and token
      showToast('success', 'Profile updated successfully')
      onClose()
    } catch (error) {
      console.error('Failed to update profile:', error)
      showToast('error', 'Failed to update profile')
    } finally {
      setIsSaving(false)
    }
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      showToast('error', 'Please select an image file')
      return
    }

    if (file.size > 5 * 1024 * 1024) { // 5MB limit
      showToast('error', 'Image must be less than 5MB')
      return
    }

    setIsUploading(true)
    try {
      const { profilePictureUrl: newUrl } = await authService.uploadProfilePicture(file)
      setProfilePictureUrl(newUrl)
      
      // Auto-save the profile picture update
      const updatedUser = await authService.updateProfile({ profilePictureUrl: newUrl })
      login(updatedUser, localStorage.getItem('access_token') || '')
      showToast('success', 'Profile picture updated')
    } catch (error) {
      console.error('Failed to upload picture:', error)
      showToast('error', 'Failed to upload profile picture')
    } finally {
      setIsUploading(false)
    }
  }

  const initials = user?.displayName
    ?.split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2) || 'U'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden">
        <div className="flex items-center justify-between p-4 border-b border-border">
          <h2 className="text-lg font-semibold text-text-primary">Profile Settings</h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-bg-subtle rounded-full text-text-secondary transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSave} className="p-4 space-y-6">
          {/* Avatar Upload */}
          <div className="flex flex-col items-center gap-4">
            <div className="relative group">
              <div className="w-24 h-24 rounded-full overflow-hidden bg-primary/10 text-primary flex items-center justify-center text-3xl font-medium border-4 border-white shadow-sm">
                {profilePictureUrl ? (
                  <img src={profilePictureUrl} alt="Profile" className="w-full h-full object-cover" />
                ) : (
                  initials
                )}
              </div>
              
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploading}
                className="absolute inset-0 flex items-center justify-center bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity disabled:opacity-100"
              >
                {isUploading ? (
                  <Loader2 className="w-6 h-6 text-white animate-spin" />
                ) : (
                  <Camera className="w-6 h-6 text-white" />
                )}
              </button>
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                accept="image/*"
                onChange={handleFileChange}
              />
            </div>
            <p className="text-sm text-text-secondary">Click to change picture</p>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-primary mb-1">
                Display Name
              </label>
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                className="input w-full"
                required
                minLength={2}
                maxLength={100}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-primary mb-1">
                Mobile Number
              </label>
              <input
                type="tel"
                value={mobileNumber}
                onChange={(e) => setMobileNumber(e.target.value)}
                placeholder="+1 (555) 000-0000"
                className="input w-full"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-text-primary mb-1">
                Email Address
              </label>
              <input
                type="email"
                value={user?.email || ''}
                className="input w-full bg-bg-subtle text-text-secondary cursor-not-allowed"
                disabled
              />
              <p className="text-xs text-text-secondary mt-1">Email cannot be changed.</p>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-border">
            <button
              type="button"
              onClick={onClose}
              className="btn btn-secondary"
              disabled={isSaving}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSaving || !displayName.trim()}
            >
              {isSaving ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                'Save Changes'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
