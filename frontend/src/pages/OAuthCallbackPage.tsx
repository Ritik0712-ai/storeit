import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../context/AuthContext'
import { authService } from '../services/authService'
import { useToast } from '../components/common/Toast'
import api from '../services/api'
import { setAccessToken } from '../services/api'

export default function OAuthCallbackPage() {
  const navigate = useNavigate()
  const { setUser } = useAuthStore()
  const { showToast } = useToast()

  useEffect(() => {
    const handleCallback = async () => {
      const params = new URLSearchParams(window.location.search)
      const code = params.get('code')
      const error = params.get('error')

      if (error) {
        showToast('error', 'Google sign-in failed, try again')
        navigate('/login')
        return
      }

      if (!code) {
        showToast('error', 'Invalid OAuth callback')
        navigate('/login')
        return
      }

      try {
        // Exchange code for tokens via backend
        const response = await api.post('/auth/oauth2/callback', { code })
        const { accessToken, user } = response.data
        setAccessToken(accessToken)
        setUser(user)
        showToast('success', `Welcome, ${user.displayName}!`)
        navigate('/drive')
      } catch (err) {
        showToast('error', 'Google sign-in failed, try again')
        navigate('/login')
      }
    }

    handleCallback()
  }, [navigate, setUser, showToast])

  return (
    <div className="min-h-screen bg-bg-subtle flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary mx-auto mb-4" />
        <p className="text-text-secondary">Signing you in...</p>
      </div>
    </div>
  )
}
