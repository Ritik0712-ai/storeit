import { useEffect } from 'react'
import { useAuthStore } from '../context/AuthContext'
import { authService } from '../services/authService'

export function useAuthInit() {
  const { setUser, setLoading, isLoading } = useAuthStore()

  useEffect(() => {
    const initAuth = async () => {
      try {
        // Try to get current user - if token is valid, this will succeed
        const user = await authService.getMe()
        setUser(user)
      } catch {
        // Token invalid or missing - user is not authenticated
        setUser(null)
      }
    }

    initAuth()
  }, [setUser, setLoading])

  return { isLoading }
}
